package com.offex7.streamhub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import java.io.ByteArrayOutputStream
import java.util.Locale

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RadioPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    // Keep the phone's radio queue intact; the car TV player is audio-only.
    private var radioPlayer: ExoPlayer? = null
    private var tvAutoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    private val mediaSessionCallback = object : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val requestedIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
            val requestedItem = mediaItems.getOrNull(requestedIndex)
            val requestedId = requestedItem?.mediaId.orEmpty()
            val isTvRequest = requestedId.startsWith(AUTO_TV_MEDIA_ID_PREFIX)
            val radioUrl = when {
                requestedId.startsWith(AUTO_RADIO_MEDIA_ID_PREFIX) ->
                    requestedItem?.localConfiguration?.uri?.toString()
                        ?: requestedId.removePrefix(AUTO_RADIO_MEDIA_ID_PREFIX)
                RADIO_STATIONS.any { it.url == requestedId } -> requestedId
                else -> null
            }

            if (isTvRequest) {
                val target = getOrCreateTvAutoPlayer()
                if (mediaSession.player !== target) {
                    (mediaSession.player as? ExoPlayer)?.pause()
                    mediaSession.setPlayer(target)
                }
                LogExporter.log("Android Auto routed playback to TV audio player")
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                )
            }

            if (radioUrl != null) {
                val target = radioPlayer
                    ?: return Futures.immediateFailedFuture(IllegalStateException("Radio player is unavailable"))
                if (mediaSession.player !== target) {
                    (mediaSession.player as? ExoPlayer)?.pause()
                    mediaSession.setPlayer(target)
                }
                // Use the original full queue so Android Auto cannot replace the phone's station catalog.
                val selectedIndex = (0 until target.mediaItemCount).firstOrNull { index ->
                    target.getMediaItemAt(index).localConfiguration?.uri?.toString() == radioUrl
                } ?: 0
                val fullRadioQueue = (0 until target.mediaItemCount).map(target::getMediaItemAt)
                LogExporter.log("Android Auto routed playback to radio station index=$selectedIndex")
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(fullRadioQueue, selectedIndex, startPositionMs)
                )
            }

            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            )
        }
    }

    private fun getOrCreateTvAutoPlayer(): ExoPlayer {
        tvAutoPlayer?.let { return it }
        return ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .also { player ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                    .build()
                tvAutoPlayer = player
            }
    }

    private companion object {
        const val AUTO_RADIO_MEDIA_ID_PREFIX = "radio:"
        const val AUTO_TV_MEDIA_ID_PREFIX = "tv:"
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timerMutex = Mutex()
    private fun launchTimerTask(action: suspend () -> Unit) {
        if (destroying) return
        serviceScope.launch {
            runCatching {
                timerMutex.withLock {
                    timerRecovery.await()
                    action()
                }
            }.onFailure { error ->
                LogExporter.log("Radio timer persistence failed: " + (error.message ?: error.javaClass.simpleName))
            }
        }
    }

    private lateinit var settingsStore: SettingsStore
    private lateinit var radioTimerStore: RadioTimerStore
    private val timerRecovery = CompletableDeferred<Unit>()
    @Volatile private var destroying = false
    private val timerCheckpointRunnable = object : Runnable {
        override fun run() {
            val player = radioPlayer ?: return
            if (destroying || player?.isPlaying != true) return
            launchTimerTask { radioTimerStore.checkpoint(System.currentTimeMillis()) }
            mainHandler.postDelayed(this, 5_000L)
        }
    }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var resumeAfterInterruption = false
    private var userPaused = false
    private var userStopped = false
    private var retryCount = 0
    private var audioEffects: RadioAudioEffects? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusHeld = false
    private var ducked = false
    private var focusRestoreVolume = 1f
    private var duckAnimator: ValueAnimator? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        mainHandler.post {
            val player = radioPlayer ?: return@post
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (!ducked) focusRestoreVolume = player.volume.coerceIn(0f, 1f)
                    ducked = true
                    animateFocusVolume(player, focusRestoreVolume * 0.22f, 260L)
                    LogExporter.log("Audio focus interrupted; applying volume duck without pausing playback")
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusHeld = true
                    if (ducked) {
                        ducked = false
                        animateFocusVolume(player, focusRestoreVolume, 420L)
                        LogExporter.log("Audio focus regained; restoring radio volume")
                    }
                }
            }
        }
    }

    private fun animateFocusVolume(player: ExoPlayer, target: Float, durationMs: Long) {
        duckAnimator?.cancel()
        duckAnimator = ValueAnimator.ofFloat(player.volume, target.coerceIn(0f, 1f)).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                if (!destroying && session?.player === player) player.volume = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun requestAppAudioFocus() {
        val manager = audioManager ?: return
        val request = audioFocusRequest ?: return
        if (focusHeld) return
        val result = manager.requestAudioFocus(request)
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (focusHeld && ducked) {
            ducked = false
            (session?.player as? ExoPlayer)?.let { animateFocusVolume(it, focusRestoreVolume, 320L) }
        }
    }

    private fun abandonAppAudioFocus() {
        val manager = audioManager
        val request = audioFocusRequest
        if (manager != null && request != null && focusHeld) runCatching { manager.abandonAudioFocusRequest(request) }
        focusHeld = false
        if (ducked) {
            ducked = false
            (session?.player as? ExoPlayer)?.let { animateFocusVolume(it, focusRestoreVolume, 180L) }
        }
    }

    private fun stationArtworkBytes(stationName: String): ByteArray? {
        val normalizedName = stationName.trim().uppercase(Locale.ROOT)
        val bitmap = if (normalizedName == "РАДИУС FM") {
            BitmapFactory.decodeResource(resources, R.drawable.radius_fm_logo)
        } else {
            val drawableName = localLogoName(stationName)
            val drawableId = if (drawableName != "logo_fallback") {
                resources.getIdentifier(drawableName, "drawable", packageName).takeIf { it != 0 }
            } else null
            drawableId?.let { BitmapFactory.decodeResource(resources, it) }
                ?: RadioLogoAssets.bitmap(stationName)
                ?: RadioLogoAssetsV38.bitmap(applicationContext, stationName)
        } ?: return null
        val artworkBitmap = if (bitmap.width > 256 || bitmap.height > 256) {
            val scale = minOf(256f / bitmap.width, 256f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap
        return runCatching {
            ByteArrayOutputStream().use { output ->
                if (artworkBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) output.toByteArray() else null
            }
        }.getOrNull()
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(applicationContext)
        radioTimerStore = RadioTimerStore(applicationContext)
        serviceScope.launch {
            try {
                runCatching { radioTimerStore.recoverAfterProcessRestart() }
                    .onFailure { error -> LogExporter.log("Radio timer recovery failed: " + (error.message ?: error.javaClass.simpleName)) }
            } finally {
                timerRecovery.complete(Unit)
            }
        }
        audioManager = getSystemService(AudioManager::class.java)
        val platformAudioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(platformAudioAttributes)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(audioFocusListener, mainHandler)
            .build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(6_000, 30_000, 2_000, 5_000)
            .build()
        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            .build()
        radioPlayer = player

        audioEffects = runCatching { RadioAudioEffects(player.audioSessionId) }.getOrNull()
        serviceScope.launch {
            settingsStore.radioEqualizerFlow().collect { audioEffects?.apply(it) }
        }

        player.setMediaItems(RADIO_STATIONS.map { station ->
            MediaItem.Builder()
                .setMediaId(station.url)
                .setUri(Uri.parse(station.url))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist("TV / Radio. Online")
                        .setArtworkUri(Uri.parse("android.resource://$packageName/${R.drawable.ic_app_icon}"))
                        .setArtworkData(stationArtworkBytes(station.name), androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                )
                .build()
        })

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                LogExporter.log("Radio isPlaying=" + isPlaying + ", state=" + player.playbackState)
                if (!destroying) {
                    if (isPlaying) {
                        mainHandler.removeCallbacks(timerCheckpointRunnable)
                        launchTimerTask {
                            val now = System.currentTimeMillis()
                            player.currentMediaItem?.mediaId?.let { radioTimerStore.resumeIfNeeded(now, it) }
                        }
                        mainHandler.postDelayed(timerCheckpointRunnable, 5_000L)
                    } else {
                        mainHandler.removeCallbacks(timerCheckpointRunnable)
                        launchTimerTask { radioTimerStore.pauseIfRunning(System.currentTimeMillis()) }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!destroying && player.isPlaying) {
                    val stationId = mediaItem?.mediaId ?: mediaItem?.localConfiguration?.uri?.toString()
                    if (!stationId.isNullOrBlank()) {
                        launchTimerTask { radioTimerStore.resumeIfNeeded(System.currentTimeMillis(), stationId) }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(isReady: Boolean, reason: Int) {
                if (isReady) requestAppAudioFocus()
                else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) abandonAppAudioFocus()
                when (reason) {
                    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                        userPaused = !isReady
                        if (isReady) userStopped = false
                        resumeAfterInterruption = false
                    }
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                    Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> {
                        if (!isReady && !userStopped) {
                            userPaused = false
                            resumeAfterInterruption = true
                            LogExporter.log("Radio playWhenReady lost due system interruption; auto-resume armed: reason="+reason)
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if ((player.isPlaying || player.playWhenReady) && !userPaused && !userStopped) {
                            resumeAfterInterruption = true
                            LogExporter.log("Radio buffering started; playback kept ready for automatic recovery")
                        }
                    }
                    Player.STATE_READY -> {
                        retryCount = 0
                        if (resumeAfterInterruption && !userPaused && !userStopped) {
                            resumeAfterInterruption = false
                            player.play()
                            LogExporter.log("Radio buffering/interruption recovered; playback resumed")
                        }
                    }
                    Player.STATE_IDLE, Player.STATE_ENDED -> {
                        if (playbackState == Player.STATE_ENDED) {
                            resumeAfterInterruption = false
                        }
                        if (!destroying) {
                            mainHandler.removeCallbacks(timerCheckpointRunnable)
                            launchTimerTask { radioTimerStore.pauseIfRunning(System.currentTimeMillis()) }
                        }
                    }
                }
            }

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                if (
                    playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE &&
                    resumeAfterInterruption &&
                    !userPaused &&
                    !userStopped
                ) {
                    resumeAfterInterruption = false
                    player.play()
                    LogExporter.log("Radio playback suppression cleared; auto-resume")
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!userPaused && !userStopped && retryCount < 1) {
                    retryCount++
                    val index = player.currentMediaItemIndex.coerceAtLeast(0)
                    LogExporter.log("Radio player error; retry #"+retryCount+": "+error.errorCodeName)
                    mainHandler.postDelayed({
                        if (!userPaused && !userStopped && session?.player === player) {
                            player.seekToDefaultPosition(index)
                            player.prepare()
                            player.playWhenReady = true
                        }
                    }, 500L)
                }
            }
        })

        session = MediaSession.Builder(this, player)
            .setCallback(mediaSessionCallback)
            .build()

        val cm = getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (!hasValidatedInternet(cm)) {
                    if (player.isPlaying || player.playWhenReady) {
                        resumeAfterInterruption = true
                    }
                    LogExporter.log("Radio network lost; resumeAfterInterruption="+resumeAfterInterruption)
                }
            }

            override fun onAvailable(network: Network) {
                if (resumeAfterInterruption && hasValidatedInternet(cm) && !userPaused && !userStopped) {
                    resumeAfterInterruption = false
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.playWhenReady = true
                    LogExporter.log("Radio network restored; auto-resume")
                }
            }
        }
        networkCallback = callback
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    private fun hasValidatedInternet(cm: ConnectivityManager): Boolean = cm.allNetworks.any { network ->
        cm.getNetworkCapabilities(network)?.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        destroying = true
        mainHandler.removeCallbacks(timerCheckpointRunnable)
        mainHandler.removeCallbacksAndMessages(null)
        duckAnimator?.cancel()
        duckAnimator = null
        abandonAppAudioFocus()
        networkCallback?.let { runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }
        audioEffects?.release()
        audioEffects = null
        session?.release()
        session = null
        radioPlayer?.release()
        radioPlayer = null
        tvAutoPlayer?.release()
        tvAutoPlayer = null
        serviceScope.launch {
            runCatching {
                timerRecovery.await()
                radioTimerStore.pauseIfRunning(System.currentTimeMillis())
            }
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (session?.player?.isPlaying != true && session?.player?.playWhenReady != true) stopSelf()
        else super.onTaskRemoved(rootIntent)
    }
}
