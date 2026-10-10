package com.radiotv.control

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
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
import com.radiotv.control.core.OtherTvRemoteController
import com.radiotv.control.core.OtherTvRemoteStatus
import com.radiotv.control.ui.TouchpadSurface
import com.radiotv.control.ui.TvRemotePad
import kotlinx.coroutines.launch
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
            grants[Manifest.permission.BLUETOOTH_ADVERTISE] == true
        ) {
            startBluetoothSession()
        } else {
            statusMessage = "Для Bluetooth HID разрешите Bluetooth Connect и Advertise."
        }
    }

    val beginBluetoothSession: () -> Unit = {
        val required = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
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

    LaunchedEffect(Unit) { refreshDevices() }

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

    MaterialTheme(colorScheme = radioTvColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 520.dp).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Radio.TV.Control", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Универсальный Wi‑Fi пульт", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Поиск устройств", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    refreshDevices()
                                }, enabled = !scanning) { Text("Обновить") }
                            }
                            if (pairedHost.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        connectPairedHost(pairedHost)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ПОДКЛЮЧИТЬ СОХРАНЁННЫЙ ТВ · " + pairedHost)
                                }
                            }
                            if (scanning) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(4.dp))
                                    Text("Сканируем mDNS, SSDP и TCP-порты…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else if (devices.isEmpty()) {
                                Text("Устройства не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Bluetooth HID", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        beginBluetoothSession()
                                    }
                                ) { Text("Подключить") }
                            }
                            Text(
                                bluetoothStatus.asUserLabel(),
                                color = if (bluetoothStatus is BluetoothHidStatus.Connected) RadioTvPalette.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                airMouseStatus.asUserLabel(),
                                color = if (airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                voiceStatus.asUserLabel(),
                                color = if (voiceStatus is VoiceInputStatus.Recording) RadioTvPalette.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        items(devices, key = { it.ip }) { device ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(deviceIcon(device.type), fontSize = 23.sp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(device.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(
                                            listOfNotNull(device.brand, device.model, device.type.label).distinct().joinToString(" · "),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                        Text(
                                            device.ip + if (device.ports.isEmpty()) "" else " · " + device.ports.sorted().joinToString(","),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    OutlinedButton(onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        chooseDevice(device)
                                    }) { Text("↗") }
                                }
                            }
                        }
                        item {
                            if (devices.isEmpty()) {
                                Button(onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    manualEntry = true
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("ВВЕСТИ IP ВРУЧНУЮ")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    manualEntry = !manualEntry
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (manualEntry) "СКРЫТЬ РУЧНОЙ ВВОД" else "ВВЕСТИ IP ВРУЧНУЮ") }
                            if (manualEntry) {
                                OutlinedTextField(
                                    value = host,
                                    onValueChange = { host = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    label = { Text("IP-адрес телевизора") },
                                    placeholder = { Text("192.168.1.100") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                )
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        connectManualHost()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = host.isNotBlank()
                                ) { Text("НАЧАТЬ СОПРЯЖЕНИЕ ПО IP") }
                            }
                        }
                        item {
                            Text(
                                when (val current = status) {
                                    RemoteStatus.Disconnected -> "Не подключено"
                                    is RemoteStatus.Connecting -> "Подключение: " + current.host
                                    is RemoteStatus.AwaitingCode -> "Ожидается код сопряжения"
                                    is RemoteStatus.Connected -> "Подключено: " + current.host
                                    is RemoteStatus.Error -> "Ошибка: " + current.message
                                },
                                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(statusMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (otherRemoteStatus !is OtherTvRemoteStatus.Disconnected) {
                                Text(
                                    otherRemoteStatus.asUserLabel(),
                                    color = if (otherConnected) RadioTvPalette.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        item {
                            if (codeRequested) {
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
                            Text("ПУЛЬТ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            TvRemotePad(
                                enabled = connected || hidConnected,
                                onKey = { key ->
                                    scope.launch {
                                        runCatching {
                                            when {
                                                wifiConnected -> remote.sendKey(key)
                                                otherConnected -> otherTvRemote.sendKey(key)
                                                hidConnected -> bluetoothHid.sendRemoteKey(key)
                                                else -> error("Сначала подключитесь к телевизору.")
                                            }
                                        }.onFailure { statusMessage = it.message ?: "Команда не отправлена." }
                                    }
                                },
                                onFeatureAction = { action ->
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
                                                "Для голосового ввода сначала подключитесь к Android TV по Wi-Fi."
                                            } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                scope.launch {
                                                    runCatching { voiceInput.start() }
                                                        .onSuccess { statusMessage = it }
                                                        .onFailure { statusMessage = it.message ?: "Не удалось включить голосовой ввод." }
                                                }
                                                "Подключаем голосовой ввод…"
                                            } else {
                                                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                "Запрашиваем разрешение на микрофон…"
                                            }
                                        }
                                        RemoteFeatureAction.KEYBOARD -> {
                                            showKeyboard = !showKeyboard
                                            if (showKeyboard) "Откройте клавиатуру и отправьте текст на подключённый ТВ." else "Клавиатура скрыта."
                                        }
                                        RemoteFeatureAction.AIR_MOUSE -> airMouse.toggle()
                                        RemoteFeatureAction.TOUCHPAD -> {
                                            showTouchpad = !showTouchpad
                                            if (showTouchpad) "Тачпад включён. Для управления курсором подключите Bluetooth HID."
                                            else "Тачпад скрыт."
                                        }
                                        RemoteFeatureAction.CAST -> {
                                            showCast = !showCast
                                            if (showCast) refreshCastDevices()
                                            if (showCast) "Открыта панель DLNA-трансляции." else "Панель трансляции скрыта."
                                        }
                                    }
                                },
                                touchpadActive = showTouchpad,
                                airMouseActive = airMouseStatus is AirMouseStatus.Active,
                                voiceActive = voiceStatus is VoiceInputStatus.Recording,
                                keyboardActive = showKeyboard
                            )
                        }
                        if (showKeyboard) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = keyboardText,
                                        onValueChange = { keyboardText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Текст для телевизора") },
                                        placeholder = { Text("Введите текст…") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                    )
                                    Button(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            scope.launch {
                                                runCatching {
                                                    when {
                                                        wifiConnected -> remote.sendText(keyboardText)
                                                        otherConnected -> otherTvRemote.sendText(keyboardText)
                                                        hidConnected -> bluetoothHid.sendText(keyboardText)
                                                        else -> error("Сначала подключитесь по Wi-Fi или Bluetooth HID.")
                                                    }
                                                }.onSuccess {
                                                    statusMessage = "Текст отправлен."
                                                    keyboardText = ""
                                                }.onFailure {
                                                    statusMessage = it.message ?: "Не удалось отправить текст."
                                                }
                                            }
                                        },
                                        enabled = keyboardText.isNotEmpty() && (connected || hidConnected),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("ОТПРАВИТЬ ТЕКСТ НА ТВ") }
                                }
                            }
                        }
                        if (showTouchpad) {
                            item {
                                TouchpadSurface(
                                    enabled = hidConnected,
                                    onMove = { dx, dy ->
                                        scope.launch {
                                            runCatching { bluetoothHid.moveMouse(dx, dy) }
                                                .onFailure { statusMessage = it.message ?: "Не удалось переместить Bluetooth-курсор." }
                                        }
                                    },
                                    onTap = {
                                        scope.launch {
                                            runCatching { bluetoothHid.clickMouse(1) }
                                                .onFailure { statusMessage = it.message ?: "Не удалось нажать Bluetooth-мышь." }
                                        }
                                    },
                                    onLongPress = {
                                        scope.launch {
                                            runCatching { bluetoothHid.clickMouse(2) }
                                                .onFailure { statusMessage = it.message ?: "Не удалось отправить контекстный клик." }
                                        }
                                    }
                                )
                            }
                        }

                        if (showCast) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("DLNA / UPnP", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = castMediaUrl,
                                        onValueChange = { castMediaUrl = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("HTTP(S)-ссылка на медиафайл") },
                                        placeholder = { Text("https://server/media.mp4") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(onClick = refreshCastDevices, enabled = !castScanning) {
                                            Text(if (castScanning) "ПОИСК…" else "ОБНОВИТЬ")
                                        }
                                        Button(
                                            onClick = {
                                                val renderer = selectedCastRenderer
                                                scope.launch {
                                                    runCatching {
                                                        check(renderer != null) { "Сначала выберите DLNA-приёмник." }
                                                        check(castMediaUrl.startsWith("http://", true) || castMediaUrl.startsWith("https://", true)) {
                                                            "Введите абсолютный HTTP(S)-URL файла, доступный телевизору."
                                                        }
                                                        val mime = when {
                                                            castMediaUrl.substringBefore('?').endsWith(".mp3", true) -> "audio/mpeg"
                                                            castMediaUrl.substringBefore('?').endsWith(".m3u8", true) -> "application/vnd.apple.mpegurl"
                                                            castMediaUrl.substringBefore('?').endsWith(".m3u", true) -> "audio/x-mpegurl"
                                                            castMediaUrl.substringBefore('?').endsWith(".jpg", true) || castMediaUrl.substringBefore('?').endsWith(".jpeg", true) -> "image/jpeg"
                                                            castMediaUrl.substringBefore('?').endsWith(".png", true) -> "image/png"
                                                            else -> "video/mp4"
                                                        }
                                                        dlnaCast.play(renderer!!, CastMedia("Radio.TV.Control media", castMediaUrl, mime))
                                                    }.onSuccess {
                                                        castMessage = "Команда Play отправлена выбранному DLNA-приёмнику."
                                                    }.onFailure {
                                                        castMessage = it.message ?: "Не удалось начать DLNA-воспроизведение."
                                                    }
                                                }
                                            },
                                            enabled = selectedCastRenderer != null && castMediaUrl.startsWith("http", true)
                                        ) { Text("▶ PLAY") }
                                        OutlinedButton(
                                            onClick = {
                                                val renderer = selectedCastRenderer
                                                scope.launch {
                                                    runCatching {
                                                        check(renderer != null) { "Сначала выберите DLNA-приёмник." }
                                                        dlnaCast.stop(renderer!!)
                                                    }.onSuccess { castMessage = "Команда Stop отправлена." }
                                                        .onFailure { castMessage = it.message ?: "Не удалось остановить воспроизведение." }
                                                }
                                            },
                                            enabled = selectedCastRenderer != null
                                        ) { Text("■") }
                                    }
                                    Text(
                                        selectedCastRenderer?.let { "Выбрано: " + (it.friendlyName ?: it.server ?: it.location) }
                                            ?: "Выберите приёмник ниже.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(castMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            items(castDevices, key = { it.location }) { renderer ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(renderer.friendlyName ?: renderer.server ?: "DLNA renderer", fontWeight = FontWeight.SemiBold)
                                            Text(renderer.location, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                        }
                                        OutlinedButton(onClick = { selectedCastRenderer = renderer }) {
                                            Text(if (selectedCastRenderer?.location == renderer.location) "ВЫБРАНО" else "ВЫБРАТЬ")
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Поиск ограничен локальной подсетью /24. Обнаружение типа устройства не гарантирует поддержку его протокола.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun deviceIcon(type: RemoteDeviceType): String = when (type) {
    RemoteDeviceType.ANDROID_TV -> "📺"
    RemoteDeviceType.CHROMECAST -> "📡"
    RemoteDeviceType.SAMSUNG -> "📺"
    RemoteDeviceType.LG -> "🖥"
    RemoteDeviceType.ROKU -> "📺"
    RemoteDeviceType.DLNA -> "🔊"
    RemoteDeviceType.AIRPLAY -> "🍎"
    RemoteDeviceType.UNKNOWN -> "🔎"
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
