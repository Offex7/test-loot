package com.offex7.streamhub

import androidx.compose.foundation.layout.WindowInsets

@Suppress("FunctionName")
fun WindowInsets.union(other: WindowInsets): WindowInsets =
    androidx.compose.foundation.layout.WindowInsets.union(this, other)
