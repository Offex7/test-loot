package com.offex7.streamhub

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wasPlaying = false

    override fun onCreate() {
        super.onCreate()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 30_000, 2_000, 5_000)
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
        player.setMediaItems(RADIO_STATIONS.map { station ->
            MediaItem.Builder()
                .setMediaId(station.url)
                .setUri(Uri.parse(station.url))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist("TV / Radio. Online")
                        .setArtworkUri(Uri.parse("android.resource://$packageName/${R.drawable.ic_app_icon}"))
                        .build()
                )
                .build()
        })
        session = MediaSession.Builder(this, player).build()

        val cm = getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (!hasValidatedInternet(cm)) {
                    wasPlaying = player.isPlaying || player.playWhenReady
                    if (wasPlaying) player.pause()
                }
            }
            override fun onAvailable(network: Network) {
                if (wasPlaying && hasValidatedInternet(cm)) {
                    wasPlaying = false
                    player.prepare()
                    player.play()
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
        networkCallback?.let { runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } }
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (session?.player?.isPlaying != true) stopSelf()
        else super.onTaskRemoved(rootIntent)
    }
}
