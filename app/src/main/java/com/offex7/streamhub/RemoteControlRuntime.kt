package com.offex7.streamhub

import android.content.Context
import android.net.Uri
import com.radiotv.control.cast.CastMedia
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.cast.DlnaDevice
import com.radiotv.control.cast.LocalMediaHttpServer
import com.radiotv.control.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.SocketTimeoutException

/** Protocol fallback hub for the integrated Radio.TV remote. */
internal class RemoteControlRuntime(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("radio_tv_remote_runtime", Context.MODE_PRIVATE)

    val remoteV2 = AndroidTvRemoteV2Transport(appContext)
    val adb = AdbRemoteDevice(appContext)
    val bluetooth = BluetoothHidController(appContext)
    val airMouse = GyroAirMouseController(appContext, bluetooth)
    val voice = AndroidTvVoiceInputController(appContext, remoteV2)
    val cast = DlnaCastController(appContext)
    val mediaServer = LocalMediaHttpServer(appContext)

    val savedHost: String
        get() = preferences.getString("last_remote_host", null)?.takeIf { it.isNotBlank() }
            ?: runCatching { remoteV2.pairedHost().orEmpty() }.getOrDefault("")

    private fun messageFor(operation: String, error: Throwable): String = when (error) {
        is SecurityException ->
            "Нет разрешения Android для функции «$operation». Проверьте разрешения Bluetooth и локальной сети."
        is UnsupportedOperationException ->
            "Подключённое устройство не поддерживает функцию «$operation»."
        is SocketTimeoutException ->
            "Истекло время ожидания при выполнении «$operation». Проверьте соединение с ТВ."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Не удалось выполнить «$operation». Проверьте подключение к ТВ."
    }

    private suspend fun <T> guarded(operation: String, action: suspend () -> T): T {
        try {
            return action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: SecurityException) {
            throw IllegalStateException(messageFor(operation, error), error)
        } catch (error: UnsupportedOperationException) {
            throw IllegalStateException(messageFor(operation, error), error)
        } catch (error: Exception) {
            throw IllegalStateException(messageFor(operation, error), error)
        }
    }

    private fun <T> guardedSync(operation: String, action: () -> T): T {
        try {
            return action()
        } catch (error: SecurityException) {
            throw IllegalStateException(messageFor(operation, error), error)
        } catch (error: UnsupportedOperationException) {
            throw IllegalStateException(messageFor(operation, error), error)
        } catch (error: Exception) {
            throw IllegalStateException(messageFor(operation, error), error)
        }
    }

    suspend fun connect(host: String, allowPairing: Boolean): String {
        val target = host.trim().ifBlank { savedHost }
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        preferences.edit().putString("last_remote_host", target).apply()

        if (remoteV2.status.value is RemoteStatus.AwaitingCode) {
            return "Ожидается код сопряжения Android TV Remote v2."
        }

        var remoteError: Throwable? = null
        try {
            guarded("подключение Android TV Remote v2") { remoteV2.connect(target) }
            return "Подключено: Android TV Remote v2"
        } catch (error: Exception) {
            remoteError = error
        }

        if (allowPairing) {
            try {
                guarded("сопряжение Android TV Remote v2") { remoteV2.beginPairing(target) }
                return "На экране телевизора появится шестизначный код сопряжения."
            } catch (error: Exception) {
                remoteError = error
            }
        }

        try {
            guarded("подключение ADB") { adb.connect(target) }
            return "Подключено: ADB"
        } catch (error: Exception) {
            if (bluetooth.status.value is BluetoothHidStatus.Connected) return "Подключено: Bluetooth HID"
            val detail = error.message ?: remoteError?.let { messageFor("подключение", it) }
                ?: "Устройство недоступно."
            return "Нет соединения. $detail"
        }
    }

    suspend fun completePairing(code: String): String = guarded("сопряжение Android TV Remote v2") {
        remoteV2.completePairing(code)
        "Подключено: Android TV Remote v2"
    }

    suspend fun sendKey(key: RemoteKey) = guarded("отправка команды") {
        var lastError: Exception? = null
        val host = savedHost

        if (remoteV2.status.value is RemoteStatus.Connected) {
            try { remoteV2.sendKey(key); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (host.isNotBlank() && remoteV2.status.value !is RemoteStatus.AwaitingCode) {
            try { remoteV2.connect(host); remoteV2.sendKey(key); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) {
            try { adb.connect(host) } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value is AdbRemoteStatus.Connected) {
            try { adb.sendKey(key); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (bluetooth.status.value is BluetoothHidStatus.Connected) {
            try { bluetooth.sendRemoteKey(key); return@guarded } catch (error: Exception) { lastError = error }
        }
        throw IllegalStateException(
            lastError?.let { messageFor("отправка команды", it) }
                ?: "Нет соединения: подключите Android TV Remote v2, ADB или Bluetooth HID.",
            lastError
        )
    }

    suspend fun toggleVoiceInput(): String = guarded("голосовой ввод") {
        if (voice.status.value is VoiceInputStatus.Recording || voice.status.value is VoiceInputStatus.Starting) {
            return@guarded voice.toggle()
        }
        val host = savedHost
        if (remoteV2.status.value !is RemoteStatus.Connected &&
            remoteV2.status.value !is RemoteStatus.AwaitingCode && host.isNotBlank()
        ) {
            runCatching { remoteV2.connect(host) }
        }
        if (remoteV2.status.value is RemoteStatus.Connected) return@guarded voice.toggle()
        if (host.isNotBlank()) {
            if (adb.status.value !is AdbRemoteStatus.Connected) adb.connect(host)
            adb.sendKey(231) // Android KEYCODE_VOICE_ASSIST opens the TV-side assistant.
            return@guarded "Открыт голосовой помощник телевизора. Для передачи микрофона телефона подключитесь по Android TV Remote v2."
        }
        throw IllegalStateException("Голосовой ввод с микрофона телефона требует Android TV Remote v2; для ADB fallback укажите IP телевизора.")
    }

    suspend fun sendText(text: String) = guarded("отправка текста") {
        require(text.isNotBlank()) { "Введите текст." }
        var lastError: Exception? = null
        val host = savedHost
        if (remoteV2.status.value is RemoteStatus.Connected) {
            try { remoteV2.sendText(text); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (host.isNotBlank() && remoteV2.status.value !is RemoteStatus.AwaitingCode) {
            try { remoteV2.connect(host); remoteV2.sendText(text); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) {
            try { adb.connect(host) } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value is AdbRemoteStatus.Connected) {
            try { adb.sendText(text); return@guarded } catch (error: Exception) { lastError = error }
        }
        if (bluetooth.status.value is BluetoothHidStatus.Connected) {
            try { bluetooth.sendText(text); return@guarded } catch (error: Exception) { lastError = error }
        }
        throw IllegalStateException(
            lastError?.let { messageFor("отправка текста", it) } ?: "Нет транспорта для отправки текста.",
            lastError
        )
    }

    suspend fun launchApp(packageName: String) = guarded("запуск приложения на ТВ") {
        val host = savedHost
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) adb.connect(host)
        adb.launchApp(packageName)
    }

    fun startBluetoothHid() = guardedSync("запуск Bluetooth HID") { bluetooth.start() }
    fun toggleAirMouse(): String = guardedSync("Air Mouse") { airMouse.toggle() }
    suspend fun moveMouse(dx: Int, dy: Int) = guarded("управление указателем") { bluetooth.moveMouse(dx, dy) }
    suspend fun clickMouse() = guarded("нажатие указателя") { bluetooth.clickMouse() }

    suspend fun shareLocalMedia(uri: Uri): CastMedia = guarded("подготовка файла для трансляции") {
        withContext(Dispatchers.IO) { mediaServer.start(uri) }
    }

    suspend fun discoverCastDevices(): List<DlnaDevice> =
        guarded("поиск DLNA-устройств") { cast.discoverRenderers() }

    suspend fun castTo(device: DlnaDevice, media: CastMedia) =
        guarded("трансляция DLNA") { cast.play(device, media) }

    override fun close() {
        runCatching { voice.close() }
        runCatching { airMouse.close() }
        runCatching { bluetooth.close() }
        runCatching { adb.close() }
        runCatching { remoteV2.close() }
        runCatching { mediaServer.close() }
    }
}
