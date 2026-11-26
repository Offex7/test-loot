package com.offex7.streamhub.auto

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.R as CarAppR
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
 * Android Auto catalog for radio and TV favorites. Playback commands go through
 * the app's single MediaSession, which routes the session to the selected player.
 */
class RadioCarAppService : CarAppService() {
    override fun createHostValidator(): androidx.car.app.validation.HostValidator {
        return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            androidx.car.app.validation.HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            androidx.car.app.validation.HostValidator.Builder(this)
                .addAllowedHosts(CarAppR.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session = RadioCarSession()
}

private class RadioCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = RadioCarScreen(carContext)
}

private enum class CarFavoriteCategory(val title: String, val path: String, val prefix: String) {
    RADIO("Избранное Радио", "radio", "radio:"),
    TV("Избранное ТВ", "tv", "tv:")
}

private data class CarFavorite(
    val mediaId: String,
    val title: String,
    val mediaUrl: String
)

private class RadioCarScreen(
    private val appCarContext: CarContext
) : Screen(appCarContext) {

    private val mainExecutor = ContextCompat.getMainExecutor(appCarContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var favoriteRadioItems: List<CarFavorite> = emptyList()
    private var favoriteTvItems: List<CarFavorite> = emptyList()
    private var favoritesLoaded = false
    private var connecting = true
    private var selectedCategory: CarFavoriteCategory? = null
    private var showingNowPlaying = false
    private var released = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshTemplate()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refreshTemplate()
        override fun onIsPlayingChanged(isPlaying: Boolean) = refreshTemplate()
    }

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) releaseController()
        })
        loadFavorites()
        connectToPhoneMediaSession()
    }

    private fun loadFavorites() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                queryFavorites("radio") to queryFavorites("tv")
            }
            favoriteRadioItems = loaded.first
            favoriteTvItems = loaded.second
            favoritesLoaded = true
            refreshTemplate()
        }
    }

    private fun queryFavorites(kind: String): List<CarFavorite> {
        val uri = Uri.parse("content://${appCarContext.packageName}.car/favorites/$kind")
        val result = mutableListOf<CarFavorite>()
        appCarContext.contentResolver.query(
            uri,
            arrayOf("media_id", "title", "media_url"),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex("media_id")
            val titleColumn = cursor.getColumnIndex("title")
            val urlColumn = cursor.getColumnIndex("media_url")
            if (idColumn >= 0 && titleColumn >= 0 && urlColumn >= 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)?.takeIf(String::isNotBlank) ?: continue
                    val title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: id
                    val url = cursor.getString(urlColumn)?.takeIf(String::isNotBlank) ?: continue
                    result += CarFavorite(id, title, url)
                }
            }
        }
        return result
    }

    private fun connectToPhoneMediaSession() {
        val token = SessionToken(
            appCarContext,
            ComponentName(appCarContext.packageName, "com.offex7.streamhub.RadioPlaybackService")
        )
        val future = MediaController.Builder(appCarContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (!released) {
                try {
                    mediaController = future.get().also { it.addListener(playerListener) }
                } catch (_: Exception) {
                    // Keep the catalog available even if playback service is not reachable.
                }
                connecting = false
                refreshTemplate()
            }
        }, mainExecutor)
    }

    override fun onGetTemplate(): Template {
        if (connecting || !favoritesLoaded) {
            return ListTemplate.Builder()
                .setTitle(selectedCategory?.title ?: "Radio.TV")
                .setLoading(true)
                .build()
        }
        return when {
            showingNowPlaying -> nowPlayingTemplate()
            selectedCategory != null -> categoryTemplate(selectedCategory!!)
            else -> rootTemplate()
        }
    }

    private fun rootTemplate(): Template {
        val list = ItemList.Builder()
        list.addItem(
            Row.Builder()
                .setTitle(CarFavoriteCategory.RADIO.title)
                .addText("${favoriteRadioItems.size} станций")
                .setOnClickListener {
                    selectedCategory = CarFavoriteCategory.RADIO
                    showingNowPlaying = false
                    refreshTemplate()
                }
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle(CarFavoriteCategory.TV.title)
                .addText(if (favoriteTvItems.isEmpty()) "Каналы из активного плейлиста" else "${favoriteTvItems.size} каналов")
                .setOnClickListener {
                    selectedCategory = CarFavoriteCategory.TV
                    showingNowPlaying = false
                    refreshTemplate()
                }
                .build()
        )
        list.addItem(
            Row.Builder()
                .setTitle("Сейчас играет")
                .addText(currentMediaTitle())
                .setOnClickListener {
                    showingNowPlaying = true
                    refreshTemplate()
                }
                .build()
        )
        return ListTemplate.Builder()
            .setTitle("Radio.TV")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list.build())
            .build()
    }

    private fun categoryTemplate(category: CarFavoriteCategory): Template {
        val list = ItemList.Builder()
        val items = favoritesFor(category)
        if (items.isEmpty()) {
            val emptyText = if (category == CarFavoriteCategory.TV) {
                "Откройте ТВ-плейлист на телефоне и добавьте каналы в избранное"
            } else {
                "Добавьте радиостанции в избранное на телефоне"
            }
            list.addItem(Row.Builder().setTitle("Список пуст").addText(emptyText).build())
        } else {
            items.take(40).forEachIndexed { index, item ->
                list.addItem(
                    Row.Builder()
                        .setTitle(item.title)
                        .addText(if (category == CarFavoriteCategory.TV) "ТВ · только звук" else "Радио")
                        .setOnClickListener { playFavorite(category, index) }
                        .build()
                )
            }
        }
        list.addItem(
            Row.Builder()
                .setTitle("Назад к разделам")
                .setOnClickListener {
                    selectedCategory = null
                    showingNowPlaying = false
                    refreshTemplate()
                }
                .build()
        )
        return ListTemplate.Builder()
            .setTitle(category.title)
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
                .addText(currentMediaTitle())
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

    private fun favoritesFor(category: CarFavoriteCategory): List<CarFavorite> =
        if (category == CarFavoriteCategory.TV) favoriteTvItems else favoriteRadioItems

    private fun playFavorite(category: CarFavoriteCategory, index: Int) {
        val player = mediaController ?: return
        val favorites = favoritesFor(category)
        if (index !in favorites.indices) return
        val mediaItems = favorites.map { favorite ->
            MediaItem.Builder()
                .setMediaId(category.prefix + favorite.mediaId)
                .setUri(Uri.parse(favorite.mediaUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(favorite.title)
                        .setArtist(if (category == CarFavoriteCategory.TV) "Radio.TV · ТВ" else "Radio.TV · Радио")
                        .build()
                )
                .build()
        }
        runCatching {
            player.setMediaItems(mediaItems, index, C.TIME_UNSET)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
            selectedCategory = category
            showingNowPlaying = true
        }
        refreshTemplate()
    }

    private fun currentMediaTitle(): String =
        mediaController?.currentMediaItem?.mediaMetadata?.title?.toString()
            ?.takeIf(String::isNotBlank) ?: "Ничего не воспроизводится"

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
        runCatching {
            player.seekToDefaultPosition(next)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            if (!player.playWhenReady) player.play()
        }
        refreshTemplate()
    }

    private fun refreshTemplate() {
        if (!released) runCatching { invalidate() }
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
