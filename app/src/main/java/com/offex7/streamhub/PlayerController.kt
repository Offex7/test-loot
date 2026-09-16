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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(private val context: Context) {
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
        .build()
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), true
        ).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _waitingForNetwork = MutableStateFlow(false)
    val waitingForNetwork: StateFlow<Boolean> = _waitingForNetwork.asStateFlow()
    private val _networkWarning = MutableStateFlow<String?>(null)
    val networkWarning: StateFlow<String?> = _networkWarning.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var reconnectAttempts = 0
    private var lastUrl: String? = null
    private var wasPlayingBeforeNetworkLoss = false
    private var fadeAnimator: ValueAnimator? = null

    private val networkWarningTask = Runnable { _networkWarning.value = "Проверьте подключение к интернету" }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (hasValidatedInternet()) {
                handler.removeCallbacks(networkWarningTask)
                _networkWarning.value = null
                _waitingForNetwork.value = false
                if (wasPlayingBeforeNetworkLoss && lastUrl != null) play(lastUrl!!)
                wasPlayingBeforeNetworkLoss = false
            }
        }
        override fun onLost(network: Network) {
            if (!hasValidatedInternet()) {
                wasPlayingBeforeNetworkLoss = player.isPlaying || player.playWhenReady
                if (wasPlayingBeforeNetworkLoss) player.pause()
                _waitingForNetwork.value = true
                handler.removeCallbacks(networkWarningTask)
                handler.postDelayed(networkWarningTask, 90_000L)
            }
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(v: Boolean) { _isPlaying.value = v }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) { reconnectAttempts = 0; _error.value = null }
            }
            override fun onPlayerError(e: PlaybackException) {
                if (lastUrl != null && reconnectAttempts < 3) {
                    reconnectAttempts++
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    _error.value = "Канал недоступен"
                    player.pause()
                }
            }
        })
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    private fun hasValidatedInternet(): Boolean = connectivityManager.allNetworks.any { network ->
        connectivityManager.getNetworkCapabilities(network)?.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true
    }

    fun play(url: String) {
        fadeAnimator?.cancel()
        player.volume = 1f
        if (lastUrl == url && player.playbackState != Player.STATE_IDLE) {
            _error.value = null
            player.play()
            return
        }
        lastUrl = url
        reconnectAttempts = 0
        _error.value = null
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun stop() { fadeAnimator?.cancel(); handler.removeCallbacks(networkWarningTask); player.stop(); lastUrl = null; _error.value = null; _waitingForNetwork.value = false; _networkWarning.value = null; _isPlaying.value = false }
    fun pause() { player.pause() }

    fun fadeOut(durationMs: Long = 10_000L) {
        fadeAnimator?.cancel()
        val start = player.volume.coerceIn(0f, 1f)
        fadeAnimator = ValueAnimator.ofFloat(start, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { player.volume = it.animatedValue as Float }
            start()
        }
    }

    fun release() {
        fadeAnimator?.cancel()
        handler.removeCallbacks(networkWarningTask)
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        player.release()
    }
}
