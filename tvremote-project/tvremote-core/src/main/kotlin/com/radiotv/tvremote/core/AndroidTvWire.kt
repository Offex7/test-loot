package com.radiotv.tvremote.core

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

private class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varint(value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    fun fieldVarint(number: Int, value: Long) {
        varint((number.toLong() shl 3) or 0L)
        varint(value)
    }

    fun fieldBytes(number: Int, value: ByteArray) {
        varint((number.toLong() shl 3) or 2L)
        varint(value.size.toLong())
        out.write(value)
    }

    fun fieldString(number: Int, value: String) =
        fieldBytes(number, value.toByteArray(StandardCharsets.UTF_8))

    fun fieldMessage(number: Int, value: ByteArray) = fieldBytes(number, value)

    fun bytes(): ByteArray = out.toByteArray()
}

private class ProtoReader(private val data: ByteArray) {
    private var pos = 0

    fun nextField(): Pair<Int, Int>? {
        if (pos >= data.size) return null
        val tag = readVarint().toInt()
        return (tag ushr 3) to (tag and 7)
    }

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift <= 63) {
            if (pos >= data.size) throw RemoteException("Unexpected end of protobuf varint")
            val b = data[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw RemoteException("Invalid protobuf varint")
    }

    fun readBytes(): ByteArray {
        val length = readVarint().toInt()
        if (length < 0 || pos + length > data.size) {
            throw RemoteException("Invalid protobuf length: $length")
        }
        return data.copyOfRange(pos, pos + length).also { pos += length }
    }

    fun readString(): String = String(readBytes(), StandardCharsets.UTF_8)

    fun skip(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> skipBytes(8)
            2 -> skipBytes(readVarint().toInt())
            5 -> skipBytes(4)
            else -> throw RemoteException("Unsupported protobuf wire type: $wireType")
        }
    }

    private fun skipBytes(count: Int) {
        if (count < 0 || pos + count > data.size) {
            throw RemoteException("Invalid protobuf skip length: $count")
        }
        pos += count
    }
}

data class AndroidTvPairingFrame(
    val kind: Kind,
    val status: Int = 200
) {
    enum class Kind {
        PAIRING_REQUEST_ACK,
        OPTIONS,
        CONFIGURATION_ACK,
        SECRET_ACK,
        OTHER
    }
}

object AndroidTvPairingWire {
    private const val STATUS_OK = 200

    fun pairingRequest(): ByteArray = outerMessage(
        fieldNumber = 10,
        payload = message {
            it.fieldString(1, "atvremote")
            it.fieldString(2, "Radio.TV Remote")
        }
    )

    fun options(): ByteArray = outerMessage(
        fieldNumber = 20,
        payload = message {
            it.fieldMessage(1, message {
                it.fieldVarint(1, 4) // HEXADECIMAL
                it.fieldVarint(2, 6)
            })
            it.fieldVarint(3, 1) // INPUT
        }
    )

    fun configuration(): ByteArray = outerMessage(
        fieldNumber = 30,
        payload = message {
            it.fieldMessage(1, message {
                it.fieldVarint(1, 4)
                it.fieldVarint(2, 6)
            })
            it.fieldVarint(2, 1) // INPUT
        }
    )

    fun secret(secret: ByteArray): ByteArray = outerMessage(
        fieldNumber = 40,
        payload = message { it.fieldBytes(1, secret) }
    )

    fun parse(data: ByteArray): AndroidTvPairingFrame {
        val reader = ProtoReader(data)
        var status = STATUS_OK
        var kind = AndroidTvPairingFrame.Kind.OTHER

        while (true) {
            val field = reader.nextField() ?: break
            when (field.first) {
                2 -> status = reader.readVarint().toInt()
                11 -> {
                    reader.readBytes()
                    kind = AndroidTvPairingFrame.Kind.PAIRING_REQUEST_ACK
                }
                20 -> {
                    reader.readBytes()
                    kind = AndroidTvPairingFrame.Kind.OPTIONS
                }
                31 -> {
                    reader.readBytes()
                    kind = AndroidTvPairingFrame.Kind.CONFIGURATION_ACK
                }
                41 -> {
                    reader.readBytes()
                    kind = AndroidTvPairingFrame.Kind.SECRET_ACK
                }
                else -> reader.skip(field.second)
            }
        }
        return AndroidTvPairingFrame(kind, status)
    }

    private fun outerMessage(fieldNumber: Int, payload: ByteArray): ByteArray =
        message {
            it.fieldVarint(1, 2)
            it.fieldVarint(2, STATUS_OK.toLong())
            it.fieldMessage(fieldNumber, payload)
        }

    private fun message(block: (ProtoWriter) -> Unit): ByteArray =
        ProtoWriter().apply(block).bytes()
}

data class AndroidTvRemoteInbound(
    val configureFeatures: Int? = null,
    val started: Boolean? = null,
    val pingValue: Int? = null,
    val imeCounter: Int? = null,
    val fieldCounter: Int? = null,
    val voiceSessionId: Int? = null
)

object AndroidTvRemoteWire {
    private const val STATUS_UNUSED = 0

    fun parse(data: ByteArray): AndroidTvRemoteInbound {
        val r = ProtoReader(data)
        var configureFeatures: Int? = null
        var started: Boolean? = null
        var pingValue: Int? = null
        var imeCounter: Int? = null
        var fieldCounter: Int? = null
        var voiceSessionId: Int? = null

        while (true) {
            val field = r.nextField() ?: break
            when (field.first) {
                1 -> parseConfigure(r.readBytes())?.let { configureFeatures = it }
                8 -> parsePingRequest(r.readBytes())?.let { pingValue = it }
                30 -> parseVoiceBegin(r.readBytes())?.let { voiceSessionId = it }
                21 -> {
                    val pair = parseImeBatch(r.readBytes())
                    if (pair != null) {
                        imeCounter = pair.first
                        fieldCounter = pair.second
                    }
                }
                40 -> {
                    val nested = ProtoReader(r.readBytes())
                    var value: Boolean? = null
                    while (true) {
                        val nestedField = nested.nextField() ?: break
                        when (nestedField.first) {
                            1 -> value = nested.readVarint() != 0L
                            else -> nested.skip(nestedField.second)
                        }
                    }
                    started = value
                }
                else -> r.skip(field.second)
            }
        }

        return AndroidTvRemoteInbound(
            configureFeatures = configureFeatures,
            started = started,
            pingValue = pingValue,
            imeCounter = imeCounter,
            fieldCounter = fieldCounter,
            voiceSessionId = voiceSessionId
        )
    }

    fun configure(features: Int): ByteArray = remoteMessage(
        1,
        message {
            it.fieldVarint(1, features.toLong())
            it.fieldMessage(2, message {
                it.fieldString(1, "Radio.TV Remote")
                it.fieldString(2, "Radio.TV")
                it.fieldVarint(3, 1)
                it.fieldString(4, "1")
                it.fieldString(5, "com.radiotv.tvremote")
                it.fieldString(6, "1.0.0")
            })
        }
    )

    fun pingResponse(value: Int): ByteArray = remoteMessage(
        9,
        message { it.fieldVarint(1, value.toLong()) }
    )

    fun keyInject(keyCode: Int, direction: Int): ByteArray = remoteMessage(
        10,
        message {
            it.fieldVarint(1, keyCode.toLong())
            it.fieldVarint(2, direction.toLong())
        }
    )

    fun imeBatchEdit(imeCounter: Int, fieldCounter: Int, text: String): ByteArray {
        val editInfo = message {
            it.fieldVarint(1, 1)
            it.fieldMessage(2, message {
                val caret = (text.length - 1).coerceAtLeast(0)
                it.fieldVarint(1, caret.toLong())
                it.fieldVarint(2, caret.toLong())
                it.fieldString(3, text)
            })
        }
        return remoteMessage(
            21,
            message {
                it.fieldVarint(1, imeCounter.toLong())
                it.fieldVarint(2, fieldCounter.toLong())
                it.fieldMessage(3, editInfo)
            }
        )
    }

    fun voiceBegin(sessionId: Int): ByteArray = remoteMessage(
        30,
        message {
            it.fieldVarint(1, sessionId.toLong())
            it.fieldString(2, "com.radiotv.tvremote")
        }
    )

    fun voicePayload(sessionId: Int, samples: ByteArray): ByteArray = remoteMessage(
        31,
        message {
            it.fieldVarint(1, sessionId.toLong())
            it.fieldBytes(2, samples)
        }
    )

    fun voiceEnd(sessionId: Int): ByteArray = remoteMessage(
        32,
        message { it.fieldVarint(1, sessionId.toLong()) }
    )

    private fun remoteMessage(fieldNumber: Int, payload: ByteArray): ByteArray =
        message { it.fieldMessage(fieldNumber, payload) }

    private fun message(block: (ProtoWriter) -> Unit): ByteArray =
        ProtoWriter().apply(block).bytes()

    private fun parseConfigure(data: ByteArray): Int? {
        val r = ProtoReader(data)
        var features: Int? = null
        while (true) {
            val field = r.nextField() ?: break
            when (field.first) {
                1 -> features = r.readVarint().toInt()
                else -> r.skip(field.second)
            }
        }
        return features
    }

    private fun parsePingRequest(data: ByteArray): Int? {
        val r = ProtoReader(data)
        var value: Int? = null
        while (true) {
            val field = r.nextField() ?: break
            when (field.first) {
                1 -> value = r.readVarint().toInt()
                else -> r.skip(field.second)
            }
        }
        return value
    }

    private fun parseVoiceBegin(data: ByteArray): Int? {
        val r = ProtoReader(data)
        var sessionId: Int? = null
        while (true) {
            val field = r.nextField() ?: break
            when (field.first) {
                1 -> sessionId = r.readVarint().toInt()
                else -> r.skip(field.second)
            }
        }
        return sessionId
    }

    private fun parseImeBatch(data: ByteArray): Pair<Int, Int>? {
        val r = ProtoReader(data)
        var ime: Int? = null
        var field: Int? = null
        while (true) {
            val next = r.nextField() ?: break
            when (next.first) {
                1 -> ime = r.readVarint().toInt()
                2 -> field = r.readVarint().toInt()
                else -> r.skip(next.second)
            }
        }
        return if (ime != null && field != null) ime!! to field!! else null
    }
}
