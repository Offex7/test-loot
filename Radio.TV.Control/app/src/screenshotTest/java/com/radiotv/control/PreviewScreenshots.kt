package com.radiotv.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.TvRemotePad
import com.radiotv.control.ui.radioTvColorScheme

@PreviewTest
@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun remoteScreenPreview() {
    MaterialTheme(colorScheme = radioTvColorScheme()) {
        TvRemotePad(
            enabled = true,
            onKey = { _: RemoteKey -> },
            deviceName = "Xiaomi TV A",
            onFeatureAction = { _: RemoteFeatureAction -> },
            onSettings = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun controlScreenPreview() {
    MaterialTheme(colorScheme = radioTvColorScheme()) {
        DigitalRemotePanel(
            deviceName = "Xiaomi TV A",
            installedApps = emptyList<InstalledTvApp>(),
            onLaunchApp = {},
            enabled = true,
            micRecording = false,
            airMouseActive = false,
            keyboardActive = false,
            onKey = { _: RemoteKey -> },
            onFeatureAction = { _: RemoteFeatureAction -> },
            onSettings = {},
            onReturnRemote = {},
            duplicationExpanded = false,
            onToggleDuplication = {},
            duplicationContent = {}
        )
    }
}

/**
 * Settings preview is intentionally isolated from Activity-bound Bluetooth/network/DataStore
 * controllers so Layoutlib can render it in a host-side screenshot test.
 */
@PreviewTest
@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun settingsScreenPreview() {
    MaterialTheme(colorScheme = radioTvColorScheme()) {
        SettingsPreviewContent()
    }
}

@Composable
private fun SettingsPreviewContent() {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(RadioTvPalette.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                color = Color(0xFF2A2A2A),
                shape = CircleShape
            ) { Box(contentAlignment = Alignment.Center) { Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium) } }
            Text("Настройки", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Text("НАСТРОЙКИ УСТРОЙСТВ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleMedium)
        Text("Телефон и телевизор должны быть в одной Wi-Fi сети.", color = RadioTvPalette.Muted)
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("СПОСОБ ПОДКЛЮЧЕНИЯ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsChoice("Wi‑Fi", selected = true, modifier = Modifier.weight(1f))
                    SettingsChoice("IP / ИК", selected = false, modifier = Modifier.weight(1f))
                    SettingsChoice("Bluetooth", selected = false, modifier = Modifier.weight(1f))
                }
                Text("Поиск устройств · Подключение к сохранённому ТВ", color = Color.White)
                Text("Ручной ввод IP · Kivi TV (ADB)", color = RadioTvPalette.Muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                SettingsSwitchRow("Вибрация при нажатии", "Короткий тактильный отклик", true)
                SettingsSwitchRow("Звук при нажатии", "Выключен по умолчанию", false)
                Text("Тип звука", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsChoice("Клик", selected = true, modifier = Modifier.weight(1f))
                    SettingsChoice("Голос", selected = false, modifier = Modifier.weight(1f))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ВОЗМОЖНОСТИ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                Text("Поиск устройств · Ручной IP · Bluetooth HID", color = Color.White)
                Text("Аэромышь · Трансляция · Дублирование", color = RadioTvPalette.Muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("В разработке", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Проверка Samsung Tizen, LG webOS, Roku, Bluetooth HID и DLNA требует физических телевизоров. Иконки приложений доступны только там, где устройство отдаёт их через API.",
                    color = RadioTvPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, description: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(description, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Composable
private fun SettingsChoice(title: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RadioTvPalette.Red else RadioTvPalette.Black
    ) {
        Box(contentAlignment = Alignment.Center) { Text(title, color = Color.White) }
    }
}
