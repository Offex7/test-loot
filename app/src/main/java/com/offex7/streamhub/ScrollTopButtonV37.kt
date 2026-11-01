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
import kotlinx.coroutines.flow.collectLatest
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
    var scrollSession by remember { mutableLongStateOf(0L) }
    var lastScrollActivityAt by remember { mutableLongStateOf(0L) }
    var scrollDirection by remember { mutableStateOf(0) }
    val ignoreProgrammaticScrollState by rememberUpdatedState(ignoreProgrammaticScroll)

    LaunchedEffect(listState) {
        var wasScrolling = false
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (index, offset, scrolling) ->
            val awayFromTop = index > 0 || offset > 0
            if (ignoreProgrammaticScrollState || !awayFromTop) {
                visible = false
                scrollSession = 0L
                scrollDirection = 0
                lastScrollActivityAt = 0L
                wasScrolling = scrolling
                previousIndex = index
                previousOffset = offset
                return@collect
            }

            val movedDown = index > previousIndex ||
                (index == previousIndex && offset > previousOffset)
            val movedUp = index < previousIndex ||
                (index == previousIndex && offset < previousOffset)

            if (scrolling && movedDown) {
                lastScrollActivityAt = System.currentTimeMillis()
                if (!wasScrolling || scrollDirection != 1) {
                    scrollSession += 1L
                    visible = false
                }
                scrollDirection = 1
            } else if (scrolling && movedUp) {
                lastScrollActivityAt = System.currentTimeMillis()
                scrollDirection = -1
                scrollSession = 0L
                visible = false
            } else if (!scrolling && !wasScrolling) {
                // No new user movement: leave the current five-second idle timer intact.
            }

            wasScrolling = scrolling
            previousIndex = index
            previousOffset = offset
        }
    }

    LaunchedEffect(scrollSession) {
        if (scrollSession <= 0L) return@LaunchedEffect
        val session = scrollSession
        delay(2_000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (session == scrollSession && scrollDirection == 1 &&
            listState.isScrollInProgress && awayFromTop && !ignoreProgrammaticScrollState
        ) {
            visible = true
        }

        // One debounced timeout coroutine instead of restarting a delay on every scroll frame.
        snapshotFlow { lastScrollActivityAt }.collectLatest { activityAt ->
            if (activityAt <= 0L) return@collectLatest
            delay(5_000L)
            val stillAwayFromTop =
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            if (session == scrollSession && activityAt == lastScrollActivityAt &&
                stillAwayFromTop && listState.canScrollForward && !listState.isScrollInProgress
            ) {
                visible = false
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v45")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v45-pulse"
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
                visible = false
                scrollSession = 0L
                scrollDirection = 0
                scope.launch { listState.animateScrollToItem(0) }
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
