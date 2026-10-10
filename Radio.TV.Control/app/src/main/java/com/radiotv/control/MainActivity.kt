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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiotv.control.core.AndroidTvRemoteV2Transport
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
    var host by rememberSaveable { mutableStateOf("") }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Телефон и телевизор должны быть в одной сети Wi‑Fi.") }
    val codeRequested = status is RemoteStatus.AwaitingCode
    val connected = status is RemoteStatus.Connected

    val startPairing: () -> Unit = {
        scope.launch {
            statusMessage = "Подключаемся к сервису сопряжения…"
            runCatching { remote.beginPairing(host) }
                .onSuccess { statusMessage = "Код появится на телевизоре. Введите его ниже." }
                .onFailure { statusMessage = it.message ?: "Не удалось начать сопряжение." }
        }
    }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startPairing()
        else statusMessage = "Для связи с телевизором разрешите доступ к локальной сети в настройках приложения."
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 520.dp).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Radio.TV.Control", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("Универсальный Wi‑Fi пульт", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Android TV / Google TV", fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(
                                        value = host,
                                        onValueChange = { host = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("IP-адрес телевизора") },
                                        placeholder = { Text("192.168.1.100") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                    )
                                    Button(
                                        onClick = {
                                            val needsLocalPermission = Build.VERSION.SDK_INT >= 37 &&
                                                context.checkSelfPermission(ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
                                            if (needsLocalPermission) localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK)
                                            else startPairing()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("НАЧАТЬ СОПРЯЖЕНИЕ") }
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
                            Text("ПУЛЬТ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            TvRemotePad(
                                enabled = connected,
                                onKey = { key ->
                                    scope.launch {
                                        runCatching { remote.sendKey(key) }
                                            .onFailure { statusMessage = it.message ?: "Команда не отправлена." }
                                    }
                                }
                            )
                        }
                        item {
                            Text(
                                "Сопряжение использует локальную сеть и TLS. Совместимость зависит от версии Android TV Remote Service на телевизоре.",
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
