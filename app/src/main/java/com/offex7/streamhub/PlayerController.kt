package com.offex7.streamhub

import android.content.Context
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

class PlayerController(context: Context) {
 private val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(2000, 5000, 2000, 2000).build()
 val player: ExoPlayer = ExoPlayer.Builder(context).setLoadControl(loadControl).setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true).build()
 private val _isPlaying = MutableStateFlow(false); val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
 private val _error = MutableStateFlow<String?>(null); val error: StateFlow<String?> = _error.asStateFlow()
 private var reconnectAttempts = 0
 private var lastUrl: String? = null
 init { player.addListener(object: Player.Listener {
  override fun onIsPlayingChanged(v: Boolean) { _isPlaying.value = v }
  override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) { reconnectAttempts = 0; _error.value = null } }
  override fun onPlayerError(e: PlaybackException) { if (lastUrl != null && reconnectAttempts < 3) { reconnectAttempts++; player.seekToDefaultPosition(); player.prepare(); player.playWhenReady = true } else { _error.value = "Канал недоступен"; player.pause() } }
 }) }
 fun play(url: String) { if (lastUrl == url && player.playbackState != Player.STATE_IDLE) { _error.value = null; player.play(); return }; lastUrl = url; reconnectAttempts = 0; _error.value = null; player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.playWhenReady = true }
 fun toggle() { if (player.isPlaying) player.pause() else player.play() }
 fun stop() { player.stop(); lastUrl = null; _error.value = null }
 fun pause() { player.pause() }
 fun release() { player.release() }
}
