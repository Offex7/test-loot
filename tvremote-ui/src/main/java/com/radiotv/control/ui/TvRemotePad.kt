package com.radiotv.control.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.ripple
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.radiotv.control.core.RemoteKey
import kotlinx.coroutines.delay

enum class RemoteFeatureAction { VOICE_INPUT, KEYBOARD, AIR_MOUSE, TOUCHPAD, CAST, APP_YOUTUBE, APP_NETFLIX }


private val LocalRemoteHaptics = staticCompositionLocalOf { true }

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
@Composable
internal fun TvRemotePad(
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureAction: (RemoteFeatureAction) -> Unit = {},
    touchpadActive: Boolean = false,
    airMouseActive: Boolean = false,
    muteActive: Boolean = false,
    voiceActive: Boolean = false,
    keyboardActive: Boolean = false,
    featureMessage: String? = null,
    showFeatureActions: Boolean = false,
    energySaving: Boolean = false,
    hapticsEnabled: Boolean = true,
    deviceName: String = "Телевизор не подключён",
    searching: Boolean = false,
    onSettings: () -> Unit = {},
    onBack: () -> Unit = { onKey(RemoteKey.BACK) },
    onStartSearch: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRemoteHaptics provides hapticsEnabled) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .background(Color(0xFF000000))
            .semantics {
                contentDescription = if (enabled) {
                    "Пульт управления телевизором"
                } else {
                    "Пульт управления телевизором. Нет подключения; нажмите «Начать поиск»."
                }
            }
    ) {
        val tight = maxHeight < 520.dp
        val compact = maxHeight < 840.dp
        val headerHeight = if (tight) 42.dp else if (compact) 50.dp else 56.dp
        val headerButtonSize = if (tight) 34.dp else if (compact) 40.dp else 44.dp
        val powerSize = if (tight) 40.dp else if (compact) 46.dp else 52.dp
        // Side pills are deliberately tall; they frame the media keys and reach the D-pad.
        val topClusterHeight = when {
            tight -> (maxHeight * 0.32f).coerceIn(156.dp, 184.dp)
            compact -> (maxHeight * 0.34f).coerceIn(190.dp, 232.dp)
            else -> (maxHeight * 0.35f).coerceIn(212.dp, 260.dp)
        }
        val volumePillWidth = (maxWidth * (if (tight) 0.21f else 0.23f)).coerceIn(if (tight) 60.dp else 76.dp, if (tight) 76.dp else 96.dp)
        val mediaButtonSize = (maxWidth * 0.195f).coerceIn(if (tight) 58.dp else 70.dp, if (tight) 72.dp else 80.dp)
        val secondRowButtonSize = (maxWidth * 0.18f).coerceIn(if (tight) 50.dp else 62.dp, if (tight) 62.dp else 74.dp)
        val footerButtonSize = (maxWidth * 0.19f).coerceIn(if (tight) 56.dp else 66.dp, if (tight) 68.dp else 78.dp)
        val arrowSize = (maxWidth * 0.20f).coerceIn(if (tight) 62.dp else 70.dp, if (tight) 72.dp else 82.dp)
        val arrowSideWidth = arrowSize * 1.28f
        val okSize = (maxWidth * 0.24f).coerceIn(if (tight) 70.dp else 86.dp, if (tight) 88.dp else 100.dp)
        // The directional buttons overlap the center key slightly, eliminating visible gaps.
        val arrowOverlap = 3.dp
        val dpadHeight = okSize + arrowSize * 2 - arrowOverlap * 2
        val lowerButtonSize = (maxWidth * 0.19f).coerceIn(if (tight) 56.dp else 66.dp, if (tight) 68.dp else 78.dp)
        val sideInset = (maxWidth * (if (tight) 0.045f else 0.06f)).coerceIn(12.dp, 26.dp)
        // The D-pad is inside the column's 8 dp side padding and has 1 dp local padding.
        // Use its actual inner width so the side arrows meet the center key without gaps.
        val arrowInset = (((maxWidth - 18.dp - okSize) / 2) - arrowSideWidth + arrowOverlap).coerceAtLeast(0.dp)
        val rowSpacing = if (tight) 5.dp else 7.dp
        val requiredContentHeight = headerHeight + topClusterHeight + dpadHeight + footerButtonSize + lowerButtonSize + rowSpacing * 4 + 22.dp +
            if (!featureMessage.isNullOrBlank()) 28.dp else 0.dp
        val adaptiveLowerRowGap = ((maxHeight - requiredContentHeight).coerceAtLeast(0.dp)).coerceAtMost(14.dp)
        val padButtonColor = Color(0xFFF8F8F5)
        val padIconColor = Color(0xFF252A29)
        val headerColor = Color(0xFF151515)
        val headerButtonColor = Color(0xFF2A2A2A)
        val headerIconColor = Color(0xFFE8E8E8)

        val transition = rememberInfiniteTransition(label = "remote-animations")
        val okPulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = if (energySaving) 1f else 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ok-pulse"
        )
        val searchPulse by transition.animateFloat(
            initialValue = if (energySaving) 1f else 0.97f,
            targetValue = if (energySaving) 1f else 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "search-pulse"
        )
        val rainbowOffset by transition.animateFloat(
            initialValue = 0f,
            targetValue = if (energySaving) 0f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rainbow-gradient-offset"
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .then(if (requiredContentHeight > maxHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
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
                Row(
                    modifier = Modifier.weight(1f)
                        .padding(horizontal = 3.dp)
                        .graphicsLayer {
                            val scale = if (deviceName == "Начать поиск" && !searching) searchPulse else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(enabled = !enabled && !searching, onClick = onStartSearch),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (searching) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = RadioTvPalette.Red,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ИЩЕМ ТЕЛЕВИЗОРЫ…",
                            color = Color.White,
                            fontSize = if (tight) 12.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = deviceName.ifBlank { "Нет подключения" },
                            color = if (deviceName == "Начать поиск") RadioTvPalette.Red else Color.White,
                            fontSize = if (tight) 16.sp else 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
                    verticalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 10.dp, Alignment.CenterVertically)
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
                            background = if (muteActive) RadioTvPalette.Red else padButtonColor,
                            onClick = { onKey(RemoteKey.MUTE) }
                        ) {
                            Icon(
                                Icons.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (muteActive) Color.White else padIconColor,
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
                                fontSize = if (tight) 10.sp else if (compact) 14.sp else 18.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
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

            // The rainbow outline remains fixed; only a bright moving highlight travels over it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dpadHeight)
                    .padding(horizontal = 1.dp)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        // Compact organic clover contour. The narrow top/bottom lobes hug
                        // the arrow keys; the side lobes bulge only far enough to frame them.
                        val path = Path().apply {
                            moveTo(w * 0.38f, h * 0.01f)
                            cubicTo(w * 0.35f, h * 0.01f, w * 0.34f, h * 0.04f, w * 0.34f, h * 0.10f)
                            cubicTo(w * 0.34f, h * 0.17f, w * 0.34f, h * 0.235f, w * 0.35f, h * 0.285f)
                            cubicTo(w * 0.39f, h * 0.32f, w * 0.34f, h * 0.325f, w * 0.28f, h * 0.325f)
                            cubicTo(w * 0.18f, h * 0.325f, w * 0.12f, h * 0.36f, w * 0.12f, h * 0.43f)
                            lineTo(w * 0.12f, h * 0.57f)
                            cubicTo(w * 0.12f, h * 0.64f, w * 0.18f, h * 0.675f, w * 0.28f, h * 0.675f)
                            cubicTo(w * 0.36f, h * 0.675f, w * 0.39f, h * 0.68f, w * 0.35f, h * 0.715f)
                            cubicTo(w * 0.34f, h * 0.765f, w * 0.34f, h * 0.83f, w * 0.34f, h * 0.90f)
                            cubicTo(w * 0.34f, h * 0.955f, w * 0.35f, h * 0.99f, w * 0.38f, h * 0.99f)
                            lineTo(w * 0.62f, h * 0.99f)
                            cubicTo(w * 0.65f, h * 0.99f, w * 0.66f, h * 0.955f, w * 0.66f, h * 0.90f)
                            cubicTo(w * 0.66f, h * 0.83f, w * 0.66f, h * 0.765f, w * 0.65f, h * 0.715f)
                            cubicTo(w * 0.61f, h * 0.68f, w * 0.66f, h * 0.675f, w * 0.72f, h * 0.675f)
                            cubicTo(w * 0.82f, h * 0.675f, w * 0.88f, h * 0.64f, w * 0.88f, h * 0.57f)
                            lineTo(w * 0.88f, h * 0.43f)
                            cubicTo(w * 0.88f, h * 0.36f, w * 0.82f, h * 0.325f, w * 0.72f, h * 0.325f)
                            cubicTo(w * 0.64f, h * 0.325f, w * 0.575f, h * 0.32f, w * 0.61f, h * 0.285f)
                            cubicTo(w * 0.66f, h * 0.235f, w * 0.66f, h * 0.17f, w * 0.66f, h * 0.10f)
                            cubicTo(w * 0.66f, h * 0.04f, w * 0.65f, h * 0.01f, w * 0.62f, h * 0.01f)
                            close()
                        }
                        val fixedRainbowBrush = Brush.sweepGradient(
                            colors = rainbowColors,
                            center = Offset(w / 2f, h / 2f)
                        )
                        drawPath(
                            path,
                            brush = fixedRainbowBrush,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
                        val pathLength = measure.length
                        if (pathLength > 0f) {
                            val segmentLength = pathLength * 0.16f
                            val segmentStart = pathLength * rainbowOffset
                            val segmentEnd = segmentStart + segmentLength
                            val movingSegment = android.graphics.Path()
                            if (segmentEnd <= pathLength) {
                                measure.getSegment(segmentStart, segmentEnd, movingSegment, true)
                            } else {
                                measure.getSegment(segmentStart, pathLength, movingSegment, true)
                                val wrappedSegment = android.graphics.Path()
                                measure.getSegment(0f, segmentEnd - pathLength, wrappedSegment, true)
                                movingSegment.addPath(wrappedSegment)
                            }
                            val snakePath = movingSegment.asComposePath()
                            drawPath(
                                snakePath,
                                color = Color.White.copy(alpha = 0.22f),
                                style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                snakePath,
                                brush = fixedRainbowBrush,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                snakePath,
                                color = Color.White.copy(alpha = 0.90f),
                                style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
            ) {
                RemoteSurfaceButton(
                    description = "Вверх",
                    modifier = Modifier.align(Alignment.TopCenter).size(arrowSize),
                    shape = RoundedCornerShape(if (tight) 14.dp else if (compact) 18.dp else 20.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.UP) },
                    pressTiltX = 8f
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 33.dp else if (compact) 48.dp else 56.dp))
                }
                RemoteSurfaceButton(
                    description = "Влево",
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(start = arrowInset)
                        .width(arrowSideWidth)
                        .height(arrowSize),
                    shape = RoundedCornerShape(if (tight) 14.dp else if (compact) 18.dp else 20.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.LEFT) },
                    pressTiltY = -8f
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 33.dp else if (compact) 48.dp else 56.dp))
                }
                RemoteSurfaceButton(
                    description = "Вправо",
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(end = arrowInset)
                        .width(arrowSideWidth)
                        .height(arrowSize),
                    shape = RoundedCornerShape(if (tight) 14.dp else if (compact) 18.dp else 20.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.RIGHT) },
                    pressTiltY = 8f
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 33.dp else if (compact) 48.dp else 56.dp))
                }
                RemoteSurfaceButton(
                    description = "Вниз",
                    modifier = Modifier.align(Alignment.BottomCenter).size(arrowSize),
                    shape = RoundedCornerShape(if (tight) 14.dp else if (compact) 18.dp else 20.dp),
                    background = padButtonColor,
                    onClick = { onKey(RemoteKey.DOWN) },
                    pressTiltX = -8f
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = padIconColor, modifier = Modifier.size(if (tight) 33.dp else if (compact) 48.dp else 56.dp))
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
                modifier = Modifier.fillMaxWidth().height(lowerButtonSize).padding(horizontal = sideInset),
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

            // Leave only a compact gap between EXIT/Cast and the four lower quick actions.
            Spacer(modifier = Modifier.height(adaptiveLowerRowGap))
            // The four lower quick actions match the reference image.
            Row(
                modifier = Modifier.fillMaxWidth().height(footerButtonSize).padding(horizontal = sideInset),
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
                    background = if (touchpadActive || airMouseActive) RadioTvPalette.Red else padButtonColor,
                    onClick = { onFeatureAction(RemoteFeatureAction.TOUCHPAD) }
                ) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, tint = if (touchpadActive || airMouseActive) Color.White else padIconColor, modifier = Modifier.size(if (tight) 24.dp else 28.dp))
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
}

@Composable
private fun RemoteSurfaceButton(
    description: String,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    background: Color,
    onClick: () -> Unit,
    pulseScale: Float = 1f,
    pressTiltX: Float = 0f,
    pressTiltY: Float = 0f,
    content: @Composable () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalRemoteHaptics.current
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(70, easing = FastOutSlowInEasing),
        label = "press-$description"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale * pulseScale
                scaleY = pressScale * pulseScale
                rotationX = if (pressed) pressTiltX else 0f
                rotationY = if (pressed) pressTiltY else 0f
                cameraDistance = 32.dp.toPx()
                alpha = if (pressed) 0.82f else 1f
            }
            .clip(shape)
            .background(background)
            .semantics {
                contentDescription = description
                role = androidx.compose.ui.semantics.Role.Button
            }
            // The clickable is on the full outer Box, not on its icon/text child.
            .clickable(
                interactionSource = interactions,
                indication = ripple(bounded = true),
                onClickLabel = description,
                onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
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
            onClick = onTop,
            pressTiltX = 7f
        ) {
            if (title == "VOL") {
                Text(topLabel, color = ink, fontSize = 32.sp, fontWeight = FontWeight.Normal)
            } else {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = ink, modifier = Modifier.size(34.dp))
            }
        }
        Text(
            title,
            color = ink,
            fontSize = if (title == "VOL") 20.sp else 21.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
        RemoteSurfaceButton(
            description = bottomDescription,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = CircleShape,
            background = white,
            onClick = onBottom,
            pressTiltX = -7f
        ) {
            if (title == "VOL") {
                Text(bottomLabel, color = ink, fontSize = 31.sp, fontWeight = FontWeight.Normal)
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
    modifier: Modifier = Modifier,
    helperText: String? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(236.dp).clip(shape)
            .background(Color(0xFF414141))
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
            Text("ТАЧПАД", color = if (enabled) Color.White else RadioTvPalette.Muted, fontWeight = FontWeight.Bold)
            Text(
                helperText ?: if (enabled) "Свайп — курсор · тап — клик · долгий тап — контекстное меню"
                else "Сначала подключите Bluetooth HID на телевизоре",
                color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
    }
}
