package com.offex7.streamhub

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DiscoveredTvAddress(val host: String, val name: String, val discovery: String)

/** Bounded mDNS + SSDP discovery. All blocking socket work stays off the main thread. */
class TvNetworkDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(NsdManager::class.java)

    suspend fun discover(timeoutMs: Long = 7_000L): List<DiscoveredTvAddress> = withContext(Dispatchers.IO) {
        val found = ConcurrentHashMap<String, DiscoveredTvAddress>()
        val resolved = ConcurrentHashMap.newKeySet<String>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val serviceName = serviceInfo.serviceName.orEmpty()
                if (!resolved.add(serviceName)) return
                runCatching {
                    nsd?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            resolved.remove(serviceName)
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress.orEmpty()
                            if (isPrivateIpv4(host)) {
                                found[host] = DiscoveredTvAddress(host, info.serviceName.orEmpty().ifBlank { "Android TV" }, "mDNS")
                            }
                        }
                    })
                }.onFailure { resolved.remove(serviceName) }
            }
        }
        var discoveryStarted = false
        var multicastLock: WifiManager.MulticastLock? = null
        try {
            runCatching {
                multicastLock = (appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                    .createMulticastLock("radio-tv-v46-discovery")
                    .apply { setReferenceCounted(false); acquire() }
            }
            if (nsd != null) {
                runCatching {
                    nsd.discoverServices("_androidtvremote2._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
                    discoveryStarted = true
                }
            }

            val deadline = SystemClock.elapsedRealtime() + timeoutMs.coerceIn(1_000L, 12_000L)
            runCatching {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 450
                    val request = (
                        "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: ssdp:all\r\n\r\n"
                        ).toByteArray(Charsets.US_ASCII)
                    val group = InetAddress.getByName("239.255.255.250")
                    repeat(2) { socket.send(DatagramPacket(request, request.size, group, 1900)) }
                    while (SystemClock.elapsedRealtime() < deadline) {
                        try {
                            val response = DatagramPacket(ByteArray(8192), 8192)
                            socket.receive(response)
                            val text = String(response.data, 0, response.length, Charsets.US_ASCII)
                            val location = text.lineSequence()
                                .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                                ?.substringAfter(':')?.trim()
                            val host = runCatching { location?.let { URI(it).host }.orEmpty() }.getOrDefault("")
                            if (isPrivateIpv4(host)) {
                                found.putIfAbsent(host, DiscoveredTvAddress(host, "Android TV", "SSDP"))
                            }
                        } catch (_: SocketTimeoutException) {
                            // Give mDNS callbacks time to resolve while maintaining a bounded search.
                        }
                    }
                }
            }
        } finally {
            if (discoveryStarted) runCatching { nsd?.stopServiceDiscovery(listener) }
            runCatching { multicastLock?.release() }
        }
        found.values.sortedBy { it.host }
    }

    private fun isPrivateIpv4(address: String): Boolean = runCatching {
        if (!address.matches(Regex("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) return@runCatching false
        val p = address.split('.').map(String::toInt)
        if (p.any { it !in 0..255 }) return@runCatching false
        p[0] == 10 ||
            (p[0] == 192 && p[1] == 168) ||
            (p[0] == 172 && p[1] in 16..31) ||
            (p[0] == 169 && p[1] == 254)
    }.getOrDefault(false)
}
