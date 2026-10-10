package com.radiotv.control.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyTest {
    @Test fun allKeysMapToUniqueProtocolValues() {
        assertEquals(RemoteKey.entries.size, RemoteKey.entries.map { it.wireCode }.toSet().size)
        @Test fun bluetoothHidMapsNavigationToKeyboardUsages() {
        assertEquals(HidInputCommand.Keyboard(0x52), hidInputFor(RemoteKey.UP))
        assertEquals(HidInputCommand.Keyboard(0x28), hidInputFor(RemoteKey.OK))
        assertEquals(HidInputCommand.Keyboard(0x27), hidInputFor(RemoteKey.NUMBER_0))
    }

    @Test fun bluetoothHidMapsMediaAndVolumeToConsumerUsages() {
        assertEquals(HidInputCommand.Consumer(0x00E9), hidInputFor(RemoteKey.VOLUME_UP))
        assertEquals(HidInputCommand.Consumer(0x00E2), hidInputFor(RemoteKey.MUTE))
        assertEquals(HidInputCommand.Consumer(0x00CD), hidInputFor(RemoteKey.PLAY_PAUSE))
    }

    @Test fun asciiHidMappingSupportsTextAndShift() {
        assertEquals(0x04 to 0, asciiHidUsage('a'))
        assertEquals(0x04 to 0x02, asciiHidUsage('A'))
        assertEquals(0x1E to 0x02, asciiHidUsage('!'))
    }

}
    @Test fun basicControlAndMediaKeysExist() {
        assertTrue(RemoteKey.UP in RemoteKey.entries)
        assertTrue(RemoteKey.OK in RemoteKey.entries)
        assertTrue(RemoteKey.PLAY_PAUSE in RemoteKey.entries)
        assertTrue(RemoteKey.SOURCE in RemoteKey.entries)
    }
}
