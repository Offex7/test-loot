package com.radiotv.control

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.radioTvColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiotv.control.cast.CastMedia
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.cast.DlnaDevice
import com.radiotv.control.cast.LocalNetworkDeviceDiscovery
import com.radiotv.control.cast.LocalMediaHttpServer
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.AndroidTvVoiceInputController
import com.radiotv.control.core.VoiceInputStatus
import com.radiotv.control.core.AirMouseStatus
import com.radiotv.control.core.GyroAirMouseController
import com.radiotv.control.core.BluetoothHidController
import com.radiotv.control.core.BluetoothHidStatus
import com.radiotv.control.core.DiscoveredRemoteDevice
import com.radiotv.control.core.RemoteDeviceType
import com.radiotv.control.core.RemoteStatus
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.OtherTvRemoteController
import com.radiotv.control.core.OtherTvRemoteStatus
import com.radiotv.control.ui.TouchpadSurface
import com.radiotv.control.ui.TvRemotePad
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {
    private val remote: AndroidTvRemoteV2Transport by inject()
    private val bluetoothHid: BluetoothHidController by inject()
    private val airMouse: GyroAirMouseController by inject()
    private val voiceInput: AndroidTvVoiceInputController by inject()
    private val otherTvRemote: OtherTvRemoteController by inject()
    private val dlnaCast: DlnaCastController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialUiScreen = intent.getStringExtra("ui-screen") ?: "remote"
        setContent { RadioTvControlScreen(remote, bluetoothHid, airMouse, voiceInput, otherTvRemote, dlnaCast, initialUiScreen) }
    }

    override fun onDestroy() {
        if (isFinishing) {
            remote.close()
            bluetoothHid.close()
            airMouse.close()
            voiceInput.close()
            otherTvRemote.close()
        }
        super.onDestroy()
    }
}

@Composable
internal fun RadioTvControlScreen(remote: AndroidTvRemoteV2Transport, bluetoothHid: BluetoothHidController, airMouse: GyroAirMouseController, voiceInput: AndroidTvVoiceInputController, otherTvRemote: OtherTvRemoteController, dlnaCast: DlnaCastController, initialUiScreen: String) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val feedbackSettings by context.radioTvFeedbackSettings.collectAsStateWithLifecycle(initialValue = RadioTvFeedbackSettings())
    val speechReady = remember { mutableStateOf(false) }
    val speechEngine = remember(context) {
        TextToSpeech(context) { result -> speechReady.value = result == TextToSpeech.SUCCESS }
    }
    DisposableEffect(speechEngine) { onDispose { speechEngine.stop(); speechEngine.shutdown() } }
    LaunchedEffect(speechEngine, speechReady.value) {
        if (speechReady.value) speechEngine.language = Locale("ru")
    }
    val sendUiFeedback: (String) -> Unit = { label ->
        if (feedbackSettings.vibrationEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        if (feedbackSettings.clickSoundEnabled) {
            if (feedbackSettings.soundType == SOUND_TYPE_VOICE && speechReady.value) {
                speechEngine.speak(label, TextToSpeech.QUEUE_FLUSH, null, "radio-tv-click")
            } else {
                context.getSystemService(AudioManager::class.java)?.playSoundEffect(AudioManager.FX_KEY_CLICK)
            }
        }
    }
    val status by remote.status.collectAsStateWithLifecycle()
    val bluetoothStatus by bluetoothHid.status.collectAsStateWithLifecycle()
    val airMouseStatus by airMouse.status.collectAsStateWithLifecycle()
    val voiceStatus by voiceInput.status.collectAsStateWithLifecycle()
    var installedApps by remember { mutableStateOf<List<InstalledTvApp>>(emptyList()) }
    val otherRemoteStatus by otherTvRemote.status.collectAsStateWithLifecycle()
    LaunchedEffect(otherRemoteStatus) {
        if (otherRemoteStatus is OtherTvRemoteStatus.Connected) {
            installedApps = runCatching { otherTvRemote.listInstalledApps() }.getOrDefault(emptyList())
        } else {
            installedApps = emptyList()
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val discovery = remember(context) { LocalNetworkDeviceDiscovery(context) }
    var host by rememberSaveable { mutableStateOf("") }
    var pairedHost by rememberSaveable { mutableStateOf(remote.pairedHost().orEmpty()) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Телефон и телевизор должны быть в одной сети Wi‑Fi.") }
    var devices by remember { mutableStateOf<List<DiscoveredRemoteDevice>>(emptyList()) }
    var selectedDeviceName by rememberSaveable { mutableStateOf("") }
    var showOnboarding by rememberSaveable { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    var showTouchpad by rememberSaveable { mutableStateOf(false) }
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var keyboardText by rememberSaveable { mutableStateOf("") }
    var castMessage by rememberSaveable { mutableStateOf("Нажмите «Обновить», чтобы найти DLNA-приёмники.") }
    var duplicationExpanded by rememberSaveable { mutableStateOf(initialUiScreen == "cast") }
    var castScanning by remember { mutableStateOf(false) }
    var castDevices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var selectedCastRenderer by remember { mutableStateOf<DlnaDevice?>(null) }
    var selectedTab by rememberSaveable {
        mutableStateOf(when (initialUiScreen) {
            "pairing" -> RadioTvTab.SETTINGS
            "settings" -> RadioTvTab.SETTINGS
            "digital", "cast" -> RadioTvTab.CONTROL
            else -> RadioTvTab.REMOTE
        })
    }
    var castMedia by remember { mutableStateOf<CastMedia?>(null) }
    var castPlaybackStatus by rememberSaveable { mutableStateOf("Остановлено") }
    var pendingLocalNetworkAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPickedUri by remember { mutableStateOf<Uri?>(null) }
    val localMediaServer = remember(context) { LocalMediaHttpServer(context) }
    DisposableEffect(localMediaServer) {
        onDispose { localMediaServer.close() }
    }

    // Picker callbacks only stage the content URI. The local-network permission is requested
    // afterwards, because merely opening the system file picker does not need LAN access.
    val handlePickedMedia: (Uri?) -> Unit = { uri ->
        if (uri == null) {
            castMessage = "Выбор файла отменён."
        } else {
            pendingPickedUri = uri
        }
    }

    val processPickedMedia: (Uri) -> Unit = { uri ->
        scope.launch {
            castMessage = "Готовим файл для передачи на телевизор…"
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val media = withContext(Dispatchers.IO) { localMediaServer.start(uri) }
                val extension = media.title.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                val mimeType = media.mimeType.substringBefore(';').lowercase(java.util.Locale.ROOT)
                val isPlaylist = extension in setOf("m3u8", "mpd") ||
                    mimeType.contains("mpegurl") || mimeType.contains("dash+xml")
                if (isPlaylist) {
                    withContext(Dispatchers.IO) { localMediaServer.close() }
                    error("Плейлисты HLS/DASH и DRM-защищённый контент не поддерживаются. Выберите локальный незащищённый файл.")
                }
                media
            }.onSuccess { media ->
                castMedia = media
                castMessage = "Подготовлено: ${media.title}. Выберите DLNA-приёмник."
                val renderer = selectedCastRenderer
                if (renderer != null) {
                    castMessage = "Подготовлено: ${media.title}. Начинаем трансляцию…"
                    runCatching { dlnaCast.play(renderer, media) }
                        .onSuccess {
                            castPlaybackStatus = "Воспроизведение"
                            castMessage = "Передаём «${media.title}» на ${renderer.friendlyName ?: renderer.server ?: renderer.location}."
                        }
                        .onFailure {
                            castPlaybackStatus = "Ошибка"
                            castMessage = it.message ?: "Не удалось начать DLNA-воспроизведение."
                        }
                } else if (castDevices.isEmpty()) {
                    castMessage = "Файл подготовлен, но DLNA-приёмник не найден. Убедитесь, что телефон и телевизор в одной Wi-Fi сети и ТВ поддерживает DLNA/UPnP."
                } else {
                    castMessage = "Подготовлено: ${media.title}. Выберите телевизор в списке DLNA-приёмников."
                }
            }.onFailure {
                castPlaybackStatus = "Ошибка"
                castMessage = it.message ?: "Не удалось подготовить выбранный файл."
            }
        }
    }

    // Photo/video and arbitrary files use ACTION_OPEN_DOCUMENT; audio uses ACTION_GET_CONTENT.
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedMedia(uri) }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> handlePickedMedia(uri) }
    val codeRequested = status is RemoteStatus.AwaitingCode
    val wifiConnected = status is RemoteStatus.Connected
    val otherConnected = otherRemoteStatus is OtherTvRemoteStatus.Connected
    val connected = wifiConnected || otherConnected
    val hidConnected = bluetoothStatus is BluetoothHidStatus.Connected
    // Two protocol sessions may remain connected at once. Route commands to the device selected
    // in Settings instead of accidentally sending keys to an older Android TV session.
    val connectedWifiHost = (status as? RemoteStatus.Connected)?.host
    val connectedOtherIp = (otherRemoteStatus as? OtherTvRemoteStatus.Connected)?.ip
    val selectedWifiConnected = wifiConnected &&
        (host.isBlank() || connectedWifiHost.equals(host, ignoreCase = true))
    val selectedOtherConnected = otherConnected &&
        (host.isBlank() || connectedOtherIp.equals(host, ignoreCase = true))

    val startPairing: (String) -> Unit = { target ->
        scope.launch {
            host = target
            statusMessage = "Подключаемся к сервису сопряжения…"
            runCatching { remote.beginPairing(target) }
                .onSuccess { statusMessage = "Код появится на телевизоре. Введите его ниже." }
                .onFailure { statusMessage = it.message ?: "Не удалось начать сопряжение." }
        }
    }

    val runDiscovery: () -> Unit = {
        scope.launch {
            scanning = true
            statusMessage = "Ищем устройства в локальной сети…"
            runCatching { discovery.discover() }
                .onSuccess { result ->
                    devices = result
                    statusMessage = if (result.isEmpty()) {
                        "Устройства пока не найдены. Проверьте Wi‑Fi и повторите поиск."
                    } else {
                        "Найдено устройств: " + result.size
                    }
                }
                .onFailure { statusMessage = it.message ?: "Ошибка поиска устройств." }
            scanning = false
        }
    }

    val discoverCastDevices: () -> Unit = {
        scope.launch {
            castScanning = true
            castMessage = "Ищем DLNA / UPnP renderers…"
            runCatching { dlnaCast.discoverRenderers() }
                .onSuccess { result ->
                    castDevices = result
                    if (selectedCastRenderer?.location !in result.map { it.location }) selectedCastRenderer = null
                    castMessage = if (result.isEmpty()) {
                        "DLNA-приёмники с AVTransport не найдены. ТВ должен поддерживать UPnP MediaRenderer."
                    } else {
                        "Найдено DLNA-приёмников: " + result.size
                    }
                }
                .onFailure { castMessage = it.message ?: "Ошибка DLNA-поиска." }
            castScanning = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingAction = pendingLocalNetworkAction
        pendingLocalNetworkAction = null
        if (granted) {
            (pendingAction ?: runDiscovery).invoke()
        } else {
            statusMessage = "Для поиска и трансляции в локальной сети разрешите доступ или введите IP вручную."
            if (pendingAction != null) {
                castMessage = "Для DLNA-трансляции разрешите доступ к локальной сети."
                pendingPickedUri = null
            }
        }
    }

    val runWithLocalNetworkPermission: ((() -> Unit) -> Unit) = { action ->
        val needsPermission = Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingLocalNetworkAction = action
            permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        } else {
            action()
        }
    }

    val refreshCastDevices: () -> Unit = {
        runWithLocalNetworkPermission { discoverCastDevices() }
    }

    val launchMediaPicker: (Array<String>) -> Unit = { mimeTypes ->
        mediaPicker.launch(mimeTypes)
    }

    val launchAudioPicker: () -> Unit = {
        audioPicker.launch("audio/*")
    }

    androidx.compose.runtime.LaunchedEffect(pendingPickedUri) {
        val uri = pendingPickedUri ?: return@LaunchedEffect
        runWithLocalNetworkPermission {
            pendingPickedUri = null
            scope.launch {
                castScanning = true
                castMessage = "Ищем DLNA-приёмники…"
                runCatching { dlnaCast.discoverRenderers() }
                    .onSuccess { result ->
                        castDevices = result
                        if (selectedCastRenderer?.location !in result.map { it.location }) {
                            selectedCastRenderer = null
                        }
                        if (selectedCastRenderer == null && result.size == 1) {
                            selectedCastRenderer = result.single()
                        }
                        castScanning = false
                        processPickedMedia(uri)
                    }
                    .onFailure { error ->
                        castScanning = false
                        castDevices = emptyList()
                        selectedCastRenderer = null
                        castMessage = error.message ?: "Не удалось найти DLNA-приёмники. Повторите поиск в разделе «Контроль»."
                        processPickedMedia(uri)
                    }
            }
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                runCatching { voiceInput.start() }
                    .onSuccess { statusMessage = it }
                    .onFailure { statusMessage = it.message ?: "Не удалось включить микрофон." }
            }
        } else {
            statusMessage = "Для голосового ввода разрешите доступ к микрофону."
        }
    }

    val discoverabilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        statusMessage = if (result.resultCode == Activity.RESULT_OK) {
            "Телефон обнаруживаем по Bluetooth. На телевизоре откройте Bluetooth → добавить устройство → Radio.TV.Control."
        } else {
            "Разрешите обнаружение телефона, чтобы телевизор мог подключиться по Bluetooth HID."
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bluetoothHid.start()
            discoverabilityLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            )
        } else {
            statusMessage = "Bluetooth не включён."
        }
    }

    val startBluetoothSession: () -> Unit = {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        when {
            adapter == null -> statusMessage = "На этом устройстве отсутствует Bluetooth."
            !runCatching { adapter.isEnabled }.getOrDefault(false) ->
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> {
                bluetoothHid.start()
                discoverabilityLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                )
            }
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            grants[Manifest.permission.BLUETOOTH_SCAN] == true &&
            grants[Manifest.permission.BLUETOOTH_ADVERTISE] == true
        ) {
            startBluetoothSession()
        } else {
            statusMessage = "Для Bluetooth HID разрешите Bluetooth Connect, Scan и Advertise."
        }
    }

    val beginBluetoothSession: () -> Unit = {
        val required = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
        val missing = required.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing) bluetoothPermissionLauncher.launch(required) else startBluetoothSession()
    }

    val refreshDevices: () -> Unit = {
        runWithLocalNetworkPermission { runDiscovery() }
    }

    val connectPairedHost: (String) -> Unit = { target ->
        host = target
        selectedDeviceName = "Android TV"
        scope.launch {
            statusMessage = "Подключаемся к ранее сопряжённому Android TV…"
            runCatching { remote.connect(target) }
                .onSuccess { statusMessage = "Подключено к сохранённому телевизору: $target"; selectedDeviceName = "Android TV"; showOnboarding = false; selectedTab = RadioTvTab.REMOTE }
                .onFailure { statusMessage = it.message ?: "Не удалось подключиться к сохранённому телевизору." }
        }
    }


    fun chooseDevice(device: DiscoveredRemoteDevice) {
        selectedDeviceName = friendlyDeviceName(device)
        showOnboarding = false
        when (device.type) {
            RemoteDeviceType.ANDROID_TV -> {
                host = device.ip
                if (device.ip == pairedHost) connectPairedHost(device.ip) else startPairing(device.ip)
            }
            RemoteDeviceType.SAMSUNG, RemoteDeviceType.LG, RemoteDeviceType.ROKU -> {
                scope.launch {
                    host = device.ip
                    statusMessage = "Подключаемся к " + device.type.label + "…"
                    runCatching { otherTvRemote.connect(device) }
                        .onSuccess {
                            statusMessage = when (val current = otherTvRemote.status.value) {
                                is OtherTvRemoteStatus.Pairing -> current.message
                                is OtherTvRemoteStatus.Connected -> "Подключено: " + current.type.label + " · " + current.ip
                                is OtherTvRemoteStatus.Error -> current.message
                                else -> "Соединение инициализировано."
                            }
                        }
                        .onFailure { statusMessage = it.message ?: "Не удалось подключить телевизор." }
                }
            }
            RemoteDeviceType.CHROMECAST -> {
                host = device.ip
                statusMessage = "Найден Google Cast по адресу " + device.ip + ". Управляющий Cast-адаптер добавляется отдельно."
            }
            RemoteDeviceType.UNKNOWN -> startPairing(device.ip)
            else -> {
                host = device.ip
                statusMessage = "Определено: " + device.type.label + " (" + device.ip + "). Для этого типа нужен отдельный протокол."
            }
        }
    }

    val connectManualHost: () -> Unit = {
        val target = host.trim()
        scope.launch {
            statusMessage = "Определяем протокол по IP-адресу…"
            runCatching { discovery.detectHost(target) }
                .onSuccess { detected ->
                    statusMessage = "Определён тип: " + detected.type.label + ". Подключаемся…"
                    chooseDevice(detected)
                }
                .onFailure { error ->
                    statusMessage = error.message ?: "Не удалось определить устройство."
                }
        }
    }

    LaunchedEffect(initialUiScreen) {
        if (initialUiScreen == "pairing") refreshDevices()
    }

    LaunchedEffect(pairedHost) {
        if (pairedHost.isNotBlank() && !connected) connectPairedHost(pairedHost)
    }

    val startFromOnboarding: (Boolean) -> Unit = { infraredPath ->
        showOnboarding = false
        selectedTab = RadioTvTab.SETTINGS
        manualEntry = infraredPath
        statusMessage = if (infraredPath) "Поиск в локальной сети запущен. Если ваш ИК-мост не найден, введите его IP вручную." else "Ищем телевизоры и медиаприёмники в локальной сети."
        refreshDevices()
    }

    val handleFeatureAction: (RemoteFeatureAction) -> Unit = { action ->
        val feedbackMessage = when (action) {
            RemoteFeatureAction.VOICE_INPUT -> {
                if (voiceStatus is VoiceInputStatus.Recording || voiceStatus is VoiceInputStatus.Starting) {
                    scope.launch {
                        runCatching { voiceInput.stop() }
                            .onSuccess { statusMessage = it }
                            .onFailure { statusMessage = it.message ?: "Не удалось остановить запись." }
                    }
                    "Останавливаем голосовой ввод…"
                } else if (!selectedWifiConnected) {
                    if (selectedOtherConnected) {
                        "Голосовой ввод доступен только через Android TV Remote v2. Выберите Android TV и подключитесь по Wi-Fi."
                    } else {
                        "Сначала подключитесь к Android TV по Wi-Fi."
                    }
                } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    scope.launch {
                        runCatching { voiceInput.start() }
                            .onSuccess { statusMessage = it }
                            .onFailure { statusMessage = it.message ?: "Не удалось включить микрофон." }
                    }
                    "Подключаем голосовую сессию. Индикатор станет красным после начала записи."
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    "Запрашиваем разрешение на микрофон…"
                }
            }
            RemoteFeatureAction.APP_YOUTUBE -> {
                "Запуск YouTube напрямую пока не поддерживается выбранным протоколом. Откройте приложение на телевизоре."
            }
            RemoteFeatureAction.APP_NETFLIX -> {
                "Запуск Netflix напрямую пока не поддерживается выбранным протоколом. Откройте приложение на телевизоре."
            }
            RemoteFeatureAction.KEYBOARD -> {
                if (selectedTab == RadioTvTab.REMOTE) {
                    selectedTab = RadioTvTab.CONTROL
                    showKeyboard = true
                    "Открыта панель ввода. Нажмите поле «Текст для телевизора», введите текст и отправьте его."
                } else {
                    showKeyboard = !showKeyboard
                    if (showKeyboard) "Откройте поле ввода и отправьте текст на подключённый телевизор." else "Клавиатура скрыта."
                }
            }
            RemoteFeatureAction.AIR_MOUSE -> {
                when {
                    airMouseStatus is AirMouseStatus.Active -> airMouse.toggle()
                    !airMouse.hasGyroscope -> airMouse.toggle()
                    !hidConnected -> {
                        beginBluetoothSession()
                        "Запущен режим сопряжения Bluetooth. Выберите Radio.TV.Control в настройках Bluetooth телевизора, затем включите аэромышь ещё раз."
                    }
                    else -> airMouse.toggle()
                }
            }
            RemoteFeatureAction.TOUCHPAD -> {
                if (selectedTab == RadioTvTab.REMOTE) {
                    selectedTab = RadioTvTab.CONTROL
                    showTouchpad = true
                    "Открыта панель тачпада. Для движения курсора подключите телевизор по Bluetooth HID."
                } else {
                    showTouchpad = !showTouchpad
                    if (showTouchpad) "Тачпад включён. Для движения курсора требуется соединение Bluetooth HID."
                    else "Тачпад скрыт."
                }
            }
            RemoteFeatureAction.CAST -> {
                // Show the system picker first; ask for LAN access only after content is selected.
                selectedTab = RadioTvTab.CONTROL
                duplicationExpanded = true
                mediaPicker.launch(arrayOf("*/*"))
                "Выберите фото, видео, аудио или файл для трансляции на DLNA-совместимый телевизор. DRM/HLS/DASH не поддерживается."
            }
        }
        sendUiFeedback(when (action) {
            RemoteFeatureAction.VOICE_INPUT -> "Микрофон"
            RemoteFeatureAction.KEYBOARD -> "Клавиатура"
            RemoteFeatureAction.AIR_MOUSE -> "Аэромышь"
            RemoteFeatureAction.TOUCHPAD -> "Тачпад"
            RemoteFeatureAction.CAST -> "Трансляция"
            RemoteFeatureAction.APP_YOUTUBE -> "YouTube"
            RemoteFeatureAction.APP_NETFLIX -> "Netflix"
        })
        statusMessage = feedbackMessage
        scope.launch {
            runCatching {
                snackbarHostState.showSnackbar(
                    message = feedbackMessage,
                    withDismissAction = true,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
            }
        }
    }

    val dispatchRemoteKey: (RemoteKey) -> Unit = { key ->
        sendUiFeedback(remoteKeyFeedbackLabel(key))
        scope.launch {
            runCatching {
                when {
                    selectedOtherConnected -> otherTvRemote.sendKey(key)
                    selectedWifiConnected -> remote.sendKey(key)
                    hidConnected -> bluetoothHid.sendRemoteKey(key)
                    else -> error("Выбранный телевизор не подключён. Выберите устройство в настройках.")
                }
            }.onFailure { statusMessage = it.message ?: "Не удалось отправить команду." }
        }
    }

    MaterialTheme(colorScheme = radioTvColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = RadioTvPalette.Black) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(RadioTvPalette.Black)) {
                val isLandscape = maxWidth > maxHeight
                // Landscape/tablet letterbox uses a centered fixed-width column; portrait phones fill safe width.
                val panelWidth = if (isLandscape) 460.dp else maxWidth
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = panelWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(RadioTvPalette.Black)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showOnboarding && !connected) {
                        OnboardingScreen(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onInfrared = { startFromOnboarding(true) },
                            onSmartTv = { startFromOnboarding(false) },
                            onUnknown = { startFromOnboarding(false) }
                        )
                    } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(selectedTab) {
                            var horizontalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount },
                                onDragEnd = {
                                    if (horizontalDrag < -100f && selectedTab == RadioTvTab.REMOTE) {
                                        selectedTab = RadioTvTab.CONTROL
                                    } else if (horizontalDrag > 100f && selectedTab != RadioTvTab.REMOTE) {
                                        selectedTab = RadioTvTab.REMOTE
                                    }
                                    horizontalDrag = 0f
                                }
                            )
                        }
                    ) {
                        when (selectedTab) {
                            RadioTvTab.REMOTE -> {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    TvRemotePad(
                                        enabled = connected || otherConnected || hidConnected,
                                        modifier = Modifier.widthIn(max = if (maxWidth > maxHeight) 420.dp else 470.dp).fillMaxWidth().fillMaxHeight().padding(horizontal = 4.dp, vertical = 2.dp),
                                        onKey = dispatchRemoteKey,
                                        onFeatureAction = handleFeatureAction,
                                        touchpadActive = showTouchpad,
                                        airMouseActive = airMouseStatus is AirMouseStatus.Active,
                                        voiceActive = voiceStatus is VoiceInputStatus.Recording,
                                        keyboardActive = showKeyboard,
                                        featureMessage = null,
                                        showFeatureActions = false,
                                        deviceName = when {
                                            selectedOtherConnected -> selectedDeviceName.ifBlank { "Телевизор" }
                                            selectedWifiConnected -> selectedDeviceName.ifBlank { "Android TV" }
                                            hidConnected -> selectedDeviceName.ifBlank { "Bluetooth HID" }
                                            else -> "Нет подключения"
                                        },
                                        onSettings = { sendUiFeedback("Настройки"); selectedTab = RadioTvTab.SETTINGS; refreshDevices() },
                                        onBack = { sendUiFeedback("Контроль"); selectedTab = RadioTvTab.CONTROL }
                                    )
                                }
                            }
                            RadioTvTab.CONTROL -> {
                                DigitalRemotePanel(modifier = Modifier.fillMaxSize(), deviceName = selectedDeviceName.ifBlank { "Телевизор не подключён" },
                                    installedApps = installedApps,
                                    onLaunchApp = { app ->
                                        sendUiFeedback(app.name)
                                        scope.launch {
                                            runCatching { otherTvRemote.launchInstalledApp(app) }
                                                .onFailure { statusMessage = it.message ?: "Не удалось запустить приложение на телевизоре." }
                                        }
                                    },
                                    enabled = connected || otherConnected || hidConnected, micRecording = voiceStatus is VoiceInputStatus.Recording,
                                    airMouseActive = airMouseStatus is AirMouseStatus.Active, keyboardActive = showKeyboard,
                                    onKey = dispatchRemoteKey, onFeatureAction = handleFeatureAction,
                                    onSettings = { sendUiFeedback("Настройки"); selectedTab = RadioTvTab.SETTINGS; refreshDevices() },
                                    onReturnRemote = { sendUiFeedback("Пульт"); selectedTab = RadioTvTab.REMOTE },
                                    duplicationExpanded = duplicationExpanded,
                                    onToggleDuplication = { sendUiFeedback("Дублирование"); duplicationExpanded = !duplicationExpanded; if (duplicationExpanded) refreshCastDevices() },
                                    duplicationContent = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { sendUiFeedback("Фото и видео"); launchMediaPicker(arrayOf("image/*", "video/*")) }, modifier = Modifier.weight(1f)) { Text("Фото / видео") }
                                            Button(onClick = { sendUiFeedback("Музыка и аудио"); launchAudioPicker() }, modifier = Modifier.weight(1f)) { Text("Музыка / аудио") }
                                        }
                                        OutlinedButton(onClick = { sendUiFeedback("Выбрать файл"); launchMediaPicker(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ВЫБРАТЬ ЛЮБОЙ ФАЙЛ") }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("DLNA-приёмники", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); OutlinedButton(onClick = { sendUiFeedback("Обновить DLNA"); refreshCastDevices() }, enabled = !castScanning) { Text(if (castScanning) "ПОИСК…" else "ОБНОВИТЬ") } }
                                        Text(castMedia?.let { "Медиа: " + it.title } ?: "Медиа ещё не выбрано.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text("Состояние: " + castPlaybackStatus, color = if (castPlaybackStatus == "Воспроизведение") RadioTvPalette.Red else RadioTvPalette.Muted)
                                        castDevices.forEach { renderer ->
                                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
                                                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(renderer.friendlyName?.takeIf { it.isNotBlank() } ?: "DLNA-устройство", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                    OutlinedButton(onClick = { sendUiFeedback("Выбрать DLNA"); selectedCastRenderer = renderer; val media = castMedia; if (media != null) scope.launch { runCatching { dlnaCast.play(renderer, media) }.onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Передаём медиа на выбранное устройство." }.onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось запустить медиа." } } }) { Text(if (selectedCastRenderer?.location == renderer.location) "ВЫБРАНО" else "ВЫБРАТЬ") }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { sendUiFeedback("Воспроизведение"); val renderer = selectedCastRenderer; val media = castMedia; if (renderer != null && media != null) scope.launch { runCatching { dlnaCast.play(renderer, media) }.onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Команда Play отправлена." }.onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось начать воспроизведение." } } }, enabled = selectedCastRenderer != null && castMedia != null, modifier = Modifier.weight(1f)) { Text("▶ Play") }
                                            OutlinedButton(onClick = { sendUiFeedback("Пауза"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.pause(renderer) }.onSuccess { castPlaybackStatus = "Пауза"; castMessage = "Команда Pause отправлена." }.onFailure { castMessage = it.message ?: "Pause не поддерживается." } } } }, enabled = selectedCastRenderer != null) { Text("Ⅱ") }
                                            OutlinedButton(onClick = { sendUiFeedback("Стоп"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.stop(renderer) }.onSuccess { castPlaybackStatus = "Остановлено"; castMessage = "Команда Stop отправлена." }.onFailure { castMessage = it.message ?: "Не удалось остановить." } } } }, enabled = selectedCastRenderer != null) { Text("■") }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { sendUiFeedback("Назад 30 секунд"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.seek(renderer, -30) }.onSuccess { castMessage = "Перемотка назад отправлена." }.onFailure { castMessage = it.message ?: "Перемотка не поддерживается." } } } }, enabled = selectedCastRenderer != null, modifier = Modifier.weight(1f)) { Text("−30 с") }
                                            OutlinedButton(onClick = { sendUiFeedback("Вперёд 30 секунд"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.seek(renderer, 30) }.onSuccess { castMessage = "Перемотка вперёд отправлена." }.onFailure { castMessage = it.message ?: "Перемотка не поддерживается." } } } }, enabled = selectedCastRenderer != null, modifier = Modifier.weight(1f)) { Text("+30 с") }
                                        }
                                        Text(castMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                )
                            }
                            RadioTvTab.SETTINGS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                                    .background(Color(0xFF2A2A2A))
                                                    .clickable { sendUiFeedback("Назад"); selectedTab = RadioTvTab.REMOTE }
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад к пульту", tint = Color.White) }
                                            Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    item {
                                        Text("НАСТРОЙКИ УСТРОЙСТВ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                        Text("Телефон и телевизор должны быть в одной Wi‑Fi сети.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
                                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Column(Modifier.weight(1f)) { Text("Вибрация при нажатии", fontWeight = FontWeight.SemiBold); Text("Короткий тактильный отклик", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall) }
                                                    Switch(checked = feedbackSettings.vibrationEnabled, onCheckedChange = { enabled -> sendUiFeedback("Вибрация"); scope.launch { context.setVibrationEnabled(enabled) } })
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Column(Modifier.weight(1f)) { Text("Звук при нажатии", fontWeight = FontWeight.SemiBold); Text(if (feedbackSettings.clickSoundEnabled) "Звук включён" else "Выключен по умолчанию", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall) }
                                                    Switch(checked = feedbackSettings.clickSoundEnabled, onCheckedChange = { enabled -> sendUiFeedback("Звук"); scope.launch { context.setClickSoundEnabled(enabled) } })
                                                }
                                                Text("Тип звука", fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                    listOf(SOUND_TYPE_CLICK to "Клик", SOUND_TYPE_VOICE to "Голос").forEach { choice ->
                                                        Box(Modifier.weight(1f).background(if (feedbackSettings.soundType == choice.first) RadioTvPalette.Red else RadioTvPalette.Black, RoundedCornerShape(10.dp)).clickable { sendUiFeedback(choice.second); scope.launch { context.setClickSoundType(choice.first) } }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(choice.second, color = Color.White, fontWeight = FontWeight.SemiBold) }
                                                    }
                                                }
                                                if (feedbackSettings.clickSoundEnabled && feedbackSettings.soundType == SOUND_TYPE_VOICE) Text("Озвучивание действия на русском языке (если установлен TTS).", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    item {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Bluetooth HID", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            OutlinedButton(onClick = { sendUiFeedback("Подключить Bluetooth HID"); beginBluetoothSession() }) { Text("ПОДКЛЮЧИТЬ") }
                                        }
                                        Text(bluetoothStatus.asUserLabel(), color = if (hidConnected) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text(airMouseStatus.asUserLabel(), color = if (airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text(voiceStatus.asUserLabel(), color = if (voiceStatus is VoiceInputStatus.Recording) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        if (installedApps.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("ПРИЛОЖЕНИЯ НА ТЕЛЕВИЗОРЕ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                                                installedApps.take(12).forEach { app ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(RadioTvPalette.Raised)
                                                            .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(app.name, modifier = Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.Medium)
                                                        OutlinedButton(onClick = {
                                                            sendUiFeedback(app.name)
                                                            scope.launch {
                                                                runCatching { otherTvRemote.launchInstalledApp(app) }
                                                                    .onFailure { statusMessage = it.message ?: "Не удалось запустить приложение на телевизоре." }
                                                            }
                                                        }) { Text("ОТКРЫТЬ") }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Text("ВОЗМОЖНОСТИ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            ControlActionButton("Микрофон", voiceStatus is VoiceInputStatus.Recording, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.VOICE_INPUT) }
                                            ControlActionButton("Аэромышь", airMouseStatus is AirMouseStatus.Active, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.AIR_MOUSE) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            ControlActionButton("Клавиатура", showKeyboard, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.KEYBOARD) }
                                            ControlActionButton("Тачпад", showTouchpad, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.TOUCHPAD) }
                                            ControlActionButton("Трансляция", false, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.CAST) }
                                        }
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            sendUiFeedback("Дублирование")
                                                            duplicationExpanded = !duplicationExpanded
                                                            if (duplicationExpanded) refreshCastDevices()
                                                        }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Дублирование / трансляция", fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            if (duplicationExpanded) "Панель развёрнута" else "Фото, видео, аудио и DLNA · нажмите, чтобы открыть",
                                                            color = RadioTvPalette.Muted,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Text(if (duplicationExpanded) "−" else "+", color = RadioTvPalette.Red, fontSize = 22.sp)
                                                }
                                                if (duplicationExpanded) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                        OutlinedButton(
                                                            onClick = { sendUiFeedback("Фото и видео"); launchMediaPicker(arrayOf("image/*", "video/*")) },
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("Фото / видео") }
                                                        OutlinedButton(
                                                            onClick = { sendUiFeedback("Музыка и аудио"); launchAudioPicker() },
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("Музыка / аудио") }
                                                    }
                                                    OutlinedButton(
                                                        onClick = { sendUiFeedback("Выбрать файл"); launchMediaPicker(arrayOf("*/*")) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) { Text("ВЫБРАТЬ ЛЮБОЙ ФАЙЛ") }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("DLNA-приёмники", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                                        OutlinedButton(
                                                            onClick = { sendUiFeedback("Обновить DLNA"); refreshCastDevices() },
                                                            enabled = !castScanning
                                                        ) { Text(if (castScanning) "ПОИСК…" else "ОБНОВИТЬ") }
                                                    }
                                                    if (castDevices.isEmpty() && !castScanning) {
                                                        Text("Нажмите «Обновить» или выберите файл: поиск DLNA начнётся автоматически.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    castDevices.take(6).forEach { renderer ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                                .background(RadioTvPalette.Black).padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                renderer.friendlyName?.takeIf { it.isNotBlank() } ?: "DLNA-устройство",
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = 2,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            OutlinedButton(
                                                                onClick = {
                                                                    sendUiFeedback("Выбрать DLNA")
                                                                    selectedCastRenderer = renderer
                                                                    val media = castMedia
                                                                    if (media != null) scope.launch {
                                                                        runCatching { dlnaCast.play(renderer, media) }
                                                                            .onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Передаём медиа на выбранное устройство." }
                                                                            .onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось запустить медиа." }
                                                                    } else {
                                                                        castMessage = "Телевизор выбран. Теперь выберите фото, видео, аудио или файл."
                                                                    }
                                                                }
                                                            ) { Text(if (selectedCastRenderer?.location == renderer.location) "ВЫБРАНО" else "ВЫБРАТЬ") }
                                                        }
                                                    }
                                                    Text(castMedia?.let { "Медиа: ${it.title}" } ?: "Медиа ещё не выбрано.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                    Text("Состояние: $castPlaybackStatus", color = if (castPlaybackStatus == "Воспроизведение") RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                    Text(castMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                        Button(
                                                            onClick = {
                                                                val renderer = selectedCastRenderer
                                                                val media = castMedia
                                                                if (renderer != null && media != null) scope.launch {
                                                                    runCatching { dlnaCast.play(renderer, media) }
                                                                        .onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Команда Play отправлена." }
                                                                        .onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось начать воспроизведение." }
                                                                }
                                                            },
                                                            enabled = selectedCastRenderer != null && castMedia != null,
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("▶ Play") }
                                                        OutlinedButton(
                                                            onClick = { selectedCastRenderer?.let { renderer -> scope.launch {
                                                                runCatching { dlnaCast.pause(renderer) }
                                                                    .onSuccess { castPlaybackStatus = "Пауза"; castMessage = "Команда Pause отправлена." }
                                                                    .onFailure { castMessage = it.message ?: "Pause не поддерживается." }
                                                            } } },
                                                            enabled = selectedCastRenderer != null,
                                                            modifier = Modifier.weight(1f)
                                                        ) { Text("Ⅱ Pause") }
                                                    }
                                                }
                                            }
                                        }
                                        Text("Состояние: $statusMessage", color = if (voiceStatus is VoiceInputStatus.Recording || airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text("Разрешение микрофона запрашивается после соединения с Android TV. Аэромышь требует гироскоп и Bluetooth HID.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }

                                    item {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Поиск устройств", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            OutlinedButton(onClick = { sendUiFeedback("Поиск устройств"); refreshDevices() }, enabled = !scanning) { Text(if (scanning) "ПОИСК…" else "ОБНОВИТЬ") }
                                        }
                                        if (pairedHost.isNotBlank()) {
                                            OutlinedButton(onClick = { sendUiFeedback("Подключить сохранённый телевизор"); connectPairedHost(pairedHost) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("ПОДКЛЮЧИТЬ СОХРАНЁННЫЙ ТВ · $pairedHost")
                                            }
                                        }
                                        if (scanning) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(4.dp))
                                                Text("Ищем устройства в локальной сети…", color = RadioTvPalette.Muted)
                                            }
                                            repeat(3) { DiscoverySkeletonCard() }
                                        } else if (devices.isEmpty()) {
                                            Text("Устройства не найдены", color = RadioTvPalette.Muted)
                                        }
                                    }
                                    items(devices, key = { it.ip }) { device ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(deviceIcon(device.type), fontSize = 11.sp, color = RadioTvPalette.Muted, fontWeight = FontWeight.Bold)
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(friendlyDeviceName(device), fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text(device.ip,
                                                        color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                OutlinedButton(onClick = { sendUiFeedback("Подключить устройство"); chooseDevice(device) }) { Text("Подключить") }
                                            }
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { sendUiFeedback("Ввести IP"); manualEntry = !manualEntry }, modifier = Modifier.fillMaxWidth()) {
                                            Text(if (manualEntry) "СКРЫТЬ РУЧНОЙ ВВОД" else "ВВЕСТИ IP ВРУЧНУЮ")
                                        }
                                        if (manualEntry) {
                                            OutlinedTextField(
                                                value = host,
                                                onValueChange = { host = it },
                                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                                label = { Text("IP-адрес телевизора") },
                                                placeholder = { Text("192.168.1.100") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                            )
                                            Button(onClick = { sendUiFeedback("Подключение по IP"); connectManualHost() }, modifier = Modifier.fillMaxWidth(), enabled = host.isNotBlank()) {
                                                Text("НАЧАТЬ СОПРЯЖЕНИЕ ПО IP")
                                            }
                                        }
                                    }
                                    item {
                                        Text(
                                            when (val current = status) {
                                                RemoteStatus.Disconnected -> "Android TV: не подключено"
                                                is RemoteStatus.Connecting -> "Подключение: ${current.host}"
                                                is RemoteStatus.AwaitingCode -> "Ожидается код сопряжения"
                                                is RemoteStatus.Connected -> "Android TV: подключено · ${current.host}"
                                                is RemoteStatus.Error -> "Не удалось подключиться. Попробуйте ещё раз."
                                            },
                                            color = if (wifiConnected) RadioTvPalette.Red else RadioTvPalette.Muted
                                        )
                                        if (otherRemoteStatus !is OtherTvRemoteStatus.Disconnected) {
                                            Text(otherRemoteStatus.asUserLabel(), color = if (otherConnected) RadioTvPalette.Red else RadioTvPalette.Muted)
                                        }
                                        Text(statusMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (codeRequested) {
                                        item {
                                            OutlinedTextField(
                                                value = pairingCode,
                                                onValueChange = { pairingCode = it.uppercase().filter { c -> c in "0123456789ABCDEF" }.take(6) },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text("Код с экрана телевизора") },
                                                placeholder = { Text("A1B2C3") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                            )
                                            Button(
                                                onClick = {
                                                    sendUiFeedback("Подтвердить код")
                                                    scope.launch {
                                                        statusMessage = "Проверяем код и сохраняем сопряжение…"
                                                        runCatching { remote.completePairing(pairingCode) }
                                                            .onSuccess {
                                                                pairedHost = remote.pairedHost().orEmpty()
                                                                statusMessage = "Телевизор подключён."
                                                                showOnboarding = false
                                                                selectedTab = RadioTvTab.REMOTE
                                                            }
                                                            .onFailure { statusMessage = it.message ?: "Не удалось завершить сопряжение." }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = pairingCode.length == 6
                                            ) { Text("ПОДТВЕРДИТЬ КОД") }
                                        }
                                    }
                                    if (showKeyboard) {
                                        item {
                                            OutlinedTextField(
                                                value = keyboardText, onValueChange = { keyboardText = it },
                                                modifier = Modifier.fillMaxWidth(), label = { Text("Текст для телевизора") },
                                                placeholder = { Text("Введите текст…") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                            )
                                            Button(
                                                onClick = {
                                                    sendUiFeedback("Отправить текст")
                                                    scope.launch {
                                                        runCatching {
                                                            when {
                                                                selectedOtherConnected -> otherTvRemote.sendText(keyboardText)
                                                                selectedWifiConnected -> remote.sendText(keyboardText)
                                                                hidConnected -> bluetoothHid.sendText(keyboardText)
                                                                else -> error("Выбранный телевизор не подключён. Выберите устройство в настройках.")
                                                            }
                                                        }.onSuccess { statusMessage = "Текст отправлен."; keyboardText = "" }
                                                            .onFailure { statusMessage = it.message ?: "Не удалось отправить текст." }
                                                    }
                                                },
                                                enabled = keyboardText.isNotEmpty() && (selectedWifiConnected || selectedOtherConnected || hidConnected),
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("ОТПРАВИТЬ ТЕКСТ НА ТВ") }
                                        }
                                    }
                                    if (showTouchpad) {
                                        item {
                                            TouchpadSurface(
                                                enabled = hidConnected,
                                                onMove = { dx, dy -> scope.launch { runCatching { bluetoothHid.moveMouse(dx, dy) }.onFailure { statusMessage = it.message ?: "Не удалось переместить Bluetooth-курсор." } } },
                                                onTap = { sendUiFeedback("Клик тачпада"); scope.launch { runCatching { bluetoothHid.clickMouse(1) }.onFailure { statusMessage = it.message ?: "Не удалось нажать Bluetooth-мышь." } } },
                                                onLongPress = { sendUiFeedback("Контекстный клик"); scope.launch { runCatching { bluetoothHid.clickMouse(2) }.onFailure { statusMessage = it.message ?: "Не удалось отправить контекстный клик." } } }
                                            )
                                        }
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(RadioTvPalette.Raised)
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            Text("В разработке", fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text(
                                                "Аппаратная проверка Samsung Tizen, LG webOS, Roku, Bluetooth HID, DLNA и аэромыши ещё не подтверждена на реальных телевизорах. План: проверить подключение, команды, повторное соединение и передачу медиа на устройствах каждого семейства.",
                                                color = RadioTvPalette.Muted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                "Голосовой ввод PCM 16-bit mono не подтверждён на физическом Android TV. План: проверить запись, передачу аудиокадров и остановку голосовой сессии на совместимом телевизоре.",
                                                color = RadioTvPalette.Muted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                "Реальные иконки приложений доступны не для всех протоколов; при отсутствии URL используется запасное обозначение. План: загружать и кешировать иконки там, где ТВ предоставляет их в API.",
                                                color = RadioTvPalette.Muted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }


                                }
                            }
                        }
                    }
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

private fun friendlyDeviceName(device: DiscoveredRemoteDevice): String {
    val name = device.name.trim()
    return when {
        name.isBlank() || name == device.ip -> "Телевизор"
        name.contains("miniupnpd", ignoreCase = true) || name.contains("upnp", ignoreCase = true) || name.contains("dlna", ignoreCase = true) -> "DLNA-устройство"
        name.contains("roku", ignoreCase = true) -> "Roku TV"
        name.contains("webos", ignoreCase = true) -> "LG webOS TV"
        name.contains("samsung", ignoreCase = true) || name.contains("tizen", ignoreCase = true) -> "Samsung TV"
        else -> name
    }
}

private fun remoteKeyFeedbackLabel(key: RemoteKey): String = when (key) {
    RemoteKey.POWER -> "Питание"
    RemoteKey.MENU -> "Меню"
    RemoteKey.OK -> "ОК"
    RemoteKey.UP -> "Вверх"
    RemoteKey.DOWN -> "Вниз"
    RemoteKey.LEFT -> "Влево"
    RemoteKey.RIGHT -> "Вправо"
    RemoteKey.BACK -> "Назад"
    RemoteKey.EXIT -> "Выход"
    RemoteKey.HOME -> "Домой"
    RemoteKey.MUTE -> "Без звука"
    RemoteKey.VOLUME_UP -> "Громкость плюс"
    RemoteKey.VOLUME_DOWN -> "Громкость минус"
    RemoteKey.CHANNEL_UP -> "Канал плюс"
    RemoteKey.CHANNEL_DOWN -> "Канал минус"
    RemoteKey.PLAY_PAUSE -> "Воспроизведение или пауза"
    RemoteKey.PLAY -> "Воспроизведение"
    RemoteKey.PAUSE -> "Пауза"
    RemoteKey.REWIND -> "Перемотка назад"
    RemoteKey.FAST_FORWARD -> "Перемотка вперёд"
    RemoteKey.SOURCE -> "Источник"
    RemoteKey.NUMBER_0 -> "Ноль"
    RemoteKey.NUMBER_1 -> "Один"
    RemoteKey.NUMBER_2 -> "Два"
    RemoteKey.NUMBER_3 -> "Три"
    RemoteKey.NUMBER_4 -> "Четыре"
    RemoteKey.NUMBER_5 -> "Пять"
    RemoteKey.NUMBER_6 -> "Шесть"
    RemoteKey.NUMBER_7 -> "Семь"
    RemoteKey.NUMBER_8 -> "Восемь"
    RemoteKey.NUMBER_9 -> "Девять"
    else -> "Кнопка пульта"
}
private enum class RadioTvTab(val title: String, val icon: String) {
    REMOTE("Пульт", "▣"),
    CONTROL("Контроль", "▦"),
    SETTINGS("Настройки", "⚙")
}

@Composable
private fun DiscoverySkeletonCard() {
    val transition = rememberInfiniteTransition(label = "device-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "device-skeleton-alpha"
    )
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(36.dp).background(RadioTvPalette.Muted.copy(alpha = alpha), RoundedCornerShape(18.dp)))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.fillMaxWidth(0.64f).height(12.dp).background(RadioTvPalette.Muted.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(0.88f).height(9.dp).background(RadioTvPalette.Muted.copy(alpha = alpha * 0.8f), RoundedCornerShape(4.dp)))
            }
            Box(Modifier.size(32.dp).background(RadioTvPalette.Muted.copy(alpha = alpha * 0.7f), RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
private fun ControlActionButton(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = tween(140),
        label = "settings-feature-button-scale"
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp).graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            alpha = if (pressed) 0.82f else 1f
        },
        interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) RadioTvPalette.Red else RadioTvPalette.Raised,
            contentColor = Color.White,
            disabledContainerColor = RadioTvPalette.Raised,
            disabledContentColor = RadioTvPalette.Muted
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        val title = label.substringAfter('\n', label).trim()
        val actionIcon = when (title) {
            "Микрофон" -> Icons.Filled.Mic
            "Аэромышь" -> Icons.Filled.Mouse
            "Клавиатура" -> Icons.Filled.Dialpad
            "Тачпад" -> Icons.Filled.TouchApp
            "Трансляция" -> Icons.Filled.Cast
            else -> null
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (actionIcon != null) Icon(actionIcon, contentDescription = title, modifier = Modifier.size(21.dp), tint = if (active) Color.White else RadioTvPalette.Red)
            Text(title, fontSize = 10.sp, lineHeight = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        }
    }
}

private fun digitKey(number: Int): RemoteKey = RemoteKey.entries.first { it.name == "NUMBER_$number" }

private fun sendRemoteKey(
    key: RemoteKey,
    remote: AndroidTvRemoteV2Transport,
    otherRemote: OtherTvRemoteController,
    bluetoothHid: BluetoothHidController,
    wifiConnected: Boolean,
    otherConnected: Boolean,
    hidConnected: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        runCatching {
            when {
                wifiConnected -> remote.sendKey(key)
                otherConnected -> otherRemote.sendKey(key)
                hidConnected -> bluetoothHid.sendRemoteKey(key)
                else -> error("Сначала подключитесь к телевизору.")
            }
        }
    }
}

private fun deviceIcon(type: RemoteDeviceType): String = when (type) {
    RemoteDeviceType.ANDROID_TV -> "TV"
    RemoteDeviceType.CHROMECAST -> "CAST"
    RemoteDeviceType.SAMSUNG -> "SAM"
    RemoteDeviceType.LG -> "LG"
    RemoteDeviceType.ROKU -> "ROKU"
    RemoteDeviceType.DLNA -> "DLNA"
    RemoteDeviceType.AIRPLAY -> "AIR"
    RemoteDeviceType.UNKNOWN -> "?"
}

private fun AirMouseStatus.asUserLabel(): String = when (this) {
    AirMouseStatus.Off -> "Аэромышь выключена."
    AirMouseStatus.Active -> "Аэромышь активна: поворачивайте телефон для движения курсора."
    is AirMouseStatus.Unavailable -> reason
    is AirMouseStatus.Error -> reason
}

private fun BluetoothHidStatus.asUserLabel(): String = when (this) {
    BluetoothHidStatus.Idle -> "HID выключен. Нажмите «Подключить» и выберите Radio.TV.Control на ТВ."
    BluetoothHidStatus.Starting -> "Запускаем HID-профиль или ждём соединения с телевизором…"
    BluetoothHidStatus.Ready -> "Профиль зарегистрирован. Откройте Bluetooth на ТВ и выберите Radio.TV.Control."
    is BluetoothHidStatus.Connected -> "Подключено: " + deviceName
    is BluetoothHidStatus.Error -> "Bluetooth HID: " + message
}

private fun VoiceInputStatus.asUserLabel(): String = when (this) {
    VoiceInputStatus.Off -> "Голосовой ввод выключен."
    VoiceInputStatus.Starting -> "Запускается голосовая сессия Android TV Remote v2…"
    VoiceInputStatus.Recording -> "Идёт запись: PCM 16-bit · mono · 8 kHz."
    is VoiceInputStatus.Error -> "Голосовой ввод: " + message
}

private fun OtherTvRemoteStatus.asUserLabel(): String = when (this) {
    OtherTvRemoteStatus.Disconnected -> "Внешний протокол отключён."
    is OtherTvRemoteStatus.Connecting -> "Подключение: " + type.label + " · " + ip
    is OtherTvRemoteStatus.Pairing -> "Ожидается сопряжение: " + message
    is OtherTvRemoteStatus.Connected -> "Подключено: " + type.label + " · " + ip
    is OtherTvRemoteStatus.Error -> "Ошибка подключения: " + message
}
