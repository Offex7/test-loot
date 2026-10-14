package com.radiotv.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction

@Composable
internal fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onInfrared: () -> Unit,
    onSmartTv: () -> Unit,
    onUnknown: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260, delayMillis = 50)) + slideInVertically(tween(300, delayMillis = 50)) { it / 7 }
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 18.dp)
                    .fillMaxWidth(0.82f)
                    .height(154.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFB5DC),
                                Color(0xFFCBB9FF),
                                Color(0xFFFFE8A4),
                                Color(0xFFFFFFFF)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFF8D8D8D), RoundedCornerShape(4.dp))
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260, delayMillis = 100)) + slideInVertically(tween(300, delayMillis = 100)) { it / 8 }
        ) {
            Text(
                "Выберите тип своего пульта дистанционного управления",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260, delayMillis = 150)) + slideInVertically(tween(300, delayMillis = 150)) { it / 8 }
        ) {
            OnboardingChoice(
                background = Color(0xFFE32F3A),
                icon = "▣◎",
                title = "ИК-пульт от ТВ",
                trailing = ")))",
                onClick = onInfrared
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260, delayMillis = 200)) + slideInVertically(tween(300, delayMillis = 200)) { it / 8 }
        ) {
            OnboardingChoice(
                background = Color(0xFF2196F3),
                icon = "▣+",
                title = "Пульт от смарт-ТВ",
                trailing = "ᛒ",
                onClick = onSmartTv,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260, delayMillis = 250)) + slideInVertically(tween(300, delayMillis = 250)) { it / 8 }
        ) {
            Text(
                "Я не знаю",
                color = Color.White,
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onUnknown)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun OnboardingChoice(
    background: Color,
    icon: String,
    title: String,
    trailing: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(150), label = "onboarding-choice")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(icon, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(trailing, color = Color.White, fontSize = if (trailing == "ᛒ") 28.sp else 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DigitalRemotePanel(
    modifier: Modifier = Modifier,
    deviceName: String,
    installedApps: List<InstalledTvApp>,
    onLaunchApp: (InstalledTvApp) -> Unit,
    enabled: Boolean,
    micRecording: Boolean,
    airMouseActive: Boolean,
    keyboardActive: Boolean,
    onKey: (RemoteKey) -> Unit,
    onFeatureAction: (RemoteFeatureAction) -> Unit,
    onSettings: () -> Unit,
    onReturnRemote: () -> Unit,
    duplicationExpanded: Boolean,
    onToggleDuplication: () -> Unit,
    duplicationContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioTvPalette.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 6 }) {
            Row(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF2A2A2A))
                        .clickable(onClick = onReturnRemote),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад к пульту", tint = Color(0xFFE8E8E8), modifier = Modifier.size(21.dp)) }
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF2A2A2A))
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = Color(0xFFE8E8E8), modifier = Modifier.size(21.dp)) }
                Text(
                    deviceName.ifBlank { "Нет подключения" },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(RadioTvPalette.Red)
                        .clickable(enabled = enabled, onClick = { onKey(RemoteKey.POWER) }),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Питание", tint = Color.White, modifier = Modifier.size(25.dp)) }
            }
        }
        if (installedApps.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                installedApps.take(12).forEach { app ->
                    TvInstalledAppShortcut(app = app, onClick = { onLaunchApp(app) })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        AnimatedVisibility(visible = true, enter = fadeIn(tween(250, delayMillis = 100)) + slideInVertically(tween(250, delayMillis = 100)) { it / 8 }) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..9).toList().chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { number ->
                            NumberButton(number.toString(), modifier = Modifier.weight(1f), onClick = { onKey(digitKeyForPanel(number)) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberButton("Cast", modifier = Modifier.weight(1f), outlined = true, onClick = { onFeatureAction(RemoteFeatureAction.CAST) })
                    NumberButton("0", modifier = Modifier.weight(1f), onClick = { onKey(RemoteKey.NUMBER_0) })
                    NumberButton("Menu", modifier = Modifier.weight(1f), outlined = true, onClick = { onKey(RemoteKey.MENU) })
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DigitalVolumePill(
                title = "VOL",
                top = "+",
                bottom = "−",
                modifier = Modifier.width(48.dp).height(145.dp),
                enabled = enabled,
                onTop = { onKey(RemoteKey.VOLUME_UP) },
                onBottom = { onKey(RemoteKey.VOLUME_DOWN) }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitalAction("▤", "Клавиатура", active = keyboardActive, modifier = Modifier.weight(1f)) { onFeatureAction(RemoteFeatureAction.KEYBOARD) }
                    DigitalAction("⌂", "Домой", modifier = Modifier.weight(1f)) { onKey(RemoteKey.HOME) }
                    DigitalAction("✥", "Аэромышь", active = airMouseActive, modifier = Modifier.weight(1f)) { onFeatureAction(RemoteFeatureAction.AIR_MOUSE) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    DigitalAction("◖×", "Без звука", modifier = Modifier.weight(1f)) { onKey(RemoteKey.MUTE) }
                    DigitalAction("●", "Микрофон", active = micRecording, recording = micRecording, modifier = Modifier.weight(1f)) { onFeatureAction(RemoteFeatureAction.VOICE_INPUT) }
                    DigitalAction("↶", "Назад", modifier = Modifier.weight(1f)) { onKey(RemoteKey.BACK) }
                }
            }
            DigitalVolumePill(
                title = "CH",
                top = "⌃",
                bottom = "⌄",
                modifier = Modifier.width(48.dp).height(145.dp),
                enabled = enabled,
                onTop = { onKey(RemoteKey.CHANNEL_UP) },
                onBottom = { onKey(RemoteKey.CHANNEL_DOWN) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF202020))
                .clickable(onClick = onToggleDuplication)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Дублирование", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(if (duplicationExpanded) "⌃" else "⌄", color = Color(0xFFB0B0B0), fontSize = 18.sp)
        }
        AnimatedVisibility(
            visible = duplicationExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 8 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                duplicationContent()
            }
        }
    }
}

@Composable
private fun TvInstalledAppShortcut(app: InstalledTvApp, onClick: () -> Unit) {
    val name = app.name.trim()
    val normalized = name.lowercase()
    val tileColor = when {
        normalized.contains("youtube") -> Color(0xFFE62117)
        normalized.contains("netflix") -> Color(0xFF8B1018)
        normalized.contains("rutube") -> Color(0xFF2D255D)
        normalized.contains("ivi") -> Color(0xFFDF153B)
        normalized.contains("кинопоиск") -> Color(0xFFF57C00)
        else -> Color(0xFF343434)
    }
    val tileMark = when {
        normalized.contains("youtube") -> "▶"
        normalized.contains("netflix") -> "N"
        normalized.contains("rutube") -> "R"
        normalized.contains("ivi") -> "ivi"
        normalized.contains("кинопоиск") -> "К"
        else -> name.take(1).uppercase().ifBlank { "•" }
    }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.91f else 1f, tween(140), label = "installed-app-scale")
    Column(
        modifier = Modifier.width(76.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale; alpha = if (pressed) 0.82f else 1f }
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactions, indication = null, onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)).background(tileColor),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconUrl.isNullOrBlank()) {
                Text(tileMark, color = Color.White, fontSize = if (tileMark.length > 1) 15.sp else 23.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.Apps, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
        }
        Text(name, color = Color(0xFFB0B0B0), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
    }
}
@Composable
private fun NumberButton(
    title: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.91f else 1f, tween(140), label = "number-key-scale")
    val bg = Color(0xFFF8F8F5)
    Box(
        modifier = modifier.height(57.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (outlined) Modifier.border(1.dp, RadioTvPalette.Red, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color(0xFF252A29), fontSize = if (title == "Menu" || title == "Cast") 15.sp else 23.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun DigitalVolumePill(
    title: String,
    top: String,
    bottom: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onTop: () -> Unit,
    onBottom: () -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFFF8F8F5)).padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val topIcon = when (title) { "VOL" -> Icons.Filled.VolumeUp; "CH" -> Icons.Filled.KeyboardArrowUp; else -> Icons.Filled.Add }
        val bottomIcon = when (title) { "VOL" -> Icons.Filled.VolumeDown; "CH" -> Icons.Filled.KeyboardArrowDown; else -> Icons.Filled.Remove }
        val topInteractions = remember { MutableInteractionSource() }
        val topPressed by topInteractions.collectIsPressedAsState()
        val topScale by animateFloatAsState(if (topPressed) 0.91f else 1f, tween(140), label = "digital-pill-top-scale")
        val bottomInteractions = remember { MutableInteractionSource() }
        val bottomPressed by bottomInteractions.collectIsPressedAsState()
        val bottomScale by animateFloatAsState(if (bottomPressed) 0.91f else 1f, tween(140), label = "digital-pill-bottom-scale")
        Icon(topIcon, contentDescription = "$title up", tint = Color(0xFF252A29), modifier = Modifier.size(26.dp)
            .graphicsLayer { scaleX = topScale; scaleY = topScale; alpha = if (topPressed) 0.82f else 1f }
            .clickable(interactionSource = topInteractions, indication = null, enabled = enabled, onClick = onTop).padding(2.dp))
        Text(title, color = Color(0xFF252A29), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(bottomIcon, contentDescription = "$title down", tint = Color(0xFF252A29), modifier = Modifier.size(26.dp)
            .graphicsLayer { scaleX = bottomScale; scaleY = bottomScale; alpha = if (bottomPressed) 0.82f else 1f }
            .clickable(interactionSource = bottomInteractions, indication = null, enabled = enabled, onClick = onBottom).padding(2.dp))
    }
}

@Composable
private fun DigitalAction(
    icon: String,
    label: String,
    active: Boolean = false,
    recording: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "recording-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650), repeatMode = RepeatMode.Reverse),
        label = "recording-pulse-scale"
    )
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.91f else if (recording) pulse else 1f, tween(140), label = "digital-action-scale")
    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .height(66.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8F8F5))
            .then(if (active) Modifier.border(1.dp, RadioTvPalette.Red, CircleShape) else Modifier)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (label) {
            "Клавиатура" -> Icon(Icons.Filled.Keyboard, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            "Домой" -> Icon(Icons.Filled.Home, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            "Аэромышь" -> Icon(Icons.Filled.Mouse, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            "Без звука" -> Icon(Icons.Filled.VolumeOff, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            "Микрофон" -> Icon(Icons.Filled.Mic, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            "Назад" -> Icon(Icons.Filled.ArrowBack, contentDescription = label, tint = Color(0xFF252A29), modifier = Modifier.size(22.dp))
            else -> Text(icon, color = Color(0xFF252A29), fontSize = 21.sp, fontWeight = FontWeight.Medium)
        }
        Text(label, color = Color(0xFF252A29), fontSize = 8.sp, maxLines = 1)
    }
}

private fun digitKeyForPanel(number: Int): RemoteKey = when (number) {
    1 -> RemoteKey.NUMBER_1
    2 -> RemoteKey.NUMBER_2
    3 -> RemoteKey.NUMBER_3
    4 -> RemoteKey.NUMBER_4
    5 -> RemoteKey.NUMBER_5
    6 -> RemoteKey.NUMBER_6
    7 -> RemoteKey.NUMBER_7
    8 -> RemoteKey.NUMBER_8
    9 -> RemoteKey.NUMBER_9
    else -> RemoteKey.NUMBER_0
}
