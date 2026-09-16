package com.offex7.streamhub

import android.app.AlertDialog
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
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
import kotlinx.coroutines.flow.snapshotFlow
import java.util.Locale
import kotlin.math.max

private val Red = Color(0xFFE53935)
private val DeepRed = Color(0xFFB71C1C)
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
            val icon = Icon.createWithResource(this, android.R.drawable.ic_media_play)
            val action = android.app.RemoteAction(icon, "Play/Pause", "Play/Pause", pending)
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
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Red,
            onPrimary = Color.White,
            secondary = Red,
            onSecondary = Color.White,
            background = Background,
            onBackground = Color.White,
            surface = Panel,
            onSurface = Color.White,
            surfaceVariant = PanelAlt,
            onSurfaceVariant = Color.White
        ),
        content = content
    )
}

@Composable
private fun StreamHubApp(store: SettingsStore, tvPlayer: PlayerController, radioPlayer: RadioMediaController, activity: MainActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var disclaimerOpen by rememberSaveable { mutableStateOf(false) }
    var restoreItem by remember { mutableStateOf<StreamItem?>(null) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var sleepNotice by remember { mutableStateOf<String?>(null) }
    var exitDialog by remember { mutableStateOf(false) }
    var sessionStarted by remember { mutableLongStateOf(0L) }
    var committedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        activity.pipEnabled = store.pipEnabled()
        val lastExit = store.lastExitTime()
        val lastSection = store.lastSection()
        val fresh = lastExit > 0L && System.currentTimeMillis() - lastExit < RESTORE_WINDOW_MS
        if (fresh && lastSection != null) {
            section = lastSection
            restoreItem = store.lastStream(lastSection)
        }
        LogoCache.preload(context)
    }

    LaunchedEffect(section) {
        val active = section ?: run {
            sessionStarted = 0L
            committedAt = 0L
            return@LaunchedEffect
        }
        sessionStarted = System.currentTimeMillis()
        committedAt = sessionStarted
        while (section == active) {
            delay(1000L)
        }
    }

    LaunchedEffect(section, sessionStarted) {
        val active = section ?: return@LaunchedEffect
        if (sessionStarted == 0L) return@LaunchedEffect
        committedAt = System.currentTimeMillis()
        while (section == active) {
            delay(10_000L)
            if (section == active) {
                val now = System.currentTimeMillis()
                val delta = (now - committedAt).coerceAtLeast(0L)
                if (delta > 0L) {
                    store.addUsageMillis(active, delta)
                    committedAt = now
                }
            }
        }
    }

    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) { remaining = 0L; return@LaunchedEffect }
        while (sleepUntil > 0L) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                remaining = 0L
                sleepUntil = 0L
                tvPlayer.stop(); radioPlayer.stop()
                activity.finishAndRemoveTask()
                break
            }
            remaining = left
            if (left in 1L..30_000L) sleepNotice = "Таймер сна сработает через 30 секунд"
            delay(1000L)
        }
    }

    LaunchedEffect(sleepNotice) {
        if (sleepNotice != null) { delay(4000L); sleepNotice = null }
    }

    fun commitSessionAndClear() {
        val active = section ?: return
        val start = sessionStarted
        val now = System.currentTimeMillis()
        if (start > 0L) scope.launch { store.addUsageMillis(active, max(0L, now - start)) }
        sessionStarted = 0L
        committedAt = 0L
    }

    fun selectSection(value: Section) {
        section = value
        settingsOpen = false
        disclaimerOpen = false
        exitDialog = false
        scope.launch { store.setSection(value) }
    }

    fun goBackFromSection() {
        commitSessionAndClear()
        section = null
        restoreItem = null
        settingsOpen = false
        disclaimerOpen = false
        activity.tvViewing = false
        tvPlayer.stop(); radioPlayer.stop()
        scope.launch { store.clearSection() }
    }

    fun openSettings() { settingsOpen = true }

    val sleepText = if (remaining > 0L) formatRemaining(remaining) else null

    Surface(Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBars.union(androidx.compose.foundation.layout.WindowInsets.displayCutout)), color = Background) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(sleepNotice != null, enter = fadeIn(), exit = fadeOut()) {
                Text(sleepNotice ?: "", Modifier.fillMaxWidth().background(PanelAlt).padding(10.dp), fontSize = 12.sp)
            }
            when {
                disclaimerOpen -> DisclaimerScreen(onBack = { disclaimerOpen = false })
                settingsOpen -> SettingsScreen(
                    store = store,
                    sleepText = sleepText,
                    pipEnabled = activity.pipEnabled,
                    onPipChange = { enabled -> activity.pipEnabled = enabled; scope.launch { store.setPipEnabled(enabled) } },
                    onBack = { settingsOpen = false },
                    onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },
                    onStatsReset = { target -> scope.launch { store.resetUsage(target) } },
                    onDisclaimer = { disclaimerOpen = true },
                    onReset = {
                        commitSessionAndClear()
                        section = null
                        settingsOpen = false
                        disclaimerOpen = false
                        activity.pipEnabled = true
                        sleepUntil = 0L
                        restoreItem = null
                        scope.launch { store.resetAll() }
                    }
                )
                section == null -> PickerScreen(onSelect = ::selectSection, onSettings = ::openSettings)
                section == Section.RADIO -> RadioScreen(
                    player = radioPlayer,
                    store = store,
                    sleepText = sleepText,
                    restoreItem = restoreItem,
                    onRestoreDismiss = { restoreItem = null },
                    onBack = ::goBackFromSection,
                    onSettings = ::openSettings,
                    onSelect = { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.RADIO, item) } },
                    haptic = haptic
                )
                else -> TvScreen(
                    player = tvPlayer,
                    store = store,
                    sleepText = sleepText,
                    restoreItem = restoreItem,
                    onRestoreDismiss = { restoreItem = null },
                    onBack = ::goBackFromSection,
                    onSettings = ::openSettings,
                    onFullScreenChanged = { activity.tvViewing = it },
                    onSelect = { item -> restoreItem = null; scope.launch { store.saveLastStream(Section.TV, item) } },
                    haptic = haptic
                )
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            exitDialog -> exitDialog = false
            disclaimerOpen -> disclaimerOpen = false
            settingsOpen -> settingsOpen = false
            section != null -> goBackFromSection()
            else -> exitDialog = true
        }
    }

    if (exitDialog) {
        AlertDialog(onDismissRequest = { exitDialog = false }, title = { Text("Выйти из приложения?") }, text = { Text("Закрыть TV / Radio. Online?") }, confirmButton = { TextButton(onClick = { activity.finish() }) { Text("Выйти", color = Red) } }, dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Отмена") } })
    }
}

@Composable
private fun PickerScreen(onSelect: (Section) -> Unit, onSettings: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp)) {
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
        val horizontal = maxWidth > 560.dp
        if (horizontal) {
            Row(Modifier.fillMaxSize().padding(top = 36.dp, bottom = 68.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PickerCard("ТВ", "https://avatars.mds.yandex.net/i?id=ea6c1bcfb22a3359c5717fc55c8ca2a48b1fda50-5354520-images-thumbs&n=13", Icons.Default.Tv, Modifier.weight(1f)) { onSelect(Section.TV) }
                PickerCard("РАДИО", "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png", Icons.Default.Radio, Modifier.weight(1f)) { onSelect(Section.RADIO) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(top = 52.dp, bottom = 62.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PickerCard("ТВ", "https://avatars.mds.yandex.net/i?id=ea6c1bcfb22a3359c5717fc55c8ca2a48b1fda50-5354520-images-thumbs&n=13", Icons.Default.Tv, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.TV) }
                PickerCard("РАДИО", "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png", Icons.Default.Radio, Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.RADIO) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { runCatching { android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(TELEGRAM_URL)) }.getOrNull()?.let {} }) { Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp) }
            Text("TV_Radio_Online_V3.0", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PickerCard(label: String, url: String, fallback: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AsyncImage(model = url, contentDescription = label, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(12.dp))
            Icon(fallback, null, tint = Red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppHeader(title: String, sleepText: String?, onBack: (() -> Unit)?, onSettings: (() -> Unit)?, searchOpen: Boolean, query: String, onOpenSearch: () -> Unit, onQueryChange: (String) -> Unit, onCloseSearch: () -> Unit, sessionText: String?) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            if (sessionText != null) Text(sessionText, color = Red, fontSize = 11.sp)
            if (sleepText != null) { Spacer(Modifier.width(6.dp)); Text(sleepText, color = Red, fontSize = 11.sp) }
            if (!searchOpen) {
                IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Поиск", tint = Red) }
                if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
            } else IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
        }
        AnimatedVisibility(searchOpen, enter = expandHorizontally(), exit = shrinkHorizontally()) {
            OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), singleLine = true, placeholder = { Text("Поиск…") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, null) } })
        }
    }
}

@Composable
private fun SearchState(open: Boolean, query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    // Kept as a tiny reusable marker for future screens.
}

@Composable
private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 500.dp) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoImage(item, 42.dp, true)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Продолжить ${item.name}?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
                            Text("Последняя сессия", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onContinue) { Text("Продолжить", color = Red) }
                        TextButton(onClick = onClose) { Text("Закрыть") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoImage(item, 46.dp, true); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text("Продолжить ${item.name}?", fontWeight = FontWeight.SemiBold, maxLines = 1); Text("Последняя сессия", fontSize = 11.sp, color = Color.LightGray) }
                    TextButton(onClick = onContinue) { Text("Продолжить", color = Red) }; TextButton(onClick = onClose) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
private fun RadioScreen(player: RadioMediaController, store: SettingsStore, sleepText: String?, restoreItem: StreamItem?, onRestoreDismiss: () -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onSelect: (StreamItem) -> Unit, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playing by player.isPlaying.collectAsState()
    val currentIndex by player.currentIndex.collectAsState()
    val connected by player.connected.collectAsState()
    val error by player.error.collectAsState()
    val networkAvailable = rememberNetworkAvailable(context)
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    var lastActivity by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var problemVisible by remember { mutableStateOf(false) }
    var problemLastShown by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) { delay(1000L); sessionSeconds += 1L }
    }
    LaunchedEffect(searchOpen, query, lastActivity) {
        if (!searchOpen) return@LaunchedEffect
        val wait = if (query.isBlank()) 5_000L else 15_000L
        val stamp = lastActivity
        delay(wait)
        if (searchOpen && lastActivity == stamp) { query = ""; searchOpen = false }
    }
    LaunchedEffect(restoreItem, connected) {
        val item = restoreItem ?: return@LaunchedEffect
        val idx = RADIO_STATIONS.indexOfFirst { it.url == item.url }
        if (idx >= 0 && connected) player.play(idx)
    }
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        delay(3000L)
        val now = System.currentTimeMillis()
        if (now - problemLastShown >= 5 * 60 * 1000L) { problemLastShown = now; problemVisible = true }
    }

    val filtered = remember(query) { fuzzySort(RADIO_STATIONS, query) }
    val current = RADIO_STATIONS.getOrNull(currentIndex)

    BackHandler(enabled = searchOpen) { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }
    RadioNetworkEffect(context, searchOpen, { lastActivity = System.currentTimeMillis() })

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            if (!networkAvailable) NetworkBanner("Сигнал утерян, перепроверьте подключение к сети")
            AppHeader("РАДИО", sleepText, onBack, onSettings, searchOpen, query, { searchOpen = true; lastActivity = System.currentTimeMillis() }, { query = it; lastActivity = System.currentTimeMillis() }, { query = ""; searchOpen = false; lastActivity = System.currentTimeMillis() }, formatSessionTime(sessionSeconds))
            if (current != null) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LogoImage(current, 42.dp, false); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(current.name.uppercase(Locale.ROOT), maxLines = 1, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(if (playing) "Воспроизведение" else "Пауза", fontSize = 10.sp, color = Color.LightGray) }
                        IconButton(onClick = { player.previous() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                        IconButton(onClick = { player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                        IconButton(onClick = { player.next() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
                        IconButton(onClick = player::stop) { Icon(Icons.Default.Stop, null, tint = Red) }
                    }
                }
            }
            AnimatedVisibility(restoreItem != null, enter = fadeIn(), exit = fadeOut()) { restoreItem?.let { RestoreBanner(it, onContinue = { val idx = RADIO_STATIONS.indexOfFirst { s -> s.url == it.url }; if (idx >= 0) { player.play(idx); onSelect(it) } }, onClose = onRestoreDismiss) } }
            LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.url }) { station ->
                    ChannelRow(station, favorite = storeFavoriteState(store, station.url), playing = station.url == current?.url && playing, offline = false, tv = false, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.play(RADIO_STATIONS.indexOf(station)); onSelect(station) }, onLongClick = { scope.launch { store.setFavorite(station.url, !store.isFavorite(station.url)) } })
                }
                if (filtered.isEmpty()) item { EmptySearchState() }
            }
        }
        ProblemToast(problemVisible, { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(TELEGRAM_URL))) }; problemVisible = false }, { problemVisible = false })
    }
}

@Composable
private fun TvScreen(player: PlayerController, store: SettingsStore, sleepText: String?, restoreItem: StreamItem?, onRestoreDismiss: () -> Unit, onBack: () -> Unit, onSettings: () -> Unit, onFullScreenChanged: (Boolean) -> Unit, onSelect: (StreamItem) -> Unit, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val context = LocalContext.current
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val playbackError by player.error.collectAsState()
    val waitingNetwork by player.waitingForNetwork.collectAsState()
    val networkAvailable = rememberNetworkAvailable(context)
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var fullScreen by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var problemVisible by remember { mutableStateOf(false) }
    var problemLastShown by remember { mutableLongStateOf(0L) }
    var sessionSeconds by remember { mutableLongStateOf(0L) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        favorites = store.favorites()
        val saved = store.scrollPosition(Section.TV)
        listState.scrollToItem(saved.first, saved.second)
        val cached = repo.loadCachedIfFresh()
        if (cached != null) channels = cached
        loading = false
        repo.refreshCombined().onSuccess { channels = it; loadError = null }.onFailure { loadError = it.message }
    }
    LaunchedEffect(Unit) { while (true) { delay(1000L); sessionSeconds += 1L } }
    LaunchedEffect(listState, query) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { pos -> store.saveScrollPosition(Section.TV, pos.first, pos.second) }
    }
    LaunchedEffect(searchOpen, query) {
        if (!searchOpen) return@LaunchedEffect
        val wait = if (query.isBlank()) 5_000L else 15_000L
        val stamp = query
        delay(wait)
        if (searchOpen && query == stamp) { query = ""; searchOpen = false }
    }
    LaunchedEffect(playbackError, loadError) {
        if (playbackError == null && loadError == null) return@LaunchedEffect
        delay(3000L)
        val now = System.currentTimeMillis()
        if (now - problemLastShown >= 5 * 60 * 1000L) { problemLastShown = now; problemVisible = true }
    }
    LaunchedEffect(restoreItem, channels) {
        val item = restoreItem ?: return@LaunchedEffect
        val idx = channels.indexOfFirst { it.url == item.url }
        if (idx >= 0) { selectedIndex = idx; if (networkAvailable) player.play(item.url) else player.pause() }
    }

    fun openPlayer(item: StreamItem) { selectedIndex = channels.indexOfFirst { it.url == item.url }; player.play(item.url); fullScreen = true; onSelect(item) }
    fun refresh() { scope.launch { refreshing = true; repo.refreshCombined().onSuccess { channels = it; loadError = null }.onFailure { loadError = it.message }; refreshing = false } }

    if (fullScreen && selectedIndex in channels.indices) {
        BackHandler(enabled = true) { fullScreen = false }
        TvPlayerScreen(player, channels[selectedIndex], playbackError, waitingNetwork || !networkAvailable, if (!networkAvailable) "Сигнал утерян, перепроверьте подключение к сети" else null, onBack = { fullScreen = false }, onPrevious = { if (channels.isNotEmpty()) { selectedIndex = if (selectedIndex <= 0) channels.lastIndex else selectedIndex - 1; player.play(channels[selectedIndex].url); onSelect(channels[selectedIndex]) } }, onNext = { if (channels.isNotEmpty()) { selectedIndex = (selectedIndex + 1) % channels.size; player.play(channels[selectedIndex].url); onSelect(channels[selectedIndex]) } }, onRefresh = ::refresh)
        return
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            if (!networkAvailable && !waitingNetwork) NetworkBanner("Сигнал утерян, перепроверьте подключение к сети")
            AppHeader("ТВ", sleepText, onBack, onSettings, searchOpen, query, { searchOpen = true }, { query = it }, { query = ""; searchOpen = false }, formatSessionTime(sessionSeconds))
            AnimatedVisibility(restoreItem != null, enter = fadeIn(), exit = fadeOut()) { restoreItem?.let { RestoreBanner(it, onContinue = { openPlayer(it); onRestoreDismiss() }, onClose = onRestoreDismiss) } }
            val ordered = remember(channels, favorites, query) { fuzzySort(channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.url) }.thenBy { it.name.lowercase(Locale.ROOT) }), query) }
            when {
                loading -> SkeletonList()
                ordered.isEmpty() -> if (query.isNotBlank()) EmptySearchState() else EmptyPlaylistState(loadError) 
                else -> androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing = refreshing, onRefresh = ::refresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = listState, contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = {}) }) {
                        items(ordered, key = { it.url }) { channel ->
                            ChannelRow(channel, favorite = favorites.contains(channel.url), playing = false, offline = false, tv = true, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); openPlayer(channel) }, onLongClick = { scope.launch { val v = !favorites.contains(channel.url); store.setFavorite(channel.url, v); favorites = store.favorites() } })
                        }
                    }
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
                            val controller = WindowInsetsControllerCompat((context as? ComponentActivity)?.window ?: return@collect, View(context))
                            if (scrolling) controller.hide(WindowInsetsCompat.Type.navigationBars())
                        }
                    }
                }
            }
        }
        ProblemToast(problemVisible, { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(TELEGRAM_URL))) }; problemVisible = false }, { problemVisible = false })
    }
}

@Composable
private fun TvPlayerScreen(player: PlayerController, channel: StreamItem, playbackError: String?, waitingNetwork: Boolean, networkWarning: String?, onBack: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val playing by player.isPlaying.collectAsState()
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var zoom by remember(channel.url) { mutableFloatStateOf(sessionZoom[channel.url] ?: 1f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) { delay(5000L); controlsVisible = false }
    }
    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = true) {
        sessionZoom.remove(channel.url)
        activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        onBack()
    }

    Surface(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().pointerInput(channel.url) { detectTapGestures(onTap = { controlsVisible = true; if (it != null) {} }) }.pointerInput(channel.url) { detectTransformGestures { _, _, zoomChange, _ -> zoom = (zoom * zoomChange).coerceIn(1f, 3f); sessionZoom[channel.url] = zoom } }) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; player = player.player } }, update = { it.player = player.player }, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom))
            AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.TopCenter), enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                Row(Modifier.fillMaxWidth().background(Color(0xE51A1A1A)).padding(top = 4.dp, bottom = 3.dp, start = 4.dp, end = 4.dp).windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
                    Text(channel.name, Modifier.weight(1f), maxLines = 1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNext() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.stop() }) { Icon(Icons.Default.Stop, null, tint = Red) }
                }
            }
            if (waitingNetwork && networkWarning != null) NetworkBanner(networkWarning, Modifier.align(Alignment.Center))
            else if (playbackError != null) TextButton(onClick = onRefresh, modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBars)) { Text("Обновить", color = Red) }
            var tapCount by remember { mutableIntStateOf(0) }
            var lastTapAt by remember { mutableLongStateOf(0L) }
            Box(Modifier.fillMaxSize().pointerInput(channel.url) { detectTapGestures(onTap = { val now = System.currentTimeMillis(); tapCount = if (now - lastTapAt < 450L) tapCount + 1 else 1; lastTapAt = now; if (tapCount >= 3) { zoom = 1f; sessionZoom[channel.url] = 1f; tapCount = 0; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }; controlsVisible = true }) })
        }
    }
}

@Composable
private fun SettingsScreen(store: SettingsStore, sleepText: String?, pipEnabled: Boolean, onPipChange: (Boolean) -> Unit, onBack: () -> Unit, onSleep: (Long) -> Unit, onStatsReset: (Section) -> Unit, onDisclaimer: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val tvTotal by store.usageFlow(Section.TV).collectAsState(initial = 0L)
    val radioTotal by store.usageFlow(Section.RADIO).collectAsState(initial = 0L)
    LaunchedEffect(copied) { if (copied) { delay(1600L); copied = false } }
    val presets = listOf("15 мин" to 15L, "30 мин" to 30L, "1 ч" to 60L, "2 ч" to 120L, "4 ч" to 240L, "8 ч" to 480L, "10 ч" to 600L, "15 ч" to 900L, "24 ч" to 1440L, "36 ч" to 2160L)
    val sourceLabel = "Объединённый (smolnp + NaggDD + DropTV + Zabava)"

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }; Text("Настройки", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); sleepText?.let { Text(it, color = Red, fontSize = 11.sp) } }
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Таймер сна", style = MaterialTheme.typography.titleMedium) }
                items(presets) { (label, minutes) -> Button(onClick = { onSleep(minutes) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(label) } }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Пожертвования", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp)); Text("USDT TRC20", fontSize = 11.sp, color = Color.LightGray)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth().background(PanelAlt, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("Адрес кошелька", color = Color.LightGray, fontSize = 11.sp); Text(DONATION_WALLET, fontSize = 12.sp, maxLines = 2) }
                                TextButton(onClick = { val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("Donation wallet", DONATION_WALLET)); copied = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) { Text(if (copied) "Скопировано" else "Копировать", color = Red, fontSize = 12.sp) }
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.size(180.dp)) { AsyncImage(model = android.resourceURI(context, "qr_donate"), contentDescription = "QR пожертвований", modifier = Modifier.fillMaxSize().padding(7.dp)) }
                            }
                        }
                    }
                }
                item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tv, null, tint = Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("PiP при сворачивании", fontWeight = FontWeight.SemiBold); Text("Мини-окно ТВ при уходе из приложения", fontSize = 11.sp, color = Color.LightGray) }; Switch(checked = pipEnabled, onCheckedChange = onPipChange) } } }
                item { Text("Статистика", style = MaterialTheme.typography.titleMedium) }
                item { StatsRow("Общее время использования ТВ", tvTotal, "Сбросить счётчик ТВ") { onStatsReset(Section.TV) } }
                item { StatsRow("Общее время использования Радио", radioTotal, "Сбросить счётчик Радио") { onStatsReset(Section.RADIO) } }
                item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("Источник ТВ-плейлиста", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); Text(sourceLabel, fontSize = 13.sp, color = Color.White); Spacer(Modifier.height(5.dp)); Text("Автоматическое объединение обновляется при запуске", fontSize = 11.sp, color = Color.Gray) } } }
                item { TextButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности", color = Color.White) } }
                item { Button(onClick = onReset, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Сбросить настройки по умолчанию") } }
            }
        }
    }
}

@Composable
private fun StatsRow(title: String, millis: Long, resetLabel: String, onReset: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(formatDuration(millis), color = Red, fontSize = 18.sp) }; TextButton(onClick = onReset) { Text(resetLabel, color = Red, fontSize = 11.sp) } } }
}

@Composable
private fun DisclaimerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize().padding(14.dp).windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBars)) {
            Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }; Text("Отказ от ответственности", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
                item { Text("Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.\n\nПриложение не хранит, не распространяет и не модифицирует транслируемый контент. Все трансляции предоставляются третьими лицами. Разработчик не несёт ответственности за содержание транслируемого контента.\n\nЕсли вы являетесь правообладателем и считаете, что ваши права нарушаются — свяжитесь с нами через Telegram:", fontSize = 14.sp, lineHeight = 21.sp) }
                item { TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(TELEGRAM_URL))) }) { Text(TELEGRAM_URL, color = Red) } }
            }
        }
    }
}

@Composable
private fun ChannelRow(item: StreamItem, favorite: Boolean, playing: Boolean, offline: Boolean, tv: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().alpha(if (offline) 0.5f else 1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { LogoImage(item, 48.dp, tv); Spacer(Modifier.width(9.dp)); Text(item.name.uppercase(Locale.ROOT).ifBlank { "БЕЗ НАЗВАНИЯ" }, Modifier.weight(1f), maxLines = 2); if (offline) Icon(Icons.Default.WifiOff, "Офлайн", tint = Color.Gray, modifier = Modifier.size(18.dp)); if (playing) Icon(Icons.Default.PlayArrow, "Играет", tint = Red, modifier = Modifier.size(18.dp)); if (favorite) { Spacer(Modifier.width(5.dp)); Icon(Icons.Default.Favorite, "Избранное", tint = Red, modifier = Modifier.size(17.dp)) } }
    }
}

@Composable
private fun LogoImage(item: StreamItem, size: Dp, tv: Boolean) {
    val context = LocalContext.current
    var sourceIndex by remember(item.url) { mutableIntStateOf(0) }
    val sources = remember(item.url, item.logoUrl, item.epgLogoUrl, tv) { listOfNotNull(LogoCache.knownLogo(item), item.logoUrl, item.epgLogoUrl).distinct() }
    if (sourceIndex < sources.size) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            AsyncImage(model = ImageRequest.Builder(context).data(sources[sourceIndex]).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.name, modifier = Modifier.size(size), onError = { sourceIndex += 1 })
        }
    } else PlaceholderLogo(size)
}

@Composable
private fun PlaceholderLogo(size: Dp) {
    Box(Modifier.size(size).background(Skeleton, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("NO Image", color = Color.Gray, fontSize = max(9f, size.value * 0.15f).sp, maxLines = 1) }
}

@Composable
private fun SkeletonList(count: Int = 10) {
    LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(count) { Row(Modifier.fillMaxWidth().background(Skeleton, RoundedCornerShape(13.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(PanelAlt, RoundedCornerShape(10.dp))); Spacer(Modifier.width(9.dp)); Box(Modifier.height(18.dp).fillMaxWidth(0.7f).background(PanelAlt, RoundedCornerShape(7.dp))) } } }
}

@Composable
private fun EmptySearchState() { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Info, null, tint = Red, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(8.dp)); Text("Ничего не найдено") } }

@Composable
private fun EmptyPlaylistState(error: String?) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(error ?: "Нет доступных каналов", color = Red); Text("Потяните вниз, чтобы обновить", color = Color.Gray, fontSize = 12.sp) } }

@Composable
private fun NetworkBanner(message: String, modifier: Modifier = Modifier) { Row(modifier.fillMaxWidth().background(PanelAlt).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WifiOff, null, tint = Red, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(message, fontSize = 13.sp) } }

@Composable
private fun ProblemToast(visible: Boolean, onGo: () -> Unit, onClose: () -> Unit) {
    AnimatedVisibility(visible, modifier = Modifier.fillMaxSize(), enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
            Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) { Text("Возникла проблема. Обсуждаем решения в Telegram.", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onGo) { Text("Перейти", color = Red) }; TextButton(onClick = onClose) { Text("Закрыть") } } }
            }
        }
    }
}

private class LogoCache {
    companion object {
        private val tvDomains = listOf("1tv.ru", "russia.tv", "ntv.ru", "tnt-online.ru", "ctc.ru", "ren.tv", "5-tv.ru", "matchtv.ru", "tvc.ru", "otr-online.ru", "zvezdanews.ru", "mir24.tv", "rbc.ru", "moscow24.ru", "karusel-tv.ru", "tvkultura.ru", "russia24.tv", "tv3.ru")
        fun knownLogo(item: StreamItem): String? {
            val n = item.name.lowercase(Locale.ROOT)
            val direct = when {
                n.contains("relax") -> "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13"
                n.contains("record") -> "https://commons.wikimedia.org/wiki/Special:FilePath/Radio%20Record%20Logo%202026.svg?width=300"
                n.contains("comedy") -> "https://comedy-radio.ru/favicon.ico"
                n.contains("авторадио") -> "https://www.radioplay.bg/assets/logos/avtoradio.png"
                n.contains("энерджи") -> "https://www.energyfm.ru/favicon.ico"
                n.contains("шоколад") -> "https://www.chocoradio.ru/favicon.ico"
                n.contains("ультра") -> "https://seeklogo.com/images/U/ultra-logo-191058.png"
                n.contains("пират") -> "https://radiorecord.ru/favicon.ico"
                n.contains("psy") -> "https://dfm.ru/favicon.ico"
                n.contains("vocal") -> "https://dfm.ru/favicon.ico"
                n.contains("chill") -> "https://radiorecord.ru/favicon.ico"
                else -> null
            }
            if (direct != null) return direct
            if (item.url.contains("http")) return null
            return null
        }
        fun preload(context: Context) {
            val loader = coil3.ImageLoader.Builder(context).build()
            val urls = tvDomains.map { "https://www.google.com/s2/favicons?sz=128&domain=$it" } + listOf(
                "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13",
                "https://comedy-radio.ru/favicon.ico",
                "https://www.energyfm.ru/favicon.ico",
                "https://www.chocoradio.ru/favicon.ico"
            )
            urls.forEach { loader.enqueue(ImageRequest.Builder(context).data(it).build()) }
        }
    }
}

@Composable
private fun rememberNetworkAvailable(context: Context): Boolean {
    var available by remember { mutableStateOf(checkNetwork(context)) }
    LaunchedEffect(Unit) { while (true) { available = checkNetwork(context); delay(1500L) } }
    return available
}

private fun checkNetwork(context: Context): Boolean {
    val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
private fun RadioNetworkEffect(context: Context, searchOpen: Boolean, onInteraction: () -> Unit) {}

@Composable
private fun storeFavoriteState(store: SettingsStore, url: String): Boolean {
    var state by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) { state = store.isFavorite(url) }
    return state
}

private fun normalizeSearch(text: String): String = text.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

private fun fuzzyScore(name: String, query: String): Int { if (query.isBlank()) return 0; val n = normalizeSearch(name); val q = normalizeSearch(query); if (q.isBlank()) return 0; if (n.contains(q)) return 10_000 - (n.length - q.length); var pos = 0; var matched = 0; q.forEach { c -> val found = n.indexOf(c, pos); if (found >= 0) { matched++; pos = found + 1 } }; return if (matched == q.length) 5_000 - (pos - q.length) else Int.MIN_VALUE }

private fun fuzzySort(items: List<StreamItem>, query: String): List<StreamItem> = if (query.isBlank()) items else items.map { it to fuzzyScore(it.name, query) }.filter { it.second != Int.MIN_VALUE }.sortedByDescending { it.second }.map { it.first }

private fun formatRemaining(ms: Long): String { val total = (ms / 1000L).coerceAtLeast(0L); val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60; return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s) }
private fun formatSessionTime(seconds: Long): String = formatRemaining(seconds * 1000L)
private fun formatDuration(ms: Long): String = formatRemaining(ms)

private fun android.resourceURI(context: Context, name: String): String = "android.resource://${context.packageName}/drawable/$name"
