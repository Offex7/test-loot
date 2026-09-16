package androidx.compose.runtime

import androidx.compose.runtime.saveable.rememberSaveable as saveableRememberSaveable

@Composable
fun <T : Any> rememberSaveable(vararg inputs: Any?, init: () -> T): T =
    saveableRememberSaveable(*inputs, init = init)
