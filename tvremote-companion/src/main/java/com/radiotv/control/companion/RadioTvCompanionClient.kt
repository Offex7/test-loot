package com.radiotv.control.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

sealed interface CompanionConnectionStatus {
    data object Searching : CompanionConnectionStatus
    data class Connected(val host: String, val serviceName: String) : CompanionConnectionStatus
    data class Unavailable(val message: String) : CompanionConnectionStatus
}

/**
 * Discovers the TV Companion with Android NSD/mDNS and keeps one WebSocket open.
 * Callers can use older remote protocols while this client is searching or unavailable.
 */
class RadioTvCompanionClient(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(NsdManager::class.java)
    private val stateMutable = MutableStateFlow<CompanionConnectionStatus>(CompanionConnectionStatus.Searching)
    val status: StateFlow<CompanionConnectionStatus> = stateMutable.asStateFlow()
    private val closed = AtomicBoolean(false)
    private val resolving = AtomicBoolean(false)
    private val sendMutex = Mutex()
    @Volatile private var socket: Socket? = null
    @Volatile private var connectingSocket: Socket? = null
    @Volatile private var discoveryListener: NsdManager.DiscoveryListener? = null

    @Synchronized
    fun start() {
        if (closed.get() || discoveryListener != null || nsd == null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                stateMutable.value = CompanionConnectionStatus.Searching
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (closed.get() || resolving.get()) return
                if (!serviceInfo.serviceType.contains(RadioTvCompanionServer.SERVICE_TYPE.removeSuffix("."))) return
                if (!resolving.compareAndSet(false, true)) return
                try {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            resolving.set(false)
                            stateMutable.value = CompanionConnectionStatus.Unavailable("Не удалось определить Radio.TV на телевизоре.")
                        }
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress
                            val port = serviceInfo.port
                            if (host.isNullOrBlank() || port <= 0) {
                                resolving.set(false)
                                return
                            }
                            connectSocket(host, port, serviceInfo.serviceName.orEmpty())
                        }
                    })
                } catch (error: Exception) {
                    resolving.set(false)
                    stateMutable.value = CompanionConnectionStatus.Unavailable("Ошибка поиска Companion.")
                    Log.w(TAG, "mDNS resolve failed", error)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                if (stateMutable.value is CompanionConnectionStatus.Connected) {
                    stateMutable.value = CompanionConnectionStatus.Searching
                    runCatching { socket?.close() }
                }
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsd.stopServiceDiscovery(this) }
                discoveryListener = null
                stateMutable.value = CompanionConnectionStatus.Unavailable("Поиск Radio.TV недоступен в локальной сети.")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        try {
            nsd.discoverServices(RadioTvCompanionServer.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: Exception) {
            discoveryListener = null
            stateMutable.value = CompanionConnectionStatus.Unavailable("Не удалось запустить поиск Companion.")
        }
    }

    private fun connectSocket(host: String, port: Int, serviceName: String) {
        Thread({
            var candidate: Socket? = null
            try {
                candidate = Socket()
                connectingSocket = candidate
                candidate.tcpNoDelay = true
                candidate.connect(InetSocketAddress(host, port), 3500)
                val input = BufferedInputStream(candidate.getInputStream())
                val output = candidate.getOutputStream()
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val key = Base64.encodeToString(nonce, Base64.NO_WRAP)
                output.write(
                    ("GET /companion HTTP/1.1\r\n" +
                        "Host: " + host + ":" + port + "\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: " + key + "\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n").toByteArray(Charsets.US_ASCII)
                )
                output.flush()
                val response = readLine(input)
                if (!response.startsWith("HTTP/1.1 101")) throw IllegalStateException("Сервер Companion отклонил соединение.")
                var totalHeaders = response.length
                var acceptHeader: String? = null
                while (true) {
                    val line = readLine(input)
                    totalHeaders += line.length
                    if (totalHeaders > MAX_HEADER_BYTES) throw IllegalStateException("Некорректный ответ Companion.")
                    if (line.isEmpty()) break
                    val divider = line.indexOf(':')
                    if (divider > 0 && line.substring(0, divider).trim().equals("Sec-WebSocket-Accept", ignoreCase = true)) {
                        acceptHeader = line.substring(divider + 1).trim()
                    }
                }
                val expectedAccept = Base64.encodeToString(
                    MessageDigest.getInstance("SHA-1").digest((key + WEBSOCKET_GUID).toByteArray(Charsets.US_ASCII)),
                    Base64.NO_WRAP
                )
                if (acceptHeader != expectedAccept) throw IllegalStateException("Не удалось подтвердить сервер Companion.")
                socket = candidate
                stateMutable.value = CompanionConnectionStatus.Connected(host, serviceName.ifBlank { "Radio.TV.Control" })
                while (!closed.get() && !candidate.isClosed) {
                    val frame = readServerFrame(input) ?: break
                    if (frame.first == 0x8) break
                    if (frame.first == 0x9) writeClientFrame(output, 0xA, frame.second)
                }
            } catch (error: Exception) {
                if (!closed.get()) {
                    stateMutable.value = CompanionConnectionStatus.Unavailable("Соединение с Radio.TV потеряно.")
                    Log.d(TAG, "Companion socket closed", error)
                }
            } finally {
                if (socket === candidate) socket = null
                if (connectingSocket === candidate) connectingSocket = null
                runCatching { candidate?.close() }
                resolving.set(false)
                if (!closed.get()) {
                    stateMutable.value = CompanionConnectionStatus.Searching
                    try { Thread.sleep(RETRY_DELAY_MS) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
                    restartDiscovery()
                }
            }
        }, "RadioTvCompanionClient").apply { isDaemon = true; start() }
    }

    suspend fun sendKeyCode(keyCode: Int) = sendCommand(JSONObject().put("type", "key").put("keyCode", keyCode))
    suspend fun sendText(text: String) = sendCommand(JSONObject().put("type", "text").put("text", text))
    suspend fun launchApp(packageName: String) = sendCommand(JSONObject().put("type", "launch").put("packageName", packageName))

    private suspend fun sendCommand(command: JSONObject) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val active = checkNotNull(socket?.takeIf { it.isConnected && !it.isClosed }) {
                "Radio.TV Companion ещё не подключён."
            }
            writeClientFrame(active.getOutputStream(), 0x1, command.toString().toByteArray(Charsets.UTF_8))
        }
    }

    @Synchronized
    private fun restartDiscovery() {
        if (closed.get()) return
        val manager = nsd ?: return
        val current = discoveryListener
        if (current != null) runCatching { manager.stopServiceDiscovery(current) }
        discoveryListener = null
        start()
    }

    private fun readLine(input: BufferedInputStream): String {
        val line = StringBuilder()
        while (line.length < MAX_HEADER_BYTES) {
            val value = input.read()
            if (value < 0) throw IllegalStateException("Сервер Companion закрыл соединение.")
            if (value == '\n'.code) break
            line.append(value.toChar())
        }
        return line.toString().removeSuffix("\r")
    }

    private fun readServerFrame(input: BufferedInputStream): Pair<Int, ByteArray>? {
        val first = input.read()
        if (first < 0) return null
        val second = input.read()
        if (second < 0) return null
        var length = (second and 0x7F).toLong()
        if (length == 126L) {
            val a = input.read()
            val b = input.read()
            if (a < 0 || b < 0) return null
            length = ((a shl 8) or b).toLong()
        } else if (length == 127L) {
            length = 0L
            repeat(8) {
                val v = input.read()
                if (v < 0) return null
                length = (length shl 8) or v.toLong()
            }
        }
        if (length > MAX_FRAME_BYTES) return null
        val payload = ByteArray(length.toInt())
        var offset = 0
        while (offset < payload.size) {
            val n = input.read(payload, offset, payload.size - offset)
            if (n < 0) return null
            offset += n
        }
        return (first and 0x0F) to payload
    }

    private fun writeClientFrame(output: java.io.OutputStream, opcode: Int, payload: ByteArray) {
        val mask = ByteArray(4).also { SecureRandom().nextBytes(it) }
        output.write(0x80 or (opcode and 0x0F))
        when {
            payload.size < 126 -> output.write(0x80 or payload.size)
            payload.size <= 0xFFFF -> {
                output.write(0x80 or 126)
                output.write((payload.size ushr 8) and 0xFF)
                output.write(payload.size and 0xFF)
            }
            else -> {
                output.write(0x80 or 127)
                for (shift in 56 downTo 0 step 8) output.write((payload.size.toLong() ushr shift).toInt() and 0xFF)
            }
        }
        output.write(mask)
        val masked = ByteArray(payload.size) { index -> (payload[index].toInt() xor mask[index % 4].toInt()).toByte() }
        output.write(masked)
        output.flush()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { discoveryListener?.let { nsd?.stopServiceDiscovery(it) } }
        discoveryListener = null
        runCatching { connectingSocket?.close() }
        connectingSocket = null
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        private const val TAG = "RadioTvCompanion"
        private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        private const val MAX_HEADER_BYTES = 8192
        private const val MAX_FRAME_BYTES = 1024 * 1024
        private const val RETRY_DELAY_MS = 1200L
    }
}
