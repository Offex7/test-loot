package com.offex7.streamhub

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
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
                snack.showSnackbar("Таймер сна — отключён", duration = SnackbarDuration.Short)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Surface(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                .padding(padding),
            color = Bg
        ) {
            when {
                disclaimer -> Disclaimer { disclaimer = false }
                settings -> Settings(
                    store = store,
                    pip = pip,
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepMinutes = sleepMinutes,
                    onPip = { enabled ->
                        pip = enabled
                        activity.pipEnabled = enabled
                        scope.launch { store.setPipEnabled(enabled) }
                    },
                    onSleep = { minutes ->
                        sleepMinutes = minutes
                        sleepUntil = System.currentTimeMillis() + minutes * 60_000L
                        scope.launch { snack.showSnackbar("Таймер сна — запущен", duration = SnackbarDuration.Short) }
                    },
                    onCancelSleep = {
                        sleepUntil = 0L
                        sleepRemaining = 0L
                        sleepMinutes = 0L
                        snack.showSnackbar("Таймер сна — отключён", duration = SnackbarDuration.Short)
                    },
                    onResetStats = { target ->
                        scope.launch {
                            runCatching { store.resetUsage(target) }
                                .onSuccess {
                                    snack.showSnackbar(
                                        if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                .onFailure {
                                    snack.showSnackbar("Не удалось сбросить счётчик", duration = SnackbarDuration.Short)
                                }
                        }
                    },
                    onSource = { index ->
                        scope.launch {
                            store.setSourceIndex(index)
                            snack.showSnackbar("Источник сохранён", duration = SnackbarDuration.Short)
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
                                    snack.showSnackbar("Настройки сброшены", duration = SnackbarDuration.Short)
                                }
                                .onFailure {
                                    snack.showSnackbar("Не удалось сбросить настройки", duration = SnackbarDuration.Short)
                                }
                        }
                    }
                )
                section == null -> Home(
                    open = { target ->
                        section = target
                        scope.launch { store.setSection(target) }
                    },
                    settings = { settings = true }
                )
                section == Section.TV -> Tv(
                    player = tv,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = { item ->
                        restore = null
                        scope.launch { store.saveLastStream(Section.TV, item) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true },
                    fullChanged = { activity.tvViewing = it }
                )
                else -> Radio(
                    player = radio,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = { item ->
                        restore = null
                        scope.launch { store.saveLastStream(Section.RADIO, item) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true }
                )
            }
        }
    }

    if (exit) {
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Выйти из приложения?") },
            text = { Text("Закрыть TV / Radio. Online?") },
            confirmButton = {
                TextButton(onClick = { activity.finishAndRemoveTask() }) { Text("Выйти", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { exit = false }) { Text("Отмена") }
            }
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
                Text("TV / Radio. Online • V4.0", color = Color.Gray, fontSize = 10.sp)
                IconButton(onClick = settings) {
                    Icon(Icons.Default.Settings, "Настройки", tint = Red)
                }
            }
            if (maxWidth >= 560.dp) {
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.weight(1f)) { open(Section.TV) }
                    HomeCard("РАДИО", R.drawable.start_radio, Modifier.weight(1f)) { open(Section.RADIO) }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.fillMaxWidth().weight(1f)) { open(Section.TV) }
                    HomeCard("РАДИО", R.drawable.start_radio, Modifier.fillMaxWidth().weight(1f)) { open(Section.RADIO) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(
                onClick = { openUrl(context, TELEGRAM) },
                Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp)
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
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(2.dp))
            Image(painterResource(logo), title, Modifier.fillMaxWidth(.58f).aspectRatio(1.1f))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
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
    onRefresh: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) {
                Icon(Icons.Default.ArrowBack, "Назад", tint = Red)
            }
            Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(timer, fontSize = 11.sp, color = Red)
            if (!searchOpen) {
                onRefresh?.let {
                    IconButton(onClick = it) { Icon(Icons.Default.Refresh, "Обновить", tint = Red) }
                }
                IconButton(onClick = onSearchOpen) {
                    Icon(Icons.Default.Search, "Поиск", tint = Red)
                }
                settings?.let {
                    IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
                }
            } else {
                IconButton(onClick = onSearchClose) {
                    Icon(Icons.Default.Close, "Закрыть", tint = Red)
                }
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                singleLine = true,
                placeholder = { Text("Поиск…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onSearchClose) { Icon(Icons.Default.Close, null) }
                    }
                }
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
    settings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val index by player.currentIndex.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val error by player.error.collectAsState()
    val network by rememberNetworkState()
    val list = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var seconds by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.RADIO)
        val saved = store.scrollPosition(Section.RADIO)
        list.scrollToItem(saved.first.coerceAtMost(RADIO_STATIONS.lastIndex), saved.second)
        val started = System.currentTimeMillis()
        while (true) {
            delay(1000L)
            seconds = (System.currentTimeMillis() - started) / 1000L
        }
    }
    LaunchedEffect(Unit) { UsageTicker(store, Section.RADIO).run() }
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) -> store.saveScrollPosition(Section.RADIO, first, offset) }
    }
    LaunchedEffect(list) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        snapshotFlow { list.isScrollInProgress }.collect { if (it) controller.hide(WindowInsetsCompat.Type.navigationBars()) }
    }
    LaunchedEffect(error) {
        if (error != null) {
            delay(3000L)
            if (System.currentTimeMillis() - lastError >= ERROR_COOLDOWN) {
                lastError = System.currentTimeMillis()
                notice = "Возникла проблема. Обсуждаем решения в Telegram."
            }
        }
    }
    SearchAutoClose(searchOpen, query, stamp) {
        query = ""
        searchOpen = false
    }
    BackHandler(enabled = searchOpen) {
        query = ""
        searchOpen = false
        stamp = System.currentTimeMillis()
    }

    val current = RADIO_STATIONS.getOrNull(index)
    val itemsList = fuzzyFilter(
        RADIO_STATIONS.sortedWith(
            compareByDescending<StreamItem> { favorites.contains(it.key) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        ),
        query
    )

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            if (!network) NetworkBanner()
            Header(
                title = "РАДИО",
                timer = formatTime(seconds),
                back = back,
                settings = settings,
                searchOpen = searchOpen,
                query = query,
                onSearchOpen = { searchOpen = true; stamp = System.currentTimeMillis() },
                onQuery = { query = it; stamp = System.currentTimeMillis() },
                onSearchClose = { query = ""; searchOpen = false; stamp = System.currentTimeMillis() }
            )

            current?.let { station ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogoImage(station, 64.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(station.name.uppercase(Locale.ROOT), fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                            Text(if (playing) "играет" else "пауза", fontSize = 11.sp, color = Color.LightGray)
                        }
                        IconButton(onClick = { player.previous() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                        IconButton(onClick = { player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                        IconButton(onClick = { player.next() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
                    }
                }
            }

            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = RADIO_STATIONS.indexOfFirst { station -> station.url == item.url }
                        if (i >= 0) {
                            player.play(i)
                            saveLast(item)
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
                items(itemsList, key = { it.key }) { station ->
                    val favorite = favorites.contains(station.key)
                    ChannelRow(
                        item = station,
                        favorite = favorite,
                        playing = station.url == current?.url && playing,
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
                                    notice = if (!favorite) "Добавлено в избранное" else "Удалено из избранного"
                                }.onFailure { notice = "Не удалось обновить избранное" }
                            }
                        },
                        logoSize = 96.dp
                    )
                }
                if (itemsList.isEmpty()) item { EmptySearchState() }
            }
            NoticeBanner(notice) { notice = null }
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
    fullChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val list = rememberLazyListState()
    val network by rememberNetworkState()
    val error by player.error.collectAsState()
    val waiting by player.waitingForNetwork.collectAsState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var health by remember { mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var full by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var seconds by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableLongStateOf(0L) }
    var sourceIndex by remember { mutableIntStateOf(0) }

    suspend fun loadSource() {
        val source = store.sourceIndex()
        sourceIndex = source
        repo.loadCached(source)?.let {
            channels = it
            loading = false
        }
        repo.loadSource(source)
            .onSuccess {
                channels = it
                loading = false
                repo.warmFallbacks()
            }
            .onFailure {
                loading = false
                notice = "Возникла проблема. Обсуждаем решения в Telegram."
            }
    }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.TV)
        val saved = store.scrollPosition(Section.TV)
        list.scrollToItem(saved.first, saved.second)
        val started = System.currentTimeMillis()
        loadSource()
        while (true) {
            delay(1000L)
            seconds = (System.currentTimeMillis() - started) / 1000L
        }
    }
    LaunchedEffect(Unit) { UsageTicker(store, Section.TV).run() }
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) -> store.saveScrollPosition(Section.TV, first, offset) }
    }
    LaunchedEffect(list) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        snapshotFlow { list.isScrollInProgress }.collect { if (it) controller.hide(WindowInsetsCompat.Type.navigationBars()) }
    }
    LaunchedEffect(channels) {
        if (channels.isEmpty()) return@LaunchedEffect
        health = scanAvailability(channels.take(15), health)
        while (true) {
            delay(30 * 60 * 1000L)
            health = scanAvailability(channels, health)
        }
    }
    LaunchedEffect(list.firstVisibleItemIndex, channels, health) {
        val first = list.firstVisibleItemIndex
        val end = minOf(channels.size, first + 20)
        if (end > first) {
            val pending = channels.subList(first, end).filter { health[it.url] == null }
            if (pending.isNotEmpty()) health = scanAvailability(pending, health)
        }
    }
    LaunchedEffect(error) {
        if (error != null) {
            delay(3000L)
            if (System.currentTimeMillis() - lastError >= ERROR_COOLDOWN) {
                lastError = System.currentTimeMillis()
                notice = "Возникла проблема. Обсуждаем решения в Telegram."
            }
        }
    }
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
    fun play(index: Int) {
        if (index in channels.indices) {
            selected = index
            scope.launch { player.playWithFallback(candidates(channels[index])) }
        }
    }
    fun neighbor(delta: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = selected.coerceIn(0, channels.lastIndex)
        repeat(channels.size) {
            cursor = (cursor + delta + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }
    fun refresh() {
        scope.launch {
            refreshing = true
            val source = store.sourceIndex()
            repo.loadSource(source)
                .onSuccess {
                    channels = it
                    health = emptyMap()
                    repo.warmFallbacks()
                }
                .onFailure { notice = "Возникла проблема. Обсуждаем решения в Telegram." }
            refreshing = false
        }
    }

    if (full && selected in channels.indices) {
        TvPlayer(
            player = player,
            channel = channels[selected],
            error = error,
            waiting = waiting || !network,
            onBack = { full = false },
            onPrev = {
                val i = neighbor(-1)
                if (i >= 0) {
                    play(i)
                    saveLast(channels[i])
                }
            },
            onNext = {
                val i = neighbor(1)
                if (i >= 0) {
                    play(i)
                    saveLast(channels[i])
                }
            },
            onResetZoom = { zoomByChannel.remove(channels[selected].key) }
        )
        return
    }

    val ordered = channels.sortedWith(
        compareByDescending<StreamItem> { favorites.contains(it.key) }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    )
    val filtered = fuzzyFilter(ordered, query)

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            if (!network) NetworkBanner()
            Header(
                title = "ТЕЛЕВИЗОР",
                timer = formatTime(seconds),
                back = back,
                settings = settings,
                searchOpen = searchOpen,
                query = query,
                onSearchOpen = { searchOpen = true; stamp = System.currentTimeMillis() },
                onQuery = { query = it; stamp = System.currentTimeMillis() },
                onSearchClose = { query = ""; searchOpen = false; stamp = System.currentTimeMillis() },
                onRefresh = ::refresh
            )
            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = channels.indexOfFirst { channel ->
                            channel.url == item.url || channel.name.equals(item.name, ignoreCase = true)
                        }
                        if (i >= 0) {
                            selected = i
                            full = true
                            play(i)
                            saveLast(channels[i])
                        }
                    },
                    onClose = dismissRestore
                )
            }
            when {
                loading -> SkeletonList()
                filtered.isEmpty() -> if (query.isBlank()) EmptyPlaylistState() else EmptySearchState()
                else -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = ::refresh,
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
                                    if (i >= 0) {
                                        selected = i
                                        full = true
                                        play(i)
                                        saveLast(channel)
                                    }
                                },
                                onFavorite = {
                                    scope.launch {
                                        runCatching {
                                            store.setFavorite(Section.TV, channel.key, !favorite)
                                            favorites = store.favorites(Section.TV)
                                            notice = if (!favorite) "Добавлено в избранное" else "Удалено из избранного"
                                        }.onFailure { notice = "Не удалось обновить избранное" }
                                    }
                                },
                                logoSize = 56.dp
                            )
                        }
                    }
                }
            }
            NoticeBanner(notice) { notice = null }
        }
    }
}

@Composable
private fun TvPlayer(
    player: PlayerController,
    channel: StreamItem,
    error: String?,
    waiting: Boolean,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onResetZoom: () -> Unit
) {
    val config = LocalConfiguration.current
    val portrait = config.screenWidthDp < config.screenHeightDp
    val haptic = LocalHapticFeedback.current
    var controls by remember(channel.key) { mutableStateOf(true) }
    var taps by remember(channel.key) { mutableIntStateOf(0) }
    var zoom by remember(channel.key) { mutableFloatStateOf(zoomByChannel[channel.key] ?: 1f) }
    val density = LocalDensity.current
    val navVisible = WindowInsets.navigationBars.getBottom(density) > 0

    LaunchedEffect(controls) {
        if (controls) {
            delay(5000L)
            controls = false
        }
    }
    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(channel.key) {
                detectTapGestures(
                    onTap = {
                        controls = true
                        taps++
                        if (taps >= 3) {
                            zoom = 1f
                            zoomByChannel.remove(channel.key)
                            onResetZoom()
                            taps = 0
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
            .pointerInput(channel.key) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                    zoomByChannel[channel.key] = zoom
                    controls = true
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = this@TvPlayer.player.player
                    setShutterBackgroundColor(AColor.BLACK)
                    setKeepContentOnPlayerReset(true)
                }
            }
        )

        val controlModifier = if (navVisible) {
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout)
        } else {
            Modifier.align(Alignment.TopCenter)
        }
        AnimatedVisibility(
            visible = controls,
            modifier = controlModifier,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut()
        ) {
            PlayerControls(channel, onBack, onPrev, onNext, portrait)
        }

        if (waiting) {
            Text(
                "Сигнал утерян, перепроверьте подключение к сети",
                Modifier.align(Alignment.BottomCenter).padding(12.dp),
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
        if (error != null) {
            Text(
                "Возникла проблема. Обсуждаем решения в Telegram.",
                Modifier.align(Alignment.Center).padding(24.dp),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlayerControls(
    channel: StreamItem,
    back: () -> Unit,
    prev: () -> Unit,
    next: () -> Unit,
    portrait: Boolean
) {
    Card(
        Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .94f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(channel.name, Modifier.weight(1f), color = Color.White, fontSize = if (portrait) 15.sp else 17.sp, maxLines = 1)
            IconButton(onClick = prev) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
            IconButton(onClick = next) { Icon(Icons.Default.SkipNext, null, tint = Red) }
        }
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
    var source by remember { mutableIntStateOf(0) }
    var sourceDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { source = store.sourceIndex() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Таймер сна", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SleepGrid(sleepRemaining, sleepUntil, sleepMinutes, onSleep, onCancelSleep)
                }
            }
        }
        item { DonationCard() }
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Red,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Gray
                        )
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Статистика", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Общее время использования ТВ: ${formatUsage(tvUsage)}", color = Color.LightGray)
                    OutlinedButton(onClick = { onResetStats(Section.TV) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик ТВ") }
                    Text("Общее время использования Радио: ${formatUsage(radioUsage)}", color = Color.LightGray)
                    OutlinedButton(onClick = { onResetStats(Section.RADIO) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик Радио") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Источник ТВ-плейлиста", fontWeight = FontWeight.Bold)
                    Text(TV_SOURCES.getOrNull(source)?.name.orEmpty(), color = Color.Gray, fontSize = 12.sp)
                    Button(
                        onClick = { sourceDialog = true },
                        Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("Выбрать источник") }
                }
            }
        }
        item {
            Button(onClick = onResetAll, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)) {
                Text("Сбросить настройки до заводских")
            }
        }
        item {
            OutlinedButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности") }
        }
    }

    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false },
            title = { Text("Источник ТВ-плейлиста") },
            text = {
                Column {
                    TV_SOURCES.forEachIndexed { index, item ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(
                                onClick = {
                                    source = index
                                    sourceDialog = false
                                    onSource(index)
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = source == index, onClick = {
                                source = index
                                sourceDialog = false
                                onSource(index)
                            })
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
@OptIn(ExperimentalLayoutApi::class)
private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val options = listOf(
        15L to "15 мин", 30L to "30 мин", 60L to "1 ч", 120L to "2 ч", 240L to "4 ч",
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
                Text("Поддержать проект", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                Image(painterResource(R.drawable.qr_donate), "QR-код для пожертвований", Modifier.fillMaxSize().padding(4.dp))
            }
        }
    }
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
    logoSize: Dp
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
            LogoImage(item, logoSize, offline)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name.uppercase(Locale.ROOT),
                    color = if (offline) Gray else Color.White,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    fontSize = 14.sp
                )
                if (offline) Text("• offline", color = Gray, fontSize = 10.sp)
                if (playing) Text("• играет", color = Red, fontSize = 10.sp)
            }
            IconButton(onClick = onFavorite, Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) {
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
private fun LogoImage(item: StreamItem, size: Dp, dimmed: Boolean = false) {
    val context = LocalContext.current
    val resourceName = localLogoName(item.name)
    val resourceId = remember(resourceName) { context.resources.getIdentifier(resourceName, "drawable", context.packageName) }
    if (resourceId != 0) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = item.name,
            modifier = Modifier.size(size).alpha(if (dimmed) .4f else 1f)
        )
    } else {
        Box(
            Modifier.size(size).background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).alpha(if (dimmed) .4f else 1f),
            contentAlignment = Alignment.Center
        ) {
            Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
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
        awaitDisposeCompat { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}

private suspend fun scanAvailability(
    items: List<StreamItem>,
    current: Map<String, AvailabilityStatus>
): Map<String, AvailabilityStatus> {
    if (items.isEmpty()) return current
    val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.SECONDS)
        .build()
    val semaphore = Semaphore(2)
    val output = ConcurrentHashMap<String, AvailabilityStatus>()
    kotlinx.coroutines.coroutineScope {
        items.map { item ->
            launch(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    output[item.url] = runCatching {
                        client.newCall(Request.Builder().url(item.url).head().build()).execute().use {
                            if (it.isSuccessful || it.code in 300..399) AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE
                        }
                    }.getOrDefault(AvailabilityStatus.OFFLINE)
                } finally {
                    semaphore.release()
                }
            }
        }.forEach { it.join() }
    }
    return current + output
}

private fun awaitDisposeCompat(onDispose: () -> Unit): suspend () -> Unit = {
    try {
        kotlinx.coroutines.awaitCancellation()
    } finally {
        onDispose()
    }
}

private class UsageTicker(private val store: SettingsStore, private val section: Section) {
    suspend fun run() {
        var pending = 0L
        var lastFlush = System.currentTimeMillis()
        try {
            while (true) {
                delay(1000L)
                pending++
                if (System.currentTimeMillis() - lastFlush >= 5000L) {
                    store.addUsageSeconds(section, pending)
                    pending = 0L
                    lastFlush = System.currentTimeMillis()
                }
            }
        } finally {
            if (pending > 0L) store.addUsageSeconds(section, pending)
        }
    }
}
