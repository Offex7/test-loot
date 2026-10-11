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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    enabled: Boolean,
    micRecording: Boolean,
    airMouseActive: Boolean,
    keyboardActive: Boolean,
    onKey: (RemoteKey) -> Unit,
    onFeatureAction: (RemoteFeatureAction) -> Unit,
    onSettings: () -> Unit,
    onQuickApp: (String) -> Unit,
    onReturnRemote: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
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
                Text("⚙", color = Color(0xFFB0B0B0), fontSize = 25.sp, modifier = Modifier.clickable(onClick = onSettings).padding(horizontal = 4.dp))
                Text(
                    deviceName.ifBlank { "Телевизор не подключён" },
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF24562A))
                        .clickable(enabled = enabled, onClick = { onKey(RemoteKey.POWER) }),
                    contentAlignment = Alignment.Center
                ) { Text("⏻", color = Color(0xFF58C45F), fontSize = 25.sp) }
            }
        }
        AnimatedVisibility(visible = true, enter = fadeIn(tween(250, delayMillis = 50)) + slideInVertically(tween(250, delayMillis = 50)) { it / 6 }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Top
            ) {
                QuickApp("▶", "YouTube", Color(0xFFE62117), onClick = { onQuickApp("YouTube") }, modifier = Modifier.weight(1f))
                QuickApp("R", "Rutube", Color(0xFF17123F), onClick = { onQuickApp("Rutube") }, modifier = Modifier.weight(1f))
                QuickApp("и", "ИВИ", Color(0xFFE91D42), onClick = { onQuickApp("ИВИ") }, modifier = Modifier.weight(1f))
                QuickApp("К", "Кинопоиск", Color(0xFFF57C00), onClick = { onQuickApp("Кинопоиск") }, modifier = Modifier.weight(1f))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF383838)))
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
                    NumberButton("TV", modifier = Modifier.weight(1f), outlined = true, onClick = { onKey(RemoteKey.SOURCE) })
                    NumberButton("0", modifier = Modifier.weight(1f), onClick = { onKey(RemoteKey.NUMBER_0) })
                    NumberButton("Menu", modifier = Modifier.weight(1f), outlined = true, onClick = { onKey(RemoteKey.MENU) })
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = Color(0xFF555555), fontSize = 10.sp, modifier = Modifier.clickable(onClick = onReturnRemote).padding(horizontal = 3.dp))
            Text("●", color = Color(0xFF2196F3), fontSize = 10.sp, modifier = Modifier.clickable(onClick = { /* already on digit page */ }).padding(horizontal = 3.dp))
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
    }
}

@Composable
private fun QuickApp(icon: String, title: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(150), label = "quick-app-scale")
    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .clip(RoundedCornerShape(11.dp)).clickable(interactionSource = interactions, indication = null, onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, color = Color(0xFFB0B0B0), fontSize = 11.sp, maxLines = 1)
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
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(150), label = "number-key-scale")
    val bg = if (outlined) Color(0xFF1A1A1A) else Color(0xFF343434)
    Box(
        modifier = modifier.height(57.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (outlined) Modifier.border(1.dp, Color(0xFF383838), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontSize = if (title == "Menu") 15.sp else 23.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
        modifier = modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFF343434)).padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(top, color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable(enabled = enabled, onClick = onTop).padding(horizontal = 7.dp))
        Text(title, color = Color(0xFFB0B0B0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(if (title == "VOL") "◖" else "", color = Color(0xFFB0B0B0), fontSize = 9.sp)
        Text(bottom, color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable(enabled = enabled, onClick = onBottom).padding(horizontal = 7.dp))
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
    val scale by animateFloatAsState(if (pressed) 0.94f else if (recording) pulse else 1f, tween(150), label = "digital-action-scale")
    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (pressed) 0.80f else 1f }
            .height(66.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFFE53935) else Color(0xFF333538))
            .clickable(interactionSource = interactions, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Text(label, color = Color(0xFFB0B0B0), fontSize = 8.sp, maxLines = 1)
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
