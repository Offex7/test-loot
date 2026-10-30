package com.offex7.streamhub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
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
    val maxPanelHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.62f).coerceAtLeast(220.dp)
    AnimatedVisibility(
        visible = info != null,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout)).padding(10.dp),
        enter = fadeIn(animationSpec = tween(160)),
        exit = fadeOut(animationSpec = tween(160))
    ) {
        info?.let { update ->
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxPanelHeight),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232323))
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Доступно обновление", color = Color.White, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
                    Text("v" + update.versionName, color = Color(0xFF81C784), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                    Text(update.changelog, color = Color.LightGray, fontSize = 12.sp, lineHeight = 17.sp, overflow = TextOverflow.Clip)
                    if (downloading) {
                        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Скачивание: " + (progress.coerceIn(0f, 1f) * 100f).toInt() + "%", color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onUpdate,
                            enabled = !downloading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color(0xFF424242))
                        ) {
                            Text(if (downloading) "Скачивание…" else "Обновить", color = Color.White, maxLines = 1, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                        Button(
                            onClick = onLater,
                            enabled = !downloading,
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303030))
                        ) {
                            Text("Позже", color = Color.White, maxLines = 1, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
