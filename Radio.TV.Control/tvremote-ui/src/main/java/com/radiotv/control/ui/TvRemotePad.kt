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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
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
import androidx.compose.ui.graphics.drawscope.rotate
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
    val Black = Color(0xFF000000)
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
@Suppress("UNUSED_PARAMETER")
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
    showFeatureActions: Boolean = false,
    deviceName: String = "Телевизор не подключён",
    onSettings: () -> Unit = {},
    onBack: () -> Unit = { onKey(RemoteKey.BACK) }
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color(0xFF000000))
    ) {
        val tight = maxHeight < 520.dp
        val compact = maxHeight < 650.dp
        val headerHeight = if (tight) 42.dp else 56.dp
        val headerButtonSize = if (tight) 34.dp else 44.dp
        val powerSize = if (tight) 40.dp else 52.dp
        // The reference's VOL/CH pills span the full upper control block; media and mute/menu sit inside it.
        val topClusterHeight = if (tight) {
            (maxHeight * 0.30f).coerceAtLeast(72.dp)
        } else {
            (maxHeight * 0.39f).coerceIn(if (compact) 190.dp else 225.dp, if (compact) 250.dp else 300.dp)
        }
        val volumePillWidth = if (tight) (maxWidth * 0.16f).coerceIn(46.dp, 60.dp) else (maxWidth * 0.205f).coerceIn(60.dp, 78.dp)
        val mediaButtonSize = if (tight) (maxWidth * 0.17f).coerceIn(48.dp, 64.dp) else (maxWidth * 0.215f).coerceIn(64.dp, 82.dp)
        val secondRowButtonSize = if (tight) 38.dp else if (compact) 54.dp else 64.dp
        val dpadHeight = if (tight) {
            (maxHeight * 0.22f).coerceAtLeast(62.dp)
        } else {
            (maxHeight * 0.34f).coerceIn(if (compact) 132.dp else 155.dp, if (compact) 220.dp else 268.dp)
        }
        val footerButtonSize = if (tight) 36.dp else if (compact) 48.dp else 54.dp
        val arrowSize = if (tight) 29.dp else if (compact) 42.dp else 50.dp
        val okSize = if (tight) 64.dp else if (compact) 84.dp else 98.dp
        val lowerButtonSize = if (tight) 38.dp else if (compact) 50.dp else 56.dp
        val sideInset = if (tight) 6.dp else 13.dp
        val padButtonColor = Color(0xFFF8F8F5)
        val padIconColor = Color(0xFF252A29)
        val headerColor = Color(0xFF1A1A1A)
        val headerButtonColor = Color(0xFF2A2A2A)
        val headerIconColor = Color(0xFFE8E8E8)

        val transition = rememberInfiniteTransition(label = "remote-animations")
        val okPulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ok-pulse"
        )
        val rainbowRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rainbow-gradient-rotation"
        )
        val rainbowColors = listOf(
            Color(0xFFFF3B30),
            Color(0xFFFF9500),
            Color(0xFFFFD60A),
            Color(0xFF34C759),
            Color(0xFF0A84FF),
            Color(0xFFBF5AF2),
            Color(0xFFFF3B30)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Device bar: Back, Settings, the live connection name, then Power.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(headerColor)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                RemoteSurfaceButton(
                    description = "Назад",
                    modifier = Modifier.size(headerButtonSize),
                    shape = CircleShape,
                    background = headerButtonColor,
                    onClick = onBack
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = headerIconColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
                RemoteSurfaceButton(
                    description = "Настройки",
                    modifier = Modifier.size(headerButtonSize),
                    shape = CircleShape,
                    background = headerButtonColor,
                    onClick = onSettings
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = headerIconColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    text = deviceName.ifBlank { "Нет подключения" },
                    modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                    color = Color.White,
                    fontSize = if (tight) 16.sp else 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RemoteSurfaceButton(
                    description = "Питание",
                    modifier = Modifier.size(powerSize),
                    shape = CircleShape,
                    background = RadioTvPalette.Red,
                    onClick = { onKey(RemoteKey.POWER) }
                ) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (tight) 25.dp else 30.dp)
                    )
                }
            }

            // Tall VOL/CH pills frame Play/Pause above Mute/MENU, matching the reference.
            Row(
                modifier = Modifier.fillMaxWidth().height(topClusterHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WhiteVolumePill(
                    title = "VOL",
                    topLabel = "+",
                    bottomLabel = "−",
                    topDescription = "Громкость плюс",
                    bottomDescription = "Громкость минус",
                    modifier = Modifier.width(volumePillWidth).fillMaxHeight(),
                    onTop = { onKey(RemoteKey.VOLUME_UP) },
                    onBottom = { onKey(RemoteKey.VOLUME_DOWN) }
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RemoteSurfaceButton(
                            description = "Воспроизведение",
                            modifier = Modifier.size(mediaButtonSize),
                            shape = RoundedCornerShape(18.dp),
                            background = padButtonColor,
                            onClick = { onKey(RemoteKey.PLAY) }
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = padIconColor,
                                modifier = Modifier.size(if (tight) 30.dp else 36.dp)
                            )
                        }
                        RemoteSurfaceButton(
                            description = "Пауза",
                            modifier = Modifier.size(mediaButtonSize),
                            shape = RoundedCornerShape(18.dp),
                            background = padButtonColor,
                            onClick = { onKey(RemoteKey.PAUSE) }
                        ) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = null,
                                tint = padIconColor,
                                modifier = Modifier.size(if (tight) 27.dp else 32.dp)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (tight) 14.dp else 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteSurfaceButton(
                            description = "Без звука",
                            modifier = Modifier.size(secondRowButtonSize),
                            shape = CircleShape,
                            background = padButtonColor,
                            onClick = { onKey(RemoteKey.MUTE) }
                        ) {
                            Icon(
                                Icons.Filled.VolumeOff,
                                contentDescription = null,
                                tint = padIconColor,
                                modifier = Modifier.size(if (tight) 25.dp else 30.dp)
                            )
                        }
                        RemoteSurfaceButton(
                            description = "Меню",
                            modifier = Modifier.size(secondRowButtonSize),
                            shape = CircleShape,
                            background = padButtonColor,
                            onClick = { onKey(RemoteKey.MENU) }
                        ) {
                            Text(
                                "MENU",
                                color = padIconColor,
                                fontSize = if (tight) 14.sp else 18.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
                WhiteVolumePill(
                    title = "CH",
                    topLabel = "⌃",
                    bottomLabel = "⌄",
                    topDescription = "Следующий канал",
                    bottomDescription = "Предыдущий канал",
                    modifier = Modifier.width(volumePillWidth).fillMaxHeight(),
                    onTop = { onKey(RemoteKey.CHANNEL_UP) },
                    onBottom = { onKey(RemoteKey.CHANNEL_DOWN) }
                )
            }

            // D-pad outline stays fixed while its rainbow gradient rotates around the cross.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dpadHeight)
                    .padding(horizontal = 1.dp)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.36f, h * 0.05f)
                            cubicTo(w * 0.36f, h * 0.025f, w * 0.40f, h * 0.02f, w * 0.44f, h * 0.02f)
                            lineTo(w * 0.56f, h * 0.02f)
                            cubicTo(w * 0.60f, h * 0.02f, w * 0.64f, h * 0.025f, w * 0.64f, h * 0.08f)
                            cubicTo(w * 0.64f, h * 0.23f, w * 0.70f, h * 0.29f, w * 0.82f, h * 0.30f)
                            cubicTo(w * 0.94f, h * 0.31f, w * 0.97f, h * 0.36f, w * 0.97f, h * 0.44f)
                            lineTo(w * 0.97f, h * 0.56f)
                            cubicTo(w * 0.97f, h * 0.64f, w * 0.94f, h * 0.69f, w * 0.82f, h * 0.70f)
                            cubicTo(w * 0.70f, h * 0.71f, w * 0.64f, h * 0.77f, w * 0.64f, h * 0.92f)
                            cubicTo(w * 0.64f, h * 0.975f, w * 0.60f, h * 0.98f, w * 0.56f, h * 0.98f)
                            lineTo(w * 0.44f, h * 0.98f)
                            cubicTo(w * 0.40f, h * 0.98f, w * 0.36f, h * 0.975f, w * 0.36f, h * 0.92f)
                            cubicTo(w * 0.36f, h * 0.77f, w * 0.30f, h * 0.71f, w * 0.18f, h * 0.70f)
                            cubicTo(w * 0.06f, h * 0.69f, w * 0.03f, h * 0.64f, w * 0.03f, h * 0.56f)
                            lineTo(w * 0.03f, h * 0.44f)
                            cubicTo(w * 0.03f, h * 0.36f, w * 0.06f, h * 0.31f, w * 0.18f, h * 0.30f)
                            cubicTo(w * 0.30f, h * 0.29f, w * 0.36f, h * 0.23f, w * 0.36f, h * 0.08f)
                            close()
                        }
                        val center = Offset(w / 2f, h / 2f)
                        drawPath(
                            path,
                            color = Color(0xFF191919),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        rotate(rainbowRotation, pivot = center) {
                            drawPath(
                                path,
                                brush = Brush.sweepGradient(colors = rainbowColors, center = center),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
            ) {
                RemoteSurfaceButton(
                    description = "Вверх",
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = if (tight) 7.dp else 13.dp)
                        .size(arrowSize),
                    shape = RoundedCornerShape(17.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.UP) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 28.dp else 34.dp))
                }
                RemoteSurfaceButton(
                    description = "Влево",
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(start = if (tight) 7.dp else 13.dp)
                        .size(arrowSize),
                    shape = RoundedCornerShape(17.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.LEFT) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 28.dp else 34.dp))
                }
                RemoteSurfaceButton(
                    description = "Вправо",
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(end = if (tight) 7.dp else 13.dp)
                        .size(arrowSize),
                    shape = RoundedCornerShape(17.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.RIGHT) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 28.dp else 34.dp))
                }
                RemoteSurfaceButton(
                    description = "Вниз",
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = if (tight) 7.dp else 13.dp)
                        .size(arrowSize),
                    shape = RoundedCornerShape(17.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.DOWN) }
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 28.dp else 34.dp))
                }
                RemoteSurfaceButton(
                    description = "ОК",
                    modifier = Modifier.align(Alignment.Center).size(okSize),
                    shape = RoundedCornerShape(if (tight) 22.dp else 28.dp),
                    background = RadioTvPalette.Red,
                    pulseScale = okPulse,
                    onClick = { onKey(RemoteKey.OK) }
                ) {
                    Text("OK", color = Color.White, fontSize = if (tight) 22.sp else 27.sp, fontWeight = FontWeight.Normal)
                }
            }

            // EXIT uses its own key where the connected TV protocol supports it.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = sideInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteSurfaceButton(
                    description = "Выход",
                    modifier = Modifier.size(lowerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.EXIT) }
                ) {
                    Text("EXIT", color = padIconColor, fontSize = if (tight) 11.sp else 14.sp, maxLines = 1)
                }
                RemoteSurfaceButton(
                    description = "Трансляция с телефона на ТВ",
                    modifier = Modifier.size(lowerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onFeatureAction(RemoteFeatureAction.CAST) }
                ) {
                    Icon(Icons.Filled.Cast, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 25.dp else 30.dp))
                }
            }

            // The four lower quick actions match the reference image.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = sideInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteSurfaceButton(
                    description = if (voiceActive) "Микрофон, запись активна" else "Микрофон",
                    modifier = Modifier.size(footerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onFeatureAction(RemoteFeatureAction.VOICE_INPUT) }
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 25.dp else 29.dp))
                }
                RemoteSurfaceButton(
                    description = if (keyboardActive) "Клавиатура, активна" else "Клавиатура",
                    modifier = Modifier.size(footerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onFeatureAction(RemoteFeatureAction.KEYBOARD) }
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 24.dp else 28.dp))
                }
                RemoteSurfaceButton(
                    description = if (touchpadActive || airMouseActive) "Тачпад, активен" else "Тачпад",
                    modifier = Modifier.size(footerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onFeatureAction(RemoteFeatureAction.TOUCHPAD) }
                ) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 24.dp else 28.dp))
                }
                RemoteSurfaceButton(
                    description = "Домой",
                    modifier = Modifier.size(footerButtonSize),
                    shape = CircleShape,
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.HOME) }
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 25.dp else 29.dp))
                }
            }

            if (!featureMessage.isNullOrBlank()) {
                Text(
                    featureMessage,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RemoteSurfaceButton(
    description: String,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    background: Color,
    onClick: () -> Unit,
    pulseScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "press-$description"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale * pulseScale
                scaleY = pressScale * pulseScale
                alpha = if (pressed) 0.82f else 1f
            }
            .clip(shape)
            .background(background)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interactions,
                indication = null,
                onClickLabel = description,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun WhiteVolumePill(
    title: String,
    topLabel: String,
    bottomLabel: String,
    topDescription: String,
    bottomDescription: String,
    modifier: Modifier = Modifier,
    onTop: () -> Unit,
    onBottom: () -> Unit
) {
    val white = Color(0xFFF8F8F5)
    val ink = Color(0xFF252A29)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(white)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        RemoteSurfaceButton(
            description = topDescription,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = CircleShape,
            background = white,
            onClick = onTop
        ) {
            if (title == "VOL") {
                Text(topLabel, color = ink, fontSize = 28.sp, fontWeight = FontWeight.Normal)
            } else {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = ink, modifier = Modifier.size(29.dp))
            }
        }
        Text(
            title,
            color = ink,
            fontSize = if (title == "VOL") 17.sp else 18.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
        RemoteSurfaceButton(
            description = bottomDescription,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = CircleShape,
            background = white,
            onClick = onBottom
        ) {
            if (title == "VOL") {
                Text(bottomLabel, color = ink, fontSize = 27.sp, fontWeight = FontWeight.Normal)
            } else {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = ink, modifier = Modifier.size(29.dp))
            }
        }
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
