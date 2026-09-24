@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.offex7.streamhub

import android.view.KeyEvent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

private val tvV9ZoomByChannel = mutableMapOf<String, Float>()

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
    notify: (String) -> Unit,
    onViewingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repo = androidx.compose.runtime.remember(context, store) { TvPlaylistRepositoryV8(context, store) }
    val sourceKey by store.activeSourceFlow().collectAsStateWithLifecycle(initialValue = builtinSourceKey(0))
    val list = rememberLazyListState()
    val network by rememberTvV9Network()
    val error by player.error.collectAsStateWithLifecycle()
    val waiting by player.waitingForNetwork.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val playerInstance by player.playerInstance.collectAsStateWithLifecycle()
    val focusRequester = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current
    val logoCache = androidx.compose.runtime.remember(context) { TvLogoCache(context) }

    var channels by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList<StreamItem>()) }
    var favorites by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
    var health by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var loading by androidx.compose.runtime.remember(sourceKey) { androidx.compose.runtime.mutableStateOf(true) }
    var loadError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var refreshing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedIndex by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(-1) }
    var fullscreen by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var searchOpen by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var query by androidx.compose.runtime.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var searchStamp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    var notice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var playbackJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    var showScrollUp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
        reload()
    }

    LaunchedEffect(Unit) {
        val saved = store.scrollPosition(Section.TV)
        runCatching { list.scrollToItem(saved.first.coerceAtLeast(0), saved.second) }
        var previous = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        androidx.compose.runtime.snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { current ->
                store.saveScrollPosition(Section.TV, current.first, current.second)
                if (current != previous) {
                    showScrollUp = current.first < previous.first ||
                        (current.first == previous.first && current.second < previous.second)
                    previous = current
                }
            }
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
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

    DisposableEffect(Unit) {
        onDispose { onViewingChanged(false) }
    }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            health = scanTvV9(channels.take(18), repo)
        }
    }

    LaunchedEffect(list.firstVisibleItemIndex, channels, health) {
        if (channels.isEmpty()) return@LaunchedEffect
        val first = list.firstVisibleItemIndex.coerceIn(0, channels.lastIndex)
        val end = minOf(channels.size, first + 20)
        val pending = channels.subList(first, end).filter { health[it.url] == null }
        if (pending.isNotEmpty()) health = health + scanTvV9(pending, repo)
    }

    fun previousIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start.coerceIn(0, channels.lastIndex)
        repeat(channels.size - 1) {
            cursor = (cursor - 1 + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun nextIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start.coerceIn(0, channels.lastIndex)
        repeat(channels.size - 1) {
            cursor = (cursor + 1) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun startPlayback(start: Int) {
        if (start !in channels.indices) return
        playbackJob?.cancel()
        fullscreen = true
        notice = null
        playbackJob = scope.launch {
            var cursor = start
            repeat(channels.size) {
                selectedIndex = cursor
                val candidate = channels[cursor]
                val result = player.playWithFallback(listOf(candidate.url))
                if (result >= 0) {
                    health = health + (candidate.url to AvailabilityStatus.ONLINE)
                    saveLast(candidate)
                    return@launch
                }
                health = health + (candidate.url to AvailabilityStatus.OFFLINE)
                notice = "Канал временно недоступен — переключаю на следующий"
                delay(3000L)
                notice = null
                cursor = nextIndex(cursor)
                if (cursor < 0) {
                    fullscreen = false
                    return@launch
                }
            }
            fullscreen = false
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
            if (favorites.contains(channel.key)) index to channel else null
        }
        TvV9Player(
            player = player,
            playerInstance = playerInstance,
            channel = channels[selectedIndex],
            favoriteChannels = favoriteChannels,
            error = if (notice == null) error else null,
            waiting = waiting || !network,
            isPlaying = isPlaying,
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
            onCancelSleep = onCancelSleep
        )
        return
    }

    BackHandler(enabled = searchOpen) {
        searchOpen = false
        query = ""
        searchStamp = System.currentTimeMillis()
        keyboardController?.hide()
    }

    val ordered = channels.sortedWith(
        compareByDescending<StreamItem> { favorites.contains(it.key) }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    )
    val filtered = if (query.isBlank()) ordered else ordered.filter {
        it.name.contains(query.trim(), true) ||
            it.groupTitle.orEmpty().contains(query.trim(), true)
    }

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

        restore?.let { item ->
            TvV9RestoreBanner(
                item = item,
                onContinue = {
                    val i = channels.indexOfFirst {
                        it.url == item.url || it.name.equals(item.name, true)
                    }
                    if (i >= 0) startPlayback(i) else notify("Сохранённый канал больше не найден")
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
                        runCatching { repo.load(sourceKey) }
                            .onSuccess {
                                channels = it.items
                                health = emptyMap()
                                loading = false
                                notify("Список обновлён")
                            }
                            .onFailure {
                                loadError = "Не удалось обновить список"
                            }
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
                            TvV9ChannelRow(
                                item = channel,
                                favorite = favorite,
                                logoCache = logoCache,
                                offline = health[channel.url] == AvailabilityStatus.OFFLINE,
                                onPlay = {
                                    channels.indexOfFirst { it.key == channel.key }
                                        .takeIf { it >= 0 }
                                        ?.let(::startPlayback)
                                },
                                onFavorite = {
                                    scope.launch {
                                        store.setFavorite(Section.TV, channel.key, !favorite)
                                        favorites = store.favorites(Section.TV)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            TvV9ScrollUpButton(
                visible = showScrollUp,
                onClick = {
                    scope.launch {
                        showScrollUp = false
                        list.animateScrollToItem(0)
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
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val iconColor by animateColorAsState(
        if (favorite) TvV9Red else Color.White,
        label = "tv-v9-favorite-color"
    )
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Избранное",
                    tint = iconColor
                )
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
private fun TvV9ScrollUpButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val transition = rememberInfiniteTransition(label = "tv-v9-scroll-up")
    val scale by transition.animateFloat(
        initialValue = .92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tv-v9-scroll-scale"
    )
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(Color.Black.copy(alpha = .55f), CircleShape)
                .border(1.5.dp, TvV9Red, CircleShape)
                .focusable()
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                "Вверх",
                tint = Color.White,
                modifier = Modifier.graphicsLayer(rotationZ = -90f)
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
    favoriteChannels: List<Pair<Int, StreamItem>>,
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
    onCancelSleep: () -> Unit
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
    var formatMode by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableIntStateOf(0) }
    var zoom by androidx.compose.runtime.remember(channel.key) {
        androidx.compose.runtime.mutableFloatStateOf(tvV9ZoomByChannel[channel.key] ?: 1f)
    }
    var playerToast by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var playerToastToken by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(0L) }
    var playerToastDuration by androidx.compose.runtime.remember(channel.key) { androidx.compose.runtime.mutableLongStateOf(5000L) }
    val playerScope = androidx.compose.runtime.rememberCoroutineScope()
    val favoriteListState = rememberLazyListState()
    val favoriteFocusers = remember(favoriteChannels.map { it.first }) {
        favoriteChannels.map { FocusRequester() }
    }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val favoritePopupHeight = minOf(420, (screenHeightDp * 0.60f).toInt()).coerceAtLeast(220).dp

    fun showPlayerToast(message: String, durationMs: Long) {
        playerToast = message
        playerToastDuration = durationMs
        playerToastToken += 1L
    }

    val formatLabel = when (formatMode) {
        1 -> "РАСТЯНУТЬ 25%"
        2 -> "ЗАПОЛНИТЬ / ZOOM"
        else -> "ОРИГИНАЛ"
    }

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
        playerView.resizeMode = when (formatMode) {
            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        insets?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insets?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    androidx.compose.runtime.LaunchedEffect(controls, locked) {
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (locked || !controls) insets?.hide(WindowInsetsCompat.Type.systemBars())
        else insets?.show(WindowInsetsCompat.Type.systemBars())
        if (controls && !locked) {
            delay(5000L)
            controls = false
            favoriteMenu = false
            sleepMenu = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(playerToastToken) {
        if (playerToast == null || playerToastToken == 0L) return@LaunchedEffect
        val token = playerToastToken
        delay(playerToastDuration)
        if (token == playerToastToken) playerToast = null
    }

    androidx.compose.runtime.LaunchedEffect(sleepMenu, sleepToken) {
        if (sleepMenu) {
            delay(5000L)
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
                .graphicsLayer(scaleX = zoom, scaleY = zoom)
                .pointerInput(locked, controls, channel.key) {
                    if (!locked) {
                        detectTapGestures(
                            onTap = { controls = !controls },
                            onDoubleTap = {
                                zoom = 1f
                                tvV9ZoomByChannel.remove(channel.key)
                            }
                        )
                    } else {
                        detectTapGestures(onTap = {})
                    }
                }
                .pointerInput(locked, channel.key) {
                    if (!locked) {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                            tvV9ZoomByChannel[channel.key] = zoom
                            showPlayerToast("Масштаб: " + (zoom * 100f).toInt() + "%", 3000L)
                        }
                    } else {
                        detectTransformGestures { _, _, _, _ -> }
                    }
                }
        )

        if (locked) {
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
                visible = controls,
                modifier = Modifier.fillMaxSize(),
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
                            sleepMenu = !sleepMenu
                            sleepToken++
                        }
                        TvV9PlayerButton(Icons.Default.AspectRatio, formatLabel) {
                            formatMode = (formatMode + 1) % 3
                            showPlayerToast("Формат: " + formatLabel, 5000L)
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
                            favoriteMenu = !favoriteMenu
                            if (favoriteMenu) sleepMenu = false
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

                    noticeMessage?.let {
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

                    if (waiting || error != null || !isPlaying) {
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
                            properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                        ) {
                            Box(
                                Modifier
                                    .padding(start = 68.dp, top = 64.dp)
                                    .width(300.dp)
                                    .height(favoritePopupHeight)
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
                                                modifier = Modifier.fillMaxSize().padding(end = 5.dp),
                                                contentPadding = PaddingValues(vertical = 2.dp)
                                            ) {
                                                items(
                                                    items = favoriteChannels,
                                                    key = { it.second.key }
                                                ) { (index, item) ->
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
                                                                            favoriteFocusers[focusIndex - 1].requestFocus()
                                                                            playerScope.launch { favoriteListState.animateScrollToItem(focusIndex - 1) }
                                                                        }
                                                                        true
                                                                    }
                                                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                                        if (focusIndex + 1 < favoriteFocusers.size) {
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
                                                            color = Color.White,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Start
                                                        )
                                                    }
                                                }
                                            }

                                            val total = favoriteChannels.size.coerceAtLeast(1)
                                            val visibleCount = favoriteListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                                            val progress = favoriteListState.firstVisibleItemIndex.toFloat() /
                                                (total - visibleCount).coerceAtLeast(1).toFloat()
                                            val thumbHeight = favoritePopupHeight *
                                                (visibleCount.toFloat() / total.toFloat()).coerceIn(.08f, 1f)
                                            val thumbY = (favoritePopupHeight - thumbHeight) *
                                                progress.coerceIn(0f, 1f)
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(vertical = 4.dp)
                                                    .width(2.dp)
                                                    .height(thumbHeight.coerceAtLeast(14.dp))
                                                    .offset(y = thumbY)
                                                    .background(Color.White.copy(alpha = .35f), RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (sleepMenu) {
                        Popup(
                            alignment = Alignment.TopStart,
                            properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
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
    IconButton(
        onClick = onClick,
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
    IconButton(
        onClick = onClick,
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
