package com.offex7.streamhub.auto

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.HostValidator
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.R as CarAppR
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Auto entry point. It deliberately uses a regular ListTemplate and the existing
 * Media3 MediaSessionController; the phone's player implementation is not migrated.
 */
class RadioCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(CarAppR.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session = RadioCarSession()
}

private class RadioCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = RadioCarScreen(carContext)
}

private class RadioCarScreen(
    private val appCarContext: CarContext
) : Screen(appCarContext) {

    private val mainExecutor = ContextCompat.getMainExecutor(appCarContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var favoriteRadioIds: Set<String> = emptySet()
    private var favoritesLoaded = false
    private var connecting = true
    private var showingNowPlaying = false
    private var released = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshTemplate()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshTemplate()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refreshTemplate()
        }
    }

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                releaseController()
            }
        })
        loadFavoriteRadioIds()
        connectToPhoneMediaSession()
    }

    private fun loadFavoriteRadioIds() {
        scope.launch {
            favoriteRadioIds = withContext(Dispatchers.IO) {
                val result = linkedSetOf<String>()
                val uri = Uri.parse("content://${appCarContext.packageName}.car/favorites/radio")
                appCarContext.contentResolver.query(
                    uri,
                    arrayOf("media_id"),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val column = cursor.getColumnIndex("media_id")
                    if (column >= 0) {
                        while (cursor.moveToNext()) {
                            cursor.getString(column)?.takeIf(String::isNotBlank)?.let(result::add)
                        }
                    }
                }
                result
            }
            favoritesLoaded = true
            refreshTemplate()
        }
    }

    private fun connectToPhoneMediaSession() {
        val token = SessionToken(
            appCarContext,
            ComponentName(
                appCarContext.packageName,
                "com.offex7.streamhub.RadioPlaybackService"
            )
        )
        val future = MediaController.Builder(appCarContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (!released) {
                try {
                    mediaController = future.get().also { it.addListener(playerListener) }
                } catch (_: Exception) {
                    // Keep the car UI available and show the disconnected state.
                }
                connecting = false
                refreshTemplate()
            }
        }, mainExecutor)
    }

    private fun favoriteRows(): List<Pair<Int, String>> {
        val player = mediaController ?: return emptyList()
        return (0 until player.mediaItemCount).mapNotNull { index ->
            val item = player.getMediaItemAt(index)
            if (item.mediaId !in favoriteRadioIds) {
                null
            } else {
                val title = item.mediaMetadata.title?.toString()
                    ?.takeIf(String::isNotBlank) ?: item.mediaId
                index to title
            }
        }
    }

    private fun currentStationTitle(): String {
        return mediaController?.currentMediaItem?.mediaMetadata?.title
            ?.toString()?.takeIf(String::isNotBlank)
            ?: "Радиостанция не выбрана"
    }

    override fun onGetTemplate(): Template {
        if (connecting || !favoritesLoaded) {
            return ListTemplate.Builder()
                .setTitle(if (showingNowPlaying) "Сейчас играет" else "Избранное")
                .setLoading(true)
                .build()
        }
        return if (showingNowPlaying) nowPlayingTemplate() else favoritesTemplate()
    }

    private fun favoritesTemplate(): Template {
        val list = ItemList.Builder()
        val rows = favoriteRows()
        if (rows.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle("Избранное радио пусто")
                    .addText("Добавьте радиостанции в избранное на телефоне")
                    .build()
            )
        } else {
            rows.take(20).forEach { (index, title) ->
                list.addItem(
                    Row.Builder()
                        .setTitle(title)
                        .addText("Радио")
                        .setOnClickListener {
                            playIndex(index)
                            showingNowPlaying = true
                            refreshTemplate()
                        }
                        .build()
                )
            }
        }
        list.addItem(
            Row.Builder()
                .setTitle("Сейчас играет")
                .addText(currentStationTitle())
                .setOnClickListener {
                    showingNowPlaying = true
                    refreshTemplate()
                }
                .build()
        )
        return ListTemplate.Builder()
            .setTitle("Избранное")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list.build())
            .build()
    }

    private fun nowPlayingTemplate(): Template {
        val player = mediaController
        val list = ItemList.Builder()
        list.addItem(
            Row.Builder()
                .setTitle("Сейчас играет")
                .addText(currentStationTitle())
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle("◀ Предыдущая")
                .setOnClickListener { skipBy(-1) }
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle(if (player?.isPlaying == true || player?.playWhenReady == true) "Пауза" else "Воспроизвести")
                .setOnClickListener { togglePlayback() }
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle("Следующая ▶")
                .setOnClickListener { skipBy(1) }
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle("К избранному")
                .setOnClickListener {
                    showingNowPlaying = false
                    refreshTemplate()
                }
                .build()
        )
        return ListTemplate.Builder()
            .setTitle("Сейчас играет")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list.build())
            .build()
    }

    private fun playIndex(index: Int) {
        val player = mediaController ?: return
        if (index !in 0 until player.mediaItemCount) return
        runCatching {
            player.seekToDefaultPosition(index)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
        refreshTemplate()
    }

    private fun togglePlayback() {
        val player = mediaController ?: return
        runCatching {
            if (player.isPlaying || player.playWhenReady) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
        refreshTemplate()
    }

    private fun skipBy(delta: Int) {
        val player = mediaController ?: return
        val count = player.mediaItemCount
        if (count <= 0) return
        val current = player.currentMediaItemIndex.takeIf { it in 0 until count } ?: 0
        val next = (current + delta + count) % count
        playIndex(next)
    }

    private fun refreshTemplate() {
        if (released) return
        runCatching { invalidate() }
    }

    private fun releaseController() {
        if (released) return
        released = true
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        scope.cancel()
    }
}
