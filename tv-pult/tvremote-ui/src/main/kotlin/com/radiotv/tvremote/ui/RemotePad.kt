package com.radiotv.tvremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radiotv.tvremote.core.RemoteKey

@Composable
fun DPad(onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.76f).aspectRatio(1f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(modifier = Modifier.align(Alignment.TopCenter), onClick = { onKey(RemoteKey.UP) }) { Icon(Icons.Filled.ArrowUpward, "Вверх") }
                IconButton(modifier = Modifier.align(Alignment.BottomCenter), onClick = { onKey(RemoteKey.DOWN) }) { Icon(Icons.Filled.ArrowDownward, "Вниз") }
                IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = { onKey(RemoteKey.LEFT) }) { Icon(Icons.Filled.ArrowBack, "Влево") }
                FilledTonalButton(onClick = { onKey(RemoteKey.OK) }, modifier = Modifier.size(82.dp)) { Icon(Icons.Filled.Check, "OK") }
                IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = { onKey(RemoteKey.RIGHT) }) { Icon(Icons.Filled.ArrowForward, "Вправо") }
            }
        }
    }
}

@Composable
fun NavigationRow(onKey: (RemoteKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { onKey(RemoteKey.BACK) }) { Icon(Icons.Filled.KeyboardBackspace, "Назад") }
        IconButton(onClick = { onKey(RemoteKey.HOME) }) { Icon(Icons.Filled.Home, "Домой") }
        IconButton(onClick = { onKey(RemoteKey.MENU) }) { Icon(Icons.Filled.Menu, "Меню") }
    }
}

@Composable
fun PowerRow(onKey: (RemoteKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilledTonalButton(onClick = { onKey(RemoteKey.POWER) }) { Icon(Icons.Filled.PowerSettingsNew, "Питание") }
        FilledTonalButton(onClick = { onKey(RemoteKey.MUTE) }) { Text("MUTE") }
        FilledTonalButton(onClick = { onKey(RemoteKey.INPUT) }) { Text("INPUT") }
    }
}
