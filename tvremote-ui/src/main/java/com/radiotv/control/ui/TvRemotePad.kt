package com.radiotv.control.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.ripple
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.graphics.Color
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


private val LocalRemoteButtonFeedback = staticCompositionLocalOf<() -> Unit> { {} }

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
    voiceTranscript: String? = null,
    onCloseVoiceInput: () -> Unit = {},
    keyboardActive: Boolean = false,
    featureMessage: String? = null,
    showFeatureActions: Boolean = false,
    energySaving: Boolean = false,
    onButtonFeedback: () -> Unit = {},
    deviceName: String = "Телевизор не подключён",
    searching: Boolean = false,
    onSettings: () -> Unit = {},
    onBack: () -> Unit = { onKey(RemoteKey.BACK) },
    onStartSearch: () -> Unit = {}
) {
    CompositionLocalProvider(LocalRemoteButtonFeedback provides onButtonFeedback) {
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
        val headerHeight = if (tight) 44.dp else if (compact) 58.dp else 62.dp
        val headerButtonSize = if (tight) 36.dp else if (compact) 44.dp else 48.dp
        val powerSize = if (tight) 42.dp else if (compact) 50.dp else 56.dp
        val rowSpacing = if (tight) 4.dp else 6.dp
        val footerGap = if (tight) 6.dp else 8.dp
        val footerButtonSize = minOf(
            (maxWidth * 0.19f).coerceAtLeast(56.dp),
            (maxHeight * 0.18f).coerceAtLeast(56.dp)
        ).coerceAtMost(104.dp)
        val controlsAvailableHeight = (
            maxHeight - headerHeight - footerGap - rowSpacing * 2 - footerButtonSize - 16.dp -
                (if (!featureMessage.isNullOrBlank()) 28.dp else 0.dp)
            ).coerceAtLeast(180.dp)
        val volumePillWidth = minOf((maxWidth * 0.22f).coerceAtLeast(62.dp), (maxHeight * 0.15f).coerceAtLeast(62.dp)).coerceAtMost(120.dp)
        val mediaButtonSize = minOf((maxWidth * 0.195f).coerceAtLeast(58.dp), (controlsAvailableHeight * 0.19f).coerceAtLeast(58.dp)).coerceAtMost(108.dp)
        val secondRowButtonSize = minOf((maxWidth * 0.18f).coerceAtLeast(50.dp), (controlsAvailableHeight * 0.16f).coerceAtLeast(50.dp)).coerceAtMost(96.dp)
        val arrowSize = minOf((maxWidth * 0.20f).coerceAtLeast(56.dp), (controlsAvailableHeight * 0.18f).coerceAtLeast(56.dp)).coerceAtMost(112.dp)
        val okSize = minOf((maxWidth * 0.24f).coerceAtLeast(64.dp), (controlsAvailableHeight * 0.20f).coerceAtLeast(64.dp)).coerceAtMost(124.dp)
        val dpadHorizontalGap = 10.dp
        val availableSideWidth = (((maxWidth - okSize) / 2f) - dpadHorizontalGap).coerceAtLeast(0.dp)
        val arrowSideWidth = minOf(arrowSize * 1.10f, availableSideWidth)
        val dpadTopInset = if (tight) 6.dp else 10.dp
        val dpadBottomInset = if (tight) 6.dp else 10.dp
        val dpadVerticalGap = if (tight) 6.dp else 8.dp
        val dpadHeight = dpadTopInset + arrowSize + dpadVerticalGap + okSize +
            dpadVerticalGap + arrowSize + dpadBottomInset
        val dpadOverlap = (arrowSize * if (tight) 0.78f else 0.90f)
            .coerceAtMost(if (tight) 48.dp else 76.dp)
        val lowerButtonSize = footerButtonSize
        val sideInset = (maxWidth * 0.045f).coerceIn(8.dp, 22.dp)
        val controlColumnGap = (maxWidth * 0.035f).coerceIn(4.dp, 12.dp)
        val sideArrowGap = dpadHorizontalGap * 2f
        val arrowInset = (((maxWidth - okSize) / 2f) - arrowSideWidth - sideArrowGap).coerceAtLeast(0.dp)
        // Fill the available height around the D-pad while preserving the portrait reference.
        val topClusterHeight = (controlsAvailableHeight - dpadHeight + dpadOverlap).coerceAtLeast(68.dp)
        val remoteControlsHeight = topClusterHeight + dpadHeight - dpadOverlap
        val requiredContentHeight = headerHeight + remoteControlsHeight + footerGap +
            footerButtonSize + rowSpacing * 2 + 16.dp +
            if (!featureMessage.isNullOrBlank()) 28.dp else 0.dp
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
        val voicePulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = if (voiceActive && !energySaving) 1.12f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "remote-voice-pulse"
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

            // The top controls and D-pad share one measured area, matching the compact reference layout.
            Box(
                modifier = Modifier.fillMaxWidth().height(remoteControlsHeight)
                    .graphicsLayer { clip = false }
            ) {
            // Tall VOL/CH pills frame Play/Pause above Mute/MENU, matching the reference.
            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(topClusterHeight)
                    .graphicsLayer { clip = false },
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
                        horizontalArrangement = Arrangement.spacedBy(controlColumnGap),
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
                        horizontalArrangement = Arrangement.spacedBy(controlColumnGap),
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
                    .align(Alignment.TopCenter)
                    .offset(y = topClusterHeight - dpadOverlap)
                    .fillMaxWidth()
                    .height(dpadHeight)
                    .padding(horizontal = 1.dp)
            ) {
                RemoteSurfaceButton(
                    description = "Вверх",
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = dpadTopInset).size(arrowSize),
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = dpadBottomInset).size(arrowSize),
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
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(lowerButtonSize).padding(horizontal = sideInset),
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

            }

            // The lower quick-action row follows the D-pad at the same compact spacing as the reference.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = footerGap).height(footerButtonSize).padding(horizontal = sideInset),
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

        AnimatedVisibility(
            visible = voiceActive,
            modifier = Modifier.align(Alignment.Center).padding(12.dp),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(1000))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(132.dp)
                        .background(Color(0xEE171717), CircleShape)
                        .border(2.dp, RadioTvPalette.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Голосовой ввод",
                        tint = RadioTvPalette.Red,
                        modifier = Modifier.size(58.dp).graphicsLayer {
                            scaleX = voicePulse
                            scaleY = voicePulse
                        }
                    )
                }
                OutlinedButton(
                    onClick = onCloseVoiceInput,
                    modifier = Modifier.width(156.dp).height(48.dp)
                        .background(RadioTvPalette.Red, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("ЗАКРЫТЬ", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        AnimatedVisibility(
            visible = !voiceTranscript.isNullOrBlank(),
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(start = 18.dp, end = 18.dp, top = 88.dp),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(650))
        ) {
            Text(
                voiceTranscript.orEmpty(),
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xEA171717))
                    .border(1.dp, RadioTvPalette.Red.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
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
    pressTiltX: Float = 0f,
    pressTiltY: Float = 0f,
    animatePressTilt: Boolean = false,
    pressOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonFeedback = LocalRemoteButtonFeedback.current
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(70, easing = FastOutSlowInEasing),
        label = "press-$description"
    )
    val pressTiltXAnimated by animateFloatAsState(
        targetValue = if (isPressed && animatePressTilt) pressTiltX else 0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "press-tilt-$description"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                // VOL/CH use a physical press instead of perspective rotation:
                // the top keys move down, the lower keys move up, and both compress vertically.
                val physicalPress = pressOffsetY != 0.dp
                scaleX = if (physicalPress) pulseScale else pressScale * pulseScale
                scaleY = (if (isPressed && physicalPress) 0.95f else pressScale) * pulseScale
                translationY = if (isPressed) pressOffsetY.toPx() else 0f
                rotationX = if (physicalPress) 0f else if (animatePressTilt) pressTiltXAnimated else if (isPressed) pressTiltX else 0f
                rotationY = if (physicalPress) 0f else if (isPressed) pressTiltY else 0f
                cameraDistance = 12f * density
                clip = false
                alpha = if (isPressed) 0.82f else 1f
            }
            .clip(shape)
            .background(if (isPressed && pressOffsetY != 0.dp) Color(0xFFE3E3E0) else background)
            .semantics {
                contentDescription = description
                role = androidx.compose.ui.semantics.Role.Button
            }
            // The clickable is on the full outer Box, not on its icon/text child.
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClickLabel = description,
                onClick = {
                    buttonFeedback()
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
            // A rounded background is painted without clipping child press layers.
            .graphicsLayer { clip = false }
            .background(white, RoundedCornerShape(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        RemoteSurfaceButton(
            description = topDescription,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            background = white,
            onClick = onTop,
            pressOffsetY = 4.dp
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
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp),
            background = white,
            onClick = onBottom,
            pressOffsetY = (-4).dp
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
