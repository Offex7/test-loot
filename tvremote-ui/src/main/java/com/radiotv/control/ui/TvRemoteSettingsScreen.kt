package com.radiotv.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("СПОСОБ ПОДКЛЮЧЕНИЯ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("auto" to "Авто", "manual" to "Ручное", "bluetooth" to "Bluetooth").forEach { (value, label) ->
                        val selected = connectionMethod == value
                        Surface(
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) RadioTvPalette.Red else RadioTvPalette.Raised,
                            onClick = { onConnectionMethodChange(value) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
                Text(
                    when (connectionMethod) {
                        "manual" -> "Подключение по IP-адресу телевизора."
                        "bluetooth" -> "Управление через Bluetooth HID после сопряжения."
                        else -> "Автоматический выбор доступного транспорта: Android TV Remote v2, ADB или Companion."
                    },
                    color = RadioTvPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("ПОДКЛЮЧЕНИЕ К ТЕЛЕВИЗОРУ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                Text(
                    connectionStatus,
                    color = when {
                        connectionStatus.startsWith("Подключено") -> Color(0xFF81C784)
                        connectionStatus.startsWith("Нет соединения") -> Color(0xFFFF8A80)
                        else -> Color(0xFFFFCC80)
                    },
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("IP-адрес телевизора") },
                    placeholder = { Text("192.168.1.100") }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onStartSearch, modifier = Modifier.weight(1f)) {
                        Text("ИСКАТЬ")
                    }
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        enabled = host.isNotBlank() || connectionMethod == "bluetooth",
                        colors = ButtonDefaults.buttonColors(containerColor = RadioTvPalette.Red)
                    ) {
                        Text("ПОДКЛЮЧИТЬ", color = Color.White)
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
                    ) {
                        Text("ПОДТВЕРДИТЬ СОПРЯЖЕНИЕ", color = Color.White)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BLUETOOTH HID", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                Text(bluetoothStatus, color = RadioTvPalette.Muted, fontSize = 13.sp)
                Text(
                    "Запустите HID и выберите Radio.TV в Bluetooth-настройках телевизора. После сопряжения клавиши и указатель смогут работать без Wi‑Fi.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Button(
                    onClick = onStartBluetooth,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303030))
                ) {
                    Text("ВКЛЮЧИТЬ BLUETOOTH HID", color = Color.White)
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
