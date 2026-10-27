package com.radiotv.control

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.TvRemotePad
import com.radiotv.control.ui.radioTvColorScheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ManualScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun remote() = capture("remote.png") {
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

    @Test
    fun control() = capture("digital-panel.png") {
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

    @Test
    fun settings() = capture("settings.png") {
        MaterialTheme(colorScheme = radioTvColorScheme()) {
            SettingsPreviewContent()
        }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { content() }
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val base = context.filesDir
        val output = File(base, "screenshots/$fileName")
        output.parentFile?.mkdirs()
        try {
            FileOutputStream(output).use { stream ->
                assertTrue("PNG не записан: $fileName", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            assertTrue("Пустой PNG: $fileName", output.isFile && output.length() > 0L)
        } finally {
            bitmap.recycle()
        }
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
        Text("Настройки", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("СПОСОБ ПОДКЛЮЧЕНИЯ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    SettingsChoice("Авто", selected = true, modifier = Modifier.weight(1f))
                    SettingsChoice("Ручное", selected = false, modifier = Modifier.weight(1f))
                    SettingsChoice("Bluetooth", selected = false, modifier = Modifier.weight(1f))
                }
                Text("Подключение к Radio.TV приложению — если оно установлено на ТВ.", color = RadioTvPalette.Muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ПОИСК УСТРОЙСТВ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                Text("Автопоиск Companion и телевизоров в локальной сети.", color = Color.White)
                Text("IP-адрес телевизора: 192.168.1.100", color = RadioTvPalette.Muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BLUETOOTH HID", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                Text("Выберите Radio.TV.Control в списке Bluetooth-устройств телевизора.", color = RadioTvPalette.Muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                SettingsSwitchRow("Вибрация", "Короткий тактильный отклик", true)
                SettingsSwitchRow("Звук", "Выключен по умолчанию", false)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("УВЕДОМЛЕНИЯ", color = RadioTvPalette.Red, style = MaterialTheme.typography.titleSmall)
                SettingsSwitchRow("Показывать уведомления", "Скрываются через 3 секунды", true)
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
