package com.radiotv.tvremote.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class BluetoothAirMouseController(private val context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) host = pluggedDevice ?: host
        }
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED) host = device
        }
    }

    fun isAvailable(): Boolean =
        android.os.Build.VERSION.SDK_INT >= 28 &&
            adapter != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    fun bondedHosts(): List<BluetoothDevice> =
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            emptyList()
        } else adapter?.bondedDevices?.toList().orEmpty()

    fun register(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 28) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false
        val bluetooth = adapter ?: return false
        return runCatching {
            bluetooth.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hid = proxy as BluetoothHidDevice
                        val sdp = BluetoothHidDeviceAppSdpSettings(
                            "Radio.TV Remote",
                            "Radio.TV Air Mouse",
                            "Radio.TV",
                            BluetoothHidDevice.SUBCLASS1_MOUSE,
                            MOUSE_DESCRIPTOR
                        )
                        hid?.registerApp(sdp, null, null, executor, callback)
                    }
                }
                override fun onServiceDisconnected(profile: Int) { if (profile == BluetoothProfile.HID_DEVICE) hid = null }
            }, BluetoothProfile.HID_DEVICE)
            true
        }.getOrDefault(false)
    }

    fun connectHost(address: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        host = device
        return runCatching { hid?.connect(device) == true }.getOrDefault(false)
    }

    fun move(dx: Int, dy: Int) {
        send(buttons = 0, dx = dx, dy = dy, wheel = 0)
    }

    fun click(left: Boolean = true) {
        send(buttons = if (left) 1 else 2, dx = 0, dy = 0, wheel = 0)
        send(buttons = 0, dx = 0, dy = 0, wheel = 0)
    }

    private fun send(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        val target = host ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val payload = byteArrayOf(buttons.toByte(), dx.coerceIn(-127, 127).toByte(), dy.coerceIn(-127, 127).toByte(), wheel.toByte())
        runCatching { hid?.sendReport(target, 0, payload) }
    }

    fun close() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            runCatching { hid?.unregisterApp() }
        }
        hid = null
        host = null
    }

    companion object {
        private val MOUSE_DESCRIPTOR = byteArrayOf(
            0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00,
            0x25, 0x01, 0x95.toByte(), 0x03, 0x75, 0x01, 0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(), 0x01,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38,
            0x15, 0x81.toByte(), 0x25, 0x7F, 0x75, 0x08, 0x95.toByte(), 0x03,
            0x81.toByte(), 0x06, 0xC0.toByte()
        )
    }
}
