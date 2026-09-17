package com.offex7.streamhub

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

class RadioMediaController(context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private var controller: MediaController? = null
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { _currentIndex.value = controller?.currentMediaItemIndex ?: -1 }
        override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_READY) _error.value = null }
        override fun onPlayerError(error: PlaybackException) { _error.value = "Поток недоступен" }
    }

    init {
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        future = MediaController.Builder(context, token).buildAsync()
        future?.addListener({
            runCatching {
                controller = future?.get()
                controller?.addListener(listener)
                _connected.value = controller != null
                _isPlaying.value = controller?.isPlaying == true
                _currentIndex.value = controller?.currentMediaItemIndex ?: -1
            }
        }, executor)
    }

    fun play(index: Int) {
        controller?.let {
            _error.value = null
            it.seekToDefaultPosition(index.coerceIn(0, RADIO_STATIONS.lastIndex))
            it.prepare()
            it.play()
        }
    }
    fun pause() { controller?.pause() }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun stop() { controller?.stop(); _error.value = null }
    fun next() { controller?.seekToNextMediaItem(); controller?.play() }
    fun previous() { controller?.seekToPreviousMediaItem(); controller?.play() }
    fun setVolume(value: Float) { controller?.volume = value.coerceIn(0f, 1f) }
    fun fadeOut(durationMs: Long, onEnd: (() -> Unit)? = null) {
        val target = controller ?: return
        val start = target.volume.coerceIn(0f, 1f)
        android.animation.ValueAnimator.ofFloat(start, 0f).apply {
            duration = durationMs
            addUpdateListener { target.volume = it.animatedValue as Float }
            addListener(object : android.animation.AnimatorListenerAdapter() { override fun onAnimationEnd(animation: android.animation.Animator) { onEnd?.invoke() } })
            start()
        }
    }
    fun release() { controller?.removeListener(listener); controller?.release(); future = null; executor.shutdownNow() }
}
