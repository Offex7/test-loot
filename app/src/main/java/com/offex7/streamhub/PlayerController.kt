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

class PlayerController(private val context: Context) {
    private val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(15_000, 50_000, 1_500, 3_000).build()
    val player: ExoPlayer = ExoPlayer.Builder(context).setLoadControl(loadControl).setAudioAttributes(
        AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true
    ).build()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _waitingForNetwork = MutableStateFlow(false)
    val waitingForNetwork: StateFlow<Boolean> = _waitingForNetwork.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var lastUrl: String? = null
    private var wasPlayingBeforeNetworkLoss = false
    private var fadeAnimator: ValueAnimator? = null
    private var reconnectAttempts = 0
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (!hasValidatedInternet()) {
                wasPlayingBeforeNetworkLoss = player.isPlaying || player.playWhenReady
                if (wasPlayingBeforeNetworkLoss) player.pause()
                _waitingForNetwork.value = true
            }
        }
        override fun onAvailable(network: Network) {
            if (hasValidatedInternet()) {
                _waitingForNetwork.value = false
                if (wasPlayingBeforeNetworkLoss && lastUrl != null) {
                    wasPlayingBeforeNetworkLoss = false
                    play(lastUrl!!)
                }
            }
        }
    }
    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { _isPlaying.value = value }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) { reconnectAttempts = 0; _error.value = null } }
            override fun onPlayerError(error: PlaybackException) {
                if (reconnectAttempts < 2 && lastUrl != null) {
                    reconnectAttempts++
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    _error.value = "Поток недоступен"
                    player.pause()
                }
            }
        })
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }
    private fun hasValidatedInternet(): Boolean = connectivityManager.allNetworks.any { network ->
        connectivityManager.getNetworkCapabilities(network)?.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true
    }
    fun play(url: String) {
        fadeAnimator?.cancel(); player.volume = 1f; reconnectAttempts = 0; _error.value = null; lastUrl = url
        player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.playWhenReady = true
    }
    suspend fun playWithFallback(urls: List<String>): Int {
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.isEmpty()) { _error.value = "Поток недоступен"; return -1 }
        for ((index, url) in candidates.withIndex()) {
            lastUrl = url; reconnectAttempts = 0; _error.value = null
            player.stop(); player.clearMediaItems(); player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.playWhenReady = true
            val until = System.currentTimeMillis() + 5_000L
            while (System.currentTimeMillis() < until) {
                if (player.playbackState == Player.STATE_READY || player.isPlaying) { _error.value = null; return index }
                if (_error.value != null) { _error.value = null; break }
                delay(200L)
            }
            player.stop()
        }
        _error.value = "Поток недоступен"
        player.pause()
        return -1
    }
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun stop() { fadeAnimator?.cancel(); handler.removeCallbacksAndMessages(null); player.stop(); player.clearMediaItems(); lastUrl = null; _error.value = null; _waitingForNetwork.value = false; _isPlaying.value = false }
    fun pause() = player.pause()
    fun fadeOut(durationMs: Long = 10_000L) {
        fadeAnimator?.cancel(); fadeAnimator = ValueAnimator.ofFloat(player.volume.coerceIn(0f,1f),0f).apply {
            duration=durationMs; interpolator=LinearInterpolator(); addUpdateListener { player.volume=it.animatedValue as Float }; start()
        }
    }
    fun release() { fadeAnimator?.cancel(); handler.removeCallbacksAndMessages(null); runCatching { connectivityManager.unregisterNetworkCallback(callback) }; player.release() }
}
