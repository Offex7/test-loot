package com.radiotv.tvremote.core
import java.io.InputStream
import java.io.OutputStream
object Framing {
    fun writeMessage(output: OutputStream, payload: ByteArray) {
        var value = payload.size
        while (value and 0x7f != 0) {
            output.write((value and 0x7f) or 0x80)
            value = value ushr 7
        }
        output.write(value)
        output.write(payload)
        output.flush()
    }
    fun readMessage(input: InputStream): ByteArray? {
        var shift = 0
        var size = 0
        while (shift <= 28) {
            val b = input.read()
            if (b < 0) return null
            size = size or ((b and 0x7f) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        if (size < 0 || size > 2_000_000) throw RemoteException("Invalid framed message length: " + size)
        val data = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(data, offset, size - offset)
            if (read < 0) throw RemoteException("Unexpected end of remote message")
            offset += read
        }
        return data
    }
}
