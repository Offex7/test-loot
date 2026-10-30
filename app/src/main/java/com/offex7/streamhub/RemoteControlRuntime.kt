package com.offex7.streamhub

import android.content.Context
import android.net.Uri
import com.radiotv.control.cast.CastMedia
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.cast.DlnaDevice
import com.radiotv.control.cast.LocalMediaHttpServer
import com.radiotv.control.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

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
            ?: remoteV2.pairedHost().orEmpty()

    suspend fun connect(host: String, allowPairing: Boolean): String {
        val target = host.trim().ifBlank { savedHost }
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        preferences.edit().putString("last_remote_host", target).apply()

        if (remoteV2.status.value is RemoteStatus.AwaitingCode) {
            return "Ожидается код сопряжения Android TV Remote v2."
        }

        var remoteError: String? = null
        try {
            remoteV2.connect(target)
            return "Подключено: Android TV Remote v2"
        } catch (error: Exception) {
            remoteError = error.message
        }

        if (allowPairing) {
            try {
                remoteV2.beginPairing(target)
                return "На экране телевизора появится шестизначный код сопряжения."
            } catch (error: Exception) {
                remoteError = error.message ?: remoteError
            }
        }

        try {
            adb.connect(target)
            return "Подключено: ADB"
        } catch (error: Exception) {
            val detail = error.message ?: remoteError ?: "Устройство недоступно."
            if (bluetooth.status.value is BluetoothHidStatus.Connected) return "Подключено: Bluetooth HID"
            return "Нет соединения. $detail"
        }
    }

    suspend fun completePairing(code: String): String {
        remoteV2.completePairing(code)
        return "Подключено: Android TV Remote v2"
    }

    suspend fun sendKey(key: RemoteKey) {
        var lastError: Exception? = null
        val host = savedHost

        if (remoteV2.status.value is RemoteStatus.Connected) {
            try { remoteV2.sendKey(key); return } catch (error: Exception) { lastError = error }
        }
        if (host.isNotBlank() && remoteV2.status.value !is RemoteStatus.AwaitingCode) {
            try { remoteV2.connect(host); remoteV2.sendKey(key); return } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) {
            try { adb.connect(host) } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value is AdbRemoteStatus.Connected) {
            try { adb.sendKey(key); return } catch (error: Exception) { lastError = error }
        }
        if (bluetooth.status.value is BluetoothHidStatus.Connected) {
            try { bluetooth.sendRemoteKey(key); return } catch (error: Exception) { lastError = error }
        }
        throw IllegalStateException(
            lastError?.message ?: "Нет соединения: подключите Android TV Remote v2, ADB или Bluetooth HID.",
            lastError
        )
    }

    suspend fun toggleVoiceInput(): String {
        if (voice.status.value is VoiceInputStatus.Recording || voice.status.value is VoiceInputStatus.Starting) {
            return voice.toggle()
        }
        val host = savedHost
        if (remoteV2.status.value !is RemoteStatus.Connected &&
            remoteV2.status.value !is RemoteStatus.AwaitingCode && host.isNotBlank()
        ) {
            runCatching { remoteV2.connect(host) }
        }
        if (remoteV2.status.value is RemoteStatus.Connected) return voice.toggle()
        if (host.isNotBlank()) {
            if (adb.status.value !is AdbRemoteStatus.Connected) adb.connect(host)
            adb.sendKey(231) // Android KEYCODE_VOICE_ASSIST opens the TV-side assistant.
            return "Открыт голосовой помощник телевизора. Для передачи микрофона телефона подключитесь по Android TV Remote v2."
        }
        throw IllegalStateException("Голосовой ввод с микрофона телефона требует Android TV Remote v2; для ADB fallback укажите IP телевизора.")
    }

    suspend fun sendText(text: String) {
        require(text.isNotBlank()) { "Введите текст." }
        var lastError: Exception? = null
        val host = savedHost
        if (remoteV2.status.value is RemoteStatus.Connected) {
            try { remoteV2.sendText(text); return } catch (error: Exception) { lastError = error }
        }
        if (host.isNotBlank() && remoteV2.status.value !is RemoteStatus.AwaitingCode) {
            try { remoteV2.connect(host); remoteV2.sendText(text); return } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) {
            try { adb.connect(host) } catch (error: Exception) { lastError = error }
        }
        if (adb.status.value is AdbRemoteStatus.Connected) {
            try { adb.sendText(text); return } catch (error: Exception) { lastError = error }
        }
        if (bluetooth.status.value is BluetoothHidStatus.Connected) {
            try { bluetooth.sendText(text); return } catch (error: Exception) { lastError = error }
        }
        throw IllegalStateException(lastError?.message ?: "Нет транспорта для отправки текста.", lastError)
    }

    suspend fun launchApp(packageName: String) {
        val host = savedHost
        if (adb.status.value !is AdbRemoteStatus.Connected && host.isNotBlank()) adb.connect(host)
        adb.launchApp(packageName)
    }

    fun startBluetoothHid() = bluetooth.start()
    suspend fun moveMouse(dx: Int, dy: Int) = bluetooth.moveMouse(dx, dy)
    suspend fun clickMouse() = bluetooth.clickMouse()
    suspend fun shareLocalMedia(uri: Uri): CastMedia = withContext(Dispatchers.IO) { mediaServer.start(uri) }
    suspend fun discoverCastDevices(): List<DlnaDevice> = cast.discoverRenderers()
    suspend fun castTo(device: DlnaDevice, media: CastMedia) = cast.play(device, media)

    override fun close() {
        runCatching { voice.close() }
        runCatching { airMouse.close() }
        runCatching { bluetooth.close() }
        runCatching { adb.close() }
        runCatching { remoteV2.close() }
        runCatching { mediaServer.close() }
    }
}
