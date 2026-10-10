package com.radiotv.control.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyTest {
    @Test fun allKeysMapToUniqueProtocolValues() {
        assertEquals(RemoteKey.entries.size, RemoteKey.entries.map { it.wireCode }.toSet().size)
    }
    @Test fun basicControlAndMediaKeysExist() {
        assertTrue(RemoteKey.UP in RemoteKey.entries)
        assertTrue(RemoteKey.OK in RemoteKey.entries)
        assertTrue(RemoteKey.PLAY_PAUSE in RemoteKey.entries)
        assertTrue(RemoteKey.SOURCE in RemoteKey.entries)
    }
}
