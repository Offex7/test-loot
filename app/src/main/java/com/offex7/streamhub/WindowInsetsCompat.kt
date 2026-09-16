package com.offex7.streamhub

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union as foundationUnion

@Suppress("FunctionName")
fun WindowInsets.union(other: WindowInsets): WindowInsets =
    foundationUnion(this, other)
