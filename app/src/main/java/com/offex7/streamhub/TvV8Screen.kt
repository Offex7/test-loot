@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.offex7.streamhub

import android.content.res.Configuration
import android.view.KeyEvent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale
import kotlin.math.abs
import java.util.concurrent.ConcurrentHashMap

private val TvV9Red = Color(0xFFE53935)
private val TvV9Panel = Color(0xFF1A1A1A)
private val TvV9PanelAlt = Color(0xFF232323)
private val TvV9Gray = Color(0xFF808080)
private val TvV9Bg = Color(0xFF121212)

private enum class TvNavigationMode { FAVORITES_THEN_GENERAL, GENERAL }

private val TvV9SleepOptions = listOf(
    5L to "5 мин", 10L to "10 мин", 15L to "15 мин", 30L to "30 мин",
    60L to "1 ч", 120L to "2 ч", 240L to "4 ч", 480L to "8 ч",
    600L to "10 ч", 900L to "15 ч", 1440L to "24 ч", 2160L to "36 ч"
)

@Composable
fun TvV8Screen(
    player: PlayerController,
    store: SettingsStore,
    restore: StreamItem?,
    dismissRestore: () -> Unit,
    saveLast: (StreamItem) -> Unit,
    back: () -> Unit,
    settings: () -> Unit,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    notify: (String, Long) -> Unit,
    onViewingChanged: (Boolean) -> Unit,
    pipMode: Boolean,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    radioPlaying: Boolean
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repo = androidx.compose.runtime.remember(context, store) { TvPlaylistRepositoryV8(context, store) }
    val sourceKey by store.activeSourceFlow().collectAsStateWithLifecycle(initialValue = builtinSourceKey(0))
    val list = rememberLazyListState()
    val network by rememberTvV9Network()
    val hiddenChannels by store.hiddenChannelsFlow().collectAsStateWithLifecycle(initialValue = emptySet())
    val error by player.error.collectAsStateWithLifecycle()
    val waiting by player.waitingForNetwork.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val playerInstance by player.playerInstance.collectAsStateWithLifecycle()
    val focusRequester = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current
    val logoCache = androidx.compose.runtime.remember(context) { TvLogoCache(context) }
    var savedZooms by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, Float>()) }
    var sourceReady by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var sourcePickerVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var userSources by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<UserPlaylist>()) }
    val configuration = LocalConfiguration.current
    val showHideIcon =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION ||
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isSwitching by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    var channels by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<StreamItem>()) }
    var favorites by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
    var favoriteTimes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, Long>()) }
    var health by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var loading by androidx.compose.runtime.remember(sourceKey) { androidx.compose.runtime.mutableStateOf(true) }
    var loadError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var refreshing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedIndex by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(-1) }
    var fullscreen by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var searchOpen by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var query by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var searchStamp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    var searchHistory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<String>()) }
    var notice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var playbackJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    var navigationSession by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<Int>()) }
    var navigationMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(TvNavigationMode.GENERAL) }
    var zoomSaveJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    var showScrollAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        loadError = null
        runCatching { repo.cached(sourceKey) }.getOrNull()?.let {
            channels = it.items
            loading = false
        }
        repo.load(sourceKey)
            .onSuccess {
                channels = it.items
                loading = false
                loadError = null
                health = emptyMap()
                sourcePickerVisible = false
            }
            .onFailure {
                loading = false
                loadError = "Источник по умолчанию недоступен. Выберите другой источник"
                sourcePickerVisible = true
            }
    }

    LaunchedEffect(Unit) {
        store.ensureInitialTvSource()
        userSources = store.userPlaylists()
        sourceReady = true
    }

    LaunchedEffect(sourceKey, sourceReady) {
        if (!sourceReady) return@LaunchedEffect
        favorites = store.favorites(Section.TV)
        favoriteTimes = store.favoriteAddedAt(Section.TV)
        savedZooms = store.channelZooms()
        reload()
    }

    LaunchedEffect(Unit) {
        val saved = store.scrollPosition(Section.TV)
        runCatching { list.scrollToItem(saved.first.coerceAtLeast(0), saved.second) }
        androidx.compose.runtime.snapshotFlow {
            list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        }.collect { current ->
            store.saveScrollPosition(Section.TV, current.first, current.second)
        }
    }

    LaunchedEffect(list.isScrollInProgress) {
        if (!list.isScrollInProgress) {
            showScrollAction = false
            return@LaunchedEffect
        }
        val started = System.currentTimeMillis()
        while (list.isScrollInProgress && System.currentTimeMillis() - started < 4_000L) {
            delay(100L)
        }
        if (
            list.isScrollInProgress &&
            (list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0)
        ) {
            showScrollAction = true
        }
        while (list.isScrollInProgress) {
            delay(100L)
        }
        showScrollAction = false
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            searchHistory = store.searchHistory(Section.TV)
            delay(80L)
            runCatching { focusRequester.requestFocus() }
            delay(40L)
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(searchOpen, query, searchStamp) {
        if (!searchOpen) return@LaunchedEffect
        val stamp = searchStamp
        delay(if (query.isBlank()) 5000L else 15000L)
        if (stamp == searchStamp) {
            searchOpen = false
            query = ""
            keyboardController?.hide()
        }
    }

    LaunchedEffect(channels, favorites) {
        if (channels.isEmpty()) return@LaunchedEffect
        val favoriteUrls = favorites.mapNotNull { key ->
            channels.firstOrNull { it.key == key }?.logoUrl
        }
        logoCache.prefetch(
            priorityUrls = favoriteUrls,
            secondaryUrls = channels.mapNotNull { it.logoUrl }
        )
    }

    LaunchedEffect(fullscreen) {
        onViewingChanged(fullscreen)
    }

    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.TV,
            channelIdProvider = { channels.getOrNull(selectedIndex)?.name },
            activeProvider = { fullscreen && selectedIndex in channels.indices && player.isPlaying.value }
        ).run()
    }

    LaunchedEffect(fullscreen) {
        val activity = context as? androidx.activity.ComponentActivity
        if (fullscreen) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? androidx.activity.ComponentActivity
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose { onViewingChanged(false) }
    }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            health = scanTvV9(channels.take(18), repo)
        }
    }

    LaunchedEffect(channels) {
        if (channels.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(30 * 60 * 1000L)
            val offlineItems = channels.filter { health[it.url] == AvailabilityStatus.OFFLINE }
            if (offlineItems.isNotEmpty()) {
                health = health + scanTvV9(offlineItems, repo)
            }
        }
    }

    LaunchedEffect(list.firstVisibleItemIndex, channels, health) {
        if (channels.isEmpty()) return@LaunchedEffect
        val first = list.firstVisibleItemIndex.coerceIn(0, channels.lastIndex)
        val end = minOf(channels.size, first + 20)
        val pending = channels.subList(first, end).filter { health[it.url] == null }
        if (pending.isNotEmpty()) health = health + scanTvV9(pending, repo)
    }

    fun normalizedChannelTitle(name: String): String = name.lowercase(Locale.ROOT)
        .replace(Regex("\\b(hd|uhd|fhd|4k|sd)\\b"), " ")
        .replace(Regex("[+_\\-()]\\s*\\d+$"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun similarChannelNames(a: String, b: String): Boolean {
        val na = normalizedChannelTitle(a)
        val nb = normalizedChannelTitle(b)
        if (na.isBlank() || nb.isBlank()) return false
        return na == nb || na.startsWith(nb + " ") || nb.startsWith(na + " ")
    }

    fun isNavigable(index: Int): Boolean {
        if (index !in channels.indices) return false
        val item = channels[index]
        return !hiddenChannels.contains(item.key) &&
            health[item.url] != AvailabilityStatus.OFFLINE
    }

    fun buildNavigationSession(start: Int, fromFavorites: Boolean): List<Int> {
        val general = channels.indices.filter(::isNavigable)
        if (!fromFavorites) return general
        val favoritesOnly = general
            .filter { favorites.contains(channels[it].key) }
            .sortedWith(
                compareByDescending<Int> { favoriteTimes[channels[it].key] ?: 0L }
                    .thenBy { channels[it].name.lowercase(Locale.ROOT) }
            )
        if (favoritesOnly.isEmpty()) return general
        val pivot = favoritesOnly.indexOf(start).let { if (it >= 0) it else 0 }
        val rotatedFavorites = favoritesOnly.drop(pivot) + favoritesOnly.take(pivot)
        val remainder = general.filterNot { favoritesOnly.contains(it) }
        return rotatedFavorites + remainder
    }

    fun adjacentIndex(start: Int, delta: Int): Int {
        val order = navigationSession
        if (order.size <= 1 || start !in channels.indices) return -1
        val pos = order.indexOf(start)
        if (pos < 0) return -1
        return order[(pos + delta + order.size) % order.size]
    }

    fun startPlayback(start: Int, fromFavorites: Boolean? = null) {
        if (start !in channels.indices || hiddenChannels.contains(channels[start].key)) return
        if (fromFavorites != null || navigationSession.isEmpty() || start !in navigationSession) {
            navigationMode =
                if (fromFavorites == true) {
                    TvNavigationMode.FAVORITES_THEN_GENERAL
                } else {
                    TvNavigationMode.GENERAL
                }
            navigationSession = buildNavigationSession(
                start,
                navigationMode == TvNavigationMode.FAVORITES_THEN_GENERAL
            )
        }
        playbackJob?.cancel()
        selectedIndex = start
        fullscreen = true
        notice = null
        isSwitching = true
        playbackJob = scope.launch {
            var cursor = start
            var attempts = 0
            try {
                while (attempts < channels.size.coerceAtLeast(1)) {
                    attempts++
                    if (!isNavigable(cursor)) {
                        cursor = adjacentIndex(cursor, 1)
                        if (cursor < 0) break
                        continue
                    }

                    val candidate = channels[cursor]
                    val result = player.playWithFallback(listOf(candidate.url))
                    if (result >= 0) {
                        selectedIndex = cursor
                        health = health + (candidate.url to AvailabilityStatus.ONLINE)
                        saveLast(candidate)
                        val neighbors = listOf(
                            adjacentIndex(cursor, -1),
                            adjacentIndex(cursor, 1)
                        ).filter { it >= 0 }.distinct()
                        if (neighbors.isNotEmpty()) {
                            scope.launch {
                                val pending = neighbors
                                    .mapNotNull { channels.getOrNull(it) }
                                    .filter { health[it.url] == null }
                                if (pending.isNotEmpty()) {
                                    health = health + scanTvV9(pending, repo)
                                }
                            }
                        }
                        return@launch
                    }

                    health = health + (candidate.url to AvailabilityStatus.OFFLINE)
                    cursor = adjacentIndex(cursor, 1)
                    if (cursor < 0) break
                }

                isSwitching = false
                fullscreen = false
                notify("Не удалось найти рабочий канал", 2500L)
            } finally {
                isSwitching = false
            }
        }
    }
    fun closePlayer() {
        playbackJob?.cancel()
        player.stop()
        notice = null
        fullscreen = false
    }

    BackHandler(enabled = fullscreen) { closePlayer() }

    if (fullscreen && selectedIndex in channels.indices) {
        val favoriteChannels = channels.mapIndexedNotNull { index, channel ->
            if (favorites.contains(channel.key) && !hiddenChannels.contains(channel.key)) {
                Triple(index, channel, health[channel.url] ?: AvailabilityStatus.UNKNOWN)
            } else null
        }.sortedWith(
            compareByDescending<Triple<Int, StreamItem, AvailabilityStatus>> { favoriteTimes[it.second.key] ?: 0L }
                .thenBy { it.second.name.lowercase(Locale.ROOT) }
        )
        TvV9Player(
            player = player,
            playerInstance = playerInstance,
            channel = channels[selectedIndex],
            favoriteChannels = favoriteChannels,
            error = if (notice == null && !isSwitching) error else null,
            waiting = isSwitching || waiting || !network,
            isPlaying = if (isSwitching) true else isPlaying,
            noticeMessage = notice,
            sleepRemaining = sleepRemaining,
            sleepUntil = sleepUntil,
            sleepMinutes = sleepMinutes,
            onBack = ::closePlayer,
            onPrev = {
                val i = adjacentIndex(selectedIndex, -1)
                if (i >= 0) startPlayback(i)
            },
            onNext = {
                val i = adjacentIndex(selectedIndex, 1)
                if (i >= 0) startPlayback(i)
            },
            onPause = { player.toggle() },
            onFavoriteSelected = { index ->
                if (index in channels.indices) startPlayback(index, true)
            },
            onEnterPip = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (context as? androidx.activity.ComponentActivity)?.enterPictureInPictureMode(
                        android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(16, 9))
                            .build()
                    )
                }
            },
            onSleep = onSleep,
            onCancelSleep = onCancelSleep,
            initialZoom = savedZooms[channels[selectedIndex].key],
            onZoomChanged = { value ->
                val key = channels[selectedIndex].key
                savedZooms = savedZooms + (key to value)
                zoomSaveJob?.cancel()
                zoomSaveJob = scope.launch {
                    delay(250L)
                    store.setChannelZoom(key, value)
                }
            },
            onZoomReset = {
                val key = channels[selectedIndex].key
                scope.launch {
                    store.removeChannelZoom(key)
                    savedZooms = savedZooms - key
                }
            },
            pipMode = pipMode,
            onWeakNetworkNotice = { message -> notify(message, 5000L) },
            switching = isSwitching,
            hapticsEnabled = hapticsEnabled,
            soundEnabled = soundEnabled,
            radioPlaying = radioPlaying
        )
        return
    }

    BackHandler(enabled = searchOpen) {
        searchOpen = false
        query = ""
        searchStamp = System.currentTimeMillis()
        keyboardController?.hide()
    }

    val ordered = channels
        .filterNot { hiddenChannels.contains(it.key) }
        .sortedWith(
            compareByDescending<StreamItem> { favorites.contains(it.key) }
                .thenByDescending { favoriteTimes[it.key] ?: 0L }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    val filtered = fuzzyFilter(ordered, query)

    Column(Modifier.fillMaxSize().background(TvV9Bg)) {
        TvV9Header(
            searchOpen = searchOpen,
            query = query,
            focusRequester = focusRequester,
            onBack = back,
            onSearchOpen = {
                searchOpen = true
                searchStamp = System.currentTimeMillis()
            },
            onQuery = {
                query = it
                searchStamp = System.currentTimeMillis()
            },
            onSearchClose = {
                if (query.isNotBlank()) scope.launch { store.rememberSearch(Section.TV, query) }
                searchOpen = false
                query = ""
                searchStamp = System.currentTimeMillis()
                keyboardController?.hide()
            },
            onRefresh = {
                scope.launch {
                    refreshing = true
                    reload()
                    refreshing = false
                }
            },
            onSettings = settings
        )

        if (searchOpen && query.isBlank()) {
            SearchHistoryPanel(
                history = searchHistory,
                onPick = {
                    query = it
                    searchStamp = System.currentTimeMillis()
                },
                onClear = {
                    scope.launch {
                        store.clearSearchHistory(Section.TV)
                        searchHistory = emptyList()
                    }
                }
            )
        }

        restore?.let { item ->
            TvV9RestoreBanner(
                item = item,
                onContinue = {
                    val i = channels.indexOfFirst {
                        it.url == item.url || it.name.equals(item.name, true)
                    }
                    if (i >= 0) startPlayback(i, false) else notify("Сохранённый канал больше не найден", 5000L)
                },
                onClose = dismissRestore
            )
        }

        Box(Modifier.fillMaxSize().weight(1f)) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        val result = try {
                            repo.load(sourceKey)
                        } catch (t: Throwable) {
                            Result.failure(t)
                        }
                        result.fold(
                            onSuccess = { loaded ->
                                channels = loaded.items
                                health = emptyMap()
                                loading = false
                                notify("Список обновлён", 5000L)
                            },
                            onFailure = {
                                loadError = "Не удалось обновить список"
                            }
                        )
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    loading -> TvV9Skeleton()
                    channels.isEmpty() -> TvV9ErrorState(loadError ?: "Плейлист пуст") {
                        scope.launch { reload() }
                    }
                    filtered.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ничего не найдено", color = TvV9Gray)
                    }
                    else -> LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.key }) { channel ->
                            val favorite = favorites.contains(channel.key)
                            val offline = health[channel.url] == AvailabilityStatus.OFFLINE
                            TvV9ChannelRow(
                                item = channel,
                                favorite = favorite,
                                logoCache = logoCache,
                                offline = offline,
                                showHideIcon = showHideIcon,
                                hapticsEnabled = hapticsEnabled,
                                soundEnabled = soundEnabled,
                                allowSound = !player.isPlaying.value && !radioPlaying,
                                onPlay = {
                                    channels.indexOfFirst { it.key == channel.key }
                                        .takeIf { it >= 0 }
                                        ?.let { index -> startPlayback(index, favorite) }
                                },
                                onFavorite = {
                                    scope.launch {
                                        val newFavorite = !favorite
                                        store.setFavorite(Section.TV, channel.key, newFavorite)
                                        favorites = store.favorites(Section.TV)
                                        favoriteTimes = store.favoriteAddedAt(Section.TV)
                                        notify(if (newFavorite) "Канал добавлен в избранное" else "Канал удалён из избранного", 3000L)
                                    }
                                },
                                onHide = {
                                    scope.launch {
                                        store.addHiddenChannel(channel.key)
                                        notify("Канал скрыт", 3000L)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ScrollTopButtonV37(
                listState = list,
                hapticsEnabled = hapticsEnabled,
                soundEnabled = soundEnabled,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                    .padding(12.dp)
            )

            if (refreshing) {
                Text(
                    "Обновление…",
                    color = TvV9Red,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = .45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (sourcePickerVisible) {
        val options = buildList {
            TV_SOURCES.forEachIndexed { index, source ->
                add(builtinSourceKey(index) to source.name)
            }
            userSources.forEach { source ->
                add(source.key to source.name)
            }
        }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Выбор источника") },
            text = {
                val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (!landscape) {
                    Column(
                        Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Источник по умолчанию недоступен. Выберите другой источник",
                            color = TvV9Gray,
                            fontSize = 12.sp
                        )
                        options.forEach { (key, name) ->
                            val active = key == sourceKey
                            TextButton(
                                onClick = {
                                    sourcePickerVisible = false
                                    scope.launch {
                                        store.setActiveSourceKey(key)
                                        sourceReady = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    name,
                                    color = if (active) TvV9Red else Color.White,
                                    maxLines = 2,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(end = 10.dp)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                scope.launch {
                                                    scrollState.scrollTo(
                                                        (scrollState.value + 72).coerceAtMost(scrollState.maxValue)
                                                    )
                                                }
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                scope.launch {
                                                    scrollState.scrollTo(
                                                        (scrollState.value - 72).coerceAtLeast(0)
                                                    )
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Источник по умолчанию недоступен. Выберите другой источник",
                                color = TvV9Gray,
                                fontSize = 12.sp
                            )
                            options.forEach { (key, name) ->
                                val active = key == sourceKey
                                TextButton(
                                    onClick = {
                                        sourcePickerVisible = false
                                        scope.launch {
                                            store.setActiveSourceKey(key)
                                            sourceReady = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        name,
                                        color = if (active) TvV9Red else Color.White,
                                        maxLines = 2,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (scrollState.maxValue > 0) {
                            Canvas(
                                Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                val viewport = size.height
                                val thumbHeight =
                                    (viewport * viewport / (viewport + scrollState.maxValue)).coerceAtLeast(24f)
                                val thumbTop =
                                    (scrollState.value / scrollState.maxValue.toFloat()).coerceIn(0f, 1f) *
                                        (viewport - thumbHeight)
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.35f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, thumbTop),
                                    size = androidx.compose.ui.geometry.Size(size.width, thumbHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width, size.width)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}


@Composable
private fun TvV9Header(
    searchOpen: Boolean,
    query: String,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onQuery: (String) -> Unit,
    onSearchClose: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Назад", tint = TvV9Red)
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Поиск…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TvV9Red) },
                trailingIcon = {
                    IconButton(onClick = onSearchClose) {
                        Icon(Icons.Default.Close, null, tint = TvV9Red)
                    }
                }
            )
        } else {
            Text(
                "ТЕЛЕВИЗОР",
                Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Обновить", tint = TvV9Red) }
            IconButton(onClick = onSearchOpen) { Icon(Icons.Default.Search, "Поиск", tint = TvV9Red) }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки", tint = TvV9Red) }
        }
    }
}

@Composable
private fun TvV9ChannelRow(
    item: StreamItem,
    favorite: Boolean,
    logoCache: TvLogoCache,
    offline: Boolean,
    showHideIcon: Boolean,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    allowSound: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit
) {
    var rowWidth by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var dragOffset by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var settleTarget by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var settling by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableStateOf(false) }
    var hiding by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableStateOf(false) }
    var thresholdReached by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableStateOf(false) }
    val feedbackContext = LocalContext.current
    val settledOffset by animateFloatAsState(
        targetValue = settleTarget,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "tv-v9-swipe-settle-offset"
    )
    val displayOffset = if (settling) settledOffset else dragOffset
    val threshold = rowWidth * 0.70f
    val progress = (abs(displayOffset) / threshold.coerceAtLeast(1f)).coerceIn(0f, 1f)

    LaunchedEffect(settling, settleTarget) {
        if (!settling) return@LaunchedEffect
        delay(220L)
        if (settleTarget < 0f) onHide()
        dragOffset = 0f
        settleTarget = 0f
        settling = false
    }

    AnimatedVisibility(
        visible = !hiding,
        enter = fadeIn(),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (dragOffset < 0f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            TvV9Red.copy(alpha = 0.10f + 0.50f * progress),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        "Скрыть канал",
                        tint = TvV9Red.copy(alpha = 0.65f + 0.35f * progress),
                        modifier = Modifier.padding(end = 14.dp).size(30.dp)
                    )
                }
            }
            Card(
                onClick = if (hiding) ({}) else onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowWidth = it.width.toFloat().coerceAtLeast(1f) }
                    .graphicsLayer {
                        translationX = displayOffset
                        alpha = 1f - (abs(displayOffset) / rowWidth).coerceIn(0f, 0.45f)
                    }
                    .pointerInput(item.key, rowWidth) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (!settling && !hiding) {
                                    dragOffset = (dragOffset + dragAmount).coerceIn(-rowWidth, 0f)
                                    val threshold = rowWidth * 0.70f
                                    val reached = dragOffset <= -threshold
                                    if (reached && !thresholdReached) {
                                        thresholdReached = true
                                        InteractionFeedback.threshold(feedbackContext, hapticsEnabled)
                                    } else if (!reached) {
                                        thresholdReached = false
                                    }
                                }
                            },
                            onDragEnd = {
                                if (hiding) return@detectHorizontalDragGestures
                                val threshold = rowWidth * 0.70f
                                settling = true
                                if (dragOffset <= -threshold) {
                                    settleTarget = -rowWidth
                                    hiding = true
                                    InteractionFeedback.success(feedbackContext, hapticsEnabled)
                                } else {
                                    settleTarget = 0f
                                    thresholdReached = false
                                }
                            },
                            onDragCancel = {
                                if (!hiding) {
                                    settling = true
                                    settleTarget = 0f
                                    thresholdReached = false
                                }
                            }
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (favorite) TvV9Red.copy(alpha = .08f) else TvV9Panel
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvV9Logo(logoCache, item.logoUrl, 60.dp, offline)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name.uppercase(Locale.ROOT),
                            color = if (offline) TvV9Gray else Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.groupTitle?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = TvV9Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (offline) Text("• временно недоступен", color = TvV9Gray, fontSize = 10.sp)
                    }
                    IconButton(onClick = {
                        InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound)
                        onFavorite()
                    }) {
                        Icon(
                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Избранное",
                            tint = if (favorite) TvV9Red else Color.White
                        )
                    }
                    if (showHideIcon) {
                        IconButton(
                            onClick = {
                                InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound)
                                if (!hiding) {
                                    settleTarget = -rowWidth
                                    settling = true
                                    hiding = true
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, "Скрыть канал", tint = TvV9Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvV9Logo(
    cache: TvLogoCache,
    url: String?,
    size: Dp,
    dimmed: Boolean
) {
    var bitmap by androidx.compose.runtime.remember(url) {
        androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    androidx.compose.runtime.LaunchedEffect(url) {
        bitmap = cache.loadImageBitmap(url)
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier.size(size).alpha(if (dimmed) .4f else 1f),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            Modifier.size(size).background(TvV9PanelAlt, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("NO\nImage", color = TvV9Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TvV9ScrollActionButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(220)) + androidx.compose.animation.scaleIn(
            initialScale = .82f,
            animationSpec = tween(220)
        ),
        exit = fadeOut(tween(220)) + androidx.compose.animation.scaleOut(
            targetScale = .82f,
            animationSpec = tween(220)
        )
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(TvV9Red, CircleShape)
                .focusable()
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                "Вверх",
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }
    }
}

@Composable
private fun TvV9Skeleton() {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(10) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(TvV9PanelAlt, RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun TvV9ErrorState(message: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = TvV9Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = retry,
            colors = ButtonDefaults.buttonColors(containerColor = TvV9Red)
        ) { Text("Повторить") }
    }
}

@Composable
private fun TvV9RestoreBanner(
    item: StreamItem,
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = TvV9PanelAlt),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "Продолжить [" + item.name + "]?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onContinue,
                    Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TvV9Red)
                ) { Text("Продолжить", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onClose,
                    Modifier.weight(1f)
                ) { Text("Закрыть", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun TvV9Player(
    player: PlayerController,
    playerInstance: androidx.media3.exoplayer.ExoPlayer,
    channel: StreamItem,
    favoriteChannels: List<Triple<Int, StreamItem, AvailabilityStatus>>,
    error: String?,
    waiting: Boolean,
    isPlaying: Boolean,
    noticeMessage: String?,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPause: () -> Unit,
    onFavoriteSelected: (Int) -> Unit,
    onEnterPip: () -> Unit,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    initialZoom: Float?,
    onZoomChanged: (Float) -> Unit,
    onZoomReset: () -> Unit,
    pipMode: Boolean,
    onWeakNetworkNotice: (String) -> Unit,
    switching: Boolean,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    radioPlaying: Boolean
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val insets = androidx.compose.runtime.remember(activity) {
        activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) }
    }
    val rootFocus = FocusRequester()

    fun hideSystemBars() {
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets?.hide(WindowInsetsCompat.Type.systemBars())
    }


    var controls by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(true) }
    var locked by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(false) }
    var favoriteMenu by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(false) }
    var sleepMenu by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(false) }
    var sleepToken by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(0) }
    var menuInteractionToken by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(0L) }
    var formatMode by androidx.compose.runtime.remember(channel.key, initialZoom) { androidx.compose.runtime.mutableIntStateOf(if (initialZoom != null) 5 else 0) }
    val orientation = LocalConfiguration.current.orientation
    var lastOrientation by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(orientation) }
    var channelNotice by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(true) }
    val channelLogoCache = androidx.compose.runtime.remember(context) { TvLogoCache(context) }
    var zoom by androidx.compose.runtime.remember(channel.key, initialZoom) {
        androidx.compose.runtime.mutableFloatStateOf(initialZoom?.coerceIn(1f, 3f) ?: 1f)
    }
    var playerToast by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var playerToastToken by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(0L) }
    var playerToastDuration by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(5000L) }
    val playerScope = androidx.compose.runtime.rememberCoroutineScope()
    val playerWindow = activity?.window
    val feedbackContext = LocalContext.current
    val buffering by player.buffering.collectAsStateWithLifecycle()
    val weakNetworkEvent by player.weakNetworkEvent.collectAsStateWithLifecycle()
    val adaptiveBufferLevel by player.adaptiveBufferLevel.collectAsStateWithLifecycle()
    val bufferPercent by player.bufferPercent.collectAsStateWithLifecycle()
    val favoriteListState = rememberLazyListState()
    val favoriteFocusers = remember(favoriteChannels.map { it.first }) {
        favoriteChannels.map { FocusRequester() }
    }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val favoritePopupMaxHeight = (screenHeightDp * 0.70f).coerceAtLeast(220f).dp
    val favoriteVisibleItems = favoriteChannels.size.coerceAtMost(8)
    val favoritePopupHeight = minOf(favoritePopupMaxHeight, (58 + favoriteVisibleItems * 48).dp)

    fun showPlayerToast(message: String, durationMs: Long) {
        playerToast = message
        playerToastDuration = durationMs
        playerToastToken += 1L
    }

    val formatLabel = when (formatMode) {
        1 -> "РАСТЯНУТЬ 125%"
        2 -> "РАСТЯНУТЬ 150%"
        3 -> "РАСТЯНУТЬ 200%"
        4 -> "ЗАПОЛНИТЬ"
        5 -> "ПОЛЬЗОВАТЕЛЬСКИЙ"
        else -> "ОРИГИНАЛ"
    }
    val formatScale = when (formatMode) {
        1 -> 1.25f
        2 -> 1.5f
        3 -> 2f
        else -> 1f
    }
    val channelNoticeName = channel.name
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .trim()
        .take(25)

    val playerView = androidx.compose.runtime.remember {
        PlayerView(context).apply {
            useController = false
            setShutterBackgroundColor(android.graphics.Color.BLACK)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    androidx.compose.runtime.DisposableEffect(playerView, playerInstance) {
        playerView.player = playerInstance
        onDispose {
            if (playerView.player === playerInstance) playerView.player = null
        }
    }

    androidx.compose.runtime.LaunchedEffect(formatMode) {
        playerView.resizeMode = if (formatMode == 4) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        insets?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insets?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    androidx.compose.runtime.DisposableEffect(playerWindow, formatMode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val window = playerWindow
            val originalMode = window?.attributes?.layoutInDisplayCutoutMode
                ?: android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            if (formatMode != 0) {
                window?.let { w ->
                    val params = w.attributes
                    params.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    w.attributes = params
                }
            }
            onDispose {
                if (formatMode != 0) {
                    window?.let { w ->
                        val params = w.attributes
                        params.layoutInDisplayCutoutMode = originalMode
                        w.attributes = params
                    }
                }
            }
        } else {
            onDispose { }
        }
    }

    androidx.compose.runtime.LaunchedEffect(channel.key, switching) {
        channelNotice = false
        if (switching) return@LaunchedEffect
        player.resetWeakNetworkSession()
        channelNotice = true
        delay(3000L)
        channelNotice = false
    }

    androidx.compose.runtime.LaunchedEffect(weakNetworkEvent) {
        if (weakNetworkEvent <= 0L) return@LaunchedEffect
        onWeakNetworkNotice(
            "Нестабильное интернет-соединение. Попробую снизить качество видеопотока или переключу на другой канал."
        )
    }

    androidx.compose.runtime.LaunchedEffect(orientation) {
        if (lastOrientation != orientation) {
            controls = false
            favoriteMenu = false
            sleepMenu = false
            hideSystemBars()
            lastOrientation = orientation
        }
    }

    androidx.compose.runtime.LaunchedEffect(controls, locked, menuInteractionToken) {
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (locked || !controls) insets?.hide(WindowInsetsCompat.Type.systemBars())
        else insets?.show(WindowInsetsCompat.Type.systemBars())
        if (controls && !locked) {
            val token = menuInteractionToken
            delay(4_000L)
            if (token == menuInteractionToken) {
                controls = false
                favoriteMenu = false
                sleepMenu = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(pipMode) {
        if (pipMode) {
            controls = false
            favoriteMenu = false
            sleepMenu = false
            playerToast = null
            hideSystemBars()
        } else {
            controls = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(playerToastToken) {
        if (playerToast == null || playerToastToken == 0L) return@LaunchedEffect
        val token = playerToastToken
        delay(playerToastDuration)
        if (token == playerToastToken) playerToast = null
    }

    androidx.compose.runtime.LaunchedEffect(favoriteMenu) {
        if (favoriteMenu && favoriteFocusers.isNotEmpty()) {
            runCatching { favoriteFocusers.first().requestFocus() }
            runCatching { favoriteListState.scrollToItem(0) }
        }
    }

    androidx.compose.runtime.LaunchedEffect(favoriteMenu, sleepMenu, menuInteractionToken) {
        if (!favoriteMenu && !sleepMenu) return@LaunchedEffect
        val token = menuInteractionToken
        delay(10_000L)
        if (token == menuInteractionToken) {
            favoriteMenu = false
            sleepMenu = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { rootFocus.requestFocus() }
    }

    BackHandler(enabled = locked) { /* unlock only through the on-screen lock icon */ }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!locked) {
                            if (!controls) controls = true else onPrev()
                            menuInteractionToken++
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!locked) {
                            if (!controls) controls = true else onNext()
                            menuInteractionToken++
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                        if (!locked) {
                            if (!controls) controls = true else onPause()
                            menuInteractionToken++
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!locked && !controls) {
                            controls = true
                            menuInteractionToken++
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (!locked) {
                            onBack()
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val baseScale = zoom * formatScale
                    transformOrigin = TransformOrigin.Center
                    scaleX = baseScale
                    scaleY = baseScale
                }
                .pointerInput(locked, controls, channel.key) {
                    if (!locked) {
                        detectTapGestures(
                            onTap = { controls = !controls; menuInteractionToken++ },
                            onDoubleTap = {
                            zoom = 1f
                            formatMode = 5
                            onZoomReset()
                            showPlayerToast("Пользовательский: 100%", 3000L)
                        }
                        )
                    } else {
                        detectTapGestures(onTap = {})
                    }
                }
                .pointerInput(locked, channel.key) {
                    if (!locked) {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            if (formatMode != 5) {
                                formatMode = 5
                                zoom = 1f
                            }
                            val nextZoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                            zoom = nextZoom
                            onZoomChanged(nextZoom)
                            showPlayerToast("Пользовательский масштаб: " + (nextZoom * 100f).toInt() + "%", 3000L)
                        }
                    } else {
                        detectTransformGestures { _, _, _, _ -> }
                    }
                }
        )

        if (locked && !pipMode) {
            IconButton(
                onClick = {
                    locked = false
                    controls = false
                    favoriteMenu = false
                    sleepMenu = false
                    runCatching { rootFocus.requestFocus() }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .padding(8.dp)
                    .size(54.dp)
                    .background(Color.Black.copy(alpha = .35f), CircleShape)
                    .border(2.dp, TvV9Red, CircleShape)
                    .focusable()
            ) {
                Icon(Icons.Default.LockOpen, "Разблокировать", tint = Color.White)
            }
        } else {
            AnimatedVisibility(
                visible = controls && !pipMode,
                modifier = Modifier.fillMaxSize().zIndex(1f),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvV9PlayerButton(Icons.Default.ArrowBack, "Назад", hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying, onClick = { menuInteractionToken++; onBack() })
                        TvV9PlayerButton(Icons.Default.AccessTime, "Таймер сна", hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying) {
                            InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound=!player.isPlaying.value&&!radioPlaying)
                            val open = !sleepMenu
                            sleepMenu = open
                            if (open) favoriteMenu = false
                            menuInteractionToken++
                            sleepToken++
                        }
                        TvV9PlayerButton(Icons.Default.AspectRatio, formatLabel, hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying) {
                            favoriteMenu = false
                            sleepMenu = false
                            val nextMode = (formatMode + 1) % 6
                            formatMode = nextMode
                            zoom = if (nextMode == 5) {
                                initialZoom?.coerceIn(1f, 3f) ?: 1f
                            } else {
                                1f
                            }
                            menuInteractionToken++
                            val nextLabel = when (formatMode) {
                                1 -> "РАСТЯНУТЬ 125%"
                                2 -> "РАСТЯНУТЬ 150%"
                                3 -> "РАСТЯНУТЬ 200%"
                                4 -> "ЗАПОЛНИТЬ"
                                5 -> "ПОЛЬЗОВАТЕЛЬСКИЙ"
                                else -> "ОРИГИНАЛ"
                            }
                            showPlayerToast("Формат: " + nextLabel, 5000L)
                        }
                        TvV9PlayerButton(Icons.Default.Lock, "Заблокировать", hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying) {
                            menuInteractionToken++
                            favoriteMenu = false
                            sleepMenu = false
                            locked = true
                            hideSystemBars()
                        }
                        TvV9PlayerButton(
                            if (favoriteMenu) Icons.Default.Star else Icons.Default.StarBorder,
                            "Избранное",
                            hapticsEnabled,
                            soundEnabled,
                            allowSound = !player.isPlaying.value && !radioPlaying,
                            onClick = {
                                InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound=!player.isPlaying.value&&!radioPlaying)
                                val open = !favoriteMenu
                                favoriteMenu = open
                                if (open) sleepMenu = false
                                menuInteractionToken++
                            }
                        )
                        TvV9PlayerButton(
    Icons.Default.PictureInPictureAlt,
    "PiP",
    hapticsEnabled,
    soundEnabled,
    allowSound = !player.isPlaying.value && !radioPlaying,
    onClick = { menuInteractionToken++; onEnterPip() }
)
                    }

                    AnimatedVisibility(
                        visible = playerToast != null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 64.dp, end = 12.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            playerToast.orEmpty(),
                            Modifier
                                .background(TvV9Panel.copy(alpha = .88f), RoundedCornerShape(8.dp))
                                .border(1.dp, TvV9Red, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val showBuffering = !pipMode &&
                        !switching &&
                        !waiting &&
                        error == null &&
                        buffering &&
                        bufferPercent < 100

                    if (showBuffering) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 140.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                Modifier.width(180.dp).padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Буферизация… $bufferPercent%",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { bufferPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = TvV9Red,
                                    trackColor = Color.White.copy(alpha = .20f)
                                )
                            }
                        }
                    }

                    if (channelNotice) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                                .padding(bottom = 92.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .72f)),
                            border = BorderStroke(1.dp, TvV9Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!channel.logoUrl.isNullOrBlank()) {
                                    TvV9Logo(channelLogoCache, channel.logoUrl, 34.dp, false)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    channelNoticeName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvV9PlayerButtonLarge(Icons.Default.SkipPrevious, "Предыдущий", hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying, { menuInteractionToken++; onPrev() })
                        TvV9PlayerButtonLarge(
                            if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow,
                            "Пауза / Старт",
                            hapticsEnabled,
                            soundEnabled,
                            allowSound = !player.isPlaying.value && !radioPlaying,
                            onClick = { menuInteractionToken++; onPause() }
                        )
                        TvV9PlayerButtonLarge(Icons.Default.SkipNext, "Следующий", hapticsEnabled, soundEnabled, allowSound = !player.isPlaying.value && !radioPlaying, { menuInteractionToken++; onNext() })
                    }

                    if (!pipMode) noticeMessage?.let {
                        Text(
                            it,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(TvV9Red.copy(alpha = .92f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    val showLoading = switching || (!waiting && error == null && !isPlaying && !buffering)

                    if (!pipMode && noticeMessage == null && (showLoading || waiting || error != null)) {
                        Text(
                            when {
                                switching -> "Загрузка…"
                                waiting -> "Ожидание сети…\nВозможно источник трансляции канала — сломался"
                                error != null -> "Поток недоступен"
                                else -> "Загрузка…"
                            },
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = .4f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        )
                    }

                    if (favoriteMenu) {
                        Popup(
                            alignment = Alignment.TopStart,
                            onDismissRequest = { favoriteMenu = false },
                            properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
                        ) {
                            Box(
                                Modifier
                                    .padding(start = 68.dp, top = 64.dp)
                                    .width(300.dp)
                                    .heightIn(min = 58.dp, max = favoritePopupHeight)
                                    .background(TvV9Panel.copy(alpha = .88f), RoundedCornerShape(14.dp))
                                    .border(1.dp, TvV9Red, RoundedCornerShape(14.dp))
                                    .padding(8.dp)
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    Text("ИЗБРАННЫЕ КАНАЛЫ", color = TvV9Red, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(5.dp))
                                    if (favoriteChannels.isEmpty()) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Пока нет избранных каналов", color = TvV9Gray, fontSize = 12.sp)
                                        }
                                    } else {
                                        Box(Modifier.fillMaxSize()) {
                                            LazyColumn(
                                                state = favoriteListState,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = favoritePopupHeight - 58.dp)
                                                    .padding(end = 5.dp),
                                                contentPadding = PaddingValues(vertical = 2.dp)
                                            ) {
                                                items(
                                                    items = favoriteChannels,
                                                    key = { it.second.key }
                                                ) { (index, item, availability) ->
                                                    val focusIndex = favoriteChannels.indexOfFirst { it.first == index }
                                                    TextButton(
                                                        onClick = {
                                                            favoriteMenu = false
                                                            onFavoriteSelected(index)
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .focusRequester(favoriteFocusers[focusIndex])
                                                            .focusable()
                                                            .onPreviewKeyEvent { event ->
                                                                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                                                                when (event.nativeKeyEvent.keyCode) {
                                                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                                                        if (focusIndex > 0) {
                                                                            menuInteractionToken++
                                                                            favoriteFocusers[focusIndex - 1].requestFocus()
                                                                            playerScope.launch { favoriteListState.animateScrollToItem(focusIndex - 1) }
                                                                        }
                                                                        true
                                                                    }
                                                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                                        if (focusIndex + 1 < favoriteFocusers.size) {
                                                                            menuInteractionToken++
                                                                            favoriteFocusers[focusIndex + 1].requestFocus()
                                                                            playerScope.launch { favoriteListState.animateScrollToItem(focusIndex + 1) }
                                                                        }
                                                                        true
                                                                    }
                                                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                                                    KeyEvent.KEYCODE_ENTER -> {
                                                                        favoriteMenu = false
                                                                        onFavoriteSelected(index)
                                                                        true
                                                                    }
                                                                    KeyEvent.KEYCODE_BACK -> {
                                                                        favoriteMenu = false
                                                                        true
                                                                    }
                                                                    else -> false
                                                                }
                                                            }
                                                    ) {
                                                        Text(
                                                            item.name,
                                                            Modifier.fillMaxWidth(),
                                                            color = if (availability == AvailabilityStatus.OFFLINE) TvV9Gray else Color.White,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Start
                                                        )
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (sleepMenu) {
                        Popup(
                            alignment = Alignment.TopStart,
                            onDismissRequest = { sleepMenu = false },
                            properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
                        ) {
                            Column(
                                Modifier
                                    .padding(start = 68.dp, top = 64.dp)
                                    .background(TvV9Panel.copy(alpha = .88f), RoundedCornerShape(14.dp))
                                    .border(1.dp, TvV9Red, RoundedCornerShape(14.dp))
                                    .padding(8.dp)
                            ) {
                                Text("ТАЙМЕР СНА", color = TvV9Red, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    maxItemsInEachRow = 4
                                ) {
                                    TvV9SleepOptions.forEach { (minutes, label) ->
                                        val active = sleepMinutes == minutes && sleepUntil > System.currentTimeMillis()
                                        val bg by animateColorAsState(
                                            if (active) TvV9Red else Color.Transparent,
                                            label = "tv-v9-sleep-" + minutes
                                        )
                                        Card(
                                            onClick = {
                                                if (active) onCancelSleep() else onSleep(minutes)
                                                sleepMenu = false
                                            },
                                            modifier = Modifier.size(62.dp),
                                            colors = CardDefaults.cardColors(containerColor = bg),
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, TvV9Red)
                                        ) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (active) formatTvV9Sleep(sleepRemaining) else label,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvV9PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    allowSound: Boolean,
    onClick: () -> Unit
) {
    val feedbackContext = LocalContext.current
    IconButton(
        onClick = {
            InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound)
            onClick()
        },
        modifier = Modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = .48f), CircleShape)
            .border(1.5.dp, TvV9Red, CircleShape)
            .focusable()
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun TvV9PlayerButtonLarge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    allowSound: Boolean,
    onClick: () -> Unit
) {
    val feedbackContext = LocalContext.current
    IconButton(
        onClick = {
            InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,allowSound)
            onClick()
        },
        modifier = Modifier
            .size(60.dp)
            .background(Color.Black.copy(alpha = .48f), CircleShape)
            .border(1.5.dp, TvV9Red, CircleShape)
            .focusable()
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(34.dp))
    }
}

private fun formatTvV9Sleep(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return if (totalSeconds >= 3600L) {
        "%02d:%02d".format(totalSeconds / 3600L, (totalSeconds / 60L) % 60L)
    } else {
        "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }
}

private suspend fun scanTvV9(
    items: List<StreamItem>,
    repo: TvPlaylistRepositoryV8
): Map<String, AvailabilityStatus> = coroutineScope {
    val result = ConcurrentHashMap<String, AvailabilityStatus>()
    val semaphore = Semaphore(4)
    items.map { item ->
        launch(Dispatchers.IO) {
            semaphore.withPermit {
                result[item.url] = repo.checkAvailability(item.url)
            }
        }
    }.forEach { it.join() }
    result
}

@Composable
private fun rememberTvV9Network(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    val state = remember { androidx.compose.runtime.mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        fun online(): Boolean =
            cm.allNetworks.any { n ->
                cm.getNetworkCapabilities(n)?.hasCapability(
                    android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
                ) == true
            }

        state.value = online()
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { state.value = online() }
            override fun onLost(network: android.net.Network) { state.value = online() }
        }

        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }

    return state
}
