package com.radiotv.control

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiotv.control.cast.LocalNetworkDeviceDiscovery
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.DiscoveredRemoteDevice
import com.radiotv.control.core.RemoteDeviceType
import com.radiotv.control.core.RemoteStatus
import com.radiotv.control.ui.TvRemotePad
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {
    private val remote: AndroidTvRemoteV2Transport by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RadioTvControlScreen(remote) }
    }

    override fun onDestroy() {
        if (isFinishing) remote.close()
        super.onDestroy()
    }
}

@Composable
private fun RadioTvControlScreen(remote: AndroidTvRemoteV2Transport) {
    val context = LocalContext.current
    val status by remote.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val discovery = remember(context) { LocalNetworkDeviceDiscovery(context) }
    var host by rememberSaveable { mutableStateOf("") }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Телефон и телевизор должны быть в одной сети Wi‑Fi.") }
    var devices by remember { mutableStateOf<List<DiscoveredRemoteDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    val codeRequested = status is RemoteStatus.AwaitingCode
    val connected = status is RemoteStatus.Connected

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) runDiscovery()
        else statusMessage = "Для поиска в локальной сети разрешите доступ или введите IP вручную."
    }

    val refreshDevices: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(ACCESS_LOCAL_NETWORK) else runDiscovery()
    }

    LaunchedEffect(Unit) { refreshDevices() }

    fun chooseDevice(device: DiscoveredRemoteDevice) {
        when (device.type) {
            RemoteDeviceType.ANDROID_TV -> startPairing(device.ip)
            RemoteDeviceType.CHROMECAST -> {
                host = device.ip
                statusMessage = "Найден Google Cast по адресу " + device.ip + ". Управление Cast будет добавлено отдельным адаптером."
            }
            else -> {
                host = device.ip
                statusMessage = "Определено: " + device.type.label + " (" + device.ip + "). Протокол этого устройства пока не подключён."
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 520.dp).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 18.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Radio.TV.Control", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Универсальный Wi‑Fi пульт", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Поиск устройств", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = refreshDevices, enabled = !scanning) { Text("Обновить") }
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
                        items(devices, key = { it.ip }) { device ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                                    OutlinedButton(onClick = { chooseDevice(device) }) { Text("↗") }
                                }
                            }
                        }
                        item {
                            if (devices.isEmpty()) {
                                Button(onClick = { manualEntry = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("ВВЕСТИ IP ВРУЧНУЮ")
                                }
                            }
                            OutlinedButton(
                                onClick = { manualEntry = !manualEntry },
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
                                    onClick = { startPairing(host) },
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
                                                .onSuccess { statusMessage = "Телевизор подключён." }
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
                            TvRemotePad(enabled = connected, onKey = { key ->
                                scope.launch {
                                    runCatching { remote.sendKey(key) }
                                        .onFailure { statusMessage = it.message ?: "Команда не отправлена." }
                                }
                            })
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
