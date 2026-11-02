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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberSaveable
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

private data class ScrollTopSample(
    val index: Int,
    val offset: Int,
    val isScrolling: Boolean,
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
    val ignored: Boolean
)

@Composable
fun ScrollTopButtonV37(
    listState: LazyListState,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    modifier: Modifier = Modifier,
    ignoreProgrammaticScroll: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The bottom pin is a direct property of the list, so it survives screen recomposition
    // and does not depend on a coroutine or lifecycle event having run at the right moment.
    val pinnedAtBottom by remember(listState) {
        derivedStateOf { listState.canScrollBackward && !listState.canScrollForward }
    }
    var visible by rememberSaveable { mutableStateOf(false) }
    var userScrollActive by remember(listState) { mutableStateOf(false) }
    var scrollSessionId by remember(listState) { mutableLongStateOf(0L) }
    var idleToken by remember(listState) { mutableLongStateOf(0L) }
    var arrowScrollInProgress by remember(listState) { mutableStateOf(false) }
    var initialized by remember(listState) { mutableStateOf(false) }
    val ignoreProgrammaticScrollState by rememberUpdatedState(ignoreProgrammaticScroll)

    // Watch real position changes. This covers touch, wheel, keyboard and D-pad scrolling.
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            ScrollTopSample(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
                isScrolling = listState.isScrollInProgress,
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
                ignored = ignoreProgrammaticScrollState || arrowScrollInProgress
            )
        }.collect { sample ->
            val moved = sample.index != previousIndex || sample.offset != previousOffset

            if (!initialized) {
                initialized = true
                // A previously saved non-top position should not lose the arrow on re-entry.
                if (sample.canScrollBackward && !sample.ignored) visible = true
            }

            when {
                sample.ignored -> {
                    visible = false
                    userScrollActive = false
                    scrollSessionId += 1L
                    idleToken += 1L
                }
                !sample.canScrollBackward -> {
                    visible = false
                    userScrollActive = false
                    scrollSessionId += 1L
                    idleToken += 1L
                }
                sample.isScrolling && moved -> {
                    if (!userScrollActive) {
                        userScrollActive = true
                        scrollSessionId += 1L
                    }
                }
                moved && !sample.isScrolling -> {
                    // A restored list position or an explicit non-user scroll is still real state.
                    visible = sample.canScrollBackward
                    idleToken += 1L
                }
                !sample.isScrolling && userScrollActive -> {
                    userScrollActive = false
                    idleToken += 1L
                }
            }

            // At the last item the arrow is pinned regardless of the 2s/5s timers.
            if (sample.canScrollBackward && !sample.canScrollForward && !sample.ignored) {
                visible = true
                if (userScrollActive) {
                    userScrollActive = false
                    idleToken += 1L
                }
            }

            previousIndex = sample.index
            previousOffset = sample.offset
        }
    }

    // Start a new timer as soon as a physical drag begins, even at the list boundary.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (
                interaction is DragInteraction.Start &&
                !ignoreProgrammaticScrollState &&
                !arrowScrollInProgress
            ) {
                if (!userScrollActive) {
                    userScrollActive = true
                    scrollSessionId += 1L
                }
                if (pinnedAtBottom) visible = false
            }
        }
    }

    // The 2s timer is tied to the start of the scroll session, not to continued movement.
    // It therefore also handles a short swipe that ends before the timer expires.
    LaunchedEffect(scrollSessionId) {
        val session = scrollSessionId
        if (session <= 0L) return@LaunchedEffect
        delay(2_000L)
        if (
            session == scrollSessionId &&
            listState.canScrollBackward &&
            !pinnedAtBottom &&
            !ignoreProgrammaticScrollState &&
            !arrowScrollInProgress
        ) {
            visible = true
        }
    }

    // After motion stops, hide after 5s of inactivity. A new drag cancels this effect.
    LaunchedEffect(idleToken, visible, userScrollActive, pinnedAtBottom) {
        if (!visible || userScrollActive || pinnedAtBottom) return@LaunchedEffect
        val token = idleToken
        delay(5_000L)
        if (
            token == idleToken &&
            visible &&
            !userScrollActive &&
            !pinnedAtBottom &&
            !listState.isScrollInProgress &&
            !ignoreProgrammaticScrollState &&
            !arrowScrollInProgress
        ) {
            visible = false
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v48")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v48-pulse"
    )

    AnimatedVisibility(
        visible = !ignoreProgrammaticScrollState && !arrowScrollInProgress && (visible || pinnedAtBottom),
        modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { width -> width }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
        exit = slideOutHorizontally(targetOffsetX = { width -> width }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
    ) {
        IconButton(
            onClick = {
                InteractionFeedback.vibrate(context, hapticsEnabled, 52L, 165)
                if (soundEnabled) InteractionFeedback.beep(context, true)
                visible = false
                userScrollActive = false
                scrollSessionId += 1L
                idleToken += 1L
                arrowScrollInProgress = true
                scope.launch {
                    try {
                        listState.animateScrollToItem(0)
                    } finally {
                        arrowScrollInProgress = false
                    }
                }
            },
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Вверх", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
