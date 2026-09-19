@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.offex7.streamhub

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

private val Red = Color(0xFFE53935)
private val Bg = Color(0xFF121212)
private val Panel = Color(0xFF1A1A1A)
private val PanelAlt = Color(0xFF232323)
private val Gray = Color(0xFF808080)

private const val TELEGRAM = "https://t.me/TvRadioOnline"
private const val WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"
private const val RESTORE_WINDOW = 10 * 60 * 1000L
private const val ERROR_COOLDOWN = 5 * 60 * 1000L
private val zoomByChannel = mutableMapOf<String, Float>()
private val logoHttpClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).callTimeout(7, TimeUnit.SECONDS).build()

class MainActivity : ComponentActivity() {
    private lateinit var store: SettingsStore
    private lateinit var tv: PlayerController
    private lateinit var radio: RadioMediaController
    internal var pipEnabled = true
    internal var tvViewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AColor.TRANSPARENT
        window.navigationBarColor = AColor.TRANSPARENT
        window.setNavigationBarDividerColor(AColor.TRANSPARENT)
        store = SettingsStore(applicationContext)
        tv = PlayerController(applicationContext)
        radio = RadioMediaController(applicationContext)
        setContent { AppTheme { App(store, tv, radio, this) } }
    }

    override fun onStop() {
        lifecycleScope.launch { store.setLastExitTime(System.currentTimeMillis()) }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        if (tvViewing && pipEnabled && Build.VERSION.SDK_INT >= 26 && !isInPictureInPictureMode) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        }
        super.onUserLeaveHint()
    }

    override fun onDestroy() {
        tv.release()
        radio.release()
        super.onDestroy()
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Red,
            secondary = Red,
            background = Bg,
            surface = Panel,
            surfaceVariant = PanelAlt
        ),
        content = content
    )
}

@Composable
private fun App(
    store: SettingsStore,
    tv: PlayerController,
    radio: RadioMediaController,
    activity: MainActivity
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var disclaimer by rememberSaveable { mutableStateOf(false) }
    var exit by rememberSaveable { mutableStateOf(false) }
    var restore by remember { mutableStateOf<StreamItem?>(null) }
    var pip by remember { mutableStateOf(true) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableLongStateOf(0L) }
    var sleepMinutes by remember { mutableLongStateOf(0L) }

    fun notify(message: String) { scope.launch { snack.showSnackbar(message, duration = SnackbarDuration.Short) } }

    LaunchedEffect(Unit) {
        pip = store.pipEnabled()
        activity.pipEnabled = pip
        val last = store.lastExitTime()
        val savedSection = store.lastSection()
        if (savedSection != null && last > 0L && System.currentTimeMillis() - last < RESTORE_WINDOW) {
            section = savedSection
            restore = store.lastStream(savedSection)
        }
    }

    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) {
            sleepRemaining = 0L
            return@LaunchedEffect
        }
        while (true) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                sleepUntil = 0L
                sleepRemaining = 0L
                sleepMinutes = 0L
                tv.stop()
                radio.stop()
                notify("Таймер сна — отключён")
                break
            }
            sleepRemaining = left
            delay(1000L)
        }
    }

    fun leaveSection() {
        section = null
        settings = false
        disclaimer = false
        restore = null
        activity.tvViewing = false
        tv.stop()
        radio.stop()
        scope.launch { store.clearSection() }
    }

    BackHandler {
        when {
            disclaimer -> disclaimer = false
            settings -> settings = false
            section != null -> leaveSection()
            else -> exit = true
        }
    }

    Scaffold(snackbarHost = {}, containerColor = Bg, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
            )
        ) {
            when {
                disclaimer -> Disclaimer { disclaimer = false }
                settings -> Settings(
                    store = store,
                    pip = pip,
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepMinutes = sleepMinutes,
                    onBack = { settings = false },
                    onPip = {
                        pip = it
                        activity.pipEnabled = it
                        scope.launch { store.setPipEnabled(it) }
                    },
                    onSleep = {
                        sleepMinutes = it
                        sleepUntil = System.currentTimeMillis() + it * 60_000L
                        notify("Таймер сна — запущен")
                    },
                    onCancelSleep = {
                        sleepUntil = 0L
                        sleepRemaining = 0L
                        sleepMinutes = 0L
                        notify("Таймер сна — отключён")
                    },
                    onResetStats = { target ->
                        scope.launch {
                            runCatching { store.resetUsage(target) }
                                .onSuccess { notify(if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен") }
                                .onFailure { notify("Не удалось сбросить счётчик") }
                        }
                    },
                    onSource = {
                        scope.launch {
                            store.setSourceIndex(it)
                            notify("Источник сохранён")
                        }
                    },
                    onDisclaimer = { disclaimer = true },
                    onResetAll = {
                        scope.launch {
                            runCatching { store.resetAll() }
                                .onSuccess {
                                    sleepUntil = 0L
                                    sleepRemaining = 0L
                                    sleepMinutes = 0L
                                    pip = true
                                    activity.pipEnabled = true
                                    section = null
                                    settings = false
                                    disclaimer = false
                                    restore = null
                                    notify("Настройки сброшены")
                                }
                                .onFailure { notify("Не удалось сбросить настройки") }
                        }
                    }
                )
                section == null -> Home(
                    open = {
                        section = it
                        scope.launch { store.setSection(it) }
                    },
                    settings = { settings = true }
                )
                section == Section.TV -> Tv(
                    player = tv,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = {
                        restore = null
                        scope.launch { store.saveLastStream(Section.TV, it) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true },
                    fullChanged = { activity.tvViewing = it },
                    notify = ::notify
                )
                else -> Radio(
                    player = radio,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = {
                        restore = null
                        scope.launch { store.saveLastStream(Section.RADIO, it) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true },
                    notify = ::notify
                )
            }

            AnimatedVisibility(
                visible = snack.currentSnackbarData != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut()
            ) { SnackbarHost(hostState = snack, modifier = Modifier.fillMaxWidth()) }
        }
    }

    if (exit) {
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Выйти из приложения?") },
            text = { Text("Закрыть TV / Radio. Online?") },
            confirmButton = { TextButton(onClick = { activity.finishAndRemoveTask() }) { Text("Выйти", color = Red) } },
            dismissButton = { TextButton(onClick = { exit = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun Home(open: (Section) -> Unit, settings: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text("TV / Radio. Online", color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                IconButton(onClick = settings) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
            }
            if (LocalConfiguration.current.screenWidthDp >= 560) {
                Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.weight(1f)) { open(Section.TV) }
                    HomeCard("РАДИО", R.drawable.start_radio, Modifier.weight(1f)) { open(Section.RADIO) }
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.fillMaxWidth().weight(1f)) { open(Section.TV) }
                    HomeCard("РАДИО", R.drawable.start_radio, Modifier.fillMaxWidth().weight(1f)) { open(Section.RADIO) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(
                onClick = { openUrl(context, TELEGRAM) },
                Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HomeCard(title: String, logo: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(logo),
                    contentDescription = title,
                    modifier = Modifier.fillMaxHeight(0.82f).aspectRatio(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    timer: String,
    back: () -> Unit,
    settings: (() -> Unit)?,
    searchOpen: Boolean,
    query: String,
    onSearchOpen: () -> Unit,
    onQuery: (String) -> Unit,
    onSearchClose: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    showSearch: Boolean = true,
    showTimer: Boolean = true
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (showTimer && timer.isNotBlank()) Text(timer, fontSize = 11.sp, color = Red)
            if (!searchOpen) {
                onRefresh?.let { IconButton(onClick = it) { Icon(Icons.Default.Refresh, "Обновить", tint = Red) } }
                if (showSearch) IconButton(onClick = onSearchOpen) { Icon(Icons.Default.Search, "Поиск", tint = Red) }
                settings?.let { IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) } }
            } else if (showSearch) {
                IconButton(onClick = onSearchClose) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
            }
        }
        if (searchOpen && showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                singleLine = true,
                maxLines = 1,
                placeholder = { Text("Поиск…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = onSearchClose) { Icon(Icons.Default.Close, null) } }
            )
        }
    }
}

@Composable
private fun SearchAutoClose(open: Boolean, query: String, stamp: Long, close: () -> Unit) {
    LaunchedEffect(open, query, stamp) {
        if (!open) return@LaunchedEffect
        val current = stamp
        delay(if (query.isBlank()) 5000L else 15000L)
        if (open && current == stamp) close()
    }
}

@Composable
private fun Radio(
    player: RadioMediaController,
    store: SettingsStore,
    restore: StreamItem?,
    dismissRestore: () -> Unit,
    saveLast: (StreamItem) -> Unit,
    back: () -> Unit,
    settings: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val index by player.currentIndex.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val error by player.error.collectAsState()
    val network by rememberNetworkState()
    val list = rememberLazyListState()
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var listenedSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var previousIndex by remember { mutableIntStateOf(index) }

    KeepSystemBarsVisible()

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.RADIO)
        val saved = store.scrollPosition(Section.RADIO)
        list.scrollToItem(saved.first.coerceAtMost(RADIO_STATIONS.lastIndex), saved.second)
    }

    LaunchedEffect(index) {
        if (index != previousIndex) listenedSeconds = 0L
        previousIndex = index
    }

    LaunchedEffect(playing, index) {
        while (playing && index >= 0) {
            delay(1000L)
            if (player.isPlaying.value && player.currentIndex.value == index) {
                listenedSeconds++
            }
        }
    }

    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.RADIO,
            channelIdProvider = { RADIO_STATIONS.getOrNull(player.currentIndex.value)?.name },
            activeProvider = { player.isPlaying.value && player.currentIndex.value >= 0 }
        ).run()
    }

    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) -> store.saveScrollPosition(Section.RADIO, first, offset) }
    }

    LaunchedEffect(error) {
        if (error != null) notify("Возникла проблема. Обсуждаем решения в Telegram.")
    }

    val current = RADIO_STATIONS.getOrNull(index)
    val orderedStations = remember(favorites) {
        RADIO_STATIONS.sortedWith(
            compareByDescending<StreamItem> { favorites.contains(it.key) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            if (!network) NetworkBanner()

            Header(
                title = "РАДИО",
                timer = "",
                back = back,
                settings = settings,
                searchOpen = false,
                query = "",
                onSearchOpen = {},
                onQuery = {},
                onSearchClose = {},
                showSearch = false,
                showTimer = false
            )

            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = RADIO_STATIONS.indexOfFirst { station -> station.url == item.url }
                        if (i >= 0) {
                            player.play(i)
                            saveLast(RADIO_STATIONS[i])
                        }
                    },
                    onClose = dismissRestore
                )
            }

            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(orderedStations, key = { it.key }) { station ->
                    val favorite = favorites.contains(station.key)
                    val active = station.url == current?.url
                    ChannelRow(
                        item = station,
                        favorite = favorite,
                        playing = active && playing,
                        offline = false,
                        onPlay = {
                            val i = RADIO_STATIONS.indexOf(station)
                            if (i >= 0) {
                                player.play(i)
                                saveLast(station)
                            }
                        },
                        onFavorite = {
                            scope.launch {
                                runCatching {
                                    store.setFavorite(Section.RADIO, station.key, !favorite)
                                    favorites = store.favorites(Section.RADIO)
                                    notify(if (!favorite) "Добавлено в избранное" else "Удалено из избранного")
                                }.onFailure {
                                    notify("Не удалось обновить избранное")
                                }
                            }
                        },
                        logoSize = 72.dp,
                        isRadio = true,
                        activeRadio = active,
                        radioStatus = if (active) {
                            if (playing) "играет" else "пауза"
                        } else null,
                        radioTimer = if (active) formatTime(listenedSeconds) else null,
                        onPrev = if (active) ({ player.previous() }) else null,
                        onToggle = if (active) ({ player.toggle() }) else null,
                        onNext = if (active) ({ player.next() }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun Tv(
    player: PlayerController,
    store: SettingsStore,
    restore: StreamItem?,
    dismissRestore: () -> Unit,
    saveLast: (StreamItem) -> Unit,
    back: () -> Unit,
    settings: () -> Unit,
    fullChanged: (Boolean) -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = remember(context) { PlaylistRepository(context.applicationContext) }
    val list = rememberLazyListState()
    val network by rememberNetworkState()
    val error by player.error.collectAsState()
    val waiting by player.waitingForNetwork.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var health by remember { mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var full by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var fallbackMessage by remember { mutableStateOf<String?>(null) }
    var playbackJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    KeepSystemBarsVisible()

    suspend fun loadSource() {
        val source = store.sourceIndex()
        repo.loadCached(source)?.let { channels = it; loading = false }
        repo.loadSource(source)
            .onSuccess { channels = it; loading = false; repo.warmFallbacks() }
            .onFailure { loading = false; notify("Возникла проблема. Обсуждаем решения в Telegram.") }
    }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.TV)
        val saved = store.scrollPosition(Section.TV)
        list.scrollToItem(saved.first.coerceAtLeast(0), saved.second)
        loadSource()
    }
    LaunchedEffect(Unit) {
        UsageTicker(
            store,
            Section.TV,
            channelIdProvider = { channels.getOrNull(selected)?.name },
            activeProvider = { full && player.isPlaying.value }
        ).run()
    }
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) -> store.saveScrollPosition(Section.TV, first, offset) }
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) health = scanAvailability(channels.take(18), health)
    }
    LaunchedEffect(list.firstVisibleItemIndex, channels, health) {
        if (channels.isEmpty()) return@LaunchedEffect
        val first = list.firstVisibleItemIndex.coerceIn(0, channels.lastIndex)
        val end = minOf(channels.size, first + 20)
        if (end > first) {
            val pending = channels.subList(first, end).filter { health[it.url] == null }
            if (pending.isNotEmpty()) health = scanAvailability(pending, health)
        }
    }
    LaunchedEffect(error) { if (error != null && full) notify("Возникла проблема. Обсуждаем решения в Telegram.") }
    SearchAutoClose(searchOpen, query, stamp) {
        query = ""
        searchOpen = false
    }
    BackHandler(enabled = searchOpen && !full) {
        query = ""
        searchOpen = false
        stamp = System.currentTimeMillis()
    }
    LaunchedEffect(full) { fullChanged(full) }

    fun candidates(item: StreamItem): List<String> = listOf(item.url) + repo.fallbackUrlsFor(item.name)

    fun previousIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start
        repeat(channels.size - 1) {
            cursor = (cursor - 1 + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun nextIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start
        repeat(channels.size - 1) {
            cursor = (cursor + 1 + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun startPlayback(startIndex: Int) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            if (channels.isEmpty()) return@launch
            var cursor = startIndex.coerceIn(0, channels.lastIndex)
            repeat(channels.size) {
                selected = cursor
                fallbackMessage = null
                val result = player.playWithFallback(candidates(channels[cursor]))
                if (result >= 0) {
                    saveLast(channels[cursor])
                    return@launch
                }
                fallbackMessage = "Канал временно недоступен — переключаю на следующий"
                delay(3000L)
                cursor = nextIndex(cursor)
                if (cursor < 0) {
                    full = false
                    fallbackMessage = null
                    return@launch
                }
            }
            full = false
            fallbackMessage = null
        }
    }

    if (full && selected in channels.indices) {
        TvPlayer(
            player = player,
            channel = channels[selected],
            error = if (fallbackMessage == null) error else null,
            waiting = waiting || !network,
            isPlaying = isPlaying,
            noticeMessage = fallbackMessage,
            onBack = { playbackJob?.cancel(); full = false },
            onSettings = { playbackJob?.cancel(); full = false; settings() },
            onPrev = {
                val i = previousIndex(selected)
                if (i >= 0) startPlayback(i)
            },
            onNext = {
                val i = nextIndex(selected)
                if (i >= 0) startPlayback(i)
            },
            onPause = { player.toggle() },
            onResetZoom = { zoomByChannel.remove(channels[selected].key) }
        )
        return
    }

    val ordered = channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.key) }.thenBy { it.name.lowercase(Locale.ROOT) })
    val filtered = fuzzyFilter(ordered, query)

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            Header(
                title = "ТЕЛЕВИЗОР",
                timer = "",
                back = back,
                settings = settings,
                searchOpen = searchOpen,
                query = query,
                onSearchOpen = { searchOpen = true; stamp = System.currentTimeMillis() },
                onQuery = { query = it; stamp = System.currentTimeMillis() },
                onSearchClose = { query = ""; searchOpen = false; stamp = System.currentTimeMillis() },
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        repo.loadSource(store.sourceIndex())
                            .onSuccess { channels = it; health = emptyMap(); repo.warmFallbacks() }
                            .onFailure { notify("Возникла проблема. Обсуждаем решения в Telegram.") }
                        refreshing = false
                    }
                }
            )
            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = channels.indexOfFirst { it.url == item.url || it.name.equals(item.name, ignoreCase = true) }
                        if (i >= 0) { selected = i; full = true; startPlayback(i) }
                        else notify("Сохранённый канал больше не найден")
                    },
                    onClose = dismissRestore
                )
            }
            when {
                loading -> SkeletonList()
                filtered.isEmpty() -> if (query.isBlank()) EmptyPlaylistState() else EmptySearchState()
                else -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            repo.loadSource(store.sourceIndex())
                                .onSuccess { channels = it; health = emptyMap(); repo.warmFallbacks() }
                                .onFailure { notify("Возникла проблема. Обсуждаем решения в Telegram.") }
                            refreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.key }) { channel ->
                            val offline = health[channel.url] == AvailabilityStatus.OFFLINE
                            val favorite = favorites.contains(channel.key)
                            ChannelRow(
                                item = channel,
                                favorite = favorite,
                                playing = false,
                                offline = offline,
                                onPlay = {
                                    val i = channels.indexOfFirst { it.key == channel.key }
                                    if (i >= 0) { selected = i; full = true; startPlayback(i) }
                                },
                                onFavorite = {
                                    scope.launch {
                                        runCatching {
                                            store.setFavorite(Section.TV, channel.key, !favorite)
                                            favorites = store.favorites(Section.TV)
                                            notify(if (!favorite) "Добавлено в избранное" else "Удалено из избранного")
                                        }.onFailure { notify("Не удалось обновить избранное") }
                                    }
                                },
                                logoSize = 56.dp,
                                preferRemoteLogo = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlayer(
    player: PlayerController,
    channel: StreamItem,
    error: String?,
    waiting: Boolean,
    isPlaying: Boolean,
    noticeMessage: String?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPause: () -> Unit,
    onResetZoom: () -> Unit
) {
    val activity = LocalContext.current as? ComponentActivity
    val portrait = LocalConfiguration.current.screenWidthDp < LocalConfiguration.current.screenHeightDp
    val haptic = LocalHapticFeedback.current
    var controls by remember(channel.key) { mutableStateOf(true) }
    var taps by remember(channel.key) { mutableIntStateOf(0) }
    var zoom by remember(channel.key) { mutableFloatStateOf(zoomByChannel[channel.key] ?: 1f) }
    val controller = remember(activity) { activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) } }

    fun showBars() { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    fun hideBars() { controller?.hide(WindowInsetsCompat.Type.systemBars()) }

    LaunchedEffect(Unit) { hideBars() }
    LaunchedEffect(controls) {
        if (controls) {
            showBars()
            delay(5000L)
            controls = false
            hideBars()
        } else hideBars()
    }
    DisposableEffect(Unit) { onDispose { showBars() } }
    BackHandler { onBack() }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(channel.key) {
                detectTapGestures(onTap = {
                    controls = true
                    taps++
                    if (taps >= 3) {
                        zoom = 1f
                        zoomByChannel.remove(channel.key)
                        onResetZoom()
                        taps = 0
                    }
                    showBars()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                })
            }
            .pointerInput(channel.key) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                    zoomByChannel[channel.key] = zoom
                    controls = true
                    showBars()
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player.player
                    setShutterBackgroundColor(AColor.BLACK)
                    setKeepContentOnPlayerReset(true)
                }
            }
        )
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut()
        ) {
            Box(Modifier.fillMaxSize().padding(top = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = { showBars(); onBack() }) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red, modifier = Modifier.size(28.dp)) }
                    Row {
                        IconButton(onClick = { zoom = 1f; zoomByChannel.remove(channel.key); showBars(); onResetZoom() }) { Icon(Icons.Default.Refresh, "Сбросить зум", tint = Red, modifier = Modifier.size(26.dp)) }
                        IconButton(onClick = { showBars(); onSettings() }) { Icon(Icons.Default.Settings, "Настройки", tint = Red, modifier = Modifier.size(26.dp)) }
                    }
                }
                Row(
                    Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    IconButton(onClick = { showBars(); onPrev() }, Modifier.size(58.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Предыдущий канал", tint = Red, modifier = Modifier.size(42.dp))
                    }
                    IconButton(onClick = { showBars(); onPause() }, Modifier.size(78.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Пауза", tint = Red, modifier = Modifier.size(60.dp))
                    }
                    IconButton(onClick = { showBars(); onNext() }, Modifier.size(58.dp)) {
                        Icon(Icons.Default.SkipNext, "Следующий канал", tint = Red, modifier = Modifier.size(42.dp))
                    }
                }
                Text(
                    channel.name,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    color = Color.White,
                    fontSize = if (portrait) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        noticeMessage?.let { Text(it, Modifier.align(Alignment.Center).padding(horizontal = 24.dp), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
        if (waiting) Text("Сигнал утерян, перепроверьте подключение к сети", Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (error != null && noticeMessage == null) Text("Возникла проблема. Обсуждаем решения в Telegram.", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Settings(
    store: SettingsStore,
    pip: Boolean,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onBack: () -> Unit,
    onPip: (Boolean) -> Unit,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    onResetStats: (Section) -> Unit,
    onSource: (Int) -> Unit,
    onDisclaimer: () -> Unit,
    onResetAll: () -> Unit
) {
    val tvUsage by store.usageFlow(Section.TV).collectAsState(0L)
    val radioUsage by store.usageFlow(Section.RADIO).collectAsState(0L)
    val tvChannels by store.channelUsageFlow(Section.TV).collectAsState(emptyMap())
    val radioStations by store.channelUsageFlow(Section.RADIO).collectAsState(emptyMap())
    var source by remember { mutableIntStateOf(0) }
    var sourceDialog by remember { mutableStateOf(false) }

    KeepSystemBarsVisible()
    LaunchedEffect(Unit) { source = store.sourceIndex() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
                Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Таймер сна", color = Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SleepGrid(sleepRemaining, sleepUntil, sleepMinutes, onSleep, onCancelSleep)
                }
            }
        }
        item { DonationCard() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Статистика", color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    UsageLine("Общее время использования ТВ", tvUsage)
                    OutlinedButton(onClick = { onResetStats(Section.TV) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик ТВ") }
                    UsageLine("Общее время использования Радио", radioUsage)
                    OutlinedButton(onClick = { onResetStats(Section.RADIO) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик Радио") }
                    Spacer(Modifier.height(6.dp))
                    Text("Топ-3 каналов", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("ТВ", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                    TopStats(tvChannels)
                    Spacer(Modifier.height(4.dp))
                    Text("Радио", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                    TopStats(radioStations)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Источник ТВ-плейлиста", color = Red, fontWeight = FontWeight.Bold)
                    Text(TV_SOURCES.getOrNull(source)?.name.orEmpty(), color = Color.Gray, fontSize = 12.sp)
                    Button(onClick = { sourceDialog = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Выбрать источник") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("PiP при сворачивании", fontSize = 16.sp)
                        Text("Сохраняется после перезапуска", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = pip,
                        onCheckedChange = onPip,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray)
                    )
                }
            }
        }
        item { Button(onClick = onResetAll, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)) { Text("Сбросить настройки до заводских") } }
        item { OutlinedButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности") } }
    }

    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false },
            title = { Text("Источник ТВ-плейлиста") },
            text = {
                Column {
                    TV_SOURCES.forEachIndexed { i, item ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick = { source = i; sourceDialog = false; onSource(i) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = source == i, onClick = { source = i; sourceDialog = false; onSource(i) })
                            Text(item.name, Modifier.padding(6.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun UsageLine(label: String, seconds: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.LightGray, modifier = Modifier.weight(1f), maxLines = 2)
        Text(formatUsage(seconds), color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TopStats(stats: Map<String, Long>) {
    val top = stats.entries.filter { it.value > 0L }.sortedByDescending { it.value }.take(3)
    if (top.isEmpty()) {
        Text("Пока нет данных", color = Color.Gray, fontSize = 12.sp)
    } else {
        top.forEachIndexed { i, entry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. ${entry.key}", Modifier.weight(1f), color = Color.LightGray, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(formatUsage(entry.value), color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val options = listOf(
        5L to "5 мин", 10L to "10 мин", 15L to "15 мин", 30L to "30 мин", 60L to "1 ч", 120L to "2 ч", 240L to "4 ч",
        480L to "8 ч", 600L to "10 ч", 900L to "15 ч", 1440L to "24 ч", 2160L to "36 ч"
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (minutes, label) ->
            val active = minutes == duration && until > System.currentTimeMillis()
            val background by animateColorAsState(if (active) Red else PanelAlt, label = "sleep-$minutes")
            Card(
                onClick = { if (active) onCancel() else onSelect(minutes) },
                modifier = Modifier.size(66.dp),
                colors = CardDefaults.cardColors(containerColor = background),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (active) Red else Gray)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (active) formatTimerCircle(remaining) else label,
                        color = if (active) Color(0xFFFFD6D6) else Color.White,
                        fontSize = if (active) 11.sp else 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DonationCard() {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Red.copy(alpha = .4f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = Red)
                Spacer(Modifier.width(8.dp))
                Text("Поддержать проект", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Red)
            }
            Spacer(Modifier.height(8.dp))
            Text(WALLET, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Wallet", WALLET))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Копировать") }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.size(180.dp), color = Color.White, shape = RoundedCornerShape(8.dp)) {
                DonationQrImage(
                    contentDescription = "QR-код для пожертвований",
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun DonationQrImage(
    contentDescription: String,
    modifier: Modifier
) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                LocalContext.current.assets.open("qr_donate.png").use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } ?: Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun Disclaimer(onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
                Text("Отказ от ответственности", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text(
                "Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.\n\nПриложение не хранит, не распространяет и не модифицирует транслируемый контент. Все трансляции предоставляются третьими лицами. Разработчик не несёт ответственности за содержание транслируемого контента.\n\nЕсли вы являетесь правообладателем и считаете, что ваши права нарушаются — свяжитесь с нами через Telegram: $TELEGRAM",
                color = Color.LightGray,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
        item {
            OutlinedButton(onClick = { openUrl(context, TELEGRAM) }, Modifier.fillMaxWidth()) { Text("Telegram") }
        }
    }
}

@Composable
private fun ChannelRow(
    item: StreamItem,
    favorite: Boolean,
    playing: Boolean,
    offline: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    logoSize: Dp,
    isRadio: Boolean = false,
    activeRadio: Boolean = false,
    radioStatus: String? = null,
    radioTimer: String? = null,
    onPrev: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    preferRemoteLogo: Boolean = false
) {
    val iconColor by animateColorAsState(if (favorite) Red else Color.White, label = "favorite-color")
    val scale by animateFloatAsState(if (favorite) 1.14f else 1f, animationSpec = spring(), label = "favorite-scale")
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (favorite) Red.copy(alpha = .08f) else Panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(
                item = item,
                size = logoSize,
                dimmed = offline,
                preferRemote = preferRemoteLogo,
                overlayText = radioTimer,
                activeRadio = activeRadio
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name.uppercase(Locale.ROOT),
                    color = if (offline) Gray else Color.White,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                radioStatus?.let { Text(it, color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                if (offline && !isRadio) Text("• временно недоступен", color = Gray, fontSize = 10.sp)
            }
            if (isRadio && activeRadio && onPrev != null && onToggle != null && onNext != null) {
                IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipPrevious, "Назад", tint = Red, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Старт / пауза", tint = Red, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipNext, "Вперёд", tint = Red, modifier = Modifier.size(20.dp)) }
            }
            IconButton(onClick = onFavorite, Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = iconColor)
            }
        }
    }
}

@Composable
private fun LogoImage(
    item: StreamItem,
    size: Dp,
    dimmed: Boolean = false,
    preferRemote: Boolean = false,
    overlayText: String? = null,
    activeRadio: Boolean = false
) {
    val context = LocalContext.current
    val resourceName = localLogoName(item.name)
    val resourceId = remember(resourceName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }

    val pulseTransition = rememberInfiniteTransition(label = "radio-logo-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radio-logo-pulse-value"
    )

    val targetScale = if (activeRadio) 1.05f + (0.03f * pulse) else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(),
        label = "active-logo-scale"
    )
    val borderColor = if (activeRadio) {
        Red.copy(alpha = 0.45f + (0.25f * pulse))
    } else {
        Color.Transparent
    }

    Box(
        Modifier.size(size).graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.BottomCenter
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .alpha(if (dimmed) .4f else 1f)
            .then(
                if (activeRadio) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
                } else Modifier
            )

        val remoteUrl = item.logoUrl ?: item.epgLogoUrl
        if (preferRemote && !remoteUrl.isNullOrBlank()) {
            RemoteLogoImage(remoteUrl, item.name, imageModifier)
        } else if (resourceId != 0) {
            Image(
                painter = painterResource(resourceId),
                contentDescription = item.name,
                modifier = imageModifier
            )
        } else {
            Box(
                imageModifier.background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }

        overlayText?.let {
            Surface(
                color = Color.Black.copy(alpha = .78f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    it,
                    color = Color(0xFF66BB6A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RemoteLogoImage(
    url: String,
    contentDescription: String,
    modifier: Modifier
) {
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                logoHttpClient.newCall(
                    Request.Builder().url(url).get().build()
                ).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use(BitmapFactory::decodeStream)?.asImageBitmap()
                    } else null
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } ?: Box(
        modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

private fun localLogoName(name: String): String = when (name.uppercase(Locale.ROOT)) {
    "RECORD" -> "logo_record"
    "CHOCOLATE" -> "logo_chocolate"
    "ЭНЕРДЖИ" -> "logo_energy"
    "ULTRA" -> "logo_ultra"
    "КАЛЬЯН РЭП" -> "logo_kalyan"
    "PIRATE STATION" -> "logo_pirate"
    "VOCAL DRUM" -> "logo_vocal"
    "CHILL HOUSE" -> "logo_chill"
    "PSY TRANCE" -> "logo_psy"
    "METALCORE" -> "logo_metalcore"
    "RELAX" -> "logo_relax"
    "COMEDY CLUB" -> "logo_comedy"
    "АВТОРАДИО" -> "logo_autoradio"
    "ЮГ МОЛОДОЙ" -> "logo_yug"
    else -> "logo_fallback"
}

@Composable
private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PanelAlt),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Продолжить [${item.name}]?", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onContinue, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                    Text("Продолжить", maxLines = 1)
                }
                OutlinedButton(onClick = onClose, Modifier.weight(1f)) { Text("Закрыть", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun NetworkBanner() {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF2B1B1B)).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WifiOff, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Сигнал утерян, перепроверьте подключение к сети", color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
private fun NoticeBanner(notice: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = notice != null,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut()
    ) {
        Card(
            Modifier.fillMaxWidth().padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = PanelAlt),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(notice.orEmpty(), Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { openUrl(context, TELEGRAM); onDismiss() }) { Text("Перейти", color = Red) }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text("Ничего не найдено", color = Color.Gray)
    }
}

@Composable
private fun EmptyPlaylistState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text("Плейлист пуст", color = Color.Gray)
    }
}

@Composable
private fun SkeletonList() {
    LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(8) {
            Box(Modifier.fillMaxWidth().height(82.dp).background(PanelAlt, RoundedCornerShape(12.dp)))
        }
    }
}

private fun fuzzyFilter(items: List<StreamItem>, query: String): List<StreamItem> =
    if (query.isBlank()) items else items.filter { it.name.contains(query.trim(), ignoreCase = true) }

@Composable
private fun KeepSystemBarsVisible() {
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity) {
        val controller = activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) }
        controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun formatTime(seconds: Long): String {
    return if (seconds < 3600L) {
        "%02d:%02d".format(seconds / 60L, seconds % 60L)
    } else {
        "%02d:%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
    }
}

private fun formatUsage(seconds: Long): String = "${seconds / 3600L} ч ${(seconds / 60L) % 60L} мин"

private fun formatTimerCircle(milliseconds: Long): String {
    val total = max(0L, milliseconds / 1000L)
    val hours = total / 3600L
    val minutes = (total / 60L) % 60L
    val seconds = total % 60L
    return if (hours > 0L) "%02d:%02d".format(hours, minutes) else "%02d:%02d".format(minutes, seconds)
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun networkNow(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    return manager.allNetworks.any { network ->
        manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
}

@Composable
private fun rememberNetworkState(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(initialValue = networkNow(context)) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        value = networkNow(context)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = networkNow(context) }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        awaitDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}

private suspend fun scanAvailability(
    items: List<StreamItem>,
    current: Map<String, AvailabilityStatus>
): Map<String, AvailabilityStatus> {
    if (items.isEmpty()) return current
    val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
    val semaphore = Semaphore(3)
    val output = ConcurrentHashMap<String, AvailabilityStatus>()
    kotlinx.coroutines.coroutineScope {
        items.map { item ->
            launch(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    output[item.url] = runCatching {
                        client.newCall(
                            Request.Builder().url(item.url)
                                .header("Range", "bytes=0-1")
                                .header("User-Agent", "TV-Radio-Online/5.0")
                                .get().build()
                        ).execute().use {
                            if (it.isSuccessful || it.code in 300..399 || it.code == 416) AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE
                        }
                    }.getOrDefault(AvailabilityStatus.OFFLINE)
                } finally { semaphore.release() }
            }
        }.forEach { it.join() }
    }
    return current + output
}
private class UsageTicker(
    private val store: SettingsStore,
    private val section: Section,
    private val channelIdProvider: () -> String?,
    private val activeProvider: () -> Boolean
) {
    suspend fun run() {
        val pending = mutableMapOf<String, Long>()
        var totalPending = 0L
        var lastFlush = System.currentTimeMillis()
        try {
            while (true) {
                delay(1000L)
                if (activeProvider()) {
                    val id = channelIdProvider()?.trim().orEmpty()
                    if (id.isNotBlank()) {
                        pending[id] = (pending[id] ?: 0L) + 1L
                        totalPending++
                    }
                }
                if (System.currentTimeMillis() - lastFlush >= 5000L) {
                    if (totalPending > 0L) {
                        store.addUsageSeconds(section, totalPending)
                        store.addChannelUsage(section, pending.toMap())
                    }
                    pending.clear()
                    totalPending = 0L
                    lastFlush = System.currentTimeMillis()
                }
            }
        } finally {
            if (totalPending > 0L) {
                store.addUsageSeconds(section, totalPending)
                store.addChannelUsage(section, pending.toMap())
            }
        }
    }
}

