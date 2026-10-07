package com.offex7.streamhub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateBannerV38(
    info: AppUpdateInfo?,
    downloading: Boolean,
    progress: Float,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val energySaving = LocalEnergySaving.current
    val pulse = rememberInfiniteTransition(label = "update-pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (info != null && !downloading && !energySaving) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "update-pulse-value"
    ).value

    AnimatedVisibility(
        visible = info != null,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(10.dp),
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
    ) {
        info?.let { update ->
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232323))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Доступно обновление", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("v${update.versionName}", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(update.changelog, color = Color.LightGray, fontSize = 12.sp)
                    }
                    if (downloading) {
                        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Скачивание: ${(progress.coerceIn(0f, 1f) * 100f).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onUpdate,
                            enabled = !downloading,
                            modifier = Modifier.weight(1f).graphicsLayer(scaleX = pulse, scaleY = pulse),
                            colors = ButtonDefaults.buttonColors(containerColor = if (downloading) Color(0xFF424242) else Color(0xFF2E7D32), disabledContainerColor = Color(0xFF424242))
                        ) { Text(if (downloading) "Скачивание…" else "Обновить", color = Color.White, maxLines = 1) }
                        Button(onClick = onLater, enabled = !downloading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303030))) {
                            Text("Позже", color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
