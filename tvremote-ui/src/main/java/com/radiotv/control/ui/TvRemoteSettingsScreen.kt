package com.radiotv.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Integrated remote settings. The host app supplies persisted preferences and transport actions,
 * so this screen stays UI-only and does not create another settings store.
 */
data class RemoteSavedDeviceUi(
    val id: String,
    val name: String,
    val connectionType: String,
    val address: String
)

data class RemoteBluetoothDeviceUi(val name: String, val address: String)

/**
 * Integrated remote settings. The host app supplies persisted preferences and transport actions,
 * so this screen stays UI-only and does not create another settings store.
 */
@Composable
fun TvRemoteSettingsScreen(
    host: String,
    onHostChange: (String) -> Unit,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
    connectionStatus: String,
    bluetoothStatus: String,
    awaitingPairingCode: Boolean,
    connectionMethod: String,
    onConnectionMethodChange: (String) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onStartSearch: () -> Unit,
    onCompletePairing: () -> Unit,
    onStartBluetooth: () -> Unit,
    savedDevices: List<RemoteSavedDeviceUi> = emptyList(),
    bondedDevices: List<RemoteBluetoothDeviceUi> = emptyList(),
    onSelectSavedDevice: (String) -> Unit = {},
    onRenameSavedDevice: (String, String) -> Unit = { _, _ -> },
    onRemoveSavedDevice: (String) -> Unit = {},
    onConnectBluetoothDevice: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<RemoteSavedDeviceUi?>(null) }
    var renameValue by remember { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Text("НАСТРОЙКИ ПУЛЬТА", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (savedDevices.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ВЫБОР ТЕЛЕВИЗОРА", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    savedDevices.forEach { device ->
                        Card(
                            onClick = { onSelectSavedDevice(device.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = RadioTvPalette.Red, modifier = Modifier.size(28.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(device.name.take(16), color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        device.connectionType + " · " + device.address,
                                        color = RadioTvPalette.Muted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { renameTarget = device; renameValue = device.name.take(16) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Переименовать", tint = Color.LightGray)
                                }
                                IconButton(onClick = { onRemoveSavedDevice(device.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color(0xFFFF8A80))
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("СПОСОБ ПОДКЛЮЧЕНИЯ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("auto" to "АВТО", "manual" to "РУЧНОЙ", "bluetooth" to "BLUETOOTH").forEach { (value, label) ->
                        val selected = connectionMethod == value
                        Surface(
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) RadioTvPalette.Red else RadioTvPalette.Raised,
                            onClick = { onConnectionMethodChange(value) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    when (connectionMethod) {
                        "manual" -> "РУЧНОЕ ПОДКЛЮЧЕНИЕ"
                        "bluetooth" -> "BLUETOOTH HID"
                        else -> "АВТОМАТИЧЕСКИЙ ПОИСК"
                    },
                    color = RadioTvPalette.Red,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    connectionStatus,
                    color = when {
                        connectionStatus.startsWith("Подключено") -> Color(0xFF81C784)
                        connectionStatus.startsWith("Нет соединения") -> Color(0xFFFF8A80)
                        else -> Color(0xFFFFCC80)
                    },
                    fontSize = 13.sp
                )

                when (connectionMethod) {
                    "manual" -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("IP-адрес телевизора") },
                            placeholder = { Text("192.168.1.100") }
                        )
                        Button(
                            onClick = onConnect,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = host.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = RadioTvPalette.Red)
                        ) { Text("ПОДКЛЮЧИТЬ", color = Color.White) }
                    }
                    "bluetooth" -> {
                        Text(bluetoothStatus, color = RadioTvPalette.Muted, fontSize = 12.sp)
                        Text(
                            "Выберите ранее сопряжённый телевизор. Для первого сопряжения запустите HID и подтвердите Radio.TV в Bluetooth-настройках ТВ.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        if (bondedDevices.isEmpty()) {
                            Text("Сопряжённые Bluetooth-устройства не найдены.", color = RadioTvPalette.Muted, fontSize = 12.sp)
                        } else {
                            bondedDevices.forEach { device ->
                                Row(
                                    Modifier.fillMaxWidth().background(RadioTvPalette.Raised, RoundedCornerShape(10.dp)).padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = RadioTvPalette.Red)
                                    Column(Modifier.weight(1f)) {
                                        Text(device.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(device.address, color = RadioTvPalette.Muted, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { onConnectBluetoothDevice(device.address) }) {
                                        Text("ПОДКЛЮЧИТЬ", color = RadioTvPalette.Red, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = onStartBluetooth,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303030))
                        ) { Text("ЗАПУСТИТЬ BLUETOOTH HID / СОПРЯЖЕНИЕ", color = Color.White, fontSize = 12.sp) }
                    }
                    else -> {
                        Text("Поиск телевизоров по mDNS и SSDP в локальной сети.", color = RadioTvPalette.Muted, fontSize = 12.sp)
                        Button(
                            onClick = onStartSearch,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RadioTvPalette.Red)
                        ) { Text("ИСКАТЬ ТЕЛЕВИЗОРЫ", color = Color.White) }
                    }
                }

                if (awaitingPairingCode) {
                    Text("Введите шестизначный HEX-код с экрана телевизора.", color = RadioTvPalette.Muted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { value ->
                            onPairingCodeChange(value.filter { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }.take(6).uppercase())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Код сопряжения") }
                    )
                    Button(
                        onClick = onCompletePairing,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pairingCode.length == 6,
                        colors = ButtonDefaults.buttonColors(containerColor = RadioTvPalette.Red)
                    ) { Text("ПОДТВЕРДИТЬ СОПРЯЖЕНИЕ", color = Color.White) }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                RemoteSettingRow("Вибрация", "Короткий тактильный отклик", hapticsEnabled, onHapticsChange)
                RemoteSettingRow("Звук", "Звуковой отклик на действия", soundEnabled, onSoundChange)
                RemoteSettingRow(
                    "Показывать уведомления",
                    "Короткие сообщения о подключении и действиях.",
                    notificationsEnabled,
                    onNotificationsChange
                )
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text("НАЗАД К ПУЛЬТУ", color = RadioTvPalette.Red)
        }
    }

    val target = renameTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать телевизор") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(16) },
                    singleLine = true,
                    label = { Text("Название (до 16 символов)") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.trim().isNotEmpty(),
                    onClick = {
                        onRenameSavedDevice(target.id, renameValue.trim().take(16))
                        renameTarget = null
                    }
                ) { Text("СОХРАНИТЬ", color = RadioTvPalette.Red) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("ОТМЕНА") } }
        )
    }
}

@Composable
private fun RemoteSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(description, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
