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
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connectivityManager =
        appContext.getSystemService(ConnectivityManager::class.java)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _waitingForNetwork = MutableStateFlow(false)
    val waitingForNetwork: StateFlow<Boolean> = _waitingForNetwork.asStateFlow()

    private val _weakNetwork = MutableStateFlow(false)
    val weakNetwork: StateFlow<Boolean> = _weakNetwork.asStateFlow()

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
    private var internalRetryEnabled = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (!hasValidatedInternet()) {
                val wasPlaying = currentPlayer.isPlaying || currentPlayer.playWhenReady
                if (wasPlaying) currentPlayer.pause()
                _waitingForNetwork.value = true
            }
        }

        override fun onAvailable(network: Network) {
            if (hasValidatedInternet()) {
                _waitingForNetwork.value = false
                if (lastUrl != null && currentPlayer.playbackState == Player.STATE_IDLE) {
                    play(lastUrl!!)
                }
            }
        }
    }

    init {
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
    }

    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 30_000, 2_000, 5_000)
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
                            Player.STATE_BUFFERING -> {
                                bufferingStartedAt = System.currentTimeMillis()
                                val generation = ++bufferingGeneration
                                handler.postDelayed({
                                    if (
                                        !released &&
                                        generation == bufferingGeneration &&
                                        newPlayer === currentPlayer &&
                                        currentPlayer.playbackState == Player.STATE_BUFFERING &&
                                        System.currentTimeMillis() - bufferingStartedAt >= 4_000L
                                    ) {
                                        _weakNetwork.value = true
                                    }
                                }, 4_000L)
                            }
                            Player.STATE_READY -> {
                                bufferingGeneration++
                                bufferingStartedAt = 0L
                                _weakNetwork.value = false
                                reconnectAttempts = 0
                                _error.value = null
                            }
                            Player.STATE_ENDED -> _isPlaying.value = false
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (newPlayer !== currentPlayer) return
                        if (internalRetryEnabled && reconnectAttempts < 2 && lastUrl != null && !released) {
                            reconnectAttempts++
                            val url = lastUrl
                            handler.postDelayed({
                                if (!released && url == lastUrl) play(url)
                            }, 400L)
                        } else {
                            _error.value = "Поток недоступен"
                            _isPlaying.value = false
                            runCatching { currentPlayer.pause() }
                        }
                    }
                })
            }
    }

    private fun replacePlayer() {
        val old = currentPlayer
        runCatching {
            old.stop()
            old.clearMediaItems()
            old.release()
        }

        val fresh = createPlayer()
        currentPlayer = fresh
        _playerInstance.value = fresh
        _isPlaying.value = false
        _error.value = null
        _weakNetwork.value = false
        bufferingGeneration++
    }

    private fun hasValidatedInternet(): Boolean =
        connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)?.run {
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } == true
        }

    fun play(url: String) {
        if (released || url.isBlank()) return
        playbackGeneration++
        internalRetryEnabled = true
        reconnectAttempts = 0
        lastUrl = url
        fadeAnimator?.cancel()
        _error.value = null
        _weakNetwork.value = false
        runCatching {
            replacePlayer()
            currentPlayer.setMediaItem(MediaItem.fromUri(url))
            currentPlayer.prepare()
            currentPlayer.playWhenReady = true
        }.onFailure {
            _error.value = "Поток недоступен"
        }
    }

    suspend fun playWithFallback(urls: List<String>): Int {
        if (released) return -1
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.isEmpty()) {
            _error.value = "Поток недоступен"
            return -1
        }

        val generation = ++playbackGeneration
        internalRetryEnabled = false
        lastUrl = null
        try {
            for ((index, url) in candidates.withIndex()) {
                if (generation != playbackGeneration || released) return -1
                lastUrl = url
                reconnectAttempts = 0
                _error.value = null

                val started = runCatching {
                    replacePlayer()
                    currentPlayer.setMediaItem(MediaItem.fromUri(url))
                    currentPlayer.prepare()
                    currentPlayer.playWhenReady = true
                }.isSuccess
                if (!started) continue

                val until = System.currentTimeMillis() + 15_000L
                while (
                    System.currentTimeMillis() < until &&
                    generation == playbackGeneration &&
                    !released
                ) {
                    if (currentPlayer.playbackState == Player.STATE_READY || currentPlayer.isPlaying) {
                        _error.value = null
                        internalRetryEnabled = true
                        return index
                    }
                    if (_error.value != null) break
                    delay(200L)
                }

                if (generation != playbackGeneration || released) return -1
                runCatching {
                    currentPlayer.stop()
                    currentPlayer.clearMediaItems()
                }
                _error.value = null
            }
        } finally {
            if (generation == playbackGeneration) internalRetryEnabled = true
        }

        if (generation == playbackGeneration) {
            _error.value = "Поток недоступен"
            runCatching { currentPlayer.pause() }
        }
        return -1
    }

    fun toggle() {
        if (released) return
        if (currentPlayer.isPlaying) currentPlayer.pause() else currentPlayer.play()
    }

    fun stop() {
        if (released) return
        playbackGeneration++
        lastUrl = null
        reconnectAttempts = 0
        internalRetryEnabled = true
        fadeAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        runCatching {
            currentPlayer.stop()
            currentPlayer.clearMediaItems()
        }
        _error.value = null
        _weakNetwork.value = false
        _waitingForNetwork.value = false
        _isPlaying.value = false
    }

    fun pause() {
        if (!released) currentPlayer.pause()
    }

    fun fadeOut(durationMs: Long = 10_000L) {
        if (released) return
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(currentPlayer.volume.coerceIn(0f, 1f), 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { currentPlayer.volume = it.animatedValue as Float }
            start()
        }
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
