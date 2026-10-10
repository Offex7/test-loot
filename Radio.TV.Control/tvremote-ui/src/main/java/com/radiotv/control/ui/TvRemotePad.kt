package com.radiotv.control.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.radiotv.control.core.RemoteKey

enum class RemoteFeatureAction { VOICE_INPUT, KEYBOARD, AIR_MOUSE, TOUCHPAD, CAST }

object RadioTvPalette {
    val Red = Color(0xFFE53935)
    val DeepRed = Color(0xFFD32F2F)
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val Raised = Color(0xFF252525)
    val Muted = Color(0xFFB0B0B0)
    val Border = Color(0xFF383838)
    val DarkRed = Color(0xFF651B1B)
    val ErrorRed = Color(0xFFFF5252)
    val White = Color(0xFFFFFFFF)
}

fun radioTvColorScheme() = darkColorScheme(
    primary = RadioTvPalette.Red,
    onPrimary = RadioTvPalette.White,
    primaryContainer = RadioTvPalette.DeepRed,
    onPrimaryContainer = RadioTvPalette.White,
    inversePrimary = RadioTvPalette.DeepRed,
    secondary = RadioTvPalette.Red,
    onSecondary = RadioTvPalette.White,
    secondaryContainer = RadioTvPalette.DeepRed,
    onSecondaryContainer = RadioTvPalette.White,
    tertiary = RadioTvPalette.Red,
    onTertiary = RadioTvPalette.White,
    tertiaryContainer = RadioTvPalette.DeepRed,
    onTertiaryContainer = RadioTvPalette.White,
    background = RadioTvPalette.Black,
    onBackground = RadioTvPalette.White,
    surface = RadioTvPalette.Surface,
    onSurface = RadioTvPalette.White,
    surfaceVariant = RadioTvPalette.Raised,
    onSurfaceVariant = RadioTvPalette.Muted,
    surfaceTint = RadioTvPalette.Red,
    inverseSurface = RadioTvPalette.Raised,
    inverseOnSurface = RadioTvPalette.White,
    outline = RadioTvPalette.Border,
    outlineVariant = RadioTvPalette.DarkRed,
    scrim = RadioTvPalette.Black,
    error = RadioTvPalette.ErrorRed,
    onError = RadioTvPalette.Black,
    errorContainer = RadioTvPalette.DeepRed,
    onErrorContainer = RadioTvPalette.White,
    surfaceDim = RadioTvPalette.Black,
    surfaceBright = RadioTvPalette.Raised,
    surfaceContainerLowest = RadioTvPalette.Black,
    surfaceContainerLow = RadioTvPalette.Surface,
    surfaceContainer = RadioTvPalette.Raised,
    surfaceContainerHigh = RadioTvPalette.Raised,
    surfaceContainerHighest = RadioTvPalette.Raised
)

/**
 * Vertical red/black remote layout. The host app owns connection state and dispatches key events.
 * The content intentionally caps its width; landscape large screens therefore keep a centered portrait column.
 */
@Composable
fun TvRemotePad(
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureAction: (RemoteFeatureAction) -> Unit = {},
    touchpadActive: Boolean = false,
    airMouseActive: Boolean = false,
    voiceActive: Boolean = false,
    keyboardActive: Boolean = false,
    featureMessage: String? = null,
    showFeatureActions: Boolean = true
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxHeight < 620.dp
        val topHeight = if (compact) 46.dp else 54.dp
        val padHeight = if (compact) 298.dp else 350.dp
        val sideHeight = if (compact) 170.dp else 218.dp
        val circleSize = if (compact) 64.dp else 74.dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopRemoteButton("⏻", "Питание", RemoteKey.POWER, enabled, onKey, Modifier.width(112.dp).height(topHeight))
                TopRemoteButton("⌂", "Домой", RemoteKey.HOME, enabled, onKey, Modifier.width(112.dp).height(topHeight))
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(padHeight).drawBehind {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.34f, h * 0.13f)
                        cubicTo(w * 0.34f, h * 0.04f, w * 0.39f, h * 0.02f, w * 0.45f, h * 0.02f)
                        lineTo(w * 0.55f, h * 0.02f)
                        cubicTo(w * 0.61f, h * 0.02f, w * 0.66f, h * 0.04f, w * 0.66f, h * 0.13f)
                        cubicTo(w * 0.66f, h * 0.27f, w * 0.72f, h * 0.31f, w * 0.84f, h * 0.34f)
                        cubicTo(w * 0.94f, h * 0.36f, w * 0.97f, h * 0.41f, w * 0.97f, h * 0.49f)
                        lineTo(w * 0.97f, h * 0.59f)
                        cubicTo(w * 0.97f, h * 0.67f, w * 0.93f, h * 0.71f, w * 0.84f, h * 0.72f)
                        cubicTo(w * 0.70f, h * 0.73f, w * 0.66f, h * 0.78f, w * 0.66f, h * 0.87f)
                        cubicTo(w * 0.66f, h * 0.95f, w * 0.61f, h * 0.98f, w * 0.55f, h * 0.98f)
                        lineTo(w * 0.45f, h * 0.98f)
                        cubicTo(w * 0.39f, h * 0.98f, w * 0.34f, h * 0.95f, w * 0.34f, h * 0.87f)
                        cubicTo(w * 0.34f, h * 0.78f, w * 0.30f, h * 0.73f, w * 0.16f, h * 0.72f)
                        cubicTo(w * 0.07f, h * 0.71f, w * 0.03f, h * 0.67f, w * 0.03f, h * 0.59f)
                        lineTo(w * 0.03f, h * 0.49f)
                        cubicTo(w * 0.03f, h * 0.41f, w * 0.06f, h * 0.36f, w * 0.16f, h * 0.34f)
                        cubicTo(w * 0.28f, h * 0.31f, w * 0.34f, h * 0.27f, w * 0.34f, h * 0.13f)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(RadioTvPalette.Red, RadioTvPalette.DeepRed, RadioTvPalette.Red),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            ) {
                val sideSize = if (compact) 68.dp else 76.dp
                SoftCircleButton("TOOLS", "Инструменты", enabled, { onKey(RemoteKey.MENU) },
                    Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 2.dp).size(sideSize), fontSize = 13)
                SoftCircleButton("INFO", "Информация", enabled, { onKey(RemoteKey.MENU) },
                    Modifier.align(Alignment.TopEnd).padding(end = 3.dp, top = 2.dp).size(sideSize), fontSize = 13)
                SoftCircleButton("▲", "Вверх", enabled, { onKey(RemoteKey.UP) },
                    Modifier.align(Alignment.TopCenter).padding(top = if (compact) 34.dp else 38.dp).size(circleSize), fontSize = 27)
                SoftCircleButton("◀", "Влево", enabled, { onKey(RemoteKey.LEFT) },
                    Modifier.align(Alignment.CenterStart).padding(start = 30.dp).size(circleSize), fontSize = 28)
                SoftCircleButton("OK", "ОК", enabled, { onKey(RemoteKey.OK) },
                    Modifier.align(Alignment.Center).size(if (compact) 82.dp else 90.dp), fontSize = 28, active = true)
                SoftCircleButton("▶", "Вправо", enabled, { onKey(RemoteKey.RIGHT) },
                    Modifier.align(Alignment.CenterEnd).padding(end = 30.dp).size(circleSize), fontSize = 28)
                SoftCircleButton("↶", "Назад", enabled, { onKey(RemoteKey.BACK) },
                    Modifier.align(Alignment.BottomStart).padding(start = 3.dp, bottom = 2.dp).size(sideSize), fontSize = 31)
                SoftCircleButton("EXIT", "Выход", enabled, { onKey(RemoteKey.BACK) },
                    Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 2.dp).size(sideSize), fontSize = 13)
                SoftCircleButton("▼", "Вниз", enabled, { onKey(RemoteKey.DOWN) },
                    Modifier.align(Alignment.BottomCenter).padding(bottom = if (compact) 34.dp else 38.dp).size(circleSize), fontSize = 27)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VolumePill("VOL", RemoteKey.VOLUME_UP, RemoteKey.VOLUME_DOWN, enabled, onKey,
                    Modifier.weight(0.88f).height(sideHeight))
                Column(
                    modifier = Modifier.weight(1.05f).height(sideHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SoftCircleButton("MENU", "Меню", enabled, { onKey(RemoteKey.MENU) }, Modifier.size(if (compact) 62.dp else 72.dp), fontSize = 14)
                    SoftCircleButton("◖×", "Без звука", enabled, { onKey(RemoteKey.MUTE) }, Modifier.size(if (compact) 62.dp else 72.dp), fontSize = 23)
                    SoftCircleButton("▣→", "Источник", enabled, { onKey(RemoteKey.SOURCE) }, Modifier.size(if (compact) 62.dp else 72.dp), fontSize = 22)
                }
                VolumePill("CH", RemoteKey.CHANNEL_UP, RemoteKey.CHANNEL_DOWN, enabled, onKey,
                    Modifier.weight(0.88f).height(sideHeight))
            }
            if (showFeatureActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    FeatureButton("◉", "Микрофон", onClick = { onFeatureAction(RemoteFeatureAction.VOICE_INPUT) }, modifier = Modifier.weight(1f), active = voiceActive)
                    FeatureButton("⌨", "Клавиатура", onClick = { onFeatureAction(RemoteFeatureAction.KEYBOARD) }, modifier = Modifier.weight(1f), active = keyboardActive)
                    FeatureButton("✥", "Аэромышь", onClick = { onFeatureAction(RemoteFeatureAction.AIR_MOUSE) }, modifier = Modifier.weight(1f), active = airMouseActive)
                    FeatureButton("▧", "Тачпад", onClick = { onFeatureAction(RemoteFeatureAction.TOUCHPAD) }, modifier = Modifier.weight(1f), active = touchpadActive)
                    FeatureButton("▣", "Трансляция", onClick = { onFeatureAction(RemoteFeatureAction.CAST) }, modifier = Modifier.weight(1f))
                }
            }
            if (!featureMessage.isNullOrBlank()) {
                Text(featureMessage, color = if (voiceActive || airMouseActive) RadioTvPalette.Red else RadioTvPalette.Muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TopRemoteButton(
    label: String,
    description: String,
    key: RemoteKey,
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(RadioTvPalette.Black)
            .border(BorderStroke(1.dp, RadioTvPalette.Border), RoundedCornerShape(22.dp))
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClickLabel = description) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(key)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = RadioTvPalette.White, fontSize = 30.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SoftCircleButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    active: Boolean = false
) {
    val view = LocalView.current
    val base = if (active) Brush.linearGradient(listOf(RadioTvPalette.Red, RadioTvPalette.DeepRed)) else Brush.linearGradient(listOf(RadioTvPalette.Raised, RadioTvPalette.Raised))
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(base)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClickLabel = description) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) RadioTvPalette.White else RadioTvPalette.Muted, fontSize = fontSize.sp,
            fontWeight = if (active || label.length <= 2) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun VolumePill(
    title: String,
    upKey: RemoteKey,
    downKey: RemoteKey,
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(
        modifier = modifier.clip(RoundedCornerShape(42.dp)).background(RadioTvPalette.Raised).padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("+", modifier = Modifier.clickable(enabled = enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onKey(upKey)
        }.padding(horizontal = 10.dp, vertical = 2.dp), color = RadioTvPalette.White, fontSize = 26.sp)
        Text(title, color = RadioTvPalette.White, fontSize = if (title == "VOL") 21.sp else 23.sp, fontWeight = FontWeight.Medium)
        Text("−", modifier = Modifier.clickable(enabled = enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onKey(downKey)
        }.padding(horizontal = 10.dp, vertical = 2.dp), color = RadioTvPalette.White, fontSize = 26.sp)
    }
}

@Composable
private fun FeatureButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
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
            containerColor = if (active) RadioTvPalette.Red else RadioTvPalette.Black,
            contentColor = if (active) RadioTvPalette.Black else RadioTvPalette.Red
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp, vertical = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, color = if (active) RadioTvPalette.Black else RadioTvPalette.Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(label, color = if (active) RadioTvPalette.Black else RadioTvPalette.Muted, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
fun TouchpadSurface(
    enabled: Boolean,
    onMove: (Int, Int) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val view = LocalView.current
    Box(
        modifier = modifier.fillMaxWidth().height(176.dp).clip(shape)
            .background(RadioTvPalette.Black)
            .border(1.dp, if (enabled) RadioTvPalette.Red else RadioTvPalette.Border, shape)
            .pointerInput(enabled) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (enabled) {
                        val dx = dragAmount.x.roundToInt()
                        val dy = dragAmount.y.roundToInt()
                        if (dx != 0 || dy != 0) onMove(dx, dy)
                    }
                }
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onTap = { if (enabled) { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onTap() } },
                    onLongPress = { if (enabled) { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); onLongPress() } }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ТАЧПАД", color = if (enabled) RadioTvPalette.Red else RadioTvPalette.Muted, fontWeight = FontWeight.Bold)
            Text(
                if (enabled) "Свайп — курсор · тап — клик · долгий тап — контекстное меню"
                else "Сначала подключите Bluetooth HID на телевизоре",
                color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
    }
}
