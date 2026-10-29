package com.radiotv.control.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyTest {
    @Test fun allKeysMapToUniqueProtocolValues() {
        assertEquals(RemoteKey.entries.size, RemoteKey.entries.map { it.wireCode }.toSet().size)
    }

    @Test fun androidKeycodeMappingIsSharedWithCompanionIntegrations() {
        assertEquals(19, RemoteKey.UP.toAndroidKeyCode())
        assertEquals(20, RemoteKey.DOWN.toAndroidKeyCode())
        assertEquals(21, RemoteKey.LEFT.toAndroidKeyCode())
        assertEquals(22, RemoteKey.RIGHT.toAndroidKeyCode())
        assertEquals(23, RemoteKey.OK.toAndroidKeyCode())
        assertEquals(4, RemoteKey.BACK.toAndroidKeyCode())
        assertEquals(26, RemoteKey.POWER.toAndroidKeyCode())
        assertEquals(24, RemoteKey.VOLUME_UP.toAndroidKeyCode())
        assertEquals(25, RemoteKey.VOLUME_DOWN.toAndroidKeyCode())
        assertEquals(166, RemoteKey.CHANNEL_UP.toAndroidKeyCode())
        assertEquals(167, RemoteKey.CHANNEL_DOWN.toAndroidKeyCode())
        assertEquals(85, RemoteKey.PLAY_PAUSE.toAndroidKeyCode())
        assertEquals(7, RemoteKey.NUMBER_0.toAndroidKeyCode())
        assertEquals(16, RemoteKey.NUMBER_9.toAndroidKeyCode())
    }

    @Test fun basicControlAndMediaKeysExist() {
        assertTrue(RemoteKey.UP in RemoteKey.entries)
        assertTrue(RemoteKey.OK in RemoteKey.entries)
        assertTrue(RemoteKey.EXIT in RemoteKey.entries)
        assertTrue(RemoteKey.PLAY_PAUSE in RemoteKey.entries)
        assertTrue(RemoteKey.PLAY in RemoteKey.entries)
        assertTrue(RemoteKey.PAUSE in RemoteKey.entries)
        assertTrue(RemoteKey.SOURCE in RemoteKey.entries)
    }

    @Test fun bluetoothHidMapsNavigationToKeyboardUsages() {
        assertEquals(HidInputCommand.Keyboard(0x52), hidInputFor(RemoteKey.UP))
        assertEquals(HidInputCommand.Keyboard(0x28), hidInputFor(RemoteKey.OK))
        assertEquals(HidInputCommand.Keyboard(0x29), hidInputFor(RemoteKey.EXIT))
        assertEquals(HidInputCommand.Keyboard(0x27), hidInputFor(RemoteKey.NUMBER_0))
    }

    @Test fun bluetoothHidMapsMediaAndVolumeToConsumerUsages() {
        assertEquals(HidInputCommand.Consumer(0x00E9), hidInputFor(RemoteKey.VOLUME_UP))
        assertEquals(HidInputCommand.Consumer(0x00E2), hidInputFor(RemoteKey.MUTE))
        assertEquals(HidInputCommand.Consumer(0x00CD), hidInputFor(RemoteKey.PLAY_PAUSE))
        assertEquals(HidInputCommand.Consumer(0x00B0), hidInputFor(RemoteKey.PLAY))
        assertEquals(HidInputCommand.Consumer(0x00B1), hidInputFor(RemoteKey.PAUSE))
    }

    @Test fun asciiHidMappingSupportsTextAndShift() {
        assertEquals(0x04 to 0, asciiHidUsage('a'))
        assertEquals(0x04 to 0x02, asciiHidUsage('A'))
        assertEquals(0x1E to 0x02, asciiHidUsage('!'))
    }
}
