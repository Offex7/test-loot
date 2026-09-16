package com.offex7.streamhub

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Red = Color(0xFFE53935)
private val DeepRed = Color(0xFFB71C1C)
private val Background = Color(0xFF090909)
private val Panel = Color(0xFF151515)
private val PanelAlt = Color(0xFF202020)
private val Skeleton = Color(0xFF2A2A2A)
private const val RESTORE_WINDOW_MS = 10 * 60 * 1000L
private const val DONATION_WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: SettingsStore
    private lateinit var tvPlayer: PlayerController
    private lateinit var radioController: RadioMediaController
    internal var tvViewing = false
    internal var pipEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).setActions(listOf(action)).build()
            )
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
    var section by remember { mutableStateOf<Section?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var restoreItem by remember { mutableStateOf<StreamItem?>(null) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var sleepNotice by remember { mutableStateOf<String?>(null) }
    var sleepFadeDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activity.pipEnabled = store.pipEnabled()
        val lastExit = store.lastExitTime()
        val lastSection = store.lastSection()
        val fresh = lastExit > 0L && System.currentTimeMillis() - lastExit < RESTORE_WINDOW_MS
        if (fresh && lastSection != null) {
            section = lastSection
            restoreItem = store.lastStream(lastSection)
        } else {
            section = null
        }
    }

    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) {
            remaining = 0L
            sleepFadeDone = false
            return@LaunchedEffect
        }
        while (sleepUntil > 0L) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                remaining = 0L
                sleepUntil = 0L
                tvPlayer.stop()
                radioPlayer.stop()
                activity.finishAndRemoveTask()
                break
            }
            remaining = left
            if (left in 1L..30_000L) sleepNotice = "Таймер сна сработает через 30 секунд"
            if (left in 1L..10_000L && !sleepFadeDone) {
                sleepFadeDone = true
                tvPlayer.fadeOut(10_000L)
                radioPlayer.fadeOut(10_000L)
            }
            delay(1000L)
        }
    }

    LaunchedEffect(sleepNotice) {
        if (sleepNotice != null) {
            delay(4000L)
            sleepNotice = null
        }
    }

    fun selectSection(value: Section) {
        section = value
        settingsOpen = false
        scope.launch { store.setSection(value) }
    }

    fun backToPicker() {
        section = null
        settingsOpen = false
        restoreItem = null
        activity.tvViewing = false
        tvPlayer.stop()
        radioPlayer.stop()
        scope.launch { store.clearSection() }
    }

    val sleepText = if (remaining > 0L) formatRemaining(remaining) else null

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            AnimatedVisibility(sleepNotice != null, enter = fadeIn(), exit = fadeOut()) {
                Text(sleepNotice ?: "", Modifier.fillMaxWidth().background(PanelAlt).padding(10.dp), fontSize = 12.sp)
            }
            when {
                settingsOpen -> SettingsScreen(
                    store = store,
                    sleepText = sleepText,
                    pipEnabled = activity.pipEnabled,
                    onPipChange = { enabled ->
                        activity.pipEnabled = enabled
                        scope.launch { store.setPipEnabled(enabled) }
                    },
                    onBack = { settingsOpen = false },
                    onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },
                    onReset = ::backToPicker
                )
                section == null -> PickerScreen(::selectSection)
                section == Section.RADIO -> RadioScreen(
                    player = radioPlayer,
                    store = store,
                    sleepText = sleepText,
                    restoreItem = restoreItem,
                    onRestoreDismiss = { restoreItem = null },
                    onBack = ::backToPicker,
                    onSettings = { settingsOpen = true },
                    onSelect = { item ->
                        restoreItem = null
                        scope.launch { store.saveLastStream(Section.RADIO, item) }
                    },
                    haptic = haptic
                )
                else -> TvScreen(
                    player = tvPlayer,
                    store = store,
                    sleepText = sleepText,
                    restoreItem = restoreItem,
                    onRestoreDismiss = { restoreItem = null },
                    onBack = ::backToPicker,
                    onSettings = { settingsOpen = true },
                    onFullScreenChanged = { activity.tvViewing = it },
                    onSelect = { item ->
                        restoreItem = null
                        scope.launch { store.saveLastStream(Section.TV, item) }
                    },
                    haptic = haptic
                )
            }
        }
    }
}

@Composable
private fun PickerScreen(onSelect: (Section) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TV / RADIO", color = Red, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("ONLINE", letterSpacing = 5.sp)
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(onClick = { onSelect(Section.TV) }, Modifier.weight(1f).height(96.dp), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(18.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Tv, null, Modifier.size(30.dp)); Spacer(Modifier.height(5.dp)); Text("TV") }
                }
                Button(onClick = { onSelect(Section.RADIO) }, Modifier.weight(1f).height(96.dp), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(18.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Radio, null, Modifier.size(30.dp)); Spacer(Modifier.height(5.dp)); Text("RADIO") }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    title: String,
    sleepText: String?,
    onBack: (() -> Unit)?,
    onSettings: (() -> Unit)?,
    searchOpen: Boolean,
    query: String,
    onOpenSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sleepText != null) Text(sleepText, color = Red, fontSize = 11.sp)
            if (!searchOpen) IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Поиск", tint = Red) }
            if (!searchOpen && onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
            if (searchOpen) IconButton(onClick = onCloseSearch) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
        }
        AnimatedVisibility(searchOpen, enter = expandHorizontally(), exit = shrinkHorizontally()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                singleLine = true,
                placeholder = { Text("Поиск канала…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) }
                }
            )
        }
    }
}

@Composable
private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(item, 48.dp, true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Продолжить ${item.name}?", fontWeight = FontWeight.SemiBold)
                Text("Последняя сессия", fontSize = 11.sp, color = Color.LightGray)
            }
            TextButton(onClick = onContinue) { Text("Продолжить", color = Red) }
            TextButton(onClick = onClose) { Text("Закрыть") }
        }
    }
}

@Composable
private fun RadioScreen(
    player: RadioMediaController,
    store: SettingsStore,
    sleepText: String?,
    restoreItem: StreamItem?,
    onRestoreDismiss: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSelect: (StreamItem) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playing by player.isPlaying.collectAsState()
    val currentIndex by player.currentIndex.collectAsState()
    val connected by player.connected.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(restoreItem, connected) {
        val item = restoreItem ?: return@LaunchedEffect
        val idx = RADIO_STATIONS.indexOfFirst { it.url == item.url }
        if (idx >= 0 && connected && isWifiConnected(context)) player.play(idx)
    }

    val filtered = remember(query) { fuzzySort(RADIO_STATIONS, query) }
    val current = RADIO_STATIONS.getOrNull(currentIndex)

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            AppHeader("RADIO", sleepText, onBack, onSettings, searchOpen, query, { searchOpen = true }, { query = it }, { query = ""; searchOpen = false })
            AnimatedVisibility(restoreItem != null, enter = fadeIn(), exit = fadeOut()) {
                restoreItem?.let { item -> RestoreBanner(item, onContinue = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val idx = RADIO_STATIONS.indexOfFirst { it.url == item.url }
                    if (idx >= 0) { player.play(idx); onSelect(item) }
                }, onClose = onRestoreDismiss) }
            }
            if (current != null) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        LogoImage(current, 64.dp, false)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(current.name, fontWeight = FontWeight.SemiBold); Text(if (playing) "Воспроизведение" else "Пауза", fontSize = 11.sp, color = Color.LightGray) }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.previous() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.next() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
                        IconButton(onClick = player::stop) { Icon(Icons.Default.Stop, null, tint = Red) }
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered, key = { it.url }) { station ->
                    ChannelRow(station, favorite = false, playing = station.url == current?.url && playing, offline = false, tv = false, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.play(RADIO_STATIONS.indexOf(station))
                        onSelect(station)
                    }, onLongClick = {
                        scope.launch { store.setFavorite(station.url, !store.isFavorite(station.url)) }
                        Toast.makeText(context, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            if (filtered.isEmpty()) EmptySearchState()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TvScreen(
    player: PlayerController,
    store: SettingsStore,
    sleepText: String?,
    restoreItem: StreamItem?,
    onRestoreDismiss: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onFullScreenChanged: (Boolean) -> Unit,
    onSelect: (StreamItem) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var sourceIndex by remember { mutableIntStateOf(0) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var availability by remember { mutableStateOf<Map<String, AvailabilityStatus>>(emptyMap()) }
    var availabilityStamp by remember { mutableLongStateOf(0L) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var fullScreen by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val playbackError by player.error.collectAsState()
    val waitingNetwork by player.waitingForNetwork.collectAsState()
    val networkWarning by player.networkWarning.collectAsState()

    LaunchedEffect(Unit) { favorites = store.favorites() }
    LaunchedEffect(fullScreen) { onFullScreenChanged(fullScreen) }
    LaunchedEffect(Unit) {
        sourceIndex = store.sourceIndex().coerceIn(0, TV_SOURCES.lastIndex)
        val cached = repo.loadCachedIfFresh()
        if (cached != null) {
            channels = cached
            loading = false
        } else {
            repo.refreshInOrder(sourceIndex).onSuccess { (idx, list) ->
                sourceIndex = idx
                channels = list
                store.setSourceIndex(idx)
                loadError = null
            }.onFailure { loadError = it.message ?: "Не удалось загрузить плейлист" }
            loading = false
        }
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty() && System.currentTimeMillis() - availabilityStamp >= 60 * 60 * 1000L) {
            availabilityStamp = System.currentTimeMillis()
            availability = repo.checkAvailability(channels.take(50))
        }
    }
    LaunchedEffect(restoreItem, channels) {
        val item = restoreItem ?: return@LaunchedEffect
        val idx = channels.indexOfFirst { it.url == item.url }
        if (idx >= 0) {
            selectedIndex = idx
            if (isWifiConnected(context)) {
                player.play(item.url)
            } else {
                player.pause()
            }
        }
    }

    suspend fun refresh() {
        refreshing = true
        repo.refreshInOrder(sourceIndex).onSuccess { (idx, list) -> sourceIndex = idx; channels = list; store.setSourceIndex(idx); availabilityStamp = 0L; loadError = null }.onFailure { loadError = it.message }
        refreshing = false
    }

    suspend fun loadNextPlaylist() {
        refreshing = true
        val next = (sourceIndex + 1) % TV_SOURCES.size
        repo.refreshInOrder(next).onSuccess { (idx, list) -> sourceIndex = idx; channels = list; store.setSourceIndex(idx); availabilityStamp = 0L; loadError = null }.onFailure { loadError = it.message ?: "Канал недоступен" }
        refreshing = false
    }

    val ordered = remember(channels, favorites, query, availability) {
        val sorted = channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.url) }.thenBy { it.name.lowercase(Locale.ROOT) })
        fuzzySort(sorted, query)
    }

    if (fullScreen && selectedIndex in channels.indices) {
        TvPlayerScreen(player, channels[selectedIndex], playbackError, waitingNetwork, networkWarning, onBack = { fullScreen = false }, onPrevious = {
            if (channels.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedIndex = if (selectedIndex <= 0) channels.lastIndex else selectedIndex - 1
                player.play(channels[selectedIndex].url)
                onSelect(channels[selectedIndex])
            }
        }, onNext = {
            if (channels.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedIndex = (selectedIndex + 1) % channels.size
                player.play(channels[selectedIndex].url)
                onSelect(channels[selectedIndex])
            }
        }, onOtherPlaylist = { scope.launch { loadNextPlaylist() } })
        return
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            AnimatedVisibility(waitingNetwork, enter = fadeIn(), exit = fadeOut()) { NetworkBanner(networkWarning) }
            AppHeader("TV", sleepText, onBack, onSettings, searchOpen, query, { searchOpen = true }, { query = it }, { query = ""; searchOpen = false })
            AnimatedVisibility(restoreItem != null, enter = fadeIn(), exit = fadeOut()) {
                restoreItem?.let { item -> RestoreBanner(item, onContinue = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val idx = channels.indexOfFirst { it.url == item.url }
                    if (idx >= 0) { selectedIndex = idx; fullScreen = true; player.play(item.url); onSelect(item) }
                }, onClose = onRestoreDismiss) }
            }
            when {
                loading -> SkeletonList()
                ordered.isEmpty() -> {
                    if (query.isNotBlank()) EmptySearchState() else EmptyPlaylistState(loadError)
                }
                else -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { scope.launch { refresh() } },
                    state = refreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(ordered, key = { it.url }) { channel ->
                            val offline = availability[channel.url] == AvailabilityStatus.OFFLINE
                            ChannelRow(channel.copy(isOffline = offline), favorite = favorites.contains(channel.url), playing = false, offline = offline, tv = true, onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = channels.indexOfFirst { it.url == channel.url }
                                player.play(channel.url)
                                fullScreen = true
                                onSelect(channel)
                            }, onLongClick = {
                                scope.launch {
                                    val newValue = !favorites.contains(channel.url)
                                    store.setFavorite(channel.url, newValue)
                                    favorites = store.favorites()
                                }
                                Toast.makeText(context, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(item: StreamItem, favorite: Boolean, playing: Boolean, offline: Boolean, tv: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().alpha(if (offline) 0.5f else 1f).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(item, 48.dp, tv)
            Spacer(Modifier.width(10.dp))
            Text(item.name, Modifier.weight(1f), maxLines = 2)
            if (offline) Icon(Icons.Default.WifiOff, "Офлайн", tint = Color.Gray, modifier = Modifier.size(18.dp))
            else if (playing) Icon(Icons.Default.PlayArrow, "Играет", tint = Red, modifier = Modifier.size(18.dp))
            if (favorite) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.Favorite, "Избранное", tint = Red, modifier = Modifier.size(17.dp)) }
        }
    }
}

@Composable
private fun LogoImage(item: StreamItem, size: Dp, tv: Boolean) {
    var sourceIndex by remember(item.url) { mutableIntStateOf(0) }
    val sources = remember(item.logoUrl, item.epgLogoUrl) { listOfNotNull(item.logoUrl, item.epgLogoUrl).distinct() }
    if (sourceIndex < sources.size) {
        var loading by remember(sources[sourceIndex]) { mutableStateOf(true) }
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            if (loading) ShimmerBox(Modifier.size(size), CircleShape)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(sources[sourceIndex]).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
                contentDescription = item.name,
                modifier = Modifier.size(size),
                onLoading = { loading = true },
                onSuccess = { loading = false },
                onError = {
                    loading = false
                    sourceIndex = if (sourceIndex + 1 < sources.size) sourceIndex + 1 else sources.size
                }
            )
        }
    } else PlaceholderLogo(item.name, size, tv)
}

@Composable
private fun PlaceholderLogo(name: String, size: Dp, tv: Boolean) {
    Box(Modifier.size(size).background(Brush.linearGradient(listOf(Red, DeepRed)), CircleShape), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
        Text(if (tv) "📺" else "📻", fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
    }
}

@Composable
private fun SkeletonList(count: Int = 10) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(count) { SkeletonRow() } }
}

@Composable
private fun SkeletonRow() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(1200)),
        label = "shimmer_shift"
    )
    val brush = Brush.linearGradient(listOf(Skeleton, Color(0xFF454545), Skeleton), start = androidx.compose.ui.geometry.Offset(shift * 500f, 0f), end = androidx.compose.ui.geometry.Offset((shift + 1f) * 500f, 0f))
    Row(Modifier.fillMaxWidth().background(brush, RoundedCornerShape(13.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(Skeleton, CircleShape))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.height(18.dp).fillMaxWidth(0.72f).background(Skeleton, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "logo_shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(900)),
        label = "logo_shift"
    )
    Box(modifier.background(Brush.linearGradient(listOf(Skeleton, Color(0xFF454545), Skeleton), start = androidx.compose.ui.geometry.Offset(x * 100f, 0f), end = androidx.compose.ui.geometry.Offset((x + 1f) * 100f, 0f)), shape))
}

@Composable
private fun EmptySearchState() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, tint = Red, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(10.dp))
        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/TvRadioOnline"))) }) { Text("Перейти в Telegram для обсуждения", color = Red) }
    }
}

@Composable
private fun EmptyPlaylistState(error: String?) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(error ?: "Нет доступных каналов", color = Red)
    }
}

@Composable
private fun NetworkBanner(message: String?) {
    Row(Modifier.fillMaxWidth().background(PanelAlt).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.WifiOff, null, tint = Red, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(message ?: "Ожидание сети…", fontSize = 13.sp)
    }
}

@Composable
private fun TvPlayerScreen(
    player: PlayerController,
    channel: StreamItem,
    playbackError: String?,
    waitingNetwork: Boolean,
    networkWarning: String?,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOtherPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val playing by player.isPlaying.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player.player } }, update = { it.player = player.player }, modifier = Modifier.fillMaxSize())
            AnimatedVisibility(waitingNetwork, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) { NetworkBanner(networkWarning) }
            Row(Modifier.fillMaxWidth().background(Color(0xD9161616), RoundedCornerShape(14.dp)).padding(4.dp).align(Alignment.BottomCenter), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
                Text(channel.name, Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Red) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); player.toggle() }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Red) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNext() }) { Icon(Icons.Default.SkipNext, null, tint = Red) }
            }
            if (playbackError != null) {
                TextButton(onClick = onOtherPlaylist, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 65.dp)) { Text("Другой плейлист", color = Red) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    store: SettingsStore,
    sleepText: String?,
    pipEnabled: Boolean,
    onPipChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSleep: (Long) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var sourceIndex by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { sourceIndex = store.sourceIndex().coerceIn(0, TV_SOURCES.lastIndex) }
    LaunchedEffect(copied) { if (copied) { delay(1800L); copied = false } }
    val presets = listOf("15 мин" to 15L, "30 мин" to 30L, "1 ч" to 60L, "2 ч" to 120L, "4 ч" to 240L, "8 ч" to 480L)

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
                Text("Настройки", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (sleepText != null) Text(sleepText, color = Red, fontSize = 11.sp)
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, null, tint = Red)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("PiP при сворачивании", fontWeight = FontWeight.SemiBold)
                                Text("Мини-окно ТВ при уходе из приложения", fontSize = 11.sp, color = Color.LightGray)
                            }
                            Switch(checked = pipEnabled, onCheckedChange = onPipChange)
                        }
                    }
                }
                item { Text("Таймер сна", style = MaterialTheme.typography.titleMedium) }
                items(presets) { (label, minutes) ->
                    Button(onClick = { onSleep(minutes) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(label) }
                }
                item {
                    OutlinedTextField(value = custom, onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) custom = it }, label = { Text("Свои минуты (1–480)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(7.dp))
                    Button(onClick = { custom.toLongOrNull()?.coerceIn(1L, 480L)?.let(onSleep) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Запустить свой таймер") }
                }
                item { Text("Источник ТВ-плейлиста", style = MaterialTheme.typography.titleMedium) }
                items(TV_SOURCES.indices.toList()) { index ->
                    TextButton(onClick = { sourceIndex = index; scope.launch { store.setSourceIndex(index) } }, modifier = Modifier.fillMaxWidth()) { Text((if (sourceIndex == index) "●  " else "○  ") + TV_SOURCES[index].name, color = if (sourceIndex == index) Red else Color.White) }
                }
                item { Text("Пожертвования", style = MaterialTheme.typography.titleMedium) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, null, tint = Red)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Поддержать проект", fontWeight = FontWeight.SemiBold)
                                    Text("USDT TRC20", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Row(Modifier.fillMaxWidth().background(PanelAlt, RoundedCornerShape(10.dp)).combinedClickable(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Donation wallet", DONATION_WALLET))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                copied = true
                            }, onLongClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Donation wallet", DONATION_WALLET))
                                copied = true
                            }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Адрес кошелька", color = Color.LightGray, fontSize = 11.sp)
                                    Text(DONATION_WALLET, color = Color.White, fontSize = 12.sp, maxLines = 2)
                                }
                                Text(if (copied) "Скопировано" else "Копировать", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item { Button(onClick = onReset, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Сбросить выбор раздела") } }
            }
        }
    }
}

private fun normalizeSearch(text: String): String = text.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

private fun fuzzyScore(name: String, query: String): Int {
    if (query.isBlank()) return 0
    val n = normalizeSearch(name)
    val q = normalizeSearch(query)
    if (q.isBlank()) return 0
    if (n.contains(q)) return 10_000 - (n.length - q.length)
    var pos = 0
    var matched = 0
    q.forEach { c ->
        val found = n.indexOf(c, pos)
        if (found >= 0) { matched++; pos = found + 1 }
    }
    return if (matched == q.length) 5_000 - (pos - q.length) else Int.MIN_VALUE
}

private fun fuzzySort(items: List<StreamItem>, query: String): List<StreamItem> = if (query.isBlank()) items else items.map { it to fuzzyScore(it.name, query) }.filter { it.second != Int.MIN_VALUE }.sortedByDescending { it.second }.map { it.first }

private fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
    return cm.activeNetwork?.let { network -> cm.getNetworkCapabilities(network)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } == true
}

private fun formatRemaining(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
