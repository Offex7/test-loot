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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun ScrollTopButtonV37(
    listState: LazyListState,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var scrollSession by remember { mutableIntStateOf(0) }
    var lastActivityAt by remember { mutableLongStateOf(0L) }

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
                lastActivityAt = 0L
                wasScrolling = scrolling
                return@collect
            }
            lastActivityAt = System.currentTimeMillis()
            if (scrolling && !wasScrolling) scrollSession++
            wasScrolling = scrolling
        }
    }

    LaunchedEffect(scrollSession) {
        if (scrollSession <= 0) return@LaunchedEffect
        val session = scrollSession
        delay(3000L)
        if (
            session == scrollSession &&
            (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        ) {
            visible = true
        }
    }

    LaunchedEffect(lastActivityAt) {
        if (lastActivityAt <= 0L) return@LaunchedEffect
        val activityAt = lastActivityAt
        delay(5000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (activityAt == lastActivityAt && awayFromTop && listState.canScrollForward) {
            visible = false
        } else if (awayFromTop && !listState.canScrollForward) {
            visible = true
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v37")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v37-pulse"
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
                if (hapticsEnabled) InteractionFeedback.vibrate(context, true, 52L, 165)
                if (soundEnabled) InteractionFeedback.beep(context, true)
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .clip(CircleShape)
                .background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Вверх", tint = Color.White)
        }
    }
}
