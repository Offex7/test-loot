package com.offex7.streamhub

import android.animation.ValueAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(context: Context) {
    private companion object {
        const val BUFFER_TARGET_MS = 6_000L
        const val BUFFER_MAX_MS = 30_000
        const val BUFFER_TIMEOUT_MS = 10_000L
        const val WEAK_NETWORK_NOTICE_MS = 3_000L
    }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connectivityManager =
        appContext.getSystemService(ConnectivityManager::class.java)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeChannelId = MutableStateFlow<String?>(null)
    val activeChannelId: StateFlow<String?> = _activeChannelId.asStateFlow()

    fun setActiveChannelId(channelId: String?) {
        _activeChannelId.value = channelId?.trim()?.takeIf { it.isNotBlank() }
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _waitingForNetwork = MutableStateFlow(false)
    val waitingForNetwork: StateFlow<Boolean> = _waitingForNetwork.asStateFlow()

    private val _weakNetwork = MutableStateFlow(false)
    val weakNetwork: StateFlow<Boolean> = _weakNetwork.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _bufferPercent = MutableStateFlow(0)
    val bufferPercent: StateFlow<Int> = _bufferPercent.asStateFlow()

    private val _weakNetworkEvent = MutableStateFlow(0L)
    val weakNetworkEvent: StateFlow<Long> = _weakNetworkEvent.asStateFlow()

    private val _weakNetworkNoticeCount = MutableStateFlow(0)
    val weakNetworkNoticeCount: StateFlow<Int> = _weakNetworkNoticeCount.asStateFlow()

    private val _adaptiveBufferLevel = MutableStateFlow(0)
    val adaptiveBufferLevel: StateFlow<Int> = _adaptiveBufferLevel.asStateFlow()

    private val initialPlayer = createPlayer()
    private var currentPlayer: ExoPlayer = initialPlayer
    private val _playerInstance = MutableStateFlow(initialPlayer)
    val playerInstance: StateFlow<ExoPlayer> = _playerInstance.asStateFlow()

    val player: ExoPlayer
        get() = currentPlayer

    private var lastUrl: String? = null
    private var reconnectAttempts = 0
    private var playbackGeneration = 0L
    private var bufferingGeneration = 0L
    private var bufferingStartedAt = 0L
    private var released = false
    private var fadeAnimator: ValueAnimator? = null
    private var fadeRestoreVolume = 1f
    private var internalRetryEnabled = true
    private var suppressBufferRecovery = false
    private var resumeAfterInterruption = false
    private var userPaused = false
    private var userStopped = false
    private var internalPlayWhenReadyChange = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (!hasValidatedInternet()) {
                val active = currentPlayer.isPlaying || currentPlayer.playWhenReady
                if (active) {
                    resumeAfterInterruption = true
                }
                _waitingForNetwork.value = true
                LogExporter.log("TV network lost; resumeAfterInterruption=" + resumeAfterInterruption)
            }
        }

        override fun onAvailable(network: Network) {
            if (hasValidatedInternet()) {
                _waitingForNetwork.value = false
                _weakNetwork.value = hasLowBandwidthNetwork()
                if (resumeAfterInterruption && lastUrl != null && !userPaused && !userStopped) {
                    resumeAfterInterruption = false
                    runCatching {
                        if (currentPlayer.playbackState == Player.STATE_IDLE) {
                            currentPlayer.prepare()
                        }
                        setPlayWhenReadySystem(currentPlayer, true)
                    }.onFailure {
                        _error.value = "Поток недоступен"
                    }
                }
            }
        }
    }

    init {
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
    }

    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(6_000, BUFFER_MAX_MS, 2_000, 5_000)
            .setBackBuffer(0, false)
            .build()

        return ExoPlayer.Builder(appContext)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .also { newPlayer ->
                newPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(value: Boolean) {
                        if (newPlayer === currentPlayer) _isPlaying.value = value
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (newPlayer !== currentPlayer) return
                        when (state) {
                            Player.STATE_BUFFERING -> beginBuffering(newPlayer)
                            Player.STATE_READY -> completeBuffering(newPlayer)
                            Player.STATE_ENDED -> {
                                stopBuffering()
                                _isPlaying.value = false
                            }
                            Player.STATE_IDLE -> stopBuffering()
                        }
                    }

                    override fun onPlayWhenReadyChanged(isReady: Boolean, reason: Int) {
                        if (newPlayer !== currentPlayer) return
                        if (internalPlayWhenReadyChange) {
                            internalPlayWhenReadyChange = false
                            return
                        }
                        when (reason) {
                            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                                userPaused = !isReady
                                if (isReady) userStopped = false
                                resumeAfterInterruption = false
                            }
                            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                            Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> {
                                if (!isReady && lastUrl != null && !userStopped) {
                                    resumeAfterInterruption = true
                                    userPaused = false
                                    LogExporter.log("TV playWhenReady lost due system interruption; auto-resume armed: reason=$reason")
                                }
                            }
                        }
                    }

                    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                        if (newPlayer !== currentPlayer) return
                        if (
                            playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE &&
                            resumeAfterInterruption &&
                            !userPaused &&
                            !userStopped &&
                            lastUrl != null
                        ) {
                            LogExporter.log("TV playback suppression cleared; auto-resume")
                            runCatching { newPlayer.play() }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (newPlayer !== currentPlayer) return
                        stopBuffering()
                        if (
                            internalRetryEnabled &&
                            reconnectAttempts < 1 &&
                            lastUrl != null &&
                            !released &&
                            !userStopped
                        ) {
                            reconnectAttempts++
                            val url = lastUrl
                            LogExporter.log("TV player error; retry #" + reconnectAttempts + ": " + error.errorCodeName)
                            if (url != null) {
                                handler.postDelayed({
                                    if (!released && url == lastUrl && !userStopped) {
                                        runCatching {
                                            currentPlayer.setMediaItem(MediaItem.fromUri(url))
                                            currentPlayer.prepare()
                                            currentPlayer.playWhenReady = !userPaused
                                        }.onFailure {
                                            _error.value = "Поток недоступен"
                                        }
                                    }
                                }, 500L)
                            }
                        } else {
                            resumeAfterInterruption = false
                            _error.value = "Поток недоступен"
                            _isPlaying.value = false
                            runCatching { currentPlayer.playWhenReady = false }
                        }
                    }
                })
            }
    }

    private fun setPlayWhenReadySystem(player: ExoPlayer, ready: Boolean) {
        internalPlayWhenReadyChange = true
        player.playWhenReady = ready
    }

    private fun beginBuffering(player: ExoPlayer) {
        bufferingGeneration++
        val generation = bufferingGeneration
        bufferingStartedAt = System.currentTimeMillis()
        val wasActive = player.isPlaying || player.playWhenReady
        if (wasActive && !userPaused && !userStopped) {
            resumeAfterInterruption = true
            LogExporter.log("TV buffering started; playback kept ready for automatic recovery")
        }
        _buffering.value = true
        _bufferPercent.value = 0
        _adaptiveBufferLevel.value = 3
        handler.postDelayed({
            if (
                !released &&
                generation == bufferingGeneration &&
                player === currentPlayer &&
                player.playbackState == Player.STATE_BUFFERING &&
                System.currentTimeMillis() - bufferingStartedAt >= WEAK_NETWORK_NOTICE_MS &&
                !suppressBufferRecovery
            ) {
                if (_weakNetworkNoticeCount.value < 1) {
                    _weakNetworkNoticeCount.value = 1
                    _weakNetworkEvent.value += 1L
                }
                _weakNetwork.value = true
                lowerVideoQuality()
            }
        }, WEAK_NETWORK_NOTICE_MS)
        handler.postDelayed({
            if (
                !released &&
                generation == bufferingGeneration &&
                player === currentPlayer &&
                player.playbackState == Player.STATE_BUFFERING &&
                System.currentTimeMillis() - bufferingStartedAt >= BUFFER_TIMEOUT_MS
            ) {
                resumeAfterInterruption = false
                _error.value = "Буферизация не завершилась за 10 секунд. Переключите канал."
                runCatching { setPlayWhenReadySystem(player, false) }
                LogExporter.log("TV buffering timeout after 10s")
            }
        }, BUFFER_TIMEOUT_MS)
        updateBufferProgress(player, generation)
    }

    private fun updateBufferProgress(player: ExoPlayer, generation: Long) {
        handler.post(object : Runnable {
            override fun run() {
                if (
                    released ||
                    generation != bufferingGeneration ||
                    player !== currentPlayer ||
                    !_buffering.value ||
                    player.playbackState != Player.STATE_BUFFERING
                ) return
                val bufferedMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)
                val percent = ((bufferedMs.toDouble() / BUFFER_TARGET_MS) * 100.0)
                    .toInt()
                    .coerceIn(0, 99)
                _bufferPercent.value = percent
                handler.postDelayed(this, 200L)
            }
        })
    }

    private fun completeBuffering(player: ExoPlayer) {
        val shouldResume =
            resumeAfterInterruption && !userPaused && !userStopped && lastUrl != null
        stopBuffering()
        if (shouldResume) {
            resumeAfterInterruption = false
            runCatching { player.play() }
            LogExporter.log("TV buffering completed; playback resumed")
        }
    }

    private fun stopBuffering() {
        bufferingGeneration++
        bufferingStartedAt = 0L
        _buffering.value = false
        _bufferPercent.value = 100
        _adaptiveBufferLevel.value = 0
    }

    private fun lowerVideoQuality() {
        runCatching {
            currentPlayer.trackSelectionParameters =
                currentPlayer.trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(854, 480)
                    .build()
        }
    }

    private fun hasValidatedInternet(): Boolean =
        connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)?.run {
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } == true
        }

    private fun hasLowBandwidthNetwork(): Boolean =
        connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { capabilities ->
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                    capabilities.linkDownstreamBandwidthKbps in 1..1200
            } == true
        }

    fun resetWeakNetworkSession() {
        bufferingGeneration++
        bufferingStartedAt = 0L
        _weakNetwork.value = false
        _weakNetworkEvent.value = 0L
        _weakNetworkNoticeCount.value = 0
        _adaptiveBufferLevel.value = 0
        if (!_buffering.value) {
            _bufferPercent.value = 0
        }
    }

    fun play(url: String) {
        if (released || url.isBlank()) return
        playbackGeneration++
        internalRetryEnabled = true
        reconnectAttempts = 0
        lastUrl = url
        userPaused = false
        userStopped = false
        resumeAfterInterruption = false
        cancelFadeOut(restore = false)
        _error.value = null
        _weakNetwork.value = false
        _waitingForNetwork.value = false
        stopBuffering()
        runCatching {
            currentPlayer.setMediaItem(MediaItem.fromUri(url))
            currentPlayer.prepare()
            currentPlayer.playWhenReady = true
        }.onFailure {
            _error.value = "Поток недоступен"
        }
    }

    suspend fun playWithFallback(urls: List<String>, attemptTimeoutMs: Long = BUFFER_TIMEOUT_MS, suppressRecovery: Boolean = false): Int {
        if (released) return -1
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.isEmpty()) {
            suppressBufferRecovery = false
            _error.value = "Поток недоступен"
            return -1
        }

        val generation = ++playbackGeneration
        internalRetryEnabled = false
        suppressBufferRecovery = suppressRecovery
        userPaused = false
        userStopped = false
        resumeAfterInterruption = false
        reconnectAttempts = 0
        cancelFadeOut(restore = false)

        try {
            for ((index, url) in candidates.withIndex()) {
                if (generation != playbackGeneration || released) return -1
                lastUrl = url
                _error.value = null
                _waitingForNetwork.value = false
                stopBuffering()
                val started = runCatching {
                    currentPlayer.setMediaItem(MediaItem.fromUri(url))
                    currentPlayer.prepare()
                    currentPlayer.playWhenReady = true
                }.isSuccess
                if (!started) continue

                val until = System.currentTimeMillis() + attemptTimeoutMs.coerceAtLeast(300L)
                while (
                    System.currentTimeMillis() < until &&
                    generation == playbackGeneration &&
                    !released
                ) {
                    if (currentPlayer.playbackState == Player.STATE_READY && currentPlayer.isPlaying) {
                        _error.value = null
                        internalRetryEnabled = true
                        return index
                    }
                    if (_error.value != null) break
                    delay(100L)
                }
                if (generation != playbackGeneration || released) return -1
                runCatching {
                    currentPlayer.playWhenReady = false
                    currentPlayer.stop()
                    currentPlayer.clearMediaItems()
                }
                _error.value = null
            }
        } finally {
            if (generation == playbackGeneration) {
                internalRetryEnabled = true
                suppressBufferRecovery = false
            }
        }

        if (generation == playbackGeneration) {
            _error.value = "Поток недоступен"
            runCatching { currentPlayer.playWhenReady = false }
        }
        return -1
    }

    fun toggle() {
        if (released) return
        if (currentPlayer.isPlaying || currentPlayer.playWhenReady) {
            userPaused = true
            resumeAfterInterruption = false
            currentPlayer.pause()
        } else {
            userPaused = false
            userStopped = false
            resumeAfterInterruption = false
            _error.value = null
            if (currentPlayer.playbackState == Player.STATE_IDLE) currentPlayer.prepare()
            currentPlayer.play()
        }
    }

    fun stop() {
        if (released) return
        playbackGeneration++
        lastUrl = null
        _activeChannelId.value = null
        suppressBufferRecovery = false
        reconnectAttempts = 0
        internalRetryEnabled = true
        userPaused = false
        userStopped = true
        resumeAfterInterruption = false
        cancelFadeOut(restore = false)
        handler.removeCallbacksAndMessages(null)
        runCatching {
            currentPlayer.playWhenReady = false
            currentPlayer.stop()
            currentPlayer.clearMediaItems()
        }
        _error.value = null
        _weakNetwork.value = false
        _buffering.value = false
        _bufferPercent.value = 0
        _adaptiveBufferLevel.value = 0
        _waitingForNetwork.value = false
        _isPlaying.value = false
    }

    fun pause() {
        if (!released) {
            userPaused = true
            resumeAfterInterruption = false
            currentPlayer.pause()
        }
    }

    fun fadeOut(durationMs: Long = 10_000L) {
        if (released) return
        fadeAnimator?.cancel()
        fadeRestoreVolume = currentPlayer.volume.coerceIn(0f, 1f)
        fadeAnimator = ValueAnimator.ofFloat(fadeRestoreVolume, 0f).apply {
            duration = durationMs.coerceAtLeast(1L)
            interpolator = LinearInterpolator()
            addUpdateListener { currentPlayer.volume = it.animatedValue as Float }
            start()
        }
    }

    fun cancelFadeOut(restore: Boolean = true) {
        fadeAnimator?.cancel()
        fadeAnimator = null
        if (restore && !released) currentPlayer.volume = fadeRestoreVolume.coerceIn(0f, 1f)
    }

    fun release() {
        if (released) return
        released = true
        playbackGeneration++
        fadeAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching {
            currentPlayer.stop()
            currentPlayer.clearMediaItems()
            currentPlayer.release()
        }
    }
}
