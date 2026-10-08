package com.radiotv.tvremote.core

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FramingTest {
    @Test fun roundTripSmallPayload() {
        val out = ByteArrayOutputStream()
        Framing.writeMessage(out, "hello".toByteArray())
        val result = Framing.readMessage(ByteArrayInputStream(out.toByteArray()))
        assertArrayEquals("hello".toByteArray(), result)
    }

    @Test fun roundTripPayloadLargerThan127Bytes() {
        val payload = ByteArray(300) { (it and 0xff).toByte() }
        val out = ByteArrayOutputStream()
        Framing.writeMessage(out, payload)
        val result = Framing.readMessage(ByteArrayInputStream(out.toByteArray()))
        assertArrayEquals(payload, result)
    }
}
