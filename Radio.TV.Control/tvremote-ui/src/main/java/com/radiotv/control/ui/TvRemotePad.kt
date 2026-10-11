package com.radiotv.control.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.radiotv.control.core.RemoteKey
import kotlinx.coroutines.delay

enum class RemoteFeatureAction { VOICE_INPUT, KEYBOARD, AIR_MOUSE, TOUCHPAD, CAST, APP_YOUTUBE, APP_NETFLIX }


object RadioTvPalette {
    val Red = Color(0xFFE53935)
    val DeepRed = Color(0xFFD32F2F)
    val Blue = Color(0xFF2196F3)
    val Green = Color(0xFF43A047)
    val Black = Color(0xFF1A1A1A)
    val Surface = Color(0xFF202020)
    val Raised = Color(0xFF333333)
    val Muted = Color(0xFFB0B0B0)
    val Border = Color(0xFF404040)
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
        var activeDirection by remember { mutableStateOf<RemoteKey?>(null) }
        LaunchedEffect(activeDirection) {
            if (activeDirection != null) {
                delay(170)
                activeDirection = null
            }
        }
        val pulseTransition = rememberInfiniteTransition(label = "ok-pulse")
        val okPulse by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
            label = "ok-pulse-scale"
        )
        val height = maxHeight
        val width = maxWidth
        val tight = height < 520.dp
        val compact = height < 650.dp
        val topSize = when { tight -> 38.dp; compact -> 44.dp; else -> 52.dp }
        val dpadHeight = (height * 0.43f).coerceIn(170.dp, 315.dp)
        val controlHeight = (height * 0.22f).coerceIn(100.dp, 155.dp)
        val smallButton = when { tight -> 38.dp; compact -> 46.dp; else -> 54.dp }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (tight) 3.dp else 6.dp, Alignment.CenterVertically)
        ) {
            // Row 1: Power, Menu, YouTube and Netflix — order matches the reference image.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopRemoteButton("Power", "Питание", RemoteKey.POWER, enabled, onKey,
                    Modifier.weight(1f).size(topSize), powerIcon = true)
                TopRemoteButton("MENU", "Меню", RemoteKey.MENU, enabled, onKey,
                    Modifier.weight(1f).size(topSize))
                TopRemoteButton("▶", "YouTube", RemoteKey.MENU, enabled, onKey,
                    Modifier.weight(1f).size(topSize), onClick = { onFeatureAction(RemoteFeatureAction.APP_YOUTUBE) })
                TopRemoteButton("NETFLIX", "Netflix", RemoteKey.MENU, enabled, onKey,
                    Modifier.weight(1f).size(topSize), onClick = { onFeatureAction(RemoteFeatureAction.APP_NETFLIX) })
            }

            // Row 2: vertical volume/channel pills with separate round media keys.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(controlHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VolumePill("VOL", RemoteKey.VOLUME_UP, RemoteKey.VOLUME_DOWN, enabled, onKey,
                    Modifier.weight(0.78f).fillMaxHeight())
                SoftCircleButton("▶", "Воспроизведение", enabled, { onKey(RemoteKey.PLAY_PAUSE) },
                    Modifier.weight(1f).size(smallButton + 8.dp), fontSize = 24)
                SoftCircleButton("Ⅱ", "Пауза", enabled, { onKey(RemoteKey.PLAY_PAUSE) },
                    Modifier.weight(1f).size(smallButton + 8.dp), fontSize = 24)
                VolumePill("CH", RemoteKey.CHANNEL_UP, RemoteKey.CHANNEL_DOWN, enabled, onKey,
                    Modifier.weight(0.78f).fillMaxHeight())
            }

            // Row 3: mute and keyboard centered above the directional pad.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftCircleButton("◖×", "Без звука", enabled, { onKey(RemoteKey.MUTE) },
                    Modifier.size(smallButton), fontSize = 20)
                SoftCircleButton("▦", "Клавиатура", enabled, { onFeatureAction(RemoteFeatureAction.KEYBOARD) },
                    Modifier.size(smallButton), fontSize = 21, active = keyboardActive)
            }

            // The large four-way cross has the reference's multicolour stroke and decorative rings.
            Box(
                modifier = Modifier.fillMaxWidth().height(dpadHeight)
                    .padding(horizontal = 16.dp)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        for (ring in 0..13) {
                            val inset = ring * 2.25.dp.toPx()
                            drawOval(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        RadioTvPalette.Red.copy(alpha = 0.11f),
                                        Color(0xFFF06292).copy(alpha = 0.18f),
                                        Color(0xFF2196F3).copy(alpha = 0.12f)
                                    )
                                ),
                                topLeft = Offset(w * 0.10f + inset, h * 0.12f + inset),
                                size = Size(w * 0.80f - inset * 2, h * 0.76f - inset * 2),
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        }
                        val path = Path().apply {
                            moveTo(w * .38f, h * .08f)
                            cubicTo(w * .38f, h * .02f, w * .42f, h * .01f, w * .46f, h * .01f)
                            lineTo(w * .54f, h * .01f)
                            cubicTo(w * .59f, h * .01f, w * .62f, h * .03f, w * .62f, h * .08f)
                            cubicTo(w * .62f, h * .23f, w * .68f, h * .29f, w * .82f, h * .30f)
                            cubicTo(w * .93f, h * .31f, w * .97f, h * .36f, w * .97f, h * .45f)
                            lineTo(w * .97f, h * .55f)
                            cubicTo(w * .97f, h * .64f, w * .93f, h * .69f, w * .82f, h * .70f)
                            cubicTo(w * .68f, h * .71f, w * .62f, h * .77f, w * .62f, h * .92f)
                            cubicTo(w * .62f, h * .97f, w * .59f, h * .99f, w * .54f, h * .99f)
                            lineTo(w * .46f, h * .99f)
                            cubicTo(w * .42f, h * .99f, w * .38f, h * .97f, w * .38f, h * .92f)
                            cubicTo(w * .38f, h * .77f, w * .32f, h * .71f, w * .18f, h * .70f)
                            cubicTo(w * .07f, h * .69f, w * .03f, h * .64f, w * .03f, h * .55f)
                            lineTo(w * .03f, h * .45f)
                            cubicTo(w * .03f, h * .36f, w * .07f, h * .31f, w * .18f, h * .30f)
                            cubicTo(w * .32f, h * .29f, w * .38f, h * .23f, w * .38f, h * .08f)
                        }
                        drawPath(path, Brush.linearGradient(
                            colors = listOf(RadioTvPalette.Red, Color(0xFFF06292), Color(0xFF2196F3)),
                            start = Offset(0f, 0f), end = Offset(w, h)
                        ), style = Stroke(width = 2.dp.toPx()))
                    }
            ) {
                val arrowSize = when { tight -> 38.dp; compact -> 44.dp; else -> 50.dp }
                SoftCircleButton("▲", "Вверх", enabled, { activeDirection = RemoteKey.UP; onKey(RemoteKey.UP) },
                    Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(arrowSize), fontSize = 25, active = activeDirection == RemoteKey.UP)
                SoftCircleButton("◀", "Влево", enabled, { activeDirection = RemoteKey.LEFT; onKey(RemoteKey.LEFT) },
                    Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(arrowSize), fontSize = 25, active = activeDirection == RemoteKey.LEFT)
                SoftCircleButton("▶", "Вправо", enabled, { activeDirection = RemoteKey.RIGHT; onKey(RemoteKey.RIGHT) },
                    Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(arrowSize), fontSize = 25, active = activeDirection == RemoteKey.RIGHT)
                SoftCircleButton("▼", "Вниз", enabled, { activeDirection = RemoteKey.DOWN; onKey(RemoteKey.DOWN) },
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(arrowSize), fontSize = 25, active = activeDirection == RemoteKey.DOWN)
                SoftCircleButton("OK", "ОК", enabled, { onKey(RemoteKey.OK) },
                    Modifier.align(Alignment.Center).size(if (tight) 78.dp else if (compact) 92.dp else 104.dp),
                    fontSize = if (tight) 23 else 26, active = true, pulseScale = okPulse)
            }

            // Row 4: Back, Exit, touchpad and Home.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftCircleButton("↶", "Назад", enabled, { onKey(RemoteKey.BACK) },
                    Modifier.weight(1f).size(smallButton), fontSize = 27)
                SoftCircleButton("EXIT", "Выход", enabled, { onKey(RemoteKey.BACK) },
                    Modifier.weight(1f).size(smallButton), fontSize = 12)
                SoftCircleButton("TP", "Тачпад", enabled, { onFeatureAction(RemoteFeatureAction.TOUCHPAD) },
                    Modifier.weight(1f).size(smallButton), fontSize = 22, active = touchpadActive)
                SoftCircleButton("⌂", "Домой", enabled, { onKey(RemoteKey.HOME) },
                    Modifier.weight(1f).size(smallButton), fontSize = 28)
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
    modifier: Modifier = Modifier,
    powerIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(150), label = "remote-button-scale")
    Box(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.78f else 1f }
            .clip(CircleShape)
            .background(if (powerIcon) RadioTvPalette.Red else RadioTvPalette.Raised)
            .border(BorderStroke(1.dp, RadioTvPalette.Border), CircleShape)
            .semantics { contentDescription = description }
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, onClickLabel = description) {
                
                if (onClick != null) onClick() else onKey(key)
            },
        contentAlignment = Alignment.Center
    ) {
        if (powerIcon) {
            Canvas(Modifier.size(30.dp)) {
                val stroke = 2.8.dp.toPx()
                val radius = size.minDimension * 0.30f
                drawArc(
                    color = RadioTvPalette.White,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(size.width / 2 - radius, size.height / 2 - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawLine(
                    color = RadioTvPalette.White,
                    start = Offset(size.width / 2, size.height * 0.10f),
                    end = Offset(size.width / 2, size.height * 0.52f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        } else {
            Text(label, color = RadioTvPalette.White, fontSize = if (label == "NETFLIX") 9.sp else if (label == "MENU") 12.sp else 27.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DirectionPadButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 25
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(150), label = "direction-scale")
    val shape = CircleShape
    Box(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.78f else 1f }
            .clip(shape)
            .background(RadioTvPalette.Raised)
            .border(BorderStroke(1.dp, RadioTvPalette.Border), shape)
            .semantics { contentDescription = description }
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, onClickLabel = description) {
                
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = RadioTvPalette.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
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
    active: Boolean = false,
    pulseScale: Float = 1f
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(150), label = "circle-scale")
    val base = when { label == "OK" -> Brush.sweepGradient(listOf(Color(0xFFFFD54F), RadioTvPalette.Red, Color(0xFFCE45AE), Color(0xFF6A3FC7), Color(0xFFFFD54F))); active -> Brush.linearGradient(listOf(RadioTvPalette.Red, RadioTvPalette.DeepRed)); else -> Brush.linearGradient(listOf(Color(0xFF333538), Color(0xFF333538))) }
    Box(
        modifier = modifier.graphicsLayer { scaleX = scale * pulseScale; scaleY = scale * pulseScale; alpha = if (pressed) 0.78f else 1f }
            .clip(CircleShape)
            .background(base)
            .semantics { contentDescription = description }
            .clickable(interactionSource = interactions, indication = null, enabled = enabled, onClickLabel = description) {
                
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        when (description) {
            "Назад" -> Icon(Icons.Filled.ArrowBack, contentDescription = description, tint = if (active) RadioTvPalette.White else RadioTvPalette.Muted, modifier = Modifier.size((fontSize + 5).dp))
            "Тачпад" -> Icon(Icons.Filled.TouchApp, contentDescription = description, tint = if (active) RadioTvPalette.White else RadioTvPalette.Muted, modifier = Modifier.size((fontSize + 6).dp))
            "Домой" -> Icon(Icons.Filled.Home, contentDescription = description, tint = if (active) RadioTvPalette.White else RadioTvPalette.Muted, modifier = Modifier.size((fontSize + 5).dp))
            else -> Text(label, color = if (active) RadioTvPalette.White else RadioTvPalette.Muted, fontSize = fontSize.sp,
                fontWeight = if (active || label.length <= 2) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
        }
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
    Column(
        modifier = modifier.clip(RoundedCornerShape(42.dp)).background(RadioTvPalette.Raised).padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("+", modifier = Modifier.clickable(enabled = enabled) {
            onKey(upKey)
        }.padding(horizontal = 10.dp, vertical = 2.dp), color = RadioTvPalette.White, fontSize = 26.sp)
        Text(title, color = RadioTvPalette.White, fontSize = if (title == "VOL") 21.sp else 23.sp, fontWeight = FontWeight.Medium)
        Text("−", modifier = Modifier.clickable(enabled = enabled) {
            onKey(downKey)
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
    OutlinedButton(
        onClick = {
            
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
                    onTap = { if (enabled) { onTap() } },
                    onLongPress = { if (enabled) { onLongPress() } }
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
