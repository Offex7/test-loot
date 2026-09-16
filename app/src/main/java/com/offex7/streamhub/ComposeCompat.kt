package com.offex7.streamhub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable as runtimeRememberSaveable
import androidx.compose.foundation.layout.WindowInsets

/** Compatibility bridge for the current Compose runtime-saveable API. */
@Composable
fun <T> rememberSaveableCompat(vararg inputs: Any?, init: () -> T): T =
    runtimeRememberSaveable(*inputs, init = init)

/** Combines insets without depending on a version-specific extension import. */
fun WindowInsets.combinedWith(other: WindowInsets): WindowInsets =
    androidx.compose.foundation.layout.WindowInsets(left = maxOf(this.getLeft(androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.unit.IntSize.Zero), 0), top = maxOf(this.getTop(androidx.compose.ui.unit.LayoutDirection.Ltr), other.getTop(androidx.compose.ui.unit.LayoutDirection.Ltr)), right = maxOf(this.getRight(androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.unit.IntSize.Zero), other.getRight(androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.unit.IntSize.Zero)), bottom = maxOf(this.getBottom(androidx.compose.ui.unit.IntSize.Zero), other.getBottom(androidx.compose.ui.unit.IntSize.Zero)))
