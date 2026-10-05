package com.radiotv.tvremote.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteKeyTest {
    @Test
    fun stableEnumNames() {
        assertEquals(RemoteKey.OK, RemoteKey.valueOf("OK"))
        assertEquals(RemoteKey.DIGIT_9, RemoteKey.valueOf("DIGIT_9"))
    }
}
