package com.offex7.streamhub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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
            val player = session?.player
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
                true
            )
            .build()

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

        session = MediaSession.Builder(this, player).build()

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
        networkCallback?.let { runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }
        audioEffects?.release()
        audioEffects = null
        session?.run {
            player.release()
            release()
        }
        session = null
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
