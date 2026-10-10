package com.radiotv.control

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.radioTvColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
        setContent { RadioTvControlScreen(remote, bluetoothHid, airMouse, voiceInput, otherTvRemote, dlnaCast) }
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
private fun RadioTvControlScreen(remote: AndroidTvRemoteV2Transport, bluetoothHid: BluetoothHidController, airMouse: GyroAirMouseController, voiceInput: AndroidTvVoiceInputController, otherTvRemote: OtherTvRemoteController, dlnaCast: DlnaCastController) {
    val context = LocalContext.current
    val view = LocalView.current
    val status by remote.status.collectAsStateWithLifecycle()
    val bluetoothStatus by bluetoothHid.status.collectAsStateWithLifecycle()
    val airMouseStatus by airMouse.status.collectAsStateWithLifecycle()
    val voiceStatus by voiceInput.status.collectAsStateWithLifecycle()
    val otherRemoteStatus by otherTvRemote.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val discovery = remember(context) { LocalNetworkDeviceDiscovery(context) }
    var host by rememberSaveable { mutableStateOf("") }
    var pairedHost by rememberSaveable { mutableStateOf(remote.pairedHost().orEmpty()) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Телефон и телевизор должны быть в одной сети Wi‑Fi.") }
    var devices by remember { mutableStateOf<List<DiscoveredRemoteDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    var showTouchpad by rememberSaveable { mutableStateOf(false) }
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var keyboardText by rememberSaveable { mutableStateOf("") }
    var showCast by rememberSaveable { mutableStateOf(false) }
    var castMediaUrl by rememberSaveable { mutableStateOf("") }
    var castMessage by rememberSaveable { mutableStateOf("Нажмите «Обновить», чтобы найти DLNA-приёмники.") }
    var castScanning by remember { mutableStateOf(false) }
    var castDevices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var selectedCastRenderer by remember { mutableStateOf<DlnaDevice?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(RadioTvTab.REMOTE) }
    var castMedia by remember { mutableStateOf<CastMedia?>(null) }
    var castPlaybackStatus by rememberSaveable { mutableStateOf("Остановлено") }
    val localMediaServer = remember(context) { LocalMediaHttpServer(context) }
    DisposableEffect(localMediaServer) {
        onDispose { localMediaServer.close() }
    }

    val handlePickedMedia: (Uri?) -> Unit = { uri ->
        if (uri == null) {
            castMessage = "Выбор файла отменён."
        } else {
            scope.launch {
                castMessage = "Готовим файл для передачи на телевизор…"
                runCatching {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.IO) { localMediaServer.start(uri) }
                }.onSuccess { media ->
                    castMedia = media
                    castMessage = "Подготовлено: ${media.title}. Выберите DLNA-приёмник."
                    val renderer = selectedCastRenderer
                    if (renderer != null) {
                        runCatching { dlnaCast.play(renderer, media) }
                            .onSuccess {
                                castPlaybackStatus = "Воспроизведение"
                                castMessage = "Передаём «${media.title}» на ${renderer.friendlyName ?: renderer.server ?: renderer.location}."
                            }
                            .onFailure {
                                castPlaybackStatus = "Ошибка"
                                castMessage = it.message ?: "Не удалось начать DLNA-воспроизведение."
                            }
                    }
                    showCast = true
                }.onFailure {
                    castPlaybackStatus = "Ошибка"
                    castMessage = it.message ?: "Не удалось подготовить выбранный файл."
                }
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

    val refreshCastDevices: () -> Unit = {
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
        if (granted) runDiscovery()
        else statusMessage = "Для поиска в локальной сети разрешите доступ или введите IP вручную."
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
        val needsPermission = Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(ACCESS_LOCAL_NETWORK) else runDiscovery()
    }

    val connectPairedHost: (String) -> Unit = { target ->
        scope.launch {
            statusMessage = "Подключаемся к ранее сопряжённому Android TV…"
            runCatching { remote.connect(target) }
                .onSuccess { statusMessage = "Подключено к сохранённому телевизору: $target" }
                .onFailure { statusMessage = it.message ?: "Не удалось подключиться к сохранённому телевизору." }
        }
    }


    fun chooseDevice(device: DiscoveredRemoteDevice) {
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

    val handleFeatureAction: (RemoteFeatureAction) -> Unit = { action ->
        statusMessage = when (action) {
            RemoteFeatureAction.VOICE_INPUT -> {
                if (voiceStatus is VoiceInputStatus.Recording || voiceStatus is VoiceInputStatus.Starting) {
                    scope.launch {
                        runCatching { voiceInput.stop() }
                            .onSuccess { statusMessage = it }
                            .onFailure { statusMessage = it.message ?: "Не удалось остановить запись." }
                    }
                    "Останавливаем голосовой ввод…"
                } else if (!wifiConnected) {
                    "Сначала подключитесь к телевизору."
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
            RemoteFeatureAction.KEYBOARD -> {
                showKeyboard = !showKeyboard
                if (showKeyboard) "Откройте клавиатуру и отправьте текст на подключённый телевизор." else "Клавиатура скрыта."
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
                showTouchpad = !showTouchpad
                if (showTouchpad) "Тачпад включён. Для движения курсора требуется соединение Bluetooth HID."
                else "Тачпад скрыт."
            }
            RemoteFeatureAction.CAST -> {
                selectedTab = RadioTvTab.DUPLICATION
                showCast = true
                refreshCastDevices()
                mediaPicker.launch(arrayOf("image/*", "video/*", "audio/*"))
                "Открываем системный выбор медиа. DRM/HLS/DASH-контент не поддерживается."
            }
        }
    }

    MaterialTheme(colorScheme = radioTvColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = RadioTvPalette.Black) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(RadioTvPalette.Black)) {
                val isLandscape = maxWidth > maxHeight
                val panelWidth = if (isLandscape) 460.dp else 520.dp
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = panelWidth)
                        .fillMaxHeight()
                        .background(RadioTvPalette.Black)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            RadioTvTab.REMOTE -> {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TvRemotePad(
                                        enabled = connected || hidConnected,
                                        modifier = Modifier
                                            .widthIn(max = if (maxWidth > maxHeight) 420.dp else 470.dp)
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        onKey = { key ->
                                            scope.launch {
                                                runCatching {
                                                    when {
                                                        wifiConnected -> remote.sendKey(key)
                                                        otherConnected -> otherTvRemote.sendKey(key)
                                                        hidConnected -> bluetoothHid.sendRemoteKey(key)
                                                        else -> error("Сначала подключитесь к телевизору через вкладку «Контроль».")
                                                    }
                                                }.onFailure { statusMessage = it.message ?: "Команда не отправлена." }
                                            }
                                        },
                                        onFeatureAction = handleFeatureAction,
                                        touchpadActive = showTouchpad,
                                        airMouseActive = airMouseStatus is AirMouseStatus.Active,
                                        voiceActive = voiceStatus is VoiceInputStatus.Recording,
                                        keyboardActive = showKeyboard,
                                        featureMessage = null,
                                        showFeatureActions = false
                                    )
                                }
                            }
                            RadioTvTab.CONTROL -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    item {
                                        Text("КОНТРОЛЬ УСТРОЙСТВ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                        Text("Телефон и телевизор должны быть в одной Wi‑Fi сети.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Поиск устройств", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            OutlinedButton(onClick = refreshDevices, enabled = !scanning) { Text(if (scanning) "ПОИСК…" else "ОБНОВИТЬ") }
                                        }
                                        if (pairedHost.isNotBlank()) {
                                            OutlinedButton(onClick = { connectPairedHost(pairedHost) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("ПОДКЛЮЧИТЬ СОХРАНЁННЫЙ ТВ · $pairedHost")
                                            }
                                        }
                                        if (scanning) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(4.dp))
                                                Text("Сканируем mDNS, SSDP и TCP-порты…", color = RadioTvPalette.Muted)
                                            }
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
                                                    Text(device.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text(listOfNotNull(device.brand, device.model, device.type.label).distinct().joinToString(" · "),
                                                        color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                                    Text(device.ip + if (device.ports.isEmpty()) "" else " · " + device.ports.sorted().joinToString(","),
                                                        color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                OutlinedButton(onClick = { chooseDevice(device) }) { Text("↗") }
                                            }
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { manualEntry = !manualEntry }, modifier = Modifier.fillMaxWidth()) {
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
                                            Button(onClick = connectManualHost, modifier = Modifier.fillMaxWidth(), enabled = host.isNotBlank()) {
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
                                                is RemoteStatus.Error -> "Ошибка: ${current.message}"
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
                                                    scope.launch {
                                                        statusMessage = "Проверяем код и сохраняем сопряжение…"
                                                        runCatching { remote.completePairing(pairingCode) }
                                                            .onSuccess {
                                                                pairedHost = remote.pairedHost().orEmpty()
                                                                statusMessage = "Телевизор подключён."
                                                            }
                                                            .onFailure { statusMessage = it.message ?: "Не удалось завершить сопряжение." }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = pairingCode.length == 6
                                            ) { Text("ПОДТВЕРДИТЬ КОД") }
                                        }
                                    }
                                    item {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Bluetooth HID", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            OutlinedButton(onClick = beginBluetoothSession) { Text("ПОДКЛЮЧИТЬ") }
                                        }
                                        Text(bluetoothStatus.asUserLabel(), color = if (hidConnected) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text(airMouseStatus.asUserLabel(), color = if (airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text(voiceStatus.asUserLabel(), color = if (voiceStatus is VoiceInputStatus.Recording) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Text("ВОЗМОЖНОСТИ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            ControlActionButton("◉\nМикрофон", voiceStatus is VoiceInputStatus.Recording, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.VOICE_INPUT) }
                                            ControlActionButton("✥\nАэромышь", airMouseStatus is AirMouseStatus.Active, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.AIR_MOUSE) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            ControlActionButton("⌨\nКлавиатура", showKeyboard, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.KEYBOARD) }
                                            ControlActionButton("▧\nТачпад", showTouchpad, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.TOUCHPAD) }
                                            ControlActionButton("▣\nТрансляция", false, Modifier.weight(1f)) { handleFeatureAction(RemoteFeatureAction.CAST) }
                                        }
                                        Text("Состояние: $statusMessage", color = if (voiceStatus is VoiceInputStatus.Recording || airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text("Разрешение микрофона запрашивается после соединения с Android TV. Аэромышь требует гироскоп и Bluetooth HID.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
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
                                                    scope.launch {
                                                        runCatching {
                                                            when {
                                                                wifiConnected -> remote.sendText(keyboardText)
                                                                otherConnected -> otherTvRemote.sendText(keyboardText)
                                                                hidConnected -> bluetoothHid.sendText(keyboardText)
                                                                else -> error("Сначала подключитесь по Wi‑Fi или Bluetooth HID.")
                                                            }
                                                        }.onSuccess { statusMessage = "Текст отправлен."; keyboardText = "" }
                                                            .onFailure { statusMessage = it.message ?: "Не удалось отправить текст." }
                                                    }
                                                },
                                                enabled = keyboardText.isNotEmpty() && (connected || hidConnected),
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("ОТПРАВИТЬ ТЕКСТ НА ТВ") }
                                        }
                                    }
                                    if (showTouchpad) {
                                        item {
                                            TouchpadSurface(
                                                enabled = hidConnected,
                                                onMove = { dx, dy -> scope.launch { runCatching { bluetoothHid.moveMouse(dx, dy) }.onFailure { statusMessage = it.message ?: "Не удалось переместить Bluetooth-курсор." } } },
                                                onTap = { scope.launch { runCatching { bluetoothHid.clickMouse(1) }.onFailure { statusMessage = it.message ?: "Не удалось нажать Bluetooth-мышь." } } },
                                                onLongPress = { scope.launch { runCatching { bluetoothHid.clickMouse(2) }.onFailure { statusMessage = it.message ?: "Не удалось отправить контекстный клик." } } }
                                            )
                                        }
                                    }
                                    item {
                                        Text("ДОПОЛНИТЕЛЬНЫЕ КЛАВИШИ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                                        (1..9).toList().chunked(3).forEach { digits ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                digits.forEach { digit ->
                                                    OutlinedButton(
                                                        onClick = { sendRemoteKey(digitKey(digit), remote, otherTvRemote, bluetoothHid, wifiConnected, otherConnected, hidConnected, scope) },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) { Text(digit.toString()) }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(onClick = { sendRemoteKey(RemoteKey.NUMBER_0, remote, otherTvRemote, bluetoothHid, wifiConnected, otherConnected, hidConnected, scope) }, modifier = Modifier.weight(1f)) { Text("0") }
                                            OutlinedButton(onClick = { sendRemoteKey(RemoteKey.REWIND, remote, otherTvRemote, bluetoothHid, wifiConnected, otherConnected, hidConnected, scope) }, modifier = Modifier.weight(1f)) { Text("⏪") }
                                            OutlinedButton(onClick = { sendRemoteKey(RemoteKey.PLAY_PAUSE, remote, otherTvRemote, bluetoothHid, wifiConnected, otherConnected, hidConnected, scope) }, modifier = Modifier.weight(1f)) { Text("▶/Ⅱ") }
                                            OutlinedButton(onClick = { sendRemoteKey(RemoteKey.FAST_FORWARD, remote, otherTvRemote, bluetoothHid, wifiConnected, otherConnected, hidConnected, scope) }, modifier = Modifier.weight(1f)) { Text("⏩") }
                                        }
                                    }
                                }
                            }
                            RadioTvTab.DUPLICATION -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    item {
                                        Text("ДУБЛИРОВАНИЕ / ТРАНСЛЯЦИЯ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                        Text("Выберите локальный файл. Телефон отдаёт его по HTTP в локальной сети, а DLNA-приёмник воспроизводит по ссылке.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { mediaPicker.launch(arrayOf("image/*", "video/*")) }, modifier = Modifier.weight(1f)) { Text("Фото / видео") }
                                            Button(onClick = { audioPicker.launch("audio/*") }, modifier = Modifier.weight(1f)) { Text("Музыка / аудио") }
                                        }
                                        OutlinedButton(onClick = { mediaPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ВЫБРАТЬ ЛЮБОЙ ФАЙЛ") }
                                    }
                                    item {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("DLNA-приёмники", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            OutlinedButton(onClick = refreshCastDevices, enabled = !castScanning) { Text(if (castScanning) "ПОИСК…" else "ОБНОВИТЬ") }
                                        }
                                        Text(castMedia?.let { "Медиа: ${it.title}" } ?: "Медиа ещё не выбрано.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text("Состояние: $castPlaybackStatus", color = if (castPlaybackStatus == "Воспроизведение") RadioTvPalette.Red else RadioTvPalette.Muted)
                                    }
                                    items(castDevices, key = { it.location }) { renderer ->
                                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(renderer.friendlyName ?: renderer.server ?: "DLNA renderer", fontWeight = FontWeight.SemiBold)
                                                    Text(renderer.location, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                                }
                                                OutlinedButton(onClick = {
                                                    selectedCastRenderer = renderer
                                                    val media = castMedia
                                                    if (media != null) scope.launch {
                                                        runCatching { dlnaCast.play(renderer, media) }
                                                            .onSuccess {
                                                                castPlaybackStatus = "Воспроизведение"
                                                                castMessage = "Передаём «${media.title}» на ${renderer.friendlyName ?: renderer.location}."
                                                            }
                                                            .onFailure {
                                                                castPlaybackStatus = "Ошибка"
                                                                castMessage = it.message ?: "Не удалось запустить выбранное медиа."
                                                            }
                                                    }
                                                }) { Text(if (selectedCastRenderer?.location == renderer.location) "ВЫБРАНО" else "ВЫБРАТЬ") }
                                            }
                                        }
                                    }
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = {
                                                    val renderer = selectedCastRenderer
                                                    val media = castMedia
                                                    scope.launch {
                                                        runCatching {
                                                            check(renderer != null) { "Сначала выберите DLNA-приёмник." }
                                                            check(media != null) { "Сначала выберите медиафайл." }
                                                            dlnaCast.play(renderer!!, media!!)
                                                        }.onSuccess {
                                                            castPlaybackStatus = "Воспроизведение"
                                                            castMessage = "Команда Play отправлена."
                                                        }.onFailure {
                                                            castPlaybackStatus = "Ошибка"
                                                            castMessage = it.message ?: "Не удалось начать воспроизведение."
                                                        }
                                                    }
                                                },
                                                enabled = selectedCastRenderer != null && castMedia != null
                                            ) { Text("▶ Play") }
                                            OutlinedButton(
                                                onClick = {
                                                    val renderer = selectedCastRenderer
                                                    scope.launch {
                                                        runCatching { check(renderer != null) { "Сначала выберите DLNA-приёмник." }; dlnaCast.pause(renderer!!) }
                                                            .onSuccess { castPlaybackStatus = "Пауза"; castMessage = "Команда Pause отправлена." }
                                                            .onFailure { castMessage = it.message ?: "Pause не поддерживается." }
                                                    }
                                                }, enabled = selectedCastRenderer != null
                                            ) { Text("Ⅱ Pause") }
                                            OutlinedButton(
                                                onClick = {
                                                    val renderer = selectedCastRenderer
                                                    scope.launch {
                                                        runCatching { check(renderer != null) { "Сначала выберите DLNA-приёмник." }; dlnaCast.stop(renderer!!) }
                                                            .onSuccess { castPlaybackStatus = "Остановлено"; castMessage = "Команда Stop отправлена." }
                                                            .onFailure { castMessage = it.message ?: "Не удалось остановить." }
                                                    }
                                                }, enabled = selectedCastRenderer != null
                                            ) { Text("■ Stop") }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    val renderer = selectedCastRenderer
                                                    scope.launch {
                                                        runCatching { check(renderer != null) { "Сначала выберите DLNA-приёмник." }; dlnaCast.seek(renderer!!, -30) }
                                                            .onSuccess { castMessage = "Перемотка назад отправлена." }
                                                            .onFailure { castMessage = it.message ?: "Приёмник не поддерживает перемотку." }
                                                    }
                                                }, enabled = selectedCastRenderer != null
                                            ) { Text("−30 с") }
                                            OutlinedButton(
                                                onClick = {
                                                    val renderer = selectedCastRenderer
                                                    scope.launch {
                                                        runCatching { check(renderer != null) { "Сначала выберите DLNA-приёмник." }; dlnaCast.seek(renderer!!, 30) }
                                                            .onSuccess { castMessage = "Перемотка вперёд отправлена." }
                                                            .onFailure { castMessage = it.message ?: "Приёмник не поддерживает перемотку." }
                                                    }
                                                }, enabled = selectedCastRenderer != null
                                            ) { Text("+30 с") }
                                        }
                                        Text(castMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            RadioTvTab.SETTINGS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 18.dp, bottom = 18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text("НАСТРОЙКИ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                    }
                                    item {
                                        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
                                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Красно-чёрная тема", fontWeight = FontWeight.SemiBold)
                                                Text("Фон #000000 / #0A0A0A · акцент #E53935 · текст #FFFFFF / #B0B0B0.", color = RadioTvPalette.Muted)
                                            }
                                        }
                                    }
                                    item {
                                        Text("Safe area", color = RadioTvPalette.Red, fontWeight = FontWeight.SemiBold)
                                        Text("Контент размещён внутри WindowInsets.safeDrawing: учитываются системные панели и displayCutout.", color = RadioTvPalette.Muted)
                                    }
                                    item {
                                        Text("Адаптация больших экранов", color = RadioTvPalette.Red, fontWeight = FontWeight.SemiBold)
                                        Text("При landscape ширина рабочей колонки ограничена 460 dp; она остаётся по центру, боковые поля чёрные.", color = RadioTvPalette.Muted)
                                    }
                                    item {
                                        Text("Состояние подключения", color = RadioTvPalette.Red, fontWeight = FontWeight.SemiBold)
                                        Text(statusMessage, color = RadioTvPalette.Muted)
                                    }
                                }
                            }
                        }
                    }
                    RadioTvBottomBar(
                        selected = selectedTab,
                        onSelect = { tab ->
                            val tabChanged = selectedTab != tab
                            selectedTab = tab
                            if (tabChanged && tab == RadioTvTab.CONTROL) refreshDevices()
                            if (tabChanged && tab == RadioTvTab.DUPLICATION) {
                                showCast = true
                                refreshCastDevices()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(72.dp)
                    )
                }
            }
        }
    }
}

private enum class RadioTvTab(val title: String, val icon: String) {
    REMOTE("Пульт", "▣"),
    CONTROL("Контроль", "▦"),
    DUPLICATION("Дублирование", "▱"),
    SETTINGS("Настройки", "⚙")
}

@Composable
private fun RadioTvBottomBar(
    selected: RadioTvTab,
    onSelect: (RadioTvTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier = modifier.background(RadioTvPalette.Black).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        RadioTvTab.entries.forEach { tab ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSelect(tab)
                    }
                    .padding(horizontal = 1.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(tab.icon, color = if (tab == selected) RadioTvPalette.Red else RadioTvPalette.Muted, fontSize = 22.sp)
                Text(
                    tab.title,
                    color = if (tab == selected) RadioTvPalette.Red else RadioTvPalette.Muted,
                    fontSize = if (tab == RadioTvTab.DUPLICATION) 10.sp else 11.sp,
                    fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
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
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) RadioTvPalette.Red else RadioTvPalette.Raised,
            contentColor = Color.White,
            disabledContainerColor = RadioTvPalette.Raised,
            disabledContentColor = RadioTvPalette.Muted
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, lineHeight = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
