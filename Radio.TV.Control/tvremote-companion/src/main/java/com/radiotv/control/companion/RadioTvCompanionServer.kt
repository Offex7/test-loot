package com.radiotv.control.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LAN-only RFC 6455 endpoint used by Radio.TV.Control on a TV.
 * Only structured key/text/app commands are accepted; arbitrary shell commands are never exposed.
 */
class RadioTvCompanionServer(
    context: Context,
    private val onCommand: (JSONObject) -> Unit
) : Closeable {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(NsdManager::class.java)
    private val running = AtomicBoolean(false)
    private val workers: ExecutorService = Executors.newCachedThreadPool()
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var registrationListener: NsdManager.RegistrationListener? = null

    @Synchronized
    fun start() {
        if (running.get()) {
            if (registrationListener == null) registerService()
            return
        }
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(PORT))
        serverSocket = socket
        running.set(true)
        registerService()
        workers.execute {
            while (running.get()) {
                try {
                    val peer = socket.accept()
                    peer.tcpNoDelay = true
                    workers.execute { servePeer(peer) }
                } catch (error: Exception) {
                    if (running.get()) Log.w(TAG, "Companion accept failed", error)
                }
            }
        }
    }

    private fun registerService() {
        val manager = nsd ?: return
        val info = NsdServiceInfo().apply {
            serviceName = (SERVICE_NAME + " - " + Build.MODEL.trim()).take(63)
            serviceType = SERVICE_TYPE
            port = PORT
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Companion service registered")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
                Log.w(TAG, "Companion mDNS registration failed: " + errorCode)
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Companion mDNS unregister failed: " + errorCode)
            }
        }
        registrationListener = listener
        runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                registrationListener = null
                Log.w(TAG, "Unable to register Companion mDNS service", it)
            }
    }

    private fun servePeer(peer: Socket) {
        peer.use { client ->
            try {
                val input = BufferedInputStream(client.getInputStream())
                val output = client.getOutputStream()
                val headers = readHeaders(input) ?: return
                val key = headers.entries.firstOrNull { it.key.equals("sec-websocket-key", true) }?.value
                    ?: return
                val accept = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1").digest((key.trim() + WEBSOCKET_GUID).toByteArray(Charsets.US_ASCII))
                )
                output.write(
                    ("HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").toByteArray(Charsets.US_ASCII)
                )
                output.flush()
                while (running.get() && !client.isClosed) {
                    val frame = readFrame(input) ?: break
                    when (frame.first) {
                        0x8 -> {
                            writeFrame(output, 0x8, byteArrayOf())
                            break
                        }
                        0x9 -> writeFrame(output, 0xA, frame.second)
                        0x1 -> {
                            val json = runCatching { JSONObject(String(frame.second, Charsets.UTF_8)) }.getOrNull()
                                ?: continue
                            runCatching { onCommand(json) }
                                .onFailure { Log.w(TAG, "Companion command rejected", it) }
                        }
                        else -> Unit
                    }
                }
            } catch (error: Exception) {
                if (running.get()) Log.d(TAG, "Companion peer disconnected", error)
            }
        }
    }

    private fun readHeaders(input: InputStream): Map<String, String>? {
        val all = StringBuilder()
        var previous = 0
        var count = 0
        while (count++ < MAX_HEADER_BYTES) {
            val next = input.read()
            if (next < 0) return null
            all.append(next.toChar())
            if (previous == '\r'.code && next == '\n'.code && all.toString().endsWith("\r\n\r\n")) break
            previous = next
        }
        if (count >= MAX_HEADER_BYTES) return null
        val lines = all.toString().split("\r\n")
        if (lines.firstOrNull()?.startsWith("GET ") != true) return null
        return lines.drop(1).mapNotNull { line ->
            val split = line.indexOf(':')
            if (split <= 0) null else line.substring(0, split).trim() to line.substring(split + 1).trim()
        }.toMap()
    }

    private fun readFrame(input: InputStream): Pair<Int, ByteArray>? {
        val first = input.read()
        if (first < 0) return null
        val second = input.read()
        if (second < 0) return null
        val opcode = first and 0x0F
        val masked = (second and 0x80) != 0
        var length = (second and 0x7F).toLong()
        if (length == 126L) {
            val a = input.read()
            val b = input.read()
            if (a < 0 || b < 0) return null
            length = ((a shl 8) or b).toLong()
        } else if (length == 127L) {
            length = 0L
            repeat(8) {
                val value = input.read()
                if (value < 0) return null
                length = (length shl 8) or value.toLong()
            }
        }
        if (length < 0 || length > MAX_FRAME_BYTES) return null
        val mask = if (masked) ByteArray(4).also { if (!input.readFullyOrFalse(it)) return null } else null
        val payload = ByteArray(length.toInt())
        if (!input.readFullyOrFalse(payload)) return null
        if (mask != null) for (index in payload.indices) {
            payload[index] = (payload[index].toInt() xor mask[index % 4].toInt()).toByte()
        }
        return opcode to payload
    }

    private fun writeFrame(output: java.io.OutputStream, opcode: Int, payload: ByteArray) {
        output.write(0x80 or (opcode and 0x0F))
        when {
            payload.size < 126 -> output.write(payload.size)
            payload.size <= 0xFFFF -> {
                output.write(126)
                output.write((payload.size ushr 8) and 0xFF)
                output.write(payload.size and 0xFF)
            }
            else -> {
                output.write(127)
                for (shift in 56 downTo 0 step 8) output.write((payload.size.toLong() ushr shift).toInt() and 0xFF)
            }
        }
        output.write(payload)
        output.flush()
    }

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        runCatching { serverSocket?.close() }
        serverSocket = null
        val listener = registrationListener
        registrationListener = null
        if (listener != null) runCatching { nsd?.unregisterService(listener) }
        workers.shutdownNow()
    }

    private fun InputStream.readFullyOrFalse(target: ByteArray): Boolean {
        var offset = 0
        while (offset < target.size) {
            val read = read(target, offset, target.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }

    companion object {
        const val PORT = 8089
        const val SERVICE_TYPE = "_radiotvcontrol._tcp."
        const val SERVICE_NAME = "Radio.TV.Control"
        private const val TAG = "RadioTvCompanion"
        private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        private const val MAX_HEADER_BYTES = 8192
        private const val MAX_FRAME_BYTES = 1024 * 1024
    }
}
