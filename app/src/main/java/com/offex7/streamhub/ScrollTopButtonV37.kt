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
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun ScrollTopButtonV37(
    listState: androidx.compose.foundation.lazy.LazyListState,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    modifier: Modifier = Modifier,
    ignoreProgrammaticScroll: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var userDragging by remember { mutableStateOf(false) }
    var userSessionActive by remember { mutableStateOf(false) }
    var userSessionId by remember { mutableLongStateOf(0L) }
    var lastScrollActivityAt by remember { mutableLongStateOf(0L) }
    val ignoreProgrammaticScrollState by rememberUpdatedState(ignoreProgrammaticScroll)

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    if (ignoreProgrammaticScrollState) return@collect
                    userDragging = true
                    if (!userSessionActive) {
                        userSessionActive = true
                        userSessionId += 1L
                    }
                    lastScrollActivityAt = android.os.SystemClock.elapsedRealtime()
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    userDragging = false
                    if (userSessionActive) lastScrollActivityAt = android.os.SystemClock.elapsedRealtime()
                }
            }
        }
    }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, listState.isScrollInProgress)
        }.collect { (index, offset, scrolling) ->
            val awayFromTop = index > 0 || offset > 0
            val moved = index != previousIndex || offset != previousOffset
            when {
                !awayFromTop -> {
                    visible = false
                    userSessionActive = false
                    userSessionId += 1L
                    lastScrollActivityAt = 0L
                }
                ignoreProgrammaticScrollState -> {
                    visible = false
                    userSessionActive = false
                    userSessionId += 1L
                    lastScrollActivityAt = 0L
                }
                userSessionActive && moved && scrolling -> {
                    // Preserve the session start; update only the 5-second idle deadline.
                    lastScrollActivityAt = android.os.SystemClock.elapsedRealtime()
                }
            }
            previousIndex = index
            previousOffset = offset
        }
    }

    LaunchedEffect(userSessionId) {
        if (!userSessionActive || userSessionId <= 0L) return@LaunchedEffect
        val session = userSessionId
        delay(2_000L)
        val awayFromTop = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (session == userSessionId && userSessionActive && listState.isScrollInProgress && awayFromTop && !ignoreProgrammaticScrollState) {
            visible = true
        }
    }

    LaunchedEffect(lastScrollActivityAt) {
        val activityAt = lastScrollActivityAt
        if (activityAt <= 0L) return@LaunchedEffect
        delay(5_000L)
        if (activityAt == lastScrollActivityAt && !userDragging && !listState.isScrollInProgress) {
            if (listState.canScrollForward) {
                visible = false
                userSessionActive = false
                userSessionId += 1L
            } else {
                visible = true
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v46")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v46-pulse"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { width -> width }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
        exit = slideOutHorizontally(targetOffsetX = { width -> width }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
    ) {
        IconButton(
            onClick = {
                InteractionFeedback.vibrate(context, hapticsEnabled, 52L, 165)
                if (soundEnabled) InteractionFeedback.beep(context, true)
                visible = false
                userSessionActive = false
                userDragging = false
                userSessionId += 1L
                lastScrollActivityAt = 0L
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier.size(52.dp).graphicsLayer(scaleX = pulse, scaleY = pulse).background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Вверх", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
