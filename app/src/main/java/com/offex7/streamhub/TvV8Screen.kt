@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.offex7.streamhub

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

private val TvV8Red = Color(0xFFE53935)
private val TvV8Orange = Color(0xFFFF9800)
private val TvV8Panel = Color(0xFF1A1A1A)
private val TvV8PanelAlt = Color(0xFF232323)
private val TvV8Gray = Color(0xFF808080)
private val TvV8Bg = Color(0xFF121212)

private val tvV8ZoomByChannel = mutableMapOf<String, Float>()
private val tvV8LogoClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .callTimeout(7, TimeUnit.SECONDS)
    .build()

private val TvV8SleepOptions = listOf(
    5L to "5 мин",
    10L to "10 мин",
    15L to "15 мин",
    30L to "30 мин",
    60L to "1 ч",
    120L to "2 ч",
    240L to "4 ч",
    480L to "8 ч",
    600L to "10 ч",
    900L to "15 ч",
    1440L to "24 ч",
    2160L to "36 ч"
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
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(context, store) { TvPlaylistRepositoryV8(context, store) }
    val sourceKey by store.activeSourceFlow().collectAsState(initial = builtinSourceKey(0))
    val network by rememberTvV8NetworkState()
    val list = rememberLazyListState()
    val error by player.error.collectAsState()
    val waiting by player.waitingForNetwork.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var health by remember { mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var loading by remember(sourceKey) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchStamp by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.TV)
        val saved = store.scrollPosition(Section.TV)
        if (saved.first >= 0) {
            runCatching { list.scrollToItem(saved.first, saved.second) }
        }
    }

    suspend fun reloadPlaylist() {
        loading = true
        loadError = null
        val cached = repo.cached(sourceKey)
        if (cached != null) {
            channels = cached.items
            loading = false
        }
        val result = repo.load(sourceKey)
        result.onSuccess {
            channels = it.items
            loading = false
            loadError = null
        }.onFailure {
            loading = false
            loadError = "Не удалось загрузить ТВ-плейлист"
        }
    }

    LaunchedEffect(sourceKey) {
        reloadPlaylist()
    }

    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) ->
                store.saveScrollPosition(Section.TV, first, offset)
            }
    }

    LaunchedEffect(channels) {
        if (channels.isEmpty()) return@LaunchedEffect
        val pending = channels.take(12).filter { health[it.url] == null }
        if (pending.isEmpty()) return@LaunchedEffect
        health = health + scanTvV8(pending, repo)
    }

    LaunchedEffect(list.firstVisibleItemIndex, channels) {
        if (channels.isEmpty()) return@LaunchedEffect
        val first = list.firstVisibleItemIndex.coerceIn(0, channels.lastIndex)
        val end = minOf(channels.size, first + 12)
        val pending = channels.subList(first, end).filter { health[it.url] == null }
        if (pending.isNotEmpty()) health = health + scanTvV8(pending, repo)
    }

    fun nextIndex(from: Int): Int {
        if (channels.size <= 1) return -1
        for (step in 1 until channels.size) {
            val index = (from + step) % channels.size
            if (health[channels[index].url] != AvailabilityStatus.OFFLINE) return index
        }
        return -1
    }

    fun startPlayback(start: Int) {
        if (start !in channels.indices) return
        playbackJob?.cancel()
        notice = null
        selectedIndex = start
        fullscreen = true
        playbackJob = scope.launch {
            var cursor = start
            repeat(channels.size) {
                selectedIndex = cursor
                val candidate = channels[cursor]
                val result = player.playWithFallback(listOf(candidate.url))
                if (result >= 0) {
                    saveLast(candidate)
                    return@launch
                }

                notice = "Канал временно недоступен — переключаю на следующий"
                delay(3000L)
                notice = null
                val next = nextIndex(cursor)
                if (next < 0) {
                    fullscreen = false
                    return@launch
                }
                cursor = next
            }
            fullscreen = false
        }
    }

    BackHandler(enabled = fullscreen) {
        playbackJob?.cancel()
        player.stop()
        notice = null
        fullscreen = false
    }

    AnimatedVisibility(
        visible = fullscreen && selectedIndex in channels.indices,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TvV8Player(
            player = player,
            channel = channels.getOrNull(selectedIndex) ?: StreamItem("", ""),
            error = error,
            waiting = waiting || !network,
            isPlaying = isPlaying,
            noticeMessage = notice,
            sleepRemaining = sleepRemaining,
            sleepUntil = sleepUntil,
            sleepMinutes = sleepMinutes,
            onBack = {
                playbackJob?.cancel()
                player.stop()
                notice = null
                fullscreen = false
            },
            onPrev = {
                val next = nextIndex(selectedIndex - 1 + channels.size)
                if (next >= 0) startPlayback(next)
            },
            onNext = {
                val next = nextIndex(selectedIndex)
                if (next >= 0) startPlayback(next)
            },
            onPause = { player.toggle() },
            onResetZoom = {
                if (selectedIndex in channels.indices) tvV8ZoomByChannel.remove(channels[selectedIndex].key)
            },
            onSleep = onSleep,
            onCancelSleep = onCancelSleep
        )
    }

    AnimatedVisibility(
        visible = !fullscreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(Modifier.fillMaxSize().background(TvV8Bg)) {
            TvV8Header(
                title = "ТЕЛЕВИЗОР",
                searchOpen = searchOpen,
                query = query,
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
                    query = ""
                    searchOpen = false
                    searchStamp = System.currentTimeMillis()
                },
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        reloadPlaylist()
                        refreshing = false
                    }
                },
                onSettings = settings
            )

            restore?.let { item ->
                TvV8RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = channels.indexOfFirst {
                            it.url == item.url || it.name.equals(item.name, ignoreCase = true)
                        }
                        if (i >= 0) startPlayback(i) else notify("Сохранённый канал больше не найден")
                    },
                    onClose = dismissRestore
                )
            }

            TvV8SearchAutoClose(searchOpen, query, searchStamp) {
                query = ""
                searchOpen = false
            }

            when {
                loading -> TvV8Skeleton()
                channels.isEmpty() -> TvV8ErrorState(loadError ?: "Плейлист пуст", onRetry = {
                    scope.launch { reloadPlaylist() }
                })
                else -> {
                    val ordered = remember(channels, favorites) {
                        channels.sortedWith(
                            compareByDescending<StreamItem> { favorites.contains(it.key) }
                                .thenBy { it.name.lowercase(Locale.ROOT) }
                        )
                    }
                    val filtered = remember(ordered, query) {
                        if (query.isBlank()) ordered else ordered.filter {
                            it.name.contains(query.trim(), true) ||
                                it.groupTitle.orEmpty().contains(query.trim(), true)
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                reloadPlaylist()
                                refreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Ничего не найдено", color = TvV8Gray)
                            }
                        } else {
                            LazyColumn(
                                state = list,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                items(filtered, key = { it.key }) { channel ->
                                    val favorite = favorites.contains(channel.key)
                                    TvV8ChannelRow(
                                        item = channel,
                                        favorite = favorite,
                                        offline = health[channel.url] == AvailabilityStatus.OFFLINE,
                                        onPlay = {
                                            val i = channels.indexOfFirst { it.key == channel.key }
                                            if (i >= 0) startPlayback(i)
                                        },
                                        onFavorite = {
                                            scope.launch {
                                                val newValue = !favorite
                                                store.setFavorite(Section.TV, channel.key, newValue)
                                                favorites = store.favorites(Section.TV)
                                                notify(
                                                    if (newValue) "Добавлено в избранное"
                                                    else "Удалено из избранного"
                                                )
                                            }
                                        }
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

@Composable
private fun TvV8Header(
    title: String,
    searchOpen: Boolean,
    query: String,
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
            Icon(Icons.Default.ArrowBack, "Назад", tint = TvV8Red)
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Поиск…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TvV8Red) },
                trailingIcon = {
                    IconButton(onClick = onSearchClose) {
                        Icon(Icons.Default.Close, null, tint = TvV8Red)
                    }
                }
            )
        } else {
            Text(
                title,
                Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Обновить", tint = TvV8Red)
            }
            IconButton(onClick = onSearchOpen) {
                Icon(Icons.Default.Search, "Поиск", tint = TvV8Red)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Настройки", tint = TvV8Red)
            }
        }
    }
}

@Composable
private fun TvV8ChannelRow(
    item: StreamItem,
    favorite: Boolean,
    offline: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val iconColor by animateColorAsState(
        if (favorite) TvV8Red else Color.White,
        label = "tv-v8-favorite-color"
    )
    val scale by animateFloatAsState(
        if (favorite) 1.12f else 1f,
        animationSpec = spring(),
        label = "tv-v8-favorite-scale"
    )

    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (favorite) TvV8Red.copy(alpha = 0.08f) else TvV8Panel
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvV8RemoteLogo(
                url = item.logoUrl,
                size = 60.dp,
                dimmed = offline
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name.uppercase(Locale.ROOT),
                    color = if (offline) TvV8Gray else Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                item.groupTitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = TvV8Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (offline) {
                    Text("• временно недоступен", color = TvV8Gray, fontSize = 10.sp)
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Избранное",
                    tint = iconColor,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }
        }
    }
}

@Composable
private fun TvV8RemoteLogo(url: String?, size: Dp, dimmed: Boolean) {
    val context = LocalContext.current
    var bitmap by remember(url) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (url.isNullOrBlank()) return@runCatching null
                tvV8LogoClient.newCall(
                    Request.Builder()
                        .url(url)
                        .header("User-Agent", "Radio.TV/8.0")
                        .build()
                ).execute().use { response ->
                    if (!response.isSuccessful) null
                    else response.body?.byteStream()?.use(BitmapFactory::decodeStream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier.size(size).alpha(if (dimmed) 0.4f else 1f),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            Modifier.size(size)
                .background(TvV8PanelAlt, RoundedCornerShape(10.dp))
                .alpha(if (dimmed) 0.5f else 1f),
            contentAlignment = Alignment.Center
        ) {
            Text("NO\nImage", color = TvV8Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TvV8RestoreBanner(
    item: StreamItem,
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = TvV8PanelAlt),
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
                    colors = ButtonDefaults.buttonColors(containerColor = TvV8Red)
                ) { Text("Продолжить", maxLines = 1) }
                OutlinedButton(onClick = onClose, Modifier.weight(1f)) {
                    Text("Закрыть", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TvV8ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = TvV8Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = TvV8Red)) {
            Text("Повторить")
        }
    }
}

@Composable
private fun TvV8Skeleton() {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(10) {
            Box(
                Modifier.fillMaxWidth().height(78.dp)
                    .background(TvV8PanelAlt, RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun TvV8SearchAutoClose(
    open: Boolean,
    query: String,
    stamp: Long,
    close: () -> Unit
) {
    LaunchedEffect(open, query, stamp) {
        if (!open) return@LaunchedEffect
        val current = stamp
        delay(if (query.isBlank()) 5000L else 15000L)
        if (current == stamp) close()
    }
}

@Composable
private fun TvV8Player(
    player: PlayerController,
    channel: StreamItem,
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
    onResetZoom: () -> Unit,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val controller = remember(activity) {
        activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) }
    }

    var controls by remember(channel.key) { mutableStateOf(true) }
    var zoom by remember(channel.key) { mutableFloatStateOf(tvV8ZoomByChannel[channel.key] ?: 1f) }
    var sleepMenu by remember(channel.key) { mutableStateOf(false) }
    var sleepMenuToken by remember(channel.key) { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    LaunchedEffect(controls) {
        if (controls) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            delay(5000L)
            controls = false
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(sleepMenu, sleepMenuToken) {
        if (!sleepMenu) return@LaunchedEffect
        delay(5000L)
        sleepMenu = false
    }

    val playerView = remember(channel.key) {
        PlayerView(context).apply {
            useController = false
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(playerView, player) {
        playerView.player = player.player
        onDispose { playerView.player = null }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = zoom, scaleY = zoom)
                .pointerInput(channel.key) {
                    detectTapGestures(
                        onTap = { controls = !controls },
                        onDoubleTap = {
                            zoom = 1f
                            tvV8ZoomByChannel.remove(channel.key)
                            onResetZoom()
                        }
                    )
                }
                .pointerInput(channel.key) {
                    detectTransformGestures { _, _, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                        tvV8ZoomByChannel[channel.key] = zoom
                    }
                }
        )

        if (!controls) {
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
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        sleepMenu = !sleepMenu
                        sleepMenuToken++
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, "Таймер сна", tint = Color.White)
                }

                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.ArrowBack, "Предыдущий", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    IconButton(onClick = onPause, modifier = Modifier.size(72.dp)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Пауза / Старт",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.PlayArrow, "Следующий", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }

                noticeMessage?.let {
                    Text(
                        it,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(TvV8Red.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (sleepMenu) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        properties = PopupProperties(focusable = false)
                    ) {
                        Card(
                            Modifier
                                .padding(top = 56.dp, end = 8.dp)
                                .heightIn(max = 330.dp),
                            colors = CardDefaults.cardColors(containerColor = TvV8Panel.copy(alpha = 0.92f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("ТАЙМЕР СНА", color = TvV8Red, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 4
                                ) {
                                    TvV8SleepOptions.forEach { (minutes, label) ->
                                        val active = sleepMinutes == minutes && sleepUntil > System.currentTimeMillis()
                                        val background by animateColorAsState(
                                            if (active) TvV8Red else TvV8PanelAlt,
                                            label = "tv-v8-sleep-" + minutes
                                        )
                                        Card(
                                            onClick = {
                                                if (active) {
                                                    onCancelSleep()
                                                } else {
                                                    onSleep(minutes)
                                                }
                                                sleepMenu = false
                                            },
                                            Modifier.size(66.dp),
                                            colors = CardDefaults.cardColors(containerColor = background),
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, if (active) TvV8Red else TvV8Gray)
                                        ) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (active) formatTvV8SleepTime(sleepRemaining) else label,
                                                    color = if (active) Color(0xFFFFD6D6) else Color.White,
                                                    fontSize = if (active) 10.sp else 12.sp,
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

private fun formatTvV8SleepTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return if (seconds >= 3600L) {
        "%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L)
    } else {
        "%02d:%02d".format(seconds / 60L, seconds % 60L)
    }
}

private suspend fun scanTvV8(
    items: List<StreamItem>,
    repo: TvPlaylistRepositoryV8
): Map<String, AvailabilityStatus> = coroutineScope {
    val output = mutableMapOf<String, AvailabilityStatus>()
    val semaphore = Semaphore(4)
    items.map { item ->
        launch(Dispatchers.IO) {
            semaphore.acquire()
            try {
                output[item.url] = repo.checkAvailability(item.url)
            } finally {
                semaphore.release()
            }
        }
    }.forEach { it.join() }
    output
}

@Composable
private fun rememberTvV8NetworkState(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(initialValue = true) {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        fun online(): Boolean {
            return cm.allNetworks.any { network ->
                cm.getNetworkCapabilities(network)?.hasCapability(
                    android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
                ) == true
            }
        }
        value = online()
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { value = online() }
            override fun onLost(network: android.net.Network) { value = online() }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        awaitDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
}
