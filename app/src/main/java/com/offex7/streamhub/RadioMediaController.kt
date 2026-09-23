package com.offex7.streamhub

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
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
    @Volatile private var pendingPlayIndex: Int? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _weakNetwork = MutableStateFlow(false)
    val weakNetwork: StateFlow<Boolean> = _weakNetwork.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var bufferingGeneration = 0L

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentIndex.value = controller?.currentMediaItemIndex ?: -1
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    val generation = ++bufferingGeneration
                    handler.postDelayed({
                        if (
                            generation == bufferingGeneration &&
                            controller?.playbackState == Player.STATE_BUFFERING
                        ) {
                            _weakNetwork.value = true
                        }
                    }, 4_000L)
                }
                Player.STATE_READY -> {
                    bufferingGeneration++
                    _weakNetwork.value = false
                    _error.value = null
                }
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    bufferingGeneration++
                    _weakNetwork.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            bufferingGeneration++
            _weakNetwork.value = false
            _error.value = "Поток недоступен"
        }
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
                _currentIndex.value = if (controller?.isPlaying == true) {
                    controller?.currentMediaItemIndex ?: -1
                } else {
                    -1
                }
                pendingPlayIndex?.let { index ->
                    pendingPlayIndex = null
                    executePlay(index)
                }
            }.onFailure {
                _connected.value = false
                _error.value = "Не удалось подключиться к плееру"
            }
        }, executor)
    }

    private fun executePlay(index: Int) {
        val target = controller ?: return
        val safeIndex = index.coerceIn(0, RADIO_STATIONS.lastIndex)
        _error.value = null
        _currentIndex.value = safeIndex
        target.seekToDefaultPosition(safeIndex)
        target.prepare()
        target.play()
    }

    fun play(index: Int) {
        val safeIndex = index.coerceIn(0, RADIO_STATIONS.lastIndex)
        if (controller == null) {
            pendingPlayIndex = safeIndex
            return
        }
        executePlay(safeIndex)
    }

    fun pause() {
        pendingPlayIndex = null
        controller?.pause()
    }

    fun toggle() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                _error.value = null
                if (it.playbackState == Player.STATE_IDLE) it.prepare()
                it.play()
            }
        }
    }

    fun stop() {
        pendingPlayIndex = null
        controller?.stop()
        _error.value = null
        _currentIndex.value = -1
        _isPlaying.value = false
    }

    fun next() {
        controller?.let {
            val current = it.currentMediaItemIndex.coerceAtLeast(0)
            play((current + 1) % RADIO_STATIONS.size)
        }
    }

    fun previous() {
        controller?.let {
            val current = it.currentMediaItemIndex.coerceAtLeast(0)
            play((current - 1 + RADIO_STATIONS.size) % RADIO_STATIONS.size)
        }
    }

    fun setVolume(value: Float) {
        controller?.volume = value.coerceIn(0f, 1f)
    }

    fun fadeOut(durationMs: Long, onEnd: (() -> Unit)? = null) {
        val target = controller ?: return
        val start = target.volume.coerceIn(0f, 1f)
        android.animation.ValueAnimator.ofFloat(start, 0f).apply {
            duration = durationMs
            addUpdateListener { target.volume = it.animatedValue as Float }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun release() {
        pendingPlayIndex = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        future = null
        _connected.value = false
        bufferingGeneration++
        _weakNetwork.value = false
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
    }
}
