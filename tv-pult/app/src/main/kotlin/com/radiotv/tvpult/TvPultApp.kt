package com.radiotv.tvpult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radiotv.tvremote.core.RemoteConnectionState
import com.radiotv.tvremote.core.RemoteKey
import com.radiotv.tvremote.ui.DPad
import com.radiotv.tvremote.ui.NavigationRow
import com.radiotv.tvremote.ui.PowerRow

private val Bg = Color(0xFF080808)
private val Panel = Color(0xFF19191B)
private val Accent = Color(0xFFE53935)

@Composable
fun TvPultApp(vm: TvPultViewModel = viewModel()) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().widthIn(max = 430.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            color = Bg
        ) {
            when (val current = screen) {
                TvScreen.Connect -> ConnectPanel(vm)
                is TvScreen.Pairing -> PairingPanel(vm, current)
                is TvScreen.Remote -> RemotePanel(vm, current)
            }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            title = { Text("TV пульт") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("ОК") } }
        )
    }
}

@Composable
private fun ConnectPanel(vm: TvPultViewModel) {
    val found by vm.found.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var ip by rememberSaveable { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.discover() }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("TV пульт", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("универсальный Wi‑Fi пульт", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionTitle("Android TV / Google TV") }

        if (found.isEmpty()) {
            item {
                Text(
                    "Поиск телевизоров в локальной сети… Если телевизор не найден, введите IP вручную.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(found.size) { index ->
                val tv = found[index]
                FilledTonalButton(
                    onClick = { vm.beginPairing(tv.host, tv.name) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Tv, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tv.name + " • " + tv.host)
                }
            }
        }

        item {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP телевизора") },
                placeholder = { Text("192.168.1.42") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = { vm.beginPairing(ip, ip) },
                enabled = ip.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (busy) "Подключение…" else "Сопрячь телевизор") }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Уже работает", fontWeight = FontWeight.SemiBold)
                    Text("Remote v2 • pairing • D‑Pad • громкость • питание • каналы • медиакнопки • IME")
                    Text("Пульт работает локально: без облака и ADB.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Следующие модули", fontWeight = FontWeight.SemiBold)
                    Text("Samsung • LG webOS • Roku • Bluetooth HID/Air Mouse • DLNA/UPnP",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PairingPanel(vm: TvPultViewModel, screen: TvScreen.Pairing) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Tv, null, tint = Accent, modifier = Modifier.height(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Сопряжение", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("На телевизоре появится 6-символьный код.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it.filter { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }
                    .uppercase().take(6)
            },
            label = { Text("Код") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = vm::cancelPairing, enabled = !busy) { Text("Отмена") }
            Button(onClick = { vm.submitPairingCode(code) }, enabled = code.length == 6 && !busy) {
                Text("Сопрячь")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(screen.name, fontWeight = FontWeight.SemiBold)
        Text(screen.host, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RemotePanel(vm: TvPultViewModel, screen: TvScreen.Remote) {
    val state by vm.connection.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    var keyboard by remember { mutableStateOf(false) }
    val ready = state is RemoteConnectionState.Ready

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = vm::backToConnect) {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("ТВ")
                }
                Column(Modifier.weight(1f)) {
                    Text(screen.name, fontWeight = FontWeight.Bold)
                    Text(
                        when (state) {
                            RemoteConnectionState.Ready -> "Подключено"
                            RemoteConnectionState.Connecting -> "Подключение…"
                            RemoteConnectionState.Disconnected -> "Отключено"
                            is RemoteConnectionState.Failed -> "Ошибка соединения"
                        },
                        color = if (ready) Color(0xFF53C878) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(volume?.level?.toString() ?: "—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { PowerRow(vm::send) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ vm.send(RemoteKey.VOLUME_DOWN) }, Modifier.weight(1f)) { Text("− Громкость") }
                FilledTonalButton({ vm.send(RemoteKey.VOLUME_UP) }, Modifier.weight(1f)) { Text("Громкость +") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ vm.send(RemoteKey.CHANNEL_DOWN) }, Modifier.weight(1f)) { Text("− Канал") }
                FilledTonalButton({ vm.send(RemoteKey.CHANNEL_UP) }, Modifier.weight(1f)) { Text("Канал +") }
            }
        }
        item { DPad(vm::send, Modifier.fillMaxWidth()) }
        item { NavigationRow(vm::send) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                keyButton("⏪", RemoteKey.REWIND, vm, Modifier.weight(1f))
                keyButton("▶", RemoteKey.PLAY, vm, Modifier.weight(1f))
                keyButton("⏸", RemoteKey.PAUSE, vm, Modifier.weight(1f))
                keyButton("⏹", RemoteKey.STOP, vm, Modifier.weight(1f))
                keyButton("⏩", RemoteKey.FAST_FORWARD, vm, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { keyboard = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Keyboard, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Клавиатура")
                }
                FilledTonalButton(onClick = { vm.send(RemoteKey.SEARCH) }, modifier = Modifier.weight(1f)) {
                    Text("ПОИСК")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.showInfo("Голосовой поток Android TV Remote v2 подготовлен в протокольном слое; запись с микрофона включаем следующим этапом.") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Mic, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Голос")
                }
                OutlinedButton(onClick = { vm.showInfo("Air Mouse будет отдельным Bluetooth HID модулем и не влияет на базовый пульт.") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Air, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Air Mouse")
                }
                OutlinedButton(onClick = { vm.showInfo("DLNA/UPnP вынесено в tvremote-cast. Реализация добавится после выбора совместимого backend.") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Cast, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Трансляция")
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (keyboard) {
        var text by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { keyboard = false },
            title = { Text("Ввод на ТВ") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { vm.sendText(text); keyboard = false }, enabled = text.isNotEmpty()) { Text("Отправить") }
            },
            dismissButton = { TextButton(onClick = { keyboard = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun keyButton(label: String, key: RemoteKey, vm: TvPultViewModel, modifier: Modifier) {
    FilledTonalButton(onClick = { vm.send(key) }, modifier = modifier) { Text(label) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

