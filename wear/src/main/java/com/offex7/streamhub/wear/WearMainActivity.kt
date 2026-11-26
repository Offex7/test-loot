package com.offex7.streamhub.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val PHONE_STATE_REQUEST_PATH = "/radio/state/request"
private const val PHONE_COMMAND_PATH = "/radio/command"

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearRadioRemoteScreen() }
    }
}

@Composable
private fun WearRadioRemoteScreen() {
    val context = LocalContext.current
    val state by WearRadioStateHolder.state.collectAsState()
    val scope = rememberCoroutineScope()
    var nodeIds by remember { mutableStateOf(emptyList<String>()) }
    var showFavorites by rememberSaveable { mutableStateOf(false) }

    suspend fun refreshPhoneNodes(): List<String> {
        val connected = runCatching {
            Wearable.getNodeClient(context).connectedNodes.await()
        }.getOrDefault(emptyList())
        nodeIds = connected.map { it.id }
        connected.forEach { node ->
            runCatching {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, PHONE_STATE_REQUEST_PATH, byteArrayOf())
                    .await()
            }
        }
        return nodeIds
    }

    fun sendCommand(command: String) {
        scope.launch {
            val connectedIds = if (nodeIds.isEmpty()) refreshPhoneNodes() else nodeIds
            connectedIds.forEach { id ->
                runCatching {
                    Wearable.getMessageClient(context)
                        .sendMessage(id, PHONE_COMMAND_PATH, command.toByteArray(Charsets.UTF_8))
                        .await()
                }
            }
            delay(350L)
            refreshPhoneNodes()
        }
    }

    LaunchedEffect(Unit) {
        refreshPhoneNodes()
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showFavorites) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text("ИЗБРАННОЕ", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (state.favorites.isEmpty()) {
                        item {
                            Text(
                                if (nodeIds.isEmpty()) "Подключите телефон" else "Нет избранных станций",
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(state.favorites.size) { index ->
                            val favorite = state.favorites[index]
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { sendCommand("play:" + favorite.index) },
                                label = {
                                    Text(favorite.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showFavorites = false },
                            label = { Text("НАЗАД") }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("СЕЙЧАС ИГРАЕТ", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.stationName.ifBlank {
                            if (nodeIds.isEmpty()) "Подключите телефон" else "Ожидание плеера…"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(modifier = Modifier.size(42.dp), onClick = { sendCommand("previous") }) { Text("◀") }
                        Button(modifier = Modifier.size(48.dp), onClick = { sendCommand("toggle") }) {
                            Text(if (state.playing) "Ⅱ" else "▶")
                        }
                        Button(modifier = Modifier.size(42.dp), onClick = { sendCommand("next") }) { Text("▶") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (nodeIds.isEmpty()) "НЕТ СВЯЗИ С ТЕЛЕФОНОМ" else "ТЕЛЕФОН ПОДКЛЮЧЁН",
                        color = if (nodeIds.isEmpty()) Color.Gray else Color(0xFF66BB6A),
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showFavorites = true },
                        label = { Text("ИЗБРАННОЕ") }
                    )
                }
            }
        }
    }
}
