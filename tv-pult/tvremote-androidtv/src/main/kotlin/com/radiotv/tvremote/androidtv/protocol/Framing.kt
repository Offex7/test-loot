package com.radiotv.tvremote.androidtv.protocol

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Both channels of the Android TV remote protocol (pairing on 6467, remote on 6466) frame
 * every protobuf message with a base-128 varint length prefix followed by the raw message
 * bytes.
 *
 * Note this is a *varint*, not a single length byte: `remote_configure` and app-link
 * messages routinely exceed 127 bytes, at which point a one-byte prefix silently desyncs
 * the stream.
 */
internal object Framing {

    /** Refuse absurd lengths rather than trying to allocate them; the stream is untrusted. */
    private const val MAX_MESSAGE_BYTES = 1 shl 20

    fun encodeVarint(value: Int): ByteArray {
        require(value >= 0) { "Length must be non-negative, was $value" }
        val bytes = ArrayList<Byte>(5)
        var remaining = value
        while (true) {
            if (remaining and 0x7F.inv() == 0) {
                bytes.add(remaining.toByte())
                return bytes.toByteArray()
            }
            bytes.add(((remaining and 0x7F) or 0x80).toByte())
            remaining = remaining ushr 7
        }
    }

    /** Returns the decoded value, or null at a clean end of stream between messages. */
    fun readVarint(input: InputStream): Int? {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input.read()
            if (b < 0) {
                if (shift == 0) return null
                throw EOFException("Stream ended part-way through a length prefix")
            }
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        throw IOException("Length prefix exceeded 32 bits")
    }

    fun writeMessage(output: OutputStream, payload: ByteArray) {
        output.write(encodeVarint(payload.size))
        output.write(payload)
        output.flush()
    }

    /** Returns the message body, or null at a clean end of stream. */
    fun readMessage(input: InputStream): ByteArray? {
        val length = readVarint(input) ?: return null
        if (length > MAX_MESSAGE_BYTES) {
            throw IOException("Implausible message length $length; stream is probably desynced")
        }
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            // A single read() can return a partial message: TCP splits and coalesces freely.
            val n = input.read(buffer, read, length - read)
            if (n < 0) throw EOFException("Stream ended $read/$length bytes into a message")
            read += n
        }
        return buffer
    }
}
