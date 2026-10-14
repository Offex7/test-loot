package com.radiotv.control

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.TvRemotePad
import com.radiotv.control.ui.radioTvColorScheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTests {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false
    )

    @Test
    fun screenshotRemote() {
        paparazzi.snapshot {
            MaterialTheme(colorScheme = radioTvColorScheme()) {
                TvRemotePad(
                    enabled = true,
                    onKey = { _: RemoteKey -> },
                    deviceName = "Xiaomi",
                    onFeatureAction = { _: RemoteFeatureAction -> },
                    onSettings = {}
                )
            }
        }
    }

    @Test
    fun screenshotControl() {
        paparazzi.snapshot {
            MaterialTheme(colorScheme = radioTvColorScheme()) {
                DigitalRemotePanel(
                    deviceName = "Xiaomi",
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
    }

    @Test
    fun screenshotSettings() {
        paparazzi.snapshot {
            MaterialTheme(colorScheme = radioTvColorScheme()) {
                SettingsSnapshotContent()
            }
        }
    }
}

/** Small pure Compose surface for settings screenshot coverage; the interactive settings live in MainActivity. */
@Composable
private fun SettingsSnapshotContent() {
    Column(
        modifier = Modifier.fillMaxSize().background(RadioTvPalette.Black)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("НАСТРОЙКИ УСТРОЙСТВ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleMedium)
        Text("Телефон и телевизор должны быть в одной Wi-Fi сети.", color = RadioTvPalette.Muted)
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                SettingSwitchRow("Вибрация при нажатии", "Короткий тактильный отклик", checked = true)
                SettingSwitchRow("Звук при нажатии", "Выключен по умолчанию", checked = false)
                Text("Тип звука", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingPill("Клик", selected = true, modifier = Modifier.weight(1f))
                    SettingPill("Голос", selected = false, modifier = Modifier.weight(1f))
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
                Text("Проверка протоколов на физических телевизорах и загрузка иконок приложений зависят от доступности API устройств.", color = RadioTvPalette.Muted)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, description: String, checked: Boolean) {
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
private fun SettingPill(title: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RadioTvPalette.Red else RadioTvPalette.Black
    ) {
        Box(contentAlignment = Alignment.Center) { Text(title, color = Color.White) }
    }
}
