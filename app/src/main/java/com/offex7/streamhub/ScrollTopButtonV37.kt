package com.offex7.streamhub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScrollTopButtonV37(
    listState: androidx.compose.foundation.lazy.LazyListState,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var scrollSession by remember { mutableLongStateOf(0L) }
    var lastScrollActivityAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (index, offset, scrolling) ->
            val awayFromTop = index > 0 || offset > 0
            if (!awayFromTop) {
                visible = false
                scrollSession = 0L
                lastScrollActivityAt = 0L
                wasScrolling = scrolling
                return@collect
            }

            if (scrolling) {
                lastScrollActivityAt = System.currentTimeMillis()
                if (!wasScrolling) scrollSession += 1L
            }
            wasScrolling = scrolling
        }
    }

    LaunchedEffect(scrollSession) {
        if (scrollSession <= 0L) return@LaunchedEffect
        val session = scrollSession
        delay(2000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (session == scrollSession && awayFromTop) {
            visible = true
        }
    }

    LaunchedEffect(lastScrollActivityAt) {
        if (lastScrollActivityAt <= 0L) return@LaunchedEffect
        val activityAt = lastScrollActivityAt
        delay(5000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (activityAt == lastScrollActivityAt && awayFromTop) {
            visible = !listState.canScrollForward
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v39")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v39-pulse"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { width -> width },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(
            targetOffsetX = { width -> width },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(220))
    ) {
        IconButton(
            onClick = {
                InteractionFeedback.vibrate(context, hapticsEnabled, 52L, 165)
                if (soundEnabled) InteractionFeedback.beep(context, true)
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Вверх", tint = Color.White, modifier = Modifier.size(31.dp))
        }
    }
}
