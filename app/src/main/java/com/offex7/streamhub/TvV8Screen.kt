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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
    pipMode: Boolean
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
    var scrollDirection by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) } // 1=up, -1=down
    var showScrollAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var scrollActivityToken by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var lastScrollSampleTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }

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
            }
            .onFailure {
                loading = false
                loadError = "Не удалось загрузить ТВ-плейлист"
            }
    }

    LaunchedEffect(sourceKey) {
        favorites = store.favorites(Section.TV)
        favoriteTimes = store.favoriteAddedAt(Section.TV)
        savedZooms = store.channelZooms()
        reload()
    }

    LaunchedEffect(Unit) {
        val saved = store.scrollPosition(Section.TV)
        runCatching { list.scrollToItem(saved.first.coerceAtLeast(0), saved.second) }
        var previous = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        var previousTime = System.currentTimeMillis()
        androidx.compose.runtime.snapshotFlow {
            Triple(
                list.firstVisibleItemIndex,
                list.firstVisibleItemScrollOffset,
                list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            )
        }.collect { current ->
            val now = System.currentTimeMillis()
            store.saveScrollPosition(Section.TV, current.first, current.second)
            if (current.first != previous.first || current.second != previous.second) {
                val deltaIndex = current.first - previous.first
                val deltaPx = current.second - previous.second
                val elapsed = (now - previousTime).coerceAtLeast(1L)
                val movingUp = deltaIndex < 0 || (deltaIndex == 0 && deltaPx < 0)
                val movingDown = deltaIndex > 0 || (deltaIndex == 0 && deltaPx > 0)
                val fast = kotlin.math.abs(deltaIndex) >= 2 ||
                    (elapsed <= 140L && kotlin.math.abs(deltaPx) >= 96)
                val visibleTotal = fuzzyFilter(
                    channels.filterNot { hiddenChannels.contains(it.key) },
                    query
                ).size
                val total = visibleTotal
                val lastVisible = current.third
                val canGoTop = current.first > 5
                val canGoBottom = total > 0 && lastVisible >= 0 && lastVisible < total - 6
                when {
                    current.first <= 0 && current.second <= 0 -> showScrollAction = false
                    total > 0 && lastVisible >= total - 1 -> showScrollAction = false
                    fast && movingDown && canGoTop -> {
                        scrollDirection = 1
                        showScrollAction = true
                        scrollActivityToken += 1L
                    }
                    fast && movingUp && canGoBottom -> {
                        scrollDirection = -1
                        showScrollAction = true
                        scrollActivityToken += 1L
                    }
                }
                previous = current.first to current.second
                previousTime = now
            }
        }
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

    LaunchedEffect(scrollActivityToken) {
        if (scrollActivityToken == 0L) return@LaunchedEffect
        val token = scrollActivityToken
        delay(5000L)
        if (token == scrollActivityToken) showScrollAction = false
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

    fun navigationOrder(): List<Int> =
        channels.indices
            .filter { index ->
                val item = channels[index]
                !hiddenChannels.contains(item.key) && health[item.url] != AvailabilityStatus.OFFLINE
            }
            .sortedWith(
                compareByDescending<Int> { favorites.contains(channels[it].key) }
                    .thenByDescending { favoriteTimes[channels[it].key] ?: 0L }
                    .thenBy { it }
            )

    fun previousIndex(start: Int): Int {
        val order = navigationOrder()
        if (order.size <= 1) return -1
        val pos = order.indexOf(start)
        val base = if (pos >= 0) pos else 0
        return order[(base - 1 + order.size) % order.size]
    }

    fun nextIndex(start: Int): Int {
        val order = navigationOrder()
        if (order.size <= 1) return -1
        val pos = order.indexOf(start)
        val base = if (pos >= 0) pos else -1
        return order[(base + 1) % order.size]
    }

    fun nextRawCandidate(start: Int): Int {
        if (channels.size <= 1) return -1
        for (step in 1 until channels.size) {
            val index = (start + step) % channels.size
            val item = channels[index]
            if (!hiddenChannels.contains(item.key) && health[item.url] != AvailabilityStatus.OFFLINE) {
                return index
            }
        }
        return -1
    }

    fun startPlayback(start: Int) {
        if (start !in channels.indices || hiddenChannels.contains(channels[start].key)) return
        playbackJob?.cancel()
        fullscreen = true
        notice = null
        isSwitching = true
        playbackJob = scope.launch {
            var cursor = start
            var attempts = 0
            try {
                while (attempts < channels.size) {
                attempts++
                if (cursor !in channels.indices || hiddenChannels.contains(channels[cursor].key)) {
                    cursor = nextIndex(cursor)
                    if (cursor < 0) break
                    continue
                }
                val candidate = channels[cursor]
                if (health[candidate.url] == AvailabilityStatus.OFFLINE) {
                    cursor = nextIndex(cursor)
                    if (cursor < 0) break
                    continue
                }
                val result = player.playWithFallback(listOf(candidate.url))
                if (result >= 0) {
                    selectedIndex = cursor
                    health = health + (candidate.url to AvailabilityStatus.ONLINE)
                    saveLast(candidate)
                    return@launch
                }
                health = health + (candidate.url to AvailabilityStatus.OFFLINE)
                cursor = nextRawCandidate(cursor)
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
            waiting = if (isSwitching) false else (waiting || !network),
            isPlaying = if (isSwitching) true else isPlaying,
            noticeMessage = notice,
            sleepRemaining = sleepRemaining,
            sleepUntil = sleepUntil,
            sleepMinutes = sleepMinutes,
            onBack = ::closePlayer,
            onPrev = {
                val i = previousIndex(selectedIndex)
                if (i >= 0) startPlayback(i)
            },
            onNext = {
                val i = nextIndex(selectedIndex)
                if (i >= 0) startPlayback(i)
            },
            onPause = { player.toggle() },
            onFavoriteSelected = { index ->
                if (index in channels.indices) startPlayback(index)
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
                scope.launch {
                    store.setChannelZoom(key, value)
                    savedZooms = savedZooms + (key to value)
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
            switching = isSwitching
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
                    if (i >= 0) startPlayback(i) else notify("Сохранённый канал больше не найден", 5000L)
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
                                onPlay = {
                                    channels.indexOfFirst { it.key == channel.key }
                                        .takeIf { it >= 0 }
                                        ?.let(::startPlayback)
                                },
                                onFavorite = {
                                    scope.launch {
                                        store.setFavorite(Section.TV, channel.key, !favorite)
                                        favorites = store.favorites(Section.TV)
                                        favoriteTimes = store.favoriteAddedAt(Section.TV)
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

            TvV9ScrollActionButton(
                visible = showScrollAction,
                direction = scrollDirection,
                onClick = {
                    scope.launch {
                        showScrollAction = false
                        if (scrollDirection == 1) {
                            list.animateScrollToItem(0)
                        } else if (filtered.isNotEmpty()) {
                            list.animateScrollToItem(filtered.lastIndex)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
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
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit
) {
    var rowWidth by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var dragOffset by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var settleTarget by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var settling by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableStateOf(false) }
    var hiding by androidx.compose.runtime.remember(item.key) { androidx.compose.runtime.mutableStateOf(false) }
    val hapticView = LocalView.current
    val haptics = !LocalEnergySaving.current
    val animatedOffset by animateFloatAsState(
        targetValue = if (settling) settleTarget else dragOffset,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "tv-v9-swipe-offset"
    )
    val progress = (abs(animatedOffset) / (rowWidth * 0.5f)).coerceIn(0f, 1f)

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
                        translationX = animatedOffset
                        alpha = 1f - (abs(animatedOffset) / rowWidth).coerceIn(0f, 0.45f)
                    }
                    .pointerInput(item.key, rowWidth) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (!settling && !hiding) {
                                    dragOffset = (dragOffset + dragAmount).coerceIn(-rowWidth, 0f)
                                    if (dragOffset <= -(rowWidth * 0.5f)) {
                                        settleTarget = -rowWidth
                                        settling = true
                                        hiding = true
                                    }
                                }
                            },
                            onDragEnd = {
                                if (hiding) return@detectHorizontalDragGestures
                                val threshold = rowWidth * 0.5f
                                settling = true
                                if (dragOffset <= -threshold) {
                                    settleTarget = -rowWidth
                                    hiding = true
                                } else {
                                    settleTarget = 0f
                                }
                            },
                            onDragCancel = {
                                if (!hiding) {
                                    settling = true
                                    settleTarget = 0f
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
                        if (haptics) hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
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
                                if (haptics) hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                if (!hiding) {
                                    settleTarget = -rowWidth
                                    settling = true
                                    hiding = true
                                }
                            },
                            modifier = Modifier.size(42.dp).border(1.dp, TvV9Red, CircleShape)
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
    direction: Int,
    onClick: () -> Unit,
    modifier: Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + androidx.compose.animation.scaleIn(initialScale = .82f, animationSpec = tween(180)),
        exit = fadeOut(tween(180)) + androidx.compose.animation.scaleOut(targetScale = .82f, animationSpec = tween(180))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(Color.Black.copy(alpha = .55f), CircleShape)
                .border(1.5.dp, TvV9Red, CircleShape)
                .focusable()
        ) {
            Icon(
                if (direction == 1) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                if (direction == 1) "Вниз" else "Вверх",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
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
                ) { Text("Продолжить") }
                OutlinedButton(
                    onClick = onClose,
                    Modifier.weight(1f)
                ) { Text("Закрыть") }
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
    switching: Boolean
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
    var formatMode by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(0) }
    val orientation = LocalConfiguration.current.orientation
    var lastOrientation by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(orientation) }
    var channelNotice by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf(true) }
    val channelLogoCache = androidx.compose.runtime.remember(context) { TvLogoCache(context) }
    var zoom by androidx.compose.runtime.remember(channel.key, initialZoom) {
        androidx.compose.runtime.mutableFloatStateOf(initialZoom?.coerceIn(1f, 3f) ?: 1f)
    }
    var panX by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var panY by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var viewportWidth by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(1) }
    var viewportHeight by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(1) }
    var playerToast by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var playerToastToken by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(0L) }
    var playerToastDuration by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(5000L) }
    val playerScope = androidx.compose.runtime.rememberCoroutineScope()
    val playerWindow = activity?.window
    val buffering by player.buffering.collectAsStateWithLifecycle()
    val weakNetworkEvent by player.weakNetworkEvent.collectAsStateWithLifecycle()
    val adaptiveBufferLevel by player.adaptiveBufferLevel.collectAsStateWithLifecycle()
    var bufferPercent by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(0) }
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
        .take(15)

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
            "Нестабильное интернет-соединение. Попробую снизить качество трансляции или переключу на другой канал."
        )
    }

    androidx.compose.runtime.LaunchedEffect(buffering, adaptiveBufferLevel, playerInstance) {
        if (adaptiveBufferLevel < 3 || !buffering) {
            if (!buffering) bufferPercent = 0
            return@LaunchedEffect
        }
        while (buffering && adaptiveBufferLevel >= 3) {
            bufferPercent = playerInstance.bufferedPercentage.coerceIn(0, 100)
            delay(200L)
        }
        if (!buffering) bufferPercent = 100
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

    androidx.compose.runtime.LaunchedEffect(controls, locked) {
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (locked || !controls) insets?.hide(WindowInsetsCompat.Type.systemBars())
        else insets?.show(WindowInsetsCompat.Type.systemBars())
        if (controls && !locked) {
            delay(10_000L)
            controls = false
            favoriteMenu = false
            sleepMenu = false
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
                        if (!locked) onPrev()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!locked) onNext()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                        if (!locked) onPause()
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
                .onSizeChanged {
                    viewportWidth = it.width.coerceAtLeast(1)
                    viewportHeight = it.height.coerceAtLeast(1)
                }
                .graphicsLayer {
                    val baseScale = zoom * formatScale
                    scaleX = baseScale
                    scaleY = baseScale
                    val maxPanX = viewportWidth * (baseScale - 1f) / 2f
                    val maxPanY = viewportHeight * (baseScale - 1f) / 2f
                    translationX = panX.coerceIn(-maxPanX, maxPanX)
                    translationY = panY.coerceIn(-maxPanY, maxPanY)
                }
                .pointerInput(locked, controls, channel.key) {
                    if (!locked) {
                        detectTapGestures(
                            onTap = { controls = !controls },
                            onDoubleTap = {
                            zoom = 1f
                            panX = 0f
                            panY = 0f
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
                        detectTransformGestures { pan, _, gestureZoom, _ ->
                            if (formatMode != 5) {
                                formatMode = 5
                                zoom = 1f
                                panX = 0f
                                panY = 0f
                                onZoomReset()
                            }
                            val nextZoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                            zoom = nextZoom
                            val effectiveScale = (nextZoom * formatScale).coerceAtLeast(1f)
                            val maxPanX = viewportWidth * (effectiveScale - 1f) / 2f
                            val maxPanY = viewportHeight * (effectiveScale - 1f) / 2f
                            panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
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
                        Modifier.align(Alignment.TopStart).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvV9PlayerButton(Icons.Default.ArrowBack, "Назад", onBack)
                        TvV9PlayerButton(Icons.Default.AccessTime, "Таймер сна") {
                            val open = !sleepMenu
                            sleepMenu = open
                            if (open) favoriteMenu = false
                            menuInteractionToken++
                            sleepToken++
                        }
                        TvV9PlayerButton(Icons.Default.AspectRatio, formatLabel) {
                            favoriteMenu = false
                            sleepMenu = false
                            formatMode = (formatMode + 1) % 6
                            zoom = 1f
                            panX = 0f
                            panY = 0f
                            onZoomReset()
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
                        TvV9PlayerButton(Icons.Default.Lock, "Заблокировать") {
                            favoriteMenu = false
                            sleepMenu = false
                            locked = true
                            hideSystemBars()
                        }
                        TvV9PlayerButton(
                            if (favoriteMenu) Icons.Default.Star else Icons.Default.StarBorder,
                            "Избранное"
                        ) {
                            val open = !favoriteMenu
                            favoriteMenu = open
                            if (open) sleepMenu = false
                            menuInteractionToken++
                        }
                        TvV9PlayerButton(Icons.Default.PictureInPictureAlt, "PiP", onEnterPip)
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

                    if (!pipMode && buffering && adaptiveBufferLevel >= 3) {
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
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvV9PlayerButtonLarge(Icons.Default.SkipPrevious, "Предыдущий", onPrev)
                        TvV9PlayerButtonLarge(
                            if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow,
                            "Пауза / Старт",
                            onPause
                        )
                        TvV9PlayerButtonLarge(Icons.Default.SkipNext, "Следующий", onNext)
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

                    if (!pipMode && (waiting || error != null || !isPlaying)) {
                        Text(
                            when {
                                error != null -> "Поток недоступен"
                                waiting -> "Ожидание сети…"
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
    onClick: () -> Unit
) {
    val hapticView = LocalView.current
    val haptics = !LocalEnergySaving.current
    IconButton(
        onClick = {
            if (haptics) hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
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
    onClick: () -> Unit
) {
    val hapticView = LocalView.current
    val haptics = !LocalEnergySaving.current
    IconButton(
        onClick = {
            if (haptics) hapticView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
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
