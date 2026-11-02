package com.offex7.streamhub

import android.content.Context
import android.net.Uri
import com.radiotv.control.cast.CastMedia
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.cast.DlnaDevice
import com.radiotv.control.cast.LocalMediaHttpServer
import com.radiotv.control.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.net.SocketTimeoutException

/** Protocol fallback hub for the integrated Radio.TV remote. */
internal class RemoteControlRuntime(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("radio_tv_remote_runtime", Context.MODE_PRIVATE)
    private val deviceHistory = RemoteDeviceHistory(appContext)
    private val networkDiscovery = TvNetworkDiscovery(appContext)
    private val connectionMutex = Mutex()
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lastResumeReconnectAt = 0L

    val remoteV2 = AndroidTvRemoteV2Transport(appContext)
    val adb = AdbRemoteDevice(appContext)
    val bluetooth = BluetoothHidController(appContext)
    val airMouse = GyroAirMouseController(appContext, bluetooth)
    val voice = AndroidTvVoiceInputController(appContext, remoteV2)
    val cast = DlnaCastController(appContext)
    val mediaServer = LocalMediaHttpServer(appContext)

    init {
        runtimeScope.launch {
            bluetooth.status.collect { status ->
                if (status is BluetoothHidStatus.Connected) {
                    deviceHistory.record(status.address, status.deviceName, "Bluetooth")
                    preferences.edit().putBoolean(KEY_HAS_CONNECTED, true).apply()
                }
            }
        }
    }

    fun savedDevices(): List<SavedRemoteDevice> = deviceHistory.devices()

    fun saveManualHost(host: String) {
        val clean = host.trim()
        if (clean.isBlank()) preferences.edit().remove("last_remote_host").apply()
        else preferences.edit().putString("last_remote_host", clean).apply()
    }
    fun renameSavedDevice(id: String, name: String): Boolean = deviceHistory.rename(id, name)
    fun removeSavedDevice(id: String) = deviceHistory.remove(id)
    fun bondedBluetoothDevices(): List<BluetoothRemoteDevice> = guardedSync("список Bluetooth-устройств") { bluetooth.bondedDevices() }
    fun rememberCompanionDevice(host: String, name: String) { deviceHistory.record(host, name.ifBlank { "Android TV" }, "Wi-Fi"); preferences.edit().putBoolean(KEY_HAS_CONNECTED, true).apply() }

    private fun rememberWifiDevice(host: String, source: String) {
        val clean = host.trim()
        if (clean.isBlank()) return
        val label = if (source.isBlank()) "Android TV" else source.take(16)
        deviceHistory.record(clean, label, "Wi-Fi")
        preferences.edit().putBoolean(KEY_HAS_CONNECTED, true).apply()
    }

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

    suspend fun connect(host: String, allowPairing: Boolean): String = connectionMutex.withLock {
        val target = host.trim().ifBlank { savedHost }
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        preferences.edit().putString("last_remote_host", target).apply()

        if (remoteV2.status.value is RemoteStatus.AwaitingCode) {
            return@withLock "Ожидается код сопряжения Android TV Remote v2."
        }

        var remoteError: Throwable? = null
        try {
            guarded("подключение Android TV Remote v2") { remoteV2.connect(target) }
            rememberWifiDevice(target, "Android TV")
            return@withLock "Подключено: Android TV Remote v2"
        } catch (error: Exception) {
            remoteError = error
        }

        if (allowPairing) {
            try {
                guarded("сопряжение Android TV Remote v2") { remoteV2.beginPairing(target) }
                return@withLock "На экране телевизора появится шестизначный код сопряжения."
            } catch (error: Exception) {
                remoteError = error
            }
        }

        try {
            guarded("подключение ADB") { adb.connect(target) }
            rememberWifiDevice(target, "Android TV")
            return@withLock "Подключено: ADB"
        } catch (error: Exception) {
            if (bluetooth.status.value is BluetoothHidStatus.Connected) return@withLock "Подключено: Bluetooth HID"
            val detail = error.message ?: remoteError?.let { messageFor("подключение", it) }
                ?: "Устройство недоступно."
            return@withLock "Нет соединения. $detail"
        }
    }

    suspend fun autoConnect(hostHint: String = ""): String {
        val hint = hostHint.trim()
        if (hint.isNotBlank()) return connect(hint, false)

        val saved = savedHost
        if (saved.isNotBlank()) {
            val result = connect(saved, false)
            if (remoteV2.status.value is RemoteStatus.Connected || adb.status.value is AdbRemoteStatus.Connected) return result
        }

        val discovered = networkDiscovery.discover()
        for (device in discovered) {
            val result = runCatching { connect(device.host, true) }.getOrElse { it.message ?: "Не удалось подключиться." }
            if (remoteV2.status.value is RemoteStatus.AwaitingCode) return result
            if (remoteV2.status.value is RemoteStatus.Connected || adb.status.value is AdbRemoteStatus.Connected) {
                deviceHistory.record(device.host, device.name, "Wi-Fi")
                preferences.edit().putBoolean(KEY_HAS_CONNECTED, true).apply()
                return result
            }
        }
        return if (discovered.isEmpty()) {
            "Телевизоры не найдены. Проверьте Wi-Fi и разрешение локальной сети."
        } else {
            "Не удалось подключиться к найденным телевизорам. Попробуйте ручной IP или Bluetooth HID."
        }
    }

    suspend fun connectBluetoothDevice(address: String): String = guarded("подключение Bluetooth HID") {
        bluetooth.connectToBondedDevice(address)
        val outcome = withTimeoutOrNull(15_000L) {
            bluetooth.status.first { it is BluetoothHidStatus.Connected || it is BluetoothHidStatus.Error }
        }
        when (outcome) {
            is BluetoothHidStatus.Connected -> {
                val device = bluetooth.bondedDevices().firstOrNull { it.address.equals(outcome.address, true) }
                deviceHistory.record(outcome.address, device?.name ?: outcome.deviceName, "Bluetooth")
                preferences.edit().putBoolean(KEY_HAS_CONNECTED, true).apply()
                "Подключено: Bluetooth HID"
            }
            is BluetoothHidStatus.Error -> throw IllegalStateException(outcome.message)
            else -> "Bluetooth HID запускается. Подтвердите подключение на телевизоре."
        }
    }

    suspend fun reconnectAfterResume(): String? {
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this) {
            if (now - lastResumeReconnectAt < 2_000L) return null
            lastResumeReconnectAt = now
        }
        if (!preferences.getBoolean(KEY_HAS_CONNECTED, false)) return null
        if (remoteV2.status.value is RemoteStatus.Connected || adb.status.value is AdbRemoteStatus.Connected || bluetooth.status.value is BluetoothHidStatus.Connected) return null
        val recent = deviceHistory.devices().firstOrNull()
        if (recent?.connectionType == "Bluetooth") return connectBluetoothDevice(recent.host)
        val target = recent?.host?.takeIf { it.isNotBlank() } ?: savedHost
        if (target.isBlank()) return null
        return connect(target, false)
    }

    suspend fun connectSavedDevice(id: String): String {
        val device = deviceHistory.devices().firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("Телевизор больше не сохранён.")
        return if (device.connectionType == "Bluetooth") connectBluetoothDevice(device.host)
        else connect(device.host, false)
    }

    suspend fun completePairing(code: String): String = guarded("сопряжение Android TV Remote v2") {
        remoteV2.completePairing(code)
        remoteV2.pairedHost()?.let { rememberWifiDevice(it, "Android TV") }
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

    private companion object { const val KEY_HAS_CONNECTED = "has_successful_remote_connection_v1" }

    override fun close() {
        runtimeScope.cancel()
        runCatching { voice.close() }
        runCatching { airMouse.close() }
        runCatching { bluetooth.close() }
        runCatching { adb.close() }
        runCatching { remoteV2.close() }
        runCatching { mediaServer.close() }
    }
}
