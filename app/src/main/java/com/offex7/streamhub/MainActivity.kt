package com.offex7.streamhub

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

private val Red = Color(0xFFE53935)
private val Background = Color(0xFF121212)
private val Panel = Color(0xFF1A1A1A)
private val PanelAlt = Color(0xFF232323)
private val OfflineGray = Color(0xFF808080)
private val ImageGray = Color(0xFF2A2A2A)
private const val TELEGRAM_URL = "https://t.me/TvRadioOnline"
private const val DONATION_WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"
private const val RESTORE_WINDOW_MS = 10 * 60 * 1000L
private val sessionZoom = mutableMapOf<String, Float>()

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: SettingsStore
    private lateinit var tvPlayer: PlayerController
    private lateinit var radioController: RadioMediaController
    internal var pipEnabled = true
    internal var tvViewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        window.setNavigationBarDividerColor(AndroidColor.TRANSPARENT)
        settingsStore = SettingsStore(applicationContext)
        tvPlayer = PlayerController(applicationContext)
        radioController = RadioMediaController(applicationContext)
        setContent { StreamHubTheme { StreamHubApp(settingsStore, tvPlayer, radioController, this) } }
    }

    override fun onUserLeaveHint() {
        if (tvViewing && pipEnabled && Build.VERSION.SDK_INT >= 26 && !isInPictureInPictureMode) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
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
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Red, secondary = Red, background = Background,
            surface = Panel, surfaceVariant = PanelAlt, onPrimary = Color.White
        ),
        content = content
    )
}

@Composable
private fun StreamHubApp(store: SettingsStore, tvPlayer: PlayerController, radioPlayer: RadioMediaController, activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var disclaimerOpen by rememberSaveable { mutableStateOf(false) }
    var exitDialog by rememberSaveable { mutableStateOf(false) }
    var restoreItem by remember { mutableStateOf<StreamItem?>(null) }
    var pipState by remember { mutableStateOf(true) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableLongStateOf(0L) }
    var sleepDurationMinutes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        pipState = store.pipEnabled()
        activity.pipEnabled = pipState
        val lastExit = store.lastExitTime()
        val saved = store.lastSection()
        if (saved != null && lastExit > 0L && System.currentTimeMillis() - lastExit < RESTORE_WINDOW_MS) {
            section = saved
            restoreItem = store.lastStream(saved)
        }
    }

    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) { sleepRemaining = 0L; return@LaunchedEffect }
        while (sleepUntil > 0L) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                sleepUntil = 0L
                sleepRemaining = 0L
                sleepDurationMinutes = 0L
                tvPlayer.stop(); radioPlayer.stop()
                snackbar.showSnackbar("Таймер сна — завершено", duration = SnackbarDuration.Short)
                break
            }
            sleepRemaining = left
            delay(1000L)
        }
    }

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, withDismissAction = false, duration = SnackbarDuration.Short)
        }
    }

    fun leaveSection() {
        section = null
        settingsOpen = false
        disclaimerOpen = false
        restoreItem = null
        activity.tvViewing = false
        tvPlayer.stop(); radioPlayer.stop()
        scope.launch { store.clearSection() }
    }

    BackHandler {
        when {
            disclaimerOpen -> disclaimerOpen = false
            settingsOpen -> settingsOpen = false
            section != null -> leaveSection()
            exitDialog -> exitDialog = false
            else -> exitDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Surface(
            Modifier.fillMaxSize().background(Background)
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                .padding(padding),
            color = Background
        ) {
            when {
                disclaimerOpen -> DisclaimerScreen { disclaimerOpen = false }
                settingsOpen -> SettingsScreen(
                    store = store,
                    pipEnabled = pipState,
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepDurationMinutes = sleepDurationMinutes,
                    onPipChange = { enabled -> pipState = enabled; activity.pipEnabled = enabled; scope.launch { store.setPipEnabled(enabled) } },
                    onSleepSelected = { minutes -> sleepDurationMinutes = minutes; sleepUntil = System.currentTimeMillis() + minutes * 60_000L; notify("Таймер сна — запущен") },
                    onSleepCancel = { sleepUntil = 0L; sleepRemaining = 0L; sleepDurationMinutes = 0L; notify("Таймер сна — отключён") },
                    onStatsReset = { target -> runCatching { store.resetUsage(target) }.onSuccess { notify(if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен") }.onFailure { notify("Не удалось сбросить счётчик") } },
                    onSourceChanged = { index -> scope.launch { store.setSourceIndex(index) }; notify("Источник сохранён") },
                    onDisclaimer = { disclaimerOpen = true },
                    onReset = {
                        scope.launch { store.resetAll() }
                        sleepUntil = 0L; sleepRemaining = 0L; sleepDurationMinutes = 0L
                        pipState = true; activity.pipEnabled = true; section = null; settingsOpen = false; disclaimerOpen = false; restoreItem = null
                        notify("Настройки сброшены")
                    }
                )
                section == null -> PickerScreen(onSelect = { value -> section = value; scope.launch { store.setSection(value) } }, onSettings = { settingsOpen = true })
                section == Section.TV -> TvScreen(
                    tvPlayer, store, restoreItem, { restoreItem = null },
                    { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.TV, item) } },
                    ::leaveSection, { settingsOpen = true }, { activity.tvViewing = it }
                )
                else -> RadioScreen(
                    radioPlayer, store, restoreItem, { restoreItem = null },
                    { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.RADIO, item) } },
                    ::leaveSection, { settingsOpen = true }
                )
            }
        }
    }

    if (exitDialog) AlertDialog(
        onDismissRequest = { exitDialog = false },
        title = { Text("Выйти из приложения?") },
        text = { Text("Закрыть TV / Radio. Online?") },
        confirmButton = { TextButton(onClick = { activity.finishAndRemoveTask() }) { Text("Выйти", color = Red) } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Отмена") } }
    )
}

@Composable
private fun PickerScreen(onSelect: (Section) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("TV_RADIO_ONLINE_V4.0", color = Color.Gray, fontSize = 10.sp)
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
        }
        val landscape = maxWidth >= 560.dp
        if (landscape) {
            Row(Modifier.fillMaxWidth().weight(1f).padding(top = 14.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.weight(1f)) { onSelect(Section.TV) }
                HomeCard("РАДИО", R.drawable.start_radio, Modifier.weight(1f)) { onSelect(Section.RADIO) }
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.TV) }
                HomeCard("РАДИО", R.drawable.start_radio, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.RADIO) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(onClick = { openUrl(context, TELEGRAM_URL) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun HomeCard(label: String, logo: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(Modifier.height(2.dp))
            Image(painterResource(logo), label, Modifier.fillMaxWidth(0.58f).aspectRatio(1.1f))
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun AppHeader(title: String, sessionText: String, onBack: () -> Unit, onSettings: (() -> Unit)?, searchOpen: Boolean, query: String, onOpenSearch: () -> Unit, onQueryChange: (String) -> Unit, onCloseSearch: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
            Text(sessionText, color = Red, fontSize = 11.sp)
            if (!searchOpen) {
                IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Поиск", tint = Red) }
                onSettings?.let { IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) } }
            } else {
                IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
            }
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
    val error by player.error.collectAsState()
    val networkAvailable = rememberNetworkAvailable(context)
    val listState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var problemShownAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.RADIO)
        val saved = store.scrollPosition(Section.RADIO)
        listState.scrollToItem(saved.first.coerceAtMost(RADIO_STATIONS.lastIndex), saved.second)
        val start = System.currentTimeMillis()
        while (true) { delay(1000L); sessionSeconds = (System.currentTimeMillis() - start) / 1000L }
    }
    LaunchedEffect(Section.RADIO) { UsageTicker(store, Section.RADIO).run() }
    LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { store.saveScrollPosition(Section.RADIO, it.first, it.second) } }
    SearchAutoClose(searchOpen, query, lastActivity) { query = ""; searchOpen = false }
    LaunchedEffect(restoreItem) { val item = restoreItem ?: return@LaunchedEffect; val idx = RADIO_STATIONS.indexOfFirst { it.url == item.url }; if (idx >= 0 && currentIndex != idx) player.play(idx) }
    LaunchedEffect(error) { if (error != null) { delay(3000L); val now = System.currentTimeMillis(); if (now - problemShownAt >= 5 * 60 * 1000L) { problemShownAt = now; notice = "Возникла проблема. Обсуждаем решения в Telegram." } } }
    BackHandler(enabled = searchOpen) { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }

    val filtered = fuzzySort(RADIO_STATIONS.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.key) }.thenBy { it.name }), query)
    val current = RADIO_STATIONS.getOrNull(currentIndex)
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize()) {
            if (!networkAvailable) NetworkBanner()
            AppHeader("РАДИО", formatSessionTime(sessionSeconds), onBack, onSettings, searchOpen, query, { searchOpen = true; lastActivity = System.currentTimeMillis() }, { query = it; lastActivity = System.currentTimeMillis() }, { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() })
            current?.let { station ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LogoImage(station, 46.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text(station.name.uppercase(Locale.ROOT), fontSize = 18.sp, maxLines = 1, fontWeight = FontWeight.SemiBold); Text(if (playing) "играет" else "пауза", fontSize = 11.sp, color = Color.LightGray) }
                        IconButton(onClick = { player.previous() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                        IconButton(onClick = { player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                        IconButton(onClick = { player.next() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
                    }
                }
            }
            restoreItem?.let { item -> RestoreBanner(item, { val idx = RADIO_STATIONS.indexOfFirst { it.url == item.url }; if (idx >= 0) { player.play(idx); onSelect(item) } }, onRestoreDismiss) }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = { it.key }) { station ->
                    val favorite = favorites.contains(station.key)
                    ChannelRow(station, favorite, station.url == current?.url && playing, false, { val idx = RADIO_STATIONS.indexOf(station); if (idx >= 0) { player.play(idx); onSelect(station) } }, {
                        scope.launch { runCatching { store.setFavorite(Section.RADIO, station.key, !favorite); favorites = store.favorites(Section.RADIO); notice = if (!favorite) "Добавлено в избранное" else "Удалено из избранного" }.onFailure { notice = "Не удалось обновить избранное" } }
                    })
                }
                if (filtered.isEmpty()) item { EmptySearchState() }
            }
            NoticeBanner(notice) { notice = null }
        }
    }
}

@Composable
private fun TvScreen(player: PlayerController, store: SettingsStore, restoreItem: StreamItem?, onRestoreDismiss: () -> Unit, onSelect: (StreamItem) -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onFullScreenChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val playbackError by player.error.collectAsState()
    val waitingNetwork by player.waitingForNetwork.collectAsState()
    val networkAvailable = rememberNetworkAvailable(context)
    val listState = rememberLazyListState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var health by remember { mutableStateOf<Map<String, AvailabilityStatus>>(emptyMap()) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }
    var problemShownAt by remember { mutableLongStateOf(0L) }
    var sourceIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        sourceIndex = store.sourceIndex()
        favorites = store.favorites(Section.TV)
        val saved = store.scrollPosition(Section.TV)
        listState.scrollToItem(saved.first, saved.second)
        repo.loadCached(sourceIndex)?.let { channels = it; loading = false }
        repo.loadSource(sourceIndex).onSuccess { channels = it; loading = false; scope.launch(Dispatchers.IO) { repo.warmFallbacks() } }.onFailure { loading = false; notice = "Возникла проблема. Обсуждаем решения в Telegram." }
        val start = System.currentTimeMillis()
        while (true) { delay(1000L); sessionSeconds = (System.currentTimeMillis() - start) / 1000L }
    }
    LaunchedEffect(Section.TV) { UsageTicker(store, Section.TV).run() }
    LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { store.saveScrollPosition(Section.TV, it.first, it.second) } }
    LaunchedEffect(listState) {
        val controller = (context as? ComponentActivity)?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) } ?: return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }.collect { if (it) controller.hide(WindowInsetsCompat.Type.navigationBars()) }
    }
    SearchAutoClose(searchOpen, query, lastActivity) { query = ""; searchOpen = false }
    LaunchedEffect(playbackError) { if (playbackError != null) { delay(3000L); val now = System.currentTimeMillis(); if (now - problemShownAt >= 5 * 60 * 1000L) { problemShownAt = now; notice = "Возникла проблема. Обсуждаем решения в Telegram." } } }
    LaunchedEffect(channels.isNotEmpty()) {
        if (channels.isEmpty()) return@LaunchedEffect
        health = health + scanUrls(channels.take(15), health)
        while (true) { delay(30 * 60 * 1000L); if (channels.isNotEmpty()) health = health + scanUrls(channels, health) }
    }
    LaunchedEffect(listState, channels) { snapshotFlow { listState.firstVisibleItemIndex }.collect { first -> val end = min(channels.size, first + 20); val pending = if (end > first) channels.subList(first, end).filter { health[it.url] == null }.take(20) else emptyList(); if (pending.isNotEmpty()) health = health + scanUrls(pending, health) } }
    LaunchedEffect(restoreItem, channels, networkAvailable) {
        val item = restoreItem ?: return@LaunchedEffect
        val idx = channels.indexOfFirst { it.url == item.url || it.name.equals(item.name, true) }
        if (idx >= 0) { selectedIndex = idx; if (networkAvailable) player.playWithFallback(listOf(channels[idx].url) + repo.fallbackUrlsFor(channels[idx].name)) }
    }
    LaunchedEffect(fullScreen) { onFullScreenChanged(fullScreen) }
    BackHandler(enabled = searchOpen && !fullScreen) { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }

    fun refresh() { scope.launch { refreshing = true; repo.loadSource(sourceIndex).onSuccess { channels = it; health = emptyMap(); launch(Dispatchers.IO) { repo.warmFallbacks() } }.onFailure { notice = "Возникла проблема. Обсуждаем решения в Telegram." }; refreshing = false } }
    if (fullScreen && selectedIndex in channels.indices) {
        TvPlayerScreen(
            player, channels[selectedIndex], playbackError, waitingNetwork || !networkAvailable,
            onBack = { fullScreen = false },
            onPrevious = { val idx = findPlayableIndex(channels, health, selectedIndex, -1); if (idx >= 0) { selectedIndex = idx; scope.launch { player.playWithFallback(listOf(channels[idx].url) + repo.fallbackUrlsFor(channels[idx].name)) }; onSelect(channels[idx]) } },
            onNext = { val idx = findPlayableIndex(channels, health, selectedIndex, 1); if (idx >= 0) { selectedIndex = idx; scope.launch { player.playWithFallback(listOf(channels[idx].url) + repo.fallbackUrlsFor(channels[idx].name)) }; onSelect(channels[idx]) } },
            onTripleTapReset = { sessionZoom.remove(channels[selectedIndex].key) }
        )
        return
    }

    val sorted = channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.key) }.thenBy { it.name.lowercase(Locale.ROOT) })
    val filtered = fuzzySort(sorted, query)
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize()) {
            if (!networkAvailable) NetworkBanner()
            AppHeader("ТВ", formatSessionTime(sessionSeconds), onBack, onSettings, searchOpen, query, { searchOpen = true; lastActivity = System.currentTimeMillis() }, { query = it; lastActivity = System.currentTimeMillis() }, { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() })
            restoreItem?.let { item -> RestoreBanner(item, { val idx = channels.indexOfFirst { c -> c.url == item.url || c.name.equals(item.name, true) }; if (idx >= 0) { selectedIndex = idx; fullScreen = true; scope.launch { player.playWithFallback(listOf(channels[idx].url) + repo.fallbackUrlsFor(channels[idx].name)) }; onSelect(channels[idx]); onRestoreDismiss() } }, onRestoreDismiss) }
            when {
                loading -> SkeletonList()
                filtered.isEmpty() -> if (query.isBlank()) EmptyPlaylistState() else EmptySearchState()
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::refresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(filtered, key = { it.key }) { channel ->
                            val status = health[channel.url] ?: AvailabilityStatus.UNKNOWN
                            val favorite = favorites.contains(channel.key)
                            ChannelRow(channel, favorite, false, status == AvailabilityStatus.OFFLINE, {
                                selectedIndex = channels.indexOfFirst { it.key == channel.key }
                                fullScreen = true
                                scope.launch { player.playWithFallback(listOf(channel.url) + repo.fallbackUrlsFor(channel.name)) }
                                onSelect(channel)
                            }, {
                                val value = !favorite
                                scope.launch { runCatching { store.setFavorite(Section.TV, channel.key, value); favorites = store.favorites(Section.TV); notice = if (value) "Добавлено в избранное" else "Удалено из избранного" }.onFailure { notice = "Не удалось обновить избранное" } }
                            })
                        }
                    }
                }
            }
            NoticeBanner(notice) { notice = null }
        }
    }
}

@Composable
private fun TvPlayerScreen(player: PlayerController, channel: StreamItem, playbackError: String?, waitingNetwork: Boolean, onBack: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onTripleTapReset: () -> Unit) {
    val activity = LocalContext.current as? ComponentActivity
    val playing by player.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val portrait = configuration.screenWidthDp < configuration.screenHeightDp
    var controlsVisible by remember(channel.key) { mutableStateOf(true) }
    var taps by remember { mutableIntStateOf(0) }
    var zoom by remember(channel.key) { mutableFloatStateOf(sessionZoom[channel.key] ?: 1f) }

    LaunchedEffect(controlsVisible) { if (controlsVisible) { delay(5000L); controlsVisible = false } }
    BackHandler { onBack() }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(channel.key) { detectTapGestures(onTap = { controlsVisible = true; taps++; if (taps >= 3) { zoom = 1f; sessionZoom.remove(channel.key); onTripleTapReset(); taps = 0 }; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, onDoubleTap = { controlsVisible = true }) }
            .pointerInput(channel.key) { detectTransformGestures { _, _, gestureZoom, _ -> zoom = (zoom * gestureZoom).coerceIn(1f, 3f); sessionZoom[channel.key] = zoom; controlsVisible = true } }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
            factory = { ctx -> PlayerView(ctx).apply { useController = false; player = player.player; setShutterBackgroundColor(AndroidColor.BLACK); setKeepContentOnPlayerReset(true) } }
        )
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout)
        ) { PlayerControls(channel, playing, onBack, onPrevious, onNext, portrait) }
        if (waitingNetwork) Text("Сигнал утерян, перепроверьте подключение к сети", Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (playbackError != null) Text("Возникла проблема. Обсуждаем решения в Telegram.", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlayerControls(channel: StreamItem, playing: Boolean, onBack: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, portrait: Boolean) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.95f)), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(channel.name, Modifier.weight(1f), color = Color.White, maxLines = 1, fontSize = if (portrait) 15.sp else 17.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Red) }
        }
    }
}

@Composable
private fun SettingsScreen(
    store: SettingsStore,
    pipEnabled: Boolean,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepDurationMinutes: Long,
    onPipChange: (Boolean) -> Unit,
    onSleepSelected: (Long) -> Unit,
    onSleepCancel: () -> Unit,
    onStatsReset: (Section) -> Unit,
    onSourceChanged: (Int) -> Unit,
    onDisclaimer: () -> Unit,
    onReset: () -> Unit
) {
    val tvUsage by store.usageFlow(Section.TV).collectAsState(initial = 0L)
    val radioUsage by store.usageFlow(Section.RADIO).collectAsState(initial = 0L)
    var sourceIndex by remember { mutableIntStateOf(0) }
    var sourceDialog by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { sourceIndex = store.sourceIndex() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Настройки", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold); IconButton(onClick = onDisclaimer) { Icon(Icons.Default.Info, "Информация", tint = Red) } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Таймер сна", fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.height(8.dp)); SleepCircleGrid(sleepRemaining, sleepUntil, sleepDurationMinutes, onSleepSelected, onSleepCancel) } } }
        item { DonationCard { notice = "Адрес кошелька скопирован" } }
        item { SettingsRow("PiP при сворачивании", "Сохраняется после перезапуска") { Switch(checked = pipEnabled, onCheckedChange = onPipChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = OfflineGray)) } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Статистика", fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Общее время использования ТВ: ${formatUsage(tvUsage)}", color = Color.LightGray); Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { onStatsReset(Section.TV) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик ТВ") }; Spacer(Modifier.height(8.dp)); Text("Общее время использования Радио: ${formatUsage(radioUsage)}", color = Color.LightGray); Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { onStatsReset(Section.RADIO) }, Modifier.fillMaxWidth()) { Text("Сбросить счётчик Радио") } } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Источник ТВ-плейлиста", fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(TV_SOURCES.getOrNull(sourceIndex)?.name ?: TV_SOURCES.first().name, color = Color.LightGray, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Button(onClick = { sourceDialog = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Выбрать источник") } } } }
        item { Button(onClick = onReset, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PanelAlt, contentColor = Color.White)) { Text("Сбросить настройки до заводских") } }
        item { OutlinedButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности") }; Text("TV / Radio. Online", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
    }
    NoticeBanner(notice) { notice = null }

    if (sourceDialog) AlertDialog(
        onDismissRequest = { sourceDialog = false }, title = { Text("Источник ТВ-плейлиста") },
        text = { Column { TV_SOURCES.forEachIndexed { index, source -> Row(Modifier.fillMaxWidth().combinedClickable(onClick = { sourceIndex = index; sourceDialog = false; onSourceChanged(index) }), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = sourceIndex == index, onClick = null); Text(source.name, Modifier.padding(vertical = 6.dp)) } } } },
        confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Закрыть") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepCircleGrid(remaining: Long, sleepUntil: Long, sleepDurationMinutes: Long, onSelect: (Long) -> Unit, onCancel: () -> Unit) {
    val values = listOf(15L to "15 мин", 30L to "30 мин", 60L to "1 ч", 120L to "2 ч", 240L to "4 ч", 480L to "8 ч", 600L to "10 ч", 900L to "15 ч", 1440L to "24 ч", 2160L to "36 ч")
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { pair ->
            val selected = pair.first == sleepDurationMinutes && sleepUntil > System.currentTimeMillis()
            val text = if (selected && remaining > 0) formatTimerCircle(remaining) else pair.second
            val bg by animateColorAsState(if (selected) Red else PanelAlt, label = "sleep-bg")
            Card(onClick = { if (selected) onCancel() else onSelect(pair.first) }, modifier = Modifier.size(66.dp), colors = CardDefaults.cardColors(containerColor = bg), shape = CircleShape, border = BorderStroke(1.dp, if (selected) Red else OfflineGray)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, fontSize = if (selected) 11.sp else 13.sp, color = Color.White, textAlign = TextAlign.Center) } }
        }
    }
}

@Composable
private fun DonationCard(onCopy: () -> Unit) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Red.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Favorite, null, tint = Red); Spacer(Modifier.width(8.dp)); Text("Поддержать проект", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp)); Text(DONATION_WALLET, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp)); Button(onClick = { val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("Wallet", DONATION_WALLET)); onCopy() }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Копировать") }
            Spacer(Modifier.height(10.dp)); Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.size(180.dp)) { Image(painterResource(R.drawable.qr_donate), "QR-код для пожертвований", Modifier.fillMaxSize().padding(4.dp)) }
        }
    }
}

@Composable
private fun DisclaimerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }; Text("Отказ от ответственности", fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
        item { Text("Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.\n\nПриложение не хранит, не распространяет и не модифицирует транслируемый контент. Все трансляции предоставляются третьими лицами. Разработчик не несёт ответственности за содержание транслируемого контента.\n\nЕсли вы являетесь правообладателем и считаете, что ваши права нарушаются — свяжитесь с нами через Telegram: $TELEGRAM_URL", color = Color.LightGray, fontSize = 14.sp, lineHeight = 21.sp) }
        item { OutlinedButton(onClick = { openUrl(context, TELEGRAM_URL) }, Modifier.fillMaxWidth()) { Text("Telegram") } }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 16.sp); Text(subtitle, fontSize = 11.sp, color = Color.Gray) }; trailing() } }
}

@Composable
private fun ChannelRow(item: StreamItem, favorite: Boolean, playing: Boolean, offline: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    val color by animateColorAsState(if (favorite) Red else Color.White, label = "favorite-color")
    val scale by animateFloatAsState(if (favorite) 1.18f else 1f, animationSpec = spring(), label = "favorite-scale")
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (favorite) Red.copy(alpha = 0.08f) else Panel), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(item, 56.dp, offline); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(item.name.uppercase(Locale.ROOT), fontSize = 14.sp, fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium, color = if (offline) OfflineGray else Color.White, maxLines = 1); if (offline) Text("• offline", fontSize = 10.sp, color = OfflineGray) }
            IconButton(onClick = onFavorite, Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = color) }
        }
    }
}

@Composable
private fun LogoImage(item: StreamItem, size: Dp, dimmed: Boolean = false) {
    val res = localRadioLogo(item.name)
    if (res != null) Image(painterResource(res), item.name, Modifier.size(size).clip(RoundedCornerShape(12.dp)).alpha(if (dimmed) 0.4f else 1f))
    else Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(ImageGray).alpha(if (dimmed) 0.4f else 1f), contentAlignment = Alignment.Center) { Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center) }
}

private fun localRadioLogo(name: String): Int? = when (name.uppercase(Locale.ROOT)) {
    "RECORD" -> R.drawable.logo_record
    "CHOCOLATE" -> R.drawable.logo_chocolate
    "ЭНЕРДЖИ" -> R.drawable.logo_energy
    "ULTRA" -> R.drawable.logo_ultra
    "КАЛЬЯН РЭП" -> R.drawable.logo_kalyan
    "PIRATE STATION" -> R.drawable.logo_pirate
    "VOCAL DRUM" -> R.drawable.logo_vocal
    "CHILL HOUSE" -> R.drawable.logo_chill
    "PSY TRANCE" -> R.drawable.logo_psy
    "METALCORE" -> R.drawable.logo_metalcore
    "ЮГ МОЛОДОЙ" -> R.drawable.logo_yug
    "RELAX" -> R.drawable.logo_relax
    else -> R.drawable.logo_fallback
}

@Composable
private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("Продолжить?", fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(item.name, color = Red, fontSize = 14.sp, maxLines = 2); Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onContinue, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Продолжить") }; OutlinedButton(onClick = onClose, Modifier.weight(1f)) { Text("Закрыть") } } }
    }
}

@Composable
private fun NetworkBanner() {
    Row(Modifier.fillMaxWidth().background(Color(0xFF2B1B1B)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WifiOff, null, tint = Color.LightGray, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Сигнал утерян, перепроверьте подключение к сети", color = Color.LightGray, fontSize = 12.sp) }
}

@Composable
private fun NoticeBanner(notice: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = notice != null, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
        Card(Modifier.fillMaxWidth().padding(10.dp), colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(notice.orEmpty(), Modifier.weight(1f), fontSize = 12.sp)
                if (notice == "Возникла проблема. Обсуждаем решения в Telegram.") TextButton(onClick = { openUrl(LocalContext.current, TELEGRAM_URL); onDismiss() }) { Text("Перейти", color = Red) }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    }
}

@Composable private fun EmptySearchState() { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Ничего не найдено", color = Color.Gray) } }
@Composable private fun EmptyPlaylistState() { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Плейлист пуст", color = Color.Gray) } }
@Composable private fun SkeletonList() { LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(8) { Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(PanelAlt)) } } }

@Composable private fun SearchAutoClose(searchOpen: Boolean, query: String, lastActivity: Long, onClose: () -> Unit) { LaunchedEffect(searchOpen, query, lastActivity) { if (searchOpen) { val stamp = lastActivity; delay(if (query.isBlank()) 5000L else 15000L); if (searchOpen && stamp == lastActivity) onClose() } } }

private suspend fun scanUrls(items: List<StreamItem>, existing: Map<String, AvailabilityStatus>): Map<String, AvailabilityStatus> {
    if (items.isEmpty()) return existing
    val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(3, TimeUnit.SECONDS).build()
    val sem = Semaphore(2)
    val result = ConcurrentHashMap<String, AvailabilityStatus>()
    kotlinx.coroutines.coroutineScope {
        items.map { item -> launch(Dispatchers.IO) { sem.acquire(); try { val req = Request.Builder().url(item.url).head().build(); result[item.url] = runCatching { client.newCall(req).execute().use { if (it.isSuccessful || it.code in 300..399) AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE } }.getOrElse { AvailabilityStatus.OFFLINE } } finally { sem.release() } } }.forEach { it.join() }
    }
    return existing + result
}

private fun findPlayableIndex(items: List<StreamItem>, health: Map<String, AvailabilityStatus>, from: Int, direction: Int): Int {
    if (items.isEmpty()) return -1
    var idx = from
    repeat(items.size) { idx = (idx + direction + items.size) % items.size; if (health[items[idx].url] != AvailabilityStatus.OFFLINE) return idx }
    return -1
}

private fun rememberNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true }
}

private fun fuzzySort(items: List<StreamItem>, query: String): List<StreamItem> = if (query.isBlank()) items else items.filter { it.name.lowercase(Locale.ROOT).contains(query.trim().lowercase(Locale.ROOT)) }
private fun formatSessionTime(seconds: Long): String = if (seconds < 3600L) "%02d:%02d".format(seconds / 60L, seconds % 60L) else "%02d:%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
private fun formatUsage(seconds: Long): String = "${seconds / 3600L} ч ${(seconds / 60L) % 60L} мин"
private fun formatTimerCircle(ms: Long): String { val total = max(0L, ms / 1000L); val h = total / 3600L; val m = (total / 60L) % 60L; val s = total % 60L; return if (h > 0) "%02d:%02d".format(h, m) else "%02d:%02d".format(m, s) }
private fun openUrl(context: Context, url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

private class UsageTicker(private val store: SettingsStore, private val section: Section) {
    suspend fun run() {
        var pending = 0L
        var lastPersist = System.currentTimeMillis()
        try {
            while (true) {
                delay(1000L); pending++
                if (System.currentTimeMillis() - lastPersist >= 5000L) { store.addUsageSeconds(section, pending); pending = 0L; lastPersist = System.currentTimeMillis() }
            }
        } finally { if (pending > 0L) store.addUsageSeconds(section, pending) }
    }
}
