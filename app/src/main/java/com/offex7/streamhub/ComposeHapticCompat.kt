package androidx.compose.ui.hapticfeedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalHapticFeedback as PlatformLocalHapticFeedback

object LocalHapticFeedback {
    val current: HapticFeedback
        @Composable get() = PlatformLocalHapticFeedback.current
}
