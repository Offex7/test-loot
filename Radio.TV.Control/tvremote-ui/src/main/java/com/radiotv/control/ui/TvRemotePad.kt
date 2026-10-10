package com.radiotv.control.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiotv.control.core.RemoteKey

enum class RemoteFeatureAction { VOICE_INPUT, KEYBOARD, AIR_MOUSE, TOUCHPAD, CAST }

object RadioTvPalette {
    val Red = Color(0xFFE53935)
    val DeepRed = Color(0xFFD32F2F)
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val Raised = Color(0xFF141414)
    val Muted = Color(0xFFB0B0B0)
}

fun radioTvColorScheme() = darkColorScheme(
    primary = RadioTvPalette.Red,
    onPrimary = Color.White,
    primaryContainer = RadioTvPalette.DeepRed,
    onPrimaryContainer = Color.White,
    secondary = RadioTvPalette.Red,
    onSecondary = Color.White,
    background = RadioTvPalette.Black,
    onBackground = Color.White,
    surface = RadioTvPalette.Surface,
    onSurface = Color.White,
    onSurfaceVariant = RadioTvPalette.Muted,
    outline = RadioTvPalette.Red,
    outlineVariant = Color(0xFF651B1B),
    error = Color(0xFFFF5252),
    onError = Color.Black
)

/** Compact remote controls, independent from the standalone demo Activity. */
@Composable
fun TvRemotePad(
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureAction: (RemoteFeatureAction) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("⏻", RemoteKey.POWER, enabled, onKey, Modifier.weight(1f))
            RemoteButton("SOURCE", RemoteKey.SOURCE, enabled, onKey, Modifier.weight(1.5f))
            RemoteButton("☰", RemoteKey.MENU, enabled, onKey, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(0.82f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RemoteButton("VOL +", RemoteKey.VOLUME_UP, enabled, onKey, Modifier.fillMaxWidth())
                RemoteButton("MUTE", RemoteKey.MUTE, enabled, onKey, Modifier.fillMaxWidth())
                RemoteButton("VOL −", RemoteKey.VOLUME_DOWN, enabled, onKey, Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RemoteButton("▲", RemoteKey.UP, enabled, onKey, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemoteButton("◀", RemoteKey.LEFT, enabled, onKey, Modifier.weight(1f))
                    val view = LocalView.current
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onKey(RemoteKey.OK)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadioTvPalette.Red,
                            contentColor = Color.White,
                            disabledContainerColor = RadioTvPalette.Raised,
                            disabledContentColor = RadioTvPalette.Muted
                        )
                    ) { Text("OK", fontWeight = FontWeight.Bold) }
                    RemoteButton("▶", RemoteKey.RIGHT, enabled, onKey, Modifier.weight(1f))
                }
                RemoteButton("▼", RemoteKey.DOWN, enabled, onKey, Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(0.82f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RemoteButton("CH +", RemoteKey.CHANNEL_UP, enabled, onKey, Modifier.fillMaxWidth())
                RemoteButton("⌃", RemoteKey.CHANNEL_UP, enabled, onKey, Modifier.fillMaxWidth())
                RemoteButton("CH −", RemoteKey.CHANNEL_DOWN, enabled, onKey, Modifier.fillMaxWidth())
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("BACK", RemoteKey.BACK, enabled, onKey, Modifier.weight(1f))
            RemoteButton("HOME", RemoteKey.HOME, enabled, onKey, Modifier.weight(1f))
        }

        Text("ЦИФРЫ", color = RadioTvPalette.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        (1..9).toList().chunked(3).forEach { digits ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                digits.forEach { number ->
                    RemoteButton(number.toString(), numberKey(number), enabled, onKey, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f).height(48.dp))
            RemoteButton("0", RemoteKey.NUMBER_0, enabled, onKey, Modifier.weight(1f))
            Spacer(Modifier.weight(1f).height(48.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            RemoteButton("⏪", RemoteKey.REWIND, enabled, onKey, Modifier.weight(1f))
            RemoteButton("▶/Ⅱ", RemoteKey.PLAY_PAUSE, enabled, onKey, Modifier.weight(1f))
            RemoteButton("⏹", RemoteKey.STOP, enabled, onKey, Modifier.weight(1f))
            RemoteButton("⏩", RemoteKey.FAST_FORWARD, enabled, onKey, Modifier.weight(1f))
        }

        Text("ИНСТРУМЕНТЫ", color = RadioTvPalette.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureButton("◉", "Микрофон", onClick = { onFeatureAction(RemoteFeatureAction.VOICE_INPUT) }, modifier = Modifier.weight(1f))
            FeatureButton("⌨", "Клавиатура", onClick = { onFeatureAction(RemoteFeatureAction.KEYBOARD) }, modifier = Modifier.weight(1f))
            FeatureButton("✥", "Аэромышь", onClick = { onFeatureAction(RemoteFeatureAction.AIR_MOUSE) }, modifier = Modifier.weight(1f))
            FeatureButton("▧", "Тачпад", onClick = { onFeatureAction(RemoteFeatureAction.TOUCHPAD) }, modifier = Modifier.weight(1f))
            FeatureButton("▣", "Трансляция", onClick = { onFeatureAction(RemoteFeatureAction.CAST) }, modifier = Modifier.weight(1f))
        }
    }
}

private fun numberKey(number: Int): RemoteKey =
    RemoteKey.entries.first { it.name == "NUMBER_" + number }

@Composable
private fun RemoteButton(
    label: String,
    key: RemoteKey,
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    OutlinedButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onKey(key)
        },
        enabled = enabled,
        modifier = modifier.height(48.dp),
        border = BorderStroke(1.dp, if (enabled) RadioTvPalette.Red else Color(0xFF424242)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RadioTvPalette.Black,
            contentColor = RadioTvPalette.Red,
            disabledContainerColor = RadioTvPalette.Raised,
            disabledContentColor = Color(0xFF666666)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 0.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun FeatureButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    OutlinedButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = modifier.height(58.dp),
        border = BorderStroke(1.dp, RadioTvPalette.Red),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RadioTvPalette.Black,
            contentColor = RadioTvPalette.Red
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp, vertical = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, color = RadioTvPalette.Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(label, color = RadioTvPalette.Muted, fontSize = 8.sp, maxLines = 1)
        }
    }
}
