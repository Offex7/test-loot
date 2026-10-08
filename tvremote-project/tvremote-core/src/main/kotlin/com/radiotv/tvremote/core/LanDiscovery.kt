package com.radiotv.tvremote.core

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket

object LanDiscovery {
    suspend fun scan(context: Context, timeoutMs: Int = 250): List<RemoteDeviceInfo> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val ip = dhcp.ipAddress
        val mask = dhcp.netmask
        if (ip == 0 || mask == 0) return@withContext emptyList()

        var network = ip and mask
        var broadcast = network or mask.inv()
        var first = network + 1
        var last = broadcast - 1
        if (last.toLong() - first.toLong() > 510) {
            val ipBytes = intToBytes(ip)
            network = bytesToInt(byteArrayOf(ipBytes[0], ipBytes[1], ipBytes[2], 0))
            broadcast = network + 255
            first = network + 1
            last = broadcast - 1
        }

        val semaphore = Semaphore(32)
        coroutineScope {
            (first..last).map { raw ->
                async {
                    semaphore.withPermit {
                        val host = intToString(raw)
                        val hits = mutableListOf<RemoteDeviceInfo>()
                        if (open(host, 8001, timeoutMs)) {
                            hits += RemoteDeviceInfo("samsung:" + host, "Samsung TV", host, 8001, DeviceProtocol.SAMSUNG_TIZEN)
                        }
                        if (open(host, 3000, timeoutMs)) {
                            hits += RemoteDeviceInfo("lg:" + host, "LG webOS TV", host, 3000, DeviceProtocol.LG_WEBOS)
                        }
                        if (open(host, 8060, timeoutMs)) {
                            hits += RemoteDeviceInfo("roku:" + host, "Roku", host, 8060, DeviceProtocol.ROKU_ECP)
                        }
                        if (open(host, 6466, timeoutMs)) {
                            hits += RemoteDeviceInfo("atv:" + host, "Android TV", host, 6466, DeviceProtocol.ANDROID_TV_V2)
                        }
                        hits
                    }
                }
            }.awaitAll().flatten()
        }
    }

    private fun open(host: String, port: Int, timeout: Int): Boolean =
        runCatching {
            Socket().use { it.connect(InetSocketAddress(host, port), timeout) }
            true
        }.getOrDefault(false)

    private fun intToBytes(value: Int) = byteArrayOf(
        (value and 0xff).toByte(), ((value ushr 8) and 0xff).toByte(),
        ((value ushr 16) and 0xff).toByte(), ((value ushr 24) and 0xff).toByte()
    )

    private fun bytesToInt(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xff) or ((bytes[1].toInt() and 0xff) shl 8) or
            ((bytes[2].toInt() and 0xff) shl 16) or ((bytes[3].toInt() and 0xff) shl 24)

    private fun intToString(value: Int) =
        (value and 0xff).toString() + "." +
            ((value ushr 8) and 0xff).toString() + "." +
            ((value ushr 16) and 0xff).toString() + "." +
            ((value ushr 24) and 0xff).toString()
}
