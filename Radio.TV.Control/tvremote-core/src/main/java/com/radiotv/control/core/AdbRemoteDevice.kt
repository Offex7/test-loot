package com.radiotv.control.core

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface AdbRemoteStatus {
    data object Disconnected : AdbRemoteStatus
    data class Connecting(val host: String) : AdbRemoteStatus
    data class Connected(val host: String, val port: Int) : AdbRemoteStatus
    data class Error(val message: String) : AdbRemoteStatus
}

/**
 * ADB-over-TCP adapter for Android-based Kivi TVs that expose the legacy ADB service.
 * On first use Android may ask the viewer to authorize this device's persistent RSA key.
 */
class AdbRemoteDevice(context: Context) : RemoteDevice, Closeable {
    private val appContext = context.applicationContext
    private val stateMutable = MutableStateFlow<AdbRemoteStatus>(AdbRemoteStatus.Disconnected)
    val status: StateFlow<AdbRemoteStatus> = stateMutable.asStateFlow()
    private val commandMutex = Mutex()

    @Volatile private var connection: AdbConnection? = null

    override suspend fun connect(host: String, port: Int) = withContext(Dispatchers.IO) {
        val target = host.trim()
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        require(port in 1..65535) { "Некорректный порт ADB." }
        disconnectInternal()
        stateMutable.value = AdbRemoteStatus.Connecting(target)
        var socket: Socket? = null
        var candidate: AdbConnection? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(target, port), SOCKET_CONNECT_TIMEOUT_MS)
            candidate = AdbConnection.create(socket, loadOrCreateCrypto())
            val ready = candidate.connect(ADB_HANDSHAKE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS, false)
            check(ready) { ADB_HELP }
            connection = candidate
            stateMutable.value = AdbRemoteStatus.Connected(target, port)
        } catch (error: Exception) {
            runCatching { candidate?.close() }
            runCatching { socket?.close() }
            val message = when (error) {
                is SocketTimeoutException, is IOException -> ADB_HELP
                else -> error.message?.takeIf { it.isNotBlank() } ?: ADB_HELP
            }
            stateMutable.value = AdbRemoteStatus.Error(message)
            throw IllegalStateException(message, error)
        }
    }

    /** Convenience overload that translates the shared remote-key model to Android key codes. */
    suspend fun sendKey(key: RemoteKey) = sendKey(key.toAndroidKeyCode())

    override suspend fun sendKey(keyCode: Int) {
        require(keyCode in 0..KEYCODE_MAX) { "Недопустимый Android keycode." }
        runShell("input keyevent $keyCode")
    }

    override suspend fun sendText(text: String) {
        require(text.isNotEmpty()) { "Введите текст для отправки." }
        // Android's input tool maps %s to a space. Shell quoting prevents arbitrary command execution.
        val encoded = text.replace("%", "%25").replace(" ", "%s")
        runShell("input text '${encoded.replace("'", "'\\''")}'")
    }

    override suspend fun launchApp(packageName: String) {
        val safePackage = packageName.trim()
        require(PACKAGE_NAME.matches(safePackage)) { "Некорректное имя приложения." }
        runShell("monkey -p '$safePackage' 1")
    }

    /** Lists third-party apps reported by Package Manager on the connected TV. */
    suspend fun listInstalledApps(): List<InstalledTvApp> {
        val output = runShellForOutput("pm list packages -3")
        return output.lineSequence()
            .map { it.trim().removePrefix("package:") }
            .filter { PACKAGE_NAME.matches(it) }
            .distinct()
            .map { packageName ->
                val label = when {
                    packageName.contains("youtube", true) -> "YouTube"
                    packageName.contains("netflix", true) -> "Netflix"
                    packageName.contains("ivi", true) -> "ivi"
                    packageName.contains("kinopoisk", true) -> "Кинопоиск"
                    packageName.contains("rutube", true) -> "Rutube"
                    else -> packageName
                }
                InstalledTvApp(id = packageName, name = label)
            }
            .toList()
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
        stateMutable.value = AdbRemoteStatus.Disconnected
    }

    override fun close() {
        disconnectInternal()
    }

    private suspend fun runShell(command: String) {
        runShellForOutput(command)
    }

    private suspend fun runShellForOutput(command: String): String = withContext(Dispatchers.IO) {
        commandMutex.withLock {
            val active = checkNotNull(connection) { ADB_HELP }
            check(stateMutable.value is AdbRemoteStatus.Connected) { ADB_HELP }
            var stream: AdbStream? = null
            val result = ByteArrayOutputStream()
            try {
                val opened = active.open("shell:$command")
                stream = opened
                while (true) {
                    result.write(opened.read())
                }
                @Suppress("UNREACHABLE_CODE")
                result.toString(Charsets.UTF_8.name())
            } catch (closed: IOException) {
                if (stream?.isClosed != true) {
                    stateMutable.value = AdbRemoteStatus.Error(closed.message ?: "ADB-соединение потеряно.")
                    throw IllegalStateException(closed.message ?: "ADB-соединение потеряно.", closed)
                }
                result.toString(Charsets.UTF_8.name())
            } finally {
                runCatching { stream?.close() }
            }
        }
    }

    private fun loadOrCreateCrypto(): AdbCrypto {
        val privateKey = File(appContext.filesDir, PRIVATE_KEY_FILE)
        val publicKey = File(appContext.filesDir, PUBLIC_KEY_FILE)
        val base64 = object : AdbBase64 {
            override fun encodeToString(data: ByteArray): String =
                Base64.encodeToString(data, Base64.NO_WRAP)
        }
        return if (privateKey.isFile && publicKey.isFile) {
            AdbCrypto.loadAdbKeyPair(base64, privateKey, publicKey)
        } else {
            AdbCrypto.generateAdbKeyPair(base64).also { it.saveAdbKeyPair(privateKey, publicKey) }
        }
    }

    private fun disconnectInternal() {
        val previous = connection
        connection = null
        runCatching { previous?.close() }
    }

    private fun RemoteKey.toAndroidKeyCode(): Int = when (this) {
        RemoteKey.UP -> 19
        RemoteKey.DOWN -> 20
        RemoteKey.LEFT -> 21
        RemoteKey.RIGHT -> 22
        RemoteKey.OK -> 23
        RemoteKey.BACK -> 4
        RemoteKey.EXIT -> 111
        RemoteKey.HOME -> 3
        RemoteKey.MENU -> 82
        RemoteKey.POWER -> 26
        RemoteKey.MUTE -> 164
        RemoteKey.VOLUME_UP -> 24
        RemoteKey.VOLUME_DOWN -> 25
        RemoteKey.CHANNEL_UP -> 166
        RemoteKey.CHANNEL_DOWN -> 167
        RemoteKey.SOURCE -> 178
        RemoteKey.PLAY_PAUSE -> 85
        RemoteKey.PLAY -> 126
        RemoteKey.PAUSE -> 127
        RemoteKey.STOP -> 86
        RemoteKey.REWIND -> 89
        RemoteKey.FAST_FORWARD -> 90
        RemoteKey.NUMBER_0 -> 7
        RemoteKey.NUMBER_1 -> 8
        RemoteKey.NUMBER_2 -> 9
        RemoteKey.NUMBER_3 -> 10
        RemoteKey.NUMBER_4 -> 11
        RemoteKey.NUMBER_5 -> 12
        RemoteKey.NUMBER_6 -> 13
        RemoteKey.NUMBER_7 -> 14
        RemoteKey.NUMBER_8 -> 15
        RemoteKey.NUMBER_9 -> 16
    }

    private companion object {
        const val DEFAULT_PORT = 5555
        const val SOCKET_CONNECT_TIMEOUT_MS = 3_000
        const val ADB_HANDSHAKE_TIMEOUT_MS = 12_000
        const val KEYCODE_MAX = 288
        const val PRIVATE_KEY_FILE = "kivi-adbkey"
        const val PUBLIC_KEY_FILE = "kivi-adbkey.pub"
        const val ADB_HELP = "Для управления Kivi включите отладку по ADB в настройках телевизора и разрешите запрос с Radio.TV.Control."
        val PACKAGE_NAME = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")
    }
}
