package com.radiotv.tvremote.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidTvWireTest {
    @Test fun parsesRemoteConfigure() {
        val parsed = AndroidTvRemoteWire.parse(AndroidTvRemoteWire.configure(1023))
        assertEquals(1023, parsed.configureFeatures)
    }

    @Test fun parsesPingRequest() {
        val payload = ProtoTestMessage.message {
            it.fieldMessage(8, ProtoTestMessage.message { it.fieldVarint(1, 77) })
        }
        val parsed = AndroidTvRemoteWire.parse(payload)
        assertEquals(77, parsed.pingValue)
    }

    @Test fun parsesImeCounters() {
        val payload = ProtoTestMessage.message {
            it.fieldMessage(21, ProtoTestMessage.message {
                it.fieldVarint(1, 12)
                it.fieldVarint(2, 34)
            })
        }
        val parsed = AndroidTvRemoteWire.parse(payload)
        assertEquals(12, parsed.imeCounter)
        assertEquals(34, parsed.fieldCounter)
    }

    @Test fun parsesVoiceBegin() {
        val payload = ProtoTestMessage.message {
            it.fieldMessage(30, ProtoTestMessage.message { it.fieldVarint(1, 99) })
        }
        val parsed = AndroidTvRemoteWire.parse(payload)
        assertEquals(99, parsed.voiceSessionId)
    }

    @Test fun configureDoesNotLookLikePing() {
        assertNull(AndroidTvRemoteWire.parse(AndroidTvRemoteWire.configure(7)).pingValue)
    }

    @Test fun pairingParserRecognizesSecretAck() {
        val outer = ProtoTestMessage.message {
            it.fieldVarint(1, 2)
            it.fieldVarint(2, 200)
            it.fieldMessage(41, ByteArray(32))
        }
        val frame = AndroidTvPairingWire.parse(outer)
        assertEquals(AndroidTvPairingFrame.Kind.SECRET_ACK, frame.kind)
        assertEquals(200, frame.status)
    }

    @Test fun pairingParserRejectsNonOkStatus() {
        val outer = ProtoTestMessage.message {
            it.fieldVarint(1, 2)
            it.fieldVarint(2, 402)
        }
        val frame = AndroidTvPairingWire.parse(outer)
        assertEquals(AndroidTvPairingFrame.Kind.OTHER, frame.kind)
        assertEquals(402, frame.status)
    }

    private object ProtoTestMessage {
        fun message(block: (Writer) -> Unit): ByteArray = Writer().apply(block).bytes()

        class Writer {
            private val out = java.io.ByteArrayOutputStream()

            fun fieldVarint(number: Int, value: Int) {
                varint(number.toLong() shl 3)
                varint(value.toLong())
            }

            fun fieldMessage(number: Int, value: ByteArray) {
                varint((number.toLong() shl 3) or 2L)
                varint(value.size.toLong())
                out.write(value)
            }

            private fun varint(value: Long) {
                var v = value
                while ((v and 0x7F.inv().toLong()) != 0L) {
                    out.write(((v and 0x7F) or 0x80).toInt())
                    v = v ushr 7
                }
                out.write(v.toInt())
            }

            fun bytes(): ByteArray = out.toByteArray()
        }
    }
}
