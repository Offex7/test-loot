package com.offex7.streamhub

import kotlinx.coroutines.awaitCancellation

/** Compatibility helper used by the Compose network-state producer. */
suspend fun awaitDispose(onDispose: () -> Unit) {
    try {
        awaitCancellation()
    } finally {
        onDispose()
    }
}
