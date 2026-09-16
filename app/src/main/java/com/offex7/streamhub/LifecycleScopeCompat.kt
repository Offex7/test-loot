package com.offex7.streamhub

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleCoroutineScope

/** Keeps MainActivity's unqualified lifecycleScope reference valid without importing the extension. */
val ComponentActivity.lifecycleScope: LifecycleCoroutineScope
    get() = androidx.lifecycle.LifecycleOwnerKt.getLifecycleScope(this)
