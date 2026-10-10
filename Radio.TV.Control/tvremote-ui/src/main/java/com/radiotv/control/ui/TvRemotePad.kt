package com.radiotv.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radiotv.control.core.RemoteKey

/** Public Compose API for embedding the remote pad as a Radio.TV feature. */
@Composable
fun TvRemotePad(enabled: Boolean, onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("⏻", RemoteKey.POWER, enabled, onKey, Modifier.weight(1f))
            RemoteButton("SOURCE", RemoteKey.SOURCE, enabled, onKey, Modifier.weight(1f))
            RemoteButton("☰", RemoteKey.MENU, enabled, onKey, Modifier.weight(1f))
        }
        RemoteButton("▲", RemoteKey.UP, enabled, onKey, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("◀", RemoteKey.LEFT, enabled, onKey, Modifier.weight(1f))
            FilledTonalButton(
                onClick = { onKey(RemoteKey.OK) }, enabled = enabled,
                modifier = Modifier.weight(1f).height(58.dp)
            ) { Text("OK", style = MaterialTheme.typography.titleMedium) }
            RemoteButton("▶", RemoteKey.RIGHT, enabled, onKey, Modifier.weight(1f))
        }
        RemoteButton("▼", RemoteKey.DOWN, enabled, onKey, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("BACK", RemoteKey.BACK, enabled, onKey, Modifier.weight(1f))
            RemoteButton("HOME", RemoteKey.HOME, enabled, onKey, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("VOL −", RemoteKey.VOLUME_DOWN, enabled, onKey, Modifier.weight(1f))
            RemoteButton("MUTE", RemoteKey.MUTE, enabled, onKey, Modifier.weight(1f))
            RemoteButton("VOL +", RemoteKey.VOLUME_UP, enabled, onKey, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("CH −", RemoteKey.CHANNEL_DOWN, enabled, onKey, Modifier.weight(1f))
            RemoteButton("CH +", RemoteKey.CHANNEL_UP, enabled, onKey, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("⏪", RemoteKey.REWIND, enabled, onKey, Modifier.weight(1f))
            RemoteButton("▶/Ⅱ", RemoteKey.PLAY_PAUSE, enabled, onKey, Modifier.weight(1f))
            RemoteButton("⏹", RemoteKey.STOP, enabled, onKey, Modifier.weight(1f))
            RemoteButton("⏩", RemoteKey.FAST_FORWARD, enabled, onKey, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (0..4).forEach { n ->
                RemoteButton(n.toString(), RemoteKey.entries.first { it.name == "NUMBER_$n" }, enabled, onKey, Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (5..9).forEach { n ->
                RemoteButton(n.toString(), RemoteKey.entries.first { it.name == "NUMBER_$n" }, enabled, onKey, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RemoteButton(label: String, key: RemoteKey, enabled: Boolean, onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = { onKey(key) }, enabled = enabled, modifier = modifier.height(52.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
