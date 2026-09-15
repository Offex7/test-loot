package com.offex7.streamhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Red = Color(0xFFE53935)
private val Background = Color(0xFF090909)
private val Panel = Color(0xFF151515)
private val PanelAlt = Color(0xFF202020)

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: SettingsStore
    private lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(applicationContext)
        playerController = PlayerController(applicationContext)
        setContent {
            StreamHubTheme {
                StreamHubApp(settingsStore, playerController)
            }
        }
    }

    override fun onStop() {
        playerController.pause()
        super.onStop()
    }

    override fun onDestroy() {
        playerController.release()
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
private fun StreamHubApp(store: SettingsStore, player: PlayerController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf<Section?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        section = store.lastSection()
    }

    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) {
            remaining = 0L
            return@LaunchedEffect
        }
        while (true) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) {
                remaining = 0L
                sleepUntil = 0L
                player.stop()
                finishAndRemoveTask()
                break
            }
            remaining = left
            delay(1000L)
        }
    }

    val sleepText = if (remaining > 0L) formatRemaining(remaining) else null

    fun resetSection() {
        section = null
        settingsOpen = false
        player.stop()
        scope.launch { store.clearSection() }
    }

    when {
        settingsOpen -> SettingsScreen(
            store = store,
            sleepText = sleepText,
            onBack = { settingsOpen = false },
            onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },
            onReset = ::resetSection
        )

        section == null -> PickerScreen { selected ->
            section = selected
            scope.launch { store.setSection(selected) }
        }

        section == Section.RADIO -> RadioScreen(
            player = player,
            sleepText = sleepText,
            onBack = ::resetSection,
            onSettings = { settingsOpen = true }
        )

        else -> TvScreen(
            player = player,
            store = store,
            sleepText = sleepText,
            onBack = ::resetSection,
            onSettings = { settingsOpen = true }
        )
    }
}

@Composable
private fun Header(
    title: String,
    sleepText: String?,
    onBack: (() -> Unit)?,
    onSettings: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("‹", color = Red, fontSize = 34.sp)
            }
        }
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (sleepText != null) {
            Text("⏱ $sleepText", color = Red)
        }
        if (onSettings != null) {
            TextButton(onClick = onSettings) {
                Text("⚙", color = Red, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun PickerScreen(onSelect: (Section) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STREAMHUB", color = Red, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onSelect(Section.TV) },
                    modifier = Modifier.weight(1f).height(90.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("📺 TV") }
                Button(
                    onClick = { onSelect(Section.RADIO) },
                    modifier = Modifier.weight(1f).height(90.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("📻 RADIO") }
            }
        }
    }
}

@Composable
private fun RadioScreen(
    player: PlayerController,
    sleepText: String?,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val playing by player.isPlaying.collectAsState()
    val error by player.error.collectAsState()
    var current by remember { mutableStateOf<StreamItem?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column {
            Header("RADIO", sleepText, onBack, onSettings)

            if (current != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(Panel, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(current!!.name, style = MaterialTheme.typography.titleMedium)
                    if (error != null) Text(error!!, color = Red)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { player.toggle() },
                            colors = ButtonDefaults.buttonColors(containerColor = Red)
                        ) {
                            Text(if (playing) "Пауза" else "Воспроизвести")
                        }
                        TextButton(onClick = { player.stop() }) {
                            Text("Стоп", color = Red)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RADIO_STATIONS, key = { it.url }) { station ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(Panel, RoundedCornerShape(14.dp))
                            .clickable {
                                current = station
                                player.play(station.url)
                            }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(station.name, modifier = Modifier.weight(1f))
                        if (current?.url == station.url && playing) Text("PLAY", color = Red)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvScreen(
    player: PlayerController,
    store: SettingsStore,
    sleepText: String?,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PlaylistRepository(context.applicationContext) }
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var sourceIndex by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var fullScreen by remember { mutableStateOf(false) }
    val playbackError by player.error.collectAsState()

    LaunchedEffect(Unit) {
        sourceIndex = store.sourceIndex().coerceIn(0, TV_SOURCES.lastIndex)
        repo.loadCachedIfFresh()?.let {
            channels = it
            loading = false
        }
        val result = repo.refreshInOrder(sourceIndex)
        result.onSuccess { (index, list) ->
            sourceIndex = index
            channels = list
            store.setSourceIndex(index)
            loadError = null
        }.onFailure {
            if (channels.isEmpty()) loadError = it.message ?: "Не удалось загрузить плейлист"
        }
        loading = false
    }

    suspend fun loadNextPlaylist() {
        loading = true
        val next = (sourceIndex + 1) % TV_SOURCES.size
        repo.refreshInOrder(next)
            .onSuccess { (index, list) ->
                sourceIndex = index
                channels = list
                store.setSourceIndex(index)
                loadError = null
                if (selectedIndex >= channels.size) selectedIndex = channels.lastIndex
                if (fullScreen && selectedIndex in channels.indices) {
                    player.play(channels[selectedIndex].url)
                }
            }
            .onFailure { loadError = it.message ?: "Канал недоступен" }
        loading = false
    }

    if (fullScreen && selectedIndex in channels.indices) {
        TvPlayerScreen(
            player = player,
            channel = channels[selectedIndex],
            playbackError = playbackError,
            onBack = { fullScreen = false },
            onPrevious = {
                selectedIndex = if (selectedIndex <= 0) channels.lastIndex else selectedIndex - 1
                player.play(channels[selectedIndex].url)
            },
            onNext = {
                selectedIndex = (selectedIndex + 1) % channels.size
                player.play(channels[selectedIndex].url)
            },
            onOtherPlaylist = { scope.launch { loadNextPlaylist() } }
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column {
            Header("TV", sleepText, onBack, onSettings)
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Red)
                }

                channels.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(loadError ?: "Нет доступных каналов", color = Red)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { scope.launch { loadNextPlaylist() } },
                            colors = ButtonDefaults.buttonColors(containerColor = Red)
                        ) { Text("Другой плейлист") }
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(channels, key = { _, channel -> channel.url }) { index, channel ->
                        Text(
                            text = channel.name,
                            modifier = Modifier.fillMaxWidth()
                                .background(Panel, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedIndex = index
                                    player.play(channel.url)
                                    fullScreen = true
                                }
                                .padding(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlayerScreen(
    player: PlayerController,
    channel: StreamItem,
    playbackError: String?,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOtherPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val playing by player.isPlaying.collectAsState()

    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    when {
                        dragAmount > 90f -> onPrevious()
                        dragAmount < -90f -> onNext()
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player.player
                }
            },
            update = { view -> view.player = player.player },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xB5121212), RoundedCornerShape(14.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("‹", color = Red, fontSize = 34.sp)
                }
                Text(channel.name, modifier = Modifier.weight(1f), maxLines = 1)
                Button(
                    onClick = { player.toggle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text(if (playing) "Пауза" else "Play")
                }
            }

            if (playbackError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0xDD111111), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(playbackError, color = Red, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOtherPlaylist) {
                        Text("Другой плейлист", color = Red)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    store: SettingsStore,
    sleepText: String?,
    onBack: () -> Unit,
    onSleep: (Long) -> Unit,
    onReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sourceIndex by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    val presets = listOf("15 мин" to 15L, "30 мин" to 30L, "1 ч" to 60L, "2 ч" to 120L, "4 ч" to 240L, "8 ч" to 480L)

    LaunchedEffect(Unit) {
        sourceIndex = store.sourceIndex().coerceIn(0, TV_SOURCES.lastIndex)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column {
            Header("Настройки", sleepText, onBack, null)
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Text("Таймер сна", style = MaterialTheme.typography.titleMedium) }
                items(presets) { (label, minutes) ->
                    Button(
                        onClick = { onSleep(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text(label) }
                }
                item {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all(Char::isDigit)) custom = value
                        },
                        label = { Text("Свои минуты (1–480)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            custom.toLongOrNull()?.coerceIn(1L, 480L)?.let(onSleep)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("Запустить свой таймер") }
                }
                item { Text("Источник ТВ-плейлиста", style = MaterialTheme.typography.titleMedium) }
                items(TV_SOURCES.indices.toList()) { index ->
                    TextButton(
                        onClick = {
                            sourceIndex = index
                            scope.launch { store.setSourceIndex(index) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (if (sourceIndex == index) "●  " else "○  ") + TV_SOURCES[index].name,
                            color = if (sourceIndex == index) Red else Color.White
                        )
                    }
                }
                item {
                    Button(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("Сбросить выбор раздела") }
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
