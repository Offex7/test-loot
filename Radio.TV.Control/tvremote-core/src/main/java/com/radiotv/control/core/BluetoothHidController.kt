package com.radiotv.control.core

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BluetoothHidStatus {
    data object Idle : BluetoothHidStatus
    data object Starting : BluetoothHidStatus
    data object Ready : BluetoothHidStatus
    data class Connected(val deviceName: String, val address: String) : BluetoothHidStatus
    data class Error(val message: String) : BluetoothHidStatus
}

/**
 * Android Bluetooth HID Device role: the phone advertises itself as a combined keyboard/mouse,
 * and the TV initiates Bluetooth pairing/connection from its own Bluetooth settings.
 *
 * Device/firmware support varies. This class only reports Connected after the framework callback;
 * callers must not assume a TV has paired just because the SDP registration succeeded.
 */
class BluetoothHidController(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val stateMutable = MutableStateFlow<BluetoothHidStatus>(BluetoothHidStatus.Idle)
    val status: StateFlow<BluetoothHidStatus> = stateMutable.asStateFlow()
    private val sendMutex = Mutex()

    @Volatile private var profile: BluetoothHidDevice? = null
    @Volatile private var connectedHost: BluetoothDevice? = null
    @Volatile private var appRegistered = false
    @Volatile private var profileRequested = false

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            if (!registered) {
                connectedHost = null
                stateMutable.value = BluetoothHidStatus.Error(
                    "Android снял регистрацию HID. Оставьте Radio.TV.Control на переднем плане и попробуйте снова."
                )
            } else if (pluggedDevice != null) {
                connectedHost = pluggedDevice
                stateMutable.value = BluetoothHidStatus.Connected(deviceName(pluggedDevice), safeAddress(pluggedDevice))
            } else {
                stateMutable.value = BluetoothHidStatus.Ready
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    stateMutable.value = BluetoothHidStatus.Connected(deviceName(device), safeAddress(device))
                }
                BluetoothProfile.STATE_CONNECTING -> stateMutable.value = BluetoothHidStatus.Starting
                BluetoothProfile.STATE_DISCONNECTED,
                BluetoothProfile.STATE_DISCONNECTING -> {
                    if (connectedHost?.address == device.address) connectedHost = null
                    stateMutable.value = if (appRegistered) BluetoothHidStatus.Ready else BluetoothHidStatus.Idle
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val expected = when (id.toInt() and 0xFF) {
                KEYBOARD_REPORT_ID -> KEYBOARD_REPORT_BYTES
                MOUSE_REPORT_ID -> MOUSE_REPORT_BYTES
                CONSUMER_REPORT_ID -> CONSUMER_REPORT_BYTES
                else -> bufferSize.coerceIn(0, MAX_REPORT_BYTES)
            }
            val bytes = ByteArray(minOf(expected, bufferSize.coerceIn(0, MAX_REPORT_BYTES)))
            runCatching { profile?.replyReport(device, type, id, bytes) }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            runCatching { profile?.disconnect(device) }
            if (connectedHost?.address == device.address) connectedHost = null
            stateMutable.value = if (appRegistered) BluetoothHidStatus.Ready else BluetoothHidStatus.Idle
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile?) {
            val hid = proxy as? BluetoothHidDevice
            if (profileId != BluetoothProfile.HID_DEVICE || hid == null) {
                stateMutable.value = BluetoothHidStatus.Error("Bluetooth HID Device profile недоступен на этом телефоне.")
                return
            }
            profile = hid
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "Radio.TV.Control",
                "Bluetooth keyboard and mouse",
                "Offex7",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HID_REPORT_DESCRIPTOR
            )
            val started = runCatching {
                hid.registerApp(sdp, null, null, appContext.mainExecutor, callback)
            }.getOrElse {
                stateMutable.value = BluetoothHidStatus.Error(it.message ?: "Не удалось зарегистрировать Bluetooth HID.")
                false
            }
            if (!started) {
                stateMutable.value = BluetoothHidStatus.Error(
                    "Bluetooth HID registration не принята системой. Проверьте поддержку HID Device на телефоне."
                )
            } else {
                stateMutable.value = BluetoothHidStatus.Starting
            }
        }

        override fun onServiceDisconnected(profileId: Int) {
            profile = null
            connectedHost = null
            appRegistered = false
            profileRequested = false
            stateMutable.value = BluetoothHidStatus.Idle
        }
    }

    fun start() {
        if (profileRequested || profile != null) return
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            stateMutable.value = BluetoothHidStatus.Error("На этом устройстве нет Bluetooth-адаптера.")
            return
        }
        if (!runCatching { adapter.isEnabled }.getOrDefault(false)) {
            stateMutable.value = BluetoothHidStatus.Error("Сначала включите Bluetooth.")
            return
        }
        stateMutable.value = BluetoothHidStatus.Starting
        profileRequested = runCatching {
            adapter.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE)
        }.getOrElse {
            stateMutable.value = BluetoothHidStatus.Error(it.message ?: "Не удалось открыть Bluetooth HID profile.")
            false
        }
        if (!profileRequested) stateMutable.value = BluetoothHidStatus.Error("Система не предоставила Bluetooth HID profile.")
    }

    suspend fun sendRemoteKey(key: RemoteKey) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            when (val command = hidInputFor(key)) {
                is HidInputCommand.Keyboard -> sendKeyboardTapLocked(command.usage, command.modifier)
                is HidInputCommand.Consumer -> sendConsumerTapLocked(command.usage)
            }
        }
    }

    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            text.forEach { character ->
                asciiHidUsage(character)?.let { (usage, modifier) -> sendKeyboardTapLocked(usage, modifier) }
            }
        }
    }

    suspend fun moveMouse(dx: Int, dy: Int, wheel: Int = 0) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val (hid, device) = requireConnection()
            val report = byteArrayOf(
                0,
                dx.coerceIn(-127, 127).toByte(),
                dy.coerceIn(-127, 127).toByte(),
                wheel.coerceIn(-127, 127).toByte()
            )
            check(hid.sendReport(device, MOUSE_REPORT_ID, report)) { "Не удалось отправить Bluetooth mouse report." }
        }
    }

    suspend fun clickMouse(button: Int = 1) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val (hid, device) = requireConnection()
            val pressed = byteArrayOf((button and 0x07).toByte(), 0, 0, 0)
            check(hid.sendReport(device, MOUSE_REPORT_ID, pressed)) { "Не удалось отправить mouse click." }
            delay(REPORT_RELEASE_DELAY_MS)
            check(hid.sendReport(device, MOUSE_REPORT_ID, byteArrayOf(0, 0, 0, 0))) {
                "Не удалось отправить mouse release report."
            }
        }
    }

    private suspend fun sendKeyboardTapLocked(usage: Int, modifier: Int) {
        val (hid, device) = requireConnection()
        val down = byteArrayOf(modifier.toByte(), 0, usage.toByte(), 0, 0, 0, 0, 0)
        check(hid.sendReport(device, KEYBOARD_REPORT_ID, down)) { "Не удалось отправить Bluetooth keyboard report." }
        delay(REPORT_RELEASE_DELAY_MS)
        check(hid.sendReport(device, KEYBOARD_REPORT_ID, ByteArray(KEYBOARD_REPORT_BYTES))) {
            "Не удалось отправить keyboard release report."
        }
        delay(REPORT_RELEASE_DELAY_MS)
    }

    private suspend fun sendConsumerTapLocked(usage: Int) {
        val (hid, device) = requireConnection()
        val down = byteArrayOf((usage and 0xFF).toByte(), ((usage shr 8) and 0xFF).toByte())
        check(hid.sendReport(device, CONSUMER_REPORT_ID, down)) { "Не удалось отправить Bluetooth consumer report." }
        delay(REPORT_RELEASE_DELAY_MS)
        check(hid.sendReport(device, CONSUMER_REPORT_ID, ByteArray(CONSUMER_REPORT_BYTES))) {
            "Не удалось отправить consumer release report."
        }
        delay(REPORT_RELEASE_DELAY_MS)
    }

    private fun requireConnection(): Pair<BluetoothHidDevice, BluetoothDevice> {
        val hid = checkNotNull(profile) { "Сначала запустите Bluetooth HID." }
        val host = checkNotNull(connectedHost) { "На телевизоре откройте Bluetooth и выберите Radio.TV.Control." }
        check(stateMutable.value is BluetoothHidStatus.Connected) { "Bluetooth HID host ещё не подключён." }
        return hid to host
    }

    private fun deviceName(device: BluetoothDevice): String =
        runCatching { device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth host" }.getOrDefault("Bluetooth host")

    private fun safeAddress(device: BluetoothDevice): String =
        runCatching { device.address.orEmpty() }.getOrDefault("")

    override fun close() {
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        val hid = profile
        profile = null
        connectedHost = null
        appRegistered = false
        profileRequested = false
        runCatching { hid?.unregisterApp() }
        runCatching {
            if (hid != null) adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
        }
        stateMutable.value = BluetoothHidStatus.Idle
    }

    companion object {
        private const val KEYBOARD_REPORT_ID = 1
        private const val MOUSE_REPORT_ID = 2
        private const val CONSUMER_REPORT_ID = 3
        private const val KEYBOARD_REPORT_BYTES = 8
        private const val MOUSE_REPORT_BYTES = 4
        private const val CONSUMER_REPORT_BYTES = 2
        private const val MAX_REPORT_BYTES = 64
        private const val REPORT_RELEASE_DELAY_MS = 35L

        // USB HID 1.11: boot-compatible keyboard, relative mouse, plus Consumer Control media keys.
        private val HID_REPORT_DESCRIPTOR = intArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xA1, 0x01, 0x85, KEYBOARD_REPORT_ID,
            0x05, 0x07, 0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01,
            0x75, 0x01, 0x95, 0x08, 0x81, 0x02, 0x95, 0x01, 0x75, 0x08, 0x81, 0x01,
            0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07, 0x19, 0x00,
            0x29, 0x65, 0x81, 0x00, 0xC0,
            0x05, 0x01, 0x09, 0x02, 0xA1, 0x01, 0x85, MOUSE_REPORT_ID,
            0x09, 0x01, 0xA1, 0x00, 0x05, 0x09, 0x19, 0x01, 0x29, 0x03,
            0x15, 0x00, 0x25, 0x01, 0x95, 0x03, 0x75, 0x01, 0x81, 0x02,
            0x95, 0x01, 0x75, 0x05, 0x81, 0x01, 0x05, 0x01, 0x09, 0x30,
            0x09, 0x31, 0x09, 0x38, 0x15, 0x81, 0x25, 0x7F, 0x75, 0x08,
            0x95, 0x03, 0x81, 0x06, 0xC0, 0xC0,
            0x05, 0x0C, 0x09, 0x01, 0xA1, 0x01, 0x85, CONSUMER_REPORT_ID,
            0x15, 0x00, 0x26, 0xFF, 0x03, 0x19, 0x00, 0x2A, 0xFF, 0x03,
            0x75, 0x10, 0x95, 0x01, 0x81, 0x00, 0xC0
        ).map { it.toByte() }.toByteArray()
    }
}

