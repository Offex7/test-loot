package com.offex7.streamhub

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

private val Red = Color(0xFFE53935)
private val Background = Color(0xFF090909)
private val Panel = Color(0xFF151515)
private val PanelAlt = Color(0xFF202020)
private val Skeleton = Color(0xFF2A2A2A)
private const val RESTORE_WINDOW_MS = 10 * 60 * 1000L
private const val DONATION_WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"
private const val TELEGRAM_URL = "https://t.me/TvRadioOnline"
private val sessionZoom = mutableMapOf<String, Float>()

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: SettingsStore
    private lateinit var tvPlayer: PlayerController
    private lateinit var radioController: RadioMediaController
    internal var tvViewing = false
    internal var pipEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        settingsStore = SettingsStore(applicationContext)
        tvPlayer = PlayerController(applicationContext)
        radioController = RadioMediaController(applicationContext)
        setContent { StreamHubTheme { StreamHubApp(settingsStore, tvPlayer, radioController, this) } }
        handlePipIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePipIntent(intent)
    }

    private fun handlePipIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PipActionReceiver.EXTRA_TOGGLE_PLAYBACK, false) == true) {
            tvPlayer.toggle()
            intent.removeExtra(PipActionReceiver.EXTRA_TOGGLE_PLAYBACK)
        }
    }

    override fun onUserLeaveHint() {
        if (tvViewing && pipEnabled && !isInPictureInPictureMode) {
            val toggleIntent = Intent(this, PipActionReceiver::class.java).setAction(PipActionReceiver.ACTION_TOGGLE)
            val pending = PendingIntent.getBroadcast(this, 4101, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val action = android.app.RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_media_play), "Play/Pause", "Play/Pause", pending)
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).setActions(listOf(action)).build())
        }
        super.onUserLeaveHint()
    }

    override fun onStop() {
        lifecycleScope.launch { settingsStore.setLastExitTime(System.currentTimeMillis()) }
        super.onStop()
    }

    override fun onDestroy() {
        tvPlayer.release()
        radioController.release()
        super.onDestroy()
    }
}

@Composable
private fun StreamHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(primary = Red, secondary = Red, background = Background, surface = Panel, surfaceVariant = PanelAlt), content = content)
}

@Composable
private fun StreamHubApp(store: SettingsStore, tvPlayer: PlayerController, radioPlayer: RadioMediaController, activity: MainActivity) {
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var disclaimerOpen by rememberSaveable { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var restoreItem by remember { mutableStateOf<StreamItem?>(null) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var sleepNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        activity.pipEnabled = store.pipEnabled()
        val lastExit = store.lastExitTime()
        val savedSection = store.lastSection()
        if (savedSection != null && lastExit > 0L && System.currentTimeMillis() - lastExit < RESTORE_WINDOW_MS) {
            section = savedSection
            restoreItem = store.lastStream(savedSection)
        }
        LogoCache.preload(activity)
    }
    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) { remaining = 0L; return@LaunchedEffect }
        while (sleepUntil > 0L) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                sleepUntil = 0L; remaining = 0L; tvPlayer.stop(); radioPlayer.stop(); activity.finishAndRemoveTask(); break
            }
            remaining = left
            if (left <= 30_000L) sleepNotice = "Таймер сна сработает через 30 секунд"
            delay(1000L)
        }
    }
    LaunchedEffect(sleepNotice) { if (sleepNotice != null) { delay(4000L); sleepNotice = null } }

    fun openSection(value: Section) {
        section = value; settingsOpen = false; disclaimerOpen = false; exitDialog = false
        scope.launch { store.setSection(value) }
    }
    fun leaveSection() {
        section = null; settingsOpen = false; disclaimerOpen = false; restoreItem = null; activity.tvViewing = false
        tvPlayer.stop(); radioPlayer.stop(); scope.launch { store.clearSection() }
    }

    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)), color = Background) {
        Column(Modifier.fillMaxSize()) {
            sleepNotice?.let { Text(it, Modifier.fillMaxWidth().background(PanelAlt).padding(8.dp), fontSize = 12.sp) }
            when {
                disclaimerOpen -> DisclaimerScreen { disclaimerOpen = false }
                settingsOpen -> SettingsScreen(
                    store = store,
                    pipEnabled = activity.pipEnabled,
                    onPipChange = { value -> activity.pipEnabled = value; scope.launch { store.setPipEnabled(value) } },
                    onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },
                    onBack = { settingsOpen = false },
                    onStatsReset = { scope.launch { store.resetUsage(it) } },
                    onDisclaimer = { disclaimerOpen = true },
                    onReset = { section = null; settingsOpen = false; disclaimerOpen = false; restoreItem = null; activity.pipEnabled = true; sleepUntil = 0L; scope.launch { store.resetAll() } }
                )
                section == null -> PickerScreen(onSelect = ::openSection, onSettings = { settingsOpen = true })
                section == Section.RADIO -> RadioScreen(radioPlayer, store, restoreItem, { restoreItem = null }, { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.RADIO, item) } }, ::leaveSection, { settingsOpen = true })
                else -> TvScreen(tvPlayer, store, restoreItem, { restoreItem = null }, { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.TV, item) } }, ::leaveSection, { settingsOpen = true }) { activity.tvViewing = it }
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            disclaimerOpen -> disclaimerOpen = false
            settingsOpen -> settingsOpen = false
            section != null -> leaveSection()
            exitDialog -> exitDialog = false
            else -> exitDialog = true
        }
    }
    if (exitDialog) AlertDialog(onDismissRequest = { exitDialog = false }, title = { Text("Выйти из приложения?") }, text = { Text("Закрыть TV / Radio. Online?") }, confirmButton = { TextButton(onClick = { activity.finish() }) { Text("Выйти", color = Red) } }, dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Отмена") } })
}

@Composable
private fun PickerScreen(onSelect: (Section) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
        val landscape = maxWidth > 560.dp
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(top = 35.dp, bottom = 55.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PickerCard("ТВ", "https://avatars.mds.yandex.net/i?id=ea6c1bcfb22a3359c5717fc55c8ca2a48b1fda50-5354520-images-thumbs&n=13", Icons.Default.Tv, Modifier.weight(1f)) { onSelect(Section.TV) }
                PickerCard("РАДИО", "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png", Icons.Default.Radio, Modifier.weight(1f)) { onSelect(Section.RADIO) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(top = 50.dp, bottom = 55.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PickerCard("ТВ", "https://avatars.mds.yandex.net/i?id=ea6c1bcfb22a3359c5717fc55c8ca2a48b1fda50-5354520-images-thumbs&n=13", Icons.Default.Tv, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.TV) }
                PickerCard("РАДИО", "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png", Icons.Default.Radio, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.RADIO) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { openUrl(context, TELEGRAM_URL) }) { Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp) }
            Text("TV_Radio_Online_V3.0", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PickerCard(label: String, url: String, fallback: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AsyncImage(model = url, contentDescription = label, modifier = Modifier.size(98.dp)); Spacer(Modifier.height(12.dp)); Icon(fallback, null, tint = Red, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(4.dp)); Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppHeader(title: String, sessionText: String, onBack: () -> Unit, onSettings: (() -> Unit)?, searchOpen: Boolean, query: String, onOpenSearch: () -> Unit, onQueryChange: (String) -> Unit, onCloseSearch: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(sessionText, color = Red, fontSize = 11.sp)
            if (!searchOpen) { IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Поиск", tint = Red) }; onSettings?.let { IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) } } }
            else IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
        }
        if (searchOpen) OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), singleLine = true, placeholder = { Text("Поиск…") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, null) } })
    }
}

@Composable
private fun RadioScreen(player: RadioMediaController, store: SettingsStore, restoreItem: StreamItem?, onRestoreDismiss: () -> Unit, onSelect: (StreamItem) -> Unit, onBack: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playing by player.isPlaying.collectAsState()
    val currentIndex by player.currentIndex.collectAsState()
    val connected by player.connected.collectAsState()
    val error by player.error.collectAsState()
    val haptic = LocalHapticFeedback.current
    val networkAvailable = rememberNetworkAvailable(context)
    val listState = rememberLazyListState()
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    var problemVisible by remember { mutableStateOf(false) }
    var problemShownAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val saved = store.scrollPosition(Section.RADIO); listState.scrollToItem(saved.first, saved.second)
        val started = System.currentTimeMillis()
        while (true) { delay(1000L); sessionSeconds = (System.currentTimeMillis() - started) / 1000L }
    }
    DisposableEffect(Unit) { val started = System.currentTimeMillis(); onDispose { scope.launch { store.addUsageMillis(Section.RADIO, System.currentTimeMillis() - started) } } }
    LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { store.saveScrollPosition(Section.RADIO, it.first, it.second) } }
    LaunchedEffect(searchOpen, query, lastActivity) {
        if (!searchOpen) return@LaunchedEffect
        val stamp = lastActivity; delay(if (query.isBlank()) 5_000L else 15_000L)
        if (searchOpen && lastActivity == stamp) { query = ""; searchOpen = false }
    }
    LaunchedEffect(restoreItem, connected) { val item = restoreItem ?: return@LaunchedEffect; val index = RADIO_STATIONS.indexOfFirst { it.url == item.url }; if (index >= 0 && connected) player.play(index) }
    LaunchedEffect(error) { if (error != null) { delay(3000L); if (System.currentTimeMillis() - problemShownAt >= 5 * 60 * 1000L) { problemShownAt = System.currentTimeMillis(); problemVisible = true } } }
    BackHandler(enabled = searchOpen) { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }

    val filtered = remember(query) { fuzzySort(RADIO_STATIONS, query) }
    val current = RADIO_STATIONS.getOrNull(currentIndex)
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            if (!networkAvailable) NetworkBanner("Сигнал утерян, перепроверьте подключение к сети")
            AppHeader("РАДИО", formatSessionTime(sessionSeconds), onBack, onSettings, searchOpen, query, { searchOpen = true; lastActivity = System.currentTimeMillis() }, { query = it; lastActivity = System.currentTimeMillis() }, { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() })
            current?.let { station -> Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) { LogoImage(station, 42.dp, false); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(station.name.uppercase(Locale.ROOT), fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold); Text(if (playing) "Воспроизведение" else "Пауза", fontSize = 10.sp, color = Color.LightGray) }; IconButton(onClick = { player.previous() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }; IconButton(onClick = { player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }; IconButton(onClick = { player.next() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }; IconButton(onClick = { player.stop() }) { Icon(Icons.Default.Stop, null, tint = Red) } } } }
            restoreItem?.let { item -> RestoreBanner(item, onContinue = { val i = RADIO_STATIONS.indexOfFirst { s -> s.url == item.url }; if (i >= 0) { player.play(i); onSelect(item) } }, onClose = onRestoreDismiss) }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered, key = { it.url }) { station -> ChannelRow(station, rememberFavorite(store, station.url), station.url == current?.url && playing, false, { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.play(RADIO_STATIONS.indexOf(station)); onSelect(station) }, { scope.launch { store.setFavorite(station.url, !store.isFavorite(station.url)) } }) }; if (filtered.isEmpty()) item { EmptySearchState() } }
        }
        if (problemVisible) AnimatedProblemToast({ openUrl(context, TELEGRAM_URL); problemVisible = false }, { problemVisible = false })
    }
}

@Composable
private fun TvScreen(player: PlayerController, store: SettingsStore, restoreItem: StreamItem?, onRestoreDismiss: () -> Unit, onSelect: (StreamItem) -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onFullScreenChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val playbackError by player.error.collectAsState()
    val waitingNetwork by player.waitingForNetwork.collectAsState()
    val networkAvailable = rememberNetworkAvailable(context)
    val listState = rememberLazyListState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var fullScreen by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    var problemVisible by remember { mutableStateOf(false) }
    var problemShownAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        favorites = store.favorites(); val saved = store.scrollPosition(Section.TV); listState.scrollToItem(saved.first, saved.second); repo.loadCachedIfFresh()?.let { channels = it }; loading = false; repo.refreshCombined().onSuccess { channels = it }
        val started = System.currentTimeMillis(); while (true) { delay(1000L); sessionSeconds = (System.currentTimeMillis() - started) / 1000L }
    }
    DisposableEffect(Unit) { val started = System.currentTimeMillis(); onDispose { scope.launch { store.addUsageMillis(Section.TV, System.currentTimeMillis() - started) } } }
    LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { store.saveScrollPosition(Section.TV, it.first, it.second) } }
    LaunchedEffect(listState) { snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scroll -> val a = context as? ComponentActivity ?: return@collect; val c = WindowInsetsControllerCompat(a.window, a.window.decorView); if (scroll) c.hide(WindowInsetsCompat.Type.navigationBars()) } }
    LaunchedEffect(searchOpen, query, lastActivity) { if (!searchOpen) return@LaunchedEffect; val stamp = lastActivity; delay(if (query.isBlank()) 5_000L else 15_000L); if (searchOpen && lastActivity == stamp) { query = ""; searchOpen = false } }
    LaunchedEffect(playbackError) { if (playbackError != null) { delay(3000L); if (System.currentTimeMillis() - problemShownAt >= 5 * 60 * 1000L) { problemShownAt = System.currentTimeMillis(); problemVisible = true } } }
    LaunchedEffect(restoreItem, channels) { val item = restoreItem ?: return@LaunchedEffect; val idx = channels.indexOfFirst { it.url == item.url }; if (idx >= 0) { selectedIndex = idx; if (networkAvailable) player.play(item.url) else player.pause() } }
    LaunchedEffect(fullScreen) { onFullScreenChanged(fullScreen) }
    BackHandler(enabled = searchOpen && !fullScreen) { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }

    fun refresh() { scope.launch { refreshing = true; repo.refreshCombined().onSuccess { channels = it }; refreshing = false } }
    if (fullScreen && selectedIndex in channels.indices) {
        TvPlayerScreen(player, channels[selectedIndex], playbackError, waitingNetwork || !networkAvailable, onBack = { fullScreen = false }, onPrevious = { selectedIndex = if (selectedIndex <= 0) channels.lastIndex else selectedIndex - 1; player.play(channels[selectedIndex].url); onSelect(channels[selectedIndex]) }, onNext = { selectedIndex = (selectedIndex + 1) % channels.size; player.play(channels[selectedIndex].url); onSelect(channels[selectedIndex]) }, onRefresh = ::refresh)
        return
    }
    val filtered = remember(channels, favorites, query) { fuzzySort(channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.url) }.thenBy { it.name.lowercase(Locale.ROOT) }), query) }
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            if (!networkAvailable) NetworkBanner("Сигнал утерян, перепроверьте подключение к сети")
            AppHeader("ТВ", formatSessionTime(sessionSeconds), onBack, onSettings, searchOpen, query, { searchOpen = true; lastActivity = System.currentTimeMillis() }, { query = it; lastActivity = System.currentTimeMillis() }, { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() })
            restoreItem?.let { item -> RestoreBanner(item, { val idx = channels.indexOfFirst { c -> c.url == item.url }; if (idx >= 0) { selectedIndex = idx; fullScreen = true; player.play(item.url); onSelect(item); onRestoreDismiss() } }, onRestoreDismiss) }
            when {
                loading -> SkeletonList()
                filtered.isEmpty() -> if (query.isBlank()) EmptyPlaylistState() else EmptySearchState()
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::refresh, modifier = Modifier.fillMaxSize()) { LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(filtered, key = { it.url }) { channel -> ChannelRow(channel, favorites.contains(channel.url), false, false, { selectedIndex = channels.indexOfFirst { it.url == channel.url }; player.play(channel.url); fullScreen = true; onSelect(channel) }, { scope.launch { store.setFavorite(channel.url, !favorites.contains(channel.url)); favorites = store.favorites() } }) } } }
            }
        }
        if (problemVisible) AnimatedProblemToast({ openUrl(context, TELEGRAM_URL); problemVisible = false }, { problemVisible = false })
    }
}

@Composable
private fun TvPlayerScreen(player: PlayerController, channel: StreamItem, playbackError: String?, waitingNetwork: Boolean, onBack: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val playing by player.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var zoom by remember(channel.url) { mutableFloatStateOf(sessionZoom[channel.url] ?: 1f) }
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsVisible) { if (controlsVisible) { delay(5000L); controlsVisible = false } }
    LaunchedEffect(Unit) { activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView).hide(WindowInsetsCompat.Type.systemBars()) } }
    DisposableEffect(Unit) { onDispose { activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) } } }
    BackHandler(enabled = true) { activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }; sessionZoom.remove(channel.url); onBack() }
    Surface(Modifier.fillMaxSize().background(Color.Black), color = Color.Black) {
        Box(Modifier.fillMaxSize().pointerInput(channel.url) { detectTransformGestures { _, _, scale, _ -> zoom = (zoom * scale).coerceIn(1f, 3f); sessionZoom[channel.url] = zoom } }.pointerInput(channel.url) { detectTapGestures(onTap = { val now = System.currentTimeMillis(); taps = if (now - lastTap < 450L) taps + 1 else 1; lastTap = now; controlsVisible = true; if (taps >= 3) { zoom = 1f; sessionZoom[channel.url] = 1f; taps = 0; haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }) }) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player.player } }, update = { it.player = player.player }, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom))
            if (controlsVisible) Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout).background(Color(0xE51A1A1A)).padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }; Text(channel.name, Modifier.weight(1f), maxLines = 1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); IconButton(onClick = { onPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }; IconButton(onClick = { player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }; IconButton(onClick = { onNext() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }; IconButton(onClick = { player.stop() }) { Icon(Icons.Default.Stop, null, tint = Red) } }
            if (waitingNetwork) NetworkBanner("Сигнал утерян, перепроверьте подключение к сети", Modifier.align(Alignment.Center)) else if (playbackError != null) TextButton(onClick = onRefresh, Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.systemBars)) { Text("Обновить", color = Red) }
        }
    }
}

@Composable
private fun SettingsScreen(store: SettingsStore, pipEnabled: Boolean, onPipChange: (Boolean) -> Unit, onSleep: (Long) -> Unit, onBack: () -> Unit, onStatsReset: (Section) -> Unit, onDisclaimer: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    val tvTotal by store.usageFlow(Section.TV).collectAsState(initial = 0L)
    val radioTotal by store.usageFlow(Section.RADIO).collectAsState(initial = 0L)
    val presets = listOf("15 мин" to 15L, "30 мин" to 30L, "1 ч" to 60L, "2 ч" to 120L, "4 ч" to 240L, "8 ч" to 480L, "10 ч" to 600L, "15 ч" to 900L, "24 ч" to 1440L, "36 ч" to 2160L)
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }; Text("Настройки", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
        item { Text("Таймер сна", style = MaterialTheme.typography.titleMedium) }
        items(presets) { (label, min) -> Button(onClick = { onSleep(min) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(label) } }
        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text("Пожертвования", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(7.dp)); Text("USDT TRC20", fontSize = 11.sp, color = Color.LightGray); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth().background(PanelAlt, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(DONATION_WALLET, Modifier.weight(1f), fontSize = 12.sp, maxLines = 2); TextButton(onClick = { val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("Donation wallet", DONATION_WALLET)); copied = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) { Text(if (copied) "Скопировано" else "Копировать", color = Red, fontSize = 12.sp) } }; Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(180.dp)) { Image(painterResource(R.drawable.qr_donate), "QR пожертвований", modifier = Modifier.fillMaxSize().padding(7.dp)) } } } } }
        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tv, null, tint = Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("PiP при сворачивании", fontWeight = FontWeight.SemiBold); Text("Мини-окно ТВ при уходе из приложения", fontSize = 11.sp, color = Color.LightGray) }; Switch(checked = pipEnabled, onCheckedChange = onPipChange) } } }
        item { Text("Статистика", style = MaterialTheme.typography.titleMedium) }
        item { StatsRow("Общее время использования ТВ", tvTotal, "Сбросить счётчик ТВ") { onStatsReset(Section.TV) } }
        item { StatsRow("Общее время использования Радио", radioTotal, "Сбросить счётчик Радио") { onStatsReset(Section.RADIO) } }
        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text("Источник ТВ-плейлиста", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); Text("Объединённый (smolnp + NaggDD + DropTV + Zabava)", fontSize = 13.sp); Text("Автоматическое объединение обновляется при запуске", fontSize = 11.sp, color = Color.Gray) } } }
        item { TextButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности", color = Color.White) } }
        item { Button(onClick = onReset, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Сбросить настройки по умолчанию") } }
    }
}

@Composable private fun StatsRow(title: String, millis: Long, reset: String, onReset: () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(formatDuration(millis), color = Red, fontSize = 18.sp) }; TextButton(onClick = onReset) { Text(reset, color = Red, fontSize = 11.sp) } } } }

@Composable private fun DisclaimerScreen(onBack: () -> Unit) { val context = LocalContext.current; Column(Modifier.fillMaxSize().padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }; Text("Отказ от ответственности", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) { item { Text("Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.\n\nПриложение не хранит, не распространяет и не модифицирует транслируемый контент. Все трансляции предоставляются третьими лицами. Разработчик не несёт ответственности за содержание транслируемого контента.\n\nЕсли вы являетесь правообладателем и считаете, что ваши права нарушаются — свяжитесь с нами через Telegram:", fontSize = 14.sp, lineHeight = 21.sp) }; item { TextButton(onClick = { openUrl(context, TELEGRAM_URL) }) { Text(TELEGRAM_URL, color = Red) } } } } }

@Composable private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) { BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) { val width = maxWidth; Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(14.dp)) { if (width < 500.dp) Column(Modifier.fillMaxWidth().padding(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { LogoImage(item, 42.dp, true); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("Продолжить ${item.name}?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2); Text("Последняя сессия", fontSize = 11.sp, color = Color.LightGray) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onContinue) { Text("Продолжить", color = Red) }; TextButton(onClick = onClose) { Text("Закрыть") } } } else Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { LogoImage(item, 46.dp, true); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("Продолжить ${item.name}?", maxLines = 1, fontWeight = FontWeight.SemiBold); Text("Последняя сессия", fontSize = 11.sp, color = Color.LightGray) }; TextButton(onClick = onContinue) { Text("Продолжить", color = Red) }; TextButton(onClick = onClose) { Text("Закрыть") } } } } }

@Composable private fun ChannelRow(item: StreamItem, favorite: Boolean, playing: Boolean, offline: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) { Card(onClick = onClick, modifier = Modifier.fillMaxWidth().alpha(if (offline) 0.5f else 1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { LogoImage(item, 48.dp, true); Spacer(Modifier.width(9.dp)); Text(item.name.uppercase(Locale.ROOT).ifBlank { "БЕЗ НАЗВАНИЯ" }, Modifier.weight(1f), maxLines = 2); if (playing) Icon(Icons.Default.PlayArrow, "Играет", tint = Red, modifier = Modifier.size(18.dp)); IconButton(onClick = onFavorite) { Icon(Icons.Default.Favorite, "Избранное", tint = if (favorite) Red else Color.Gray, modifier = Modifier.size(18.dp)) } } } }

@Composable private fun LogoImage(item: StreamItem, size: Dp, tv: Boolean) { val context = LocalContext.current; var idx by remember(item.url) { mutableIntStateOf(0) }; val sources = remember(item.url, item.logoUrl, item.epgLogoUrl, tv) { listOfNotNull(LogoCache.knownLogo(item), item.logoUrl, item.epgLogoUrl).distinct() }; if (idx < sources.size) AsyncImage(model = ImageRequest.Builder(context).data(sources[idx]).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.name, modifier = Modifier.size(size), onError = { idx++ }) else PlaceholderLogo(size) }
@Composable private fun PlaceholderLogo(size: Dp) { Box(Modifier.size(size).background(Skeleton, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("NO Image", color = Color.Gray, fontSize = max(9f, size.value * 0.15f).sp, maxLines = 1) } }
@Composable private fun SkeletonList() { LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(10) { Row(Modifier.fillMaxWidth().background(Skeleton, RoundedCornerShape(13.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(PanelAlt, RoundedCornerShape(10.dp))); Spacer(Modifier.width(9.dp)); Box(Modifier.height(18.dp).fillMaxWidth(0.7f).background(PanelAlt, RoundedCornerShape(7.dp))) } } } }
@Composable private fun EmptySearchState() { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Info, null, tint = Red, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(7.dp)); Text("Ничего не найдено") } }
@Composable private fun EmptyPlaylistState() { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Нет доступных каналов", color = Red); Text("Потяните вниз, чтобы обновить", color = Color.Gray, fontSize = 12.sp) } }
@Composable private fun NetworkBanner(message: String, modifier: Modifier = Modifier) { Row(modifier.fillMaxWidth().background(PanelAlt).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WifiOff, null, tint = Red, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(message, fontSize = 13.sp) } }
@Composable private fun AnimatedProblemToast(onGo: () -> Unit, onClose: () -> Unit) { AnimatedVisibility(true, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomCenter) { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("Возникла проблема. Обсуждаем решения в Telegram.", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); TextButton(onClick = onGo) { Text("Перейти", color = Red) }; TextButton(onClick = onClose) { Text("Закрыть") } } } } } }

@Composable private fun rememberNetworkAvailable(context: Context): Boolean { var state by remember { mutableStateOf(isNetworkAvailable(context)) }; LaunchedEffect(Unit) { while (true) { state = isNetworkAvailable(context); delay(1500L) } }; return state }
@Composable private fun rememberFavorite(store: SettingsStore, url: String): Boolean { var value by remember(url) { mutableStateOf(false) }; LaunchedEffect(url) { value = store.isFavorite(url) }; return value }
private class LogoCache { companion object { private val tvDomains = listOf("1tv.ru", "russia.tv", "ntv.ru", "tnt-online.ru", "ctc.ru", "ren.tv", "5-tv.ru", "matchtv.ru", "tvc.ru", "otr-online.ru", "zvezdanews.ru", "mir24.tv", "rbc.ru", "moscow24.ru", "karusel-tv.ru", "tvkultura.ru", "russia24.tv", "tv3.ru"); fun knownLogo(item: StreamItem): String? = when { item.name.contains("RELAX", true) -> "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13"; item.name.contains("Рекорд", true) -> "https://radiorecord.ru/favicon.ico"; item.name.contains("COMEDY", true) -> "https://comedy-radio.ru/favicon.ico"; item.name.contains("АВТОРАДИО", true) -> "https://www.radioplay.bg/assets/logos/avtoradio.png"; item.name.contains("Енерджи", true) -> "https://www.energyfm.ru/favicon.ico"; item.name.contains("Шоколад", true) -> "https://www.chocoradio.ru/favicon.ico"; item.name.contains("Ультра", true) -> "https://seeklogo.com/images/U/ultra-logo-191058.png"; item.name.contains("Пират", true) -> "https://radiorecord.ru/favicon.ico"; item.name.contains("Chill", true) -> "https://radiorecord.ru/favicon.ico"; item.name.contains("Psy", true) || item.name.contains("Vocal", true) -> "https://dfm.ru/favicon.ico"; else -> null }; fun preload(context: Context) { val loader = coil3.ImageLoader.Builder(context).build(); tvDomains.forEach { d -> loader.enqueue(ImageRequest.Builder(context).data("https://www.google.com/s2/favicons?sz=128&domain=$d").build()) } } } }
private fun isNetworkAvailable(context: Context): Boolean { val cm = context.getSystemService(ConnectivityManager::class.java); val net = cm.activeNetwork ?: return false; val caps = cm.getNetworkCapabilities(net) ?: return false; return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
private fun openUrl(context: Context, url: String) { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
private fun normalizeSearch(text: String): String = text.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
private fun fuzzyScore(name: String, query: String): Int { if (query.isBlank()) return 0; val n = normalizeSearch(name); val q = normalizeSearch(query); if (q.isBlank()) return 0; if (n.contains(q)) return 10_000 - (n.length - q.length); var pos = 0; var matched = 0; q.forEach { c -> val found = n.indexOf(c, pos); if (found >= 0) { matched++; pos = found + 1 } }; return if (matched == q.length) 5_000 - (pos - q.length) else Int.MIN_VALUE }
private fun fuzzySort(items: List<StreamItem>, query: String): List<StreamItem> = if (query.isBlank()) items else items.map { it to fuzzyScore(it.name, query) }.filter { it.second != Int.MIN_VALUE }.sortedByDescending { it.second }.map { it.first }
private fun formatSessionTime(seconds: Long): String = formatDuration(seconds * 1000L)
private fun formatDuration(ms: Long): String { val total = (ms / 1000L).coerceAtLeast(0L); val h = total / 3600L; val m = (total % 3600L) / 60L; val s = total % 60L; return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s) }
