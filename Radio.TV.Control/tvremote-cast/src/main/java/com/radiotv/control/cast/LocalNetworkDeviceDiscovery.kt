package com.radiotv.control.cast

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.radiotv.control.core.DiscoveredRemoteDevice
import com.radiotv.control.core.RemoteDeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** Best-effort mDNS, SSDP and bounded TCP-port discovery for the local Wi-Fi network. */
class LocalNetworkDeviceDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun discover(): List<DiscoveredRemoteDevice> = withContext(Dispatchers.IO) {
        val found = ConcurrentHashMap<String, DiscoveredRemoteDevice>()
        coroutineScope {
            val mdns = async {
                listOf("_androidtvremote2._tcp.", "_googlecast._tcp.", "_airplay._tcp.")
                    .map { serviceType -> async(Dispatchers.Main) { browseMdns(serviceType, found) } }
                    .awaitAll()
            }
            val ssdp = async { discoverSsdp(found) }
            val tcp = async { scanSubnet(found) }
            awaitAll(mdns, ssdp, tcp)
        }
        found.values.sortedWith(compareBy<DiscoveredRemoteDevice> { rank(it.type) }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    private suspend fun browseMdns(
        serviceType: String,
        found: ConcurrentHashMap<String, DiscoveredRemoteDevice>
    ) {
        withTimeoutOrNull(MDNS_WINDOW_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        runCatching {
                            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                                override fun onServiceResolved(info: NsdServiceInfo) {
                                    val ip = info.host?.hostAddress?.takeIf { !it.contains(':') } ?: return
                                    val actual = info.serviceType ?: serviceType
                                    val attrs = info.attributes.mapValues { (_, value) -> value.toString(Charsets.UTF_8) }
                                    val kind = typeForService(actual)
                                    val brand = when (kind) {
                                        RemoteDeviceType.ANDROID_TV, RemoteDeviceType.CHROMECAST -> "Google"
                                        RemoteDeviceType.AIRPLAY -> "Apple"
                                        else -> null
                                    }
                                    add(found, DiscoveredRemoteDevice(
                                        ip = ip,
                                        name = attrs["fn"] ?: info.serviceName.ifBlank { kind.label },
                                        type = kind,
                                        brand = brand,
                                        model = attrs["md"],
                                        ports = setOf(info.port),
                                        services = setOf(actual)
                                    ))
                                }
                            })
                        }
                    }
                    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                    override fun onDiscoveryStopped(serviceType: String) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        runCatching { nsdManager.stopServiceDiscovery(this) }
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                try {
                    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                continuation.invokeOnCancellation { runCatching { nsdManager.stopServiceDiscovery(listener) } }
            }
        }
    }

    private suspend fun discoverSsdp(found: ConcurrentHashMap<String, DiscoveredRemoteDevice>) =
        withContext(Dispatchers.IO) {
            runCatching {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.bind(InetSocketAddress(0))
                    socket.soTimeout = SSDP_READ_TIMEOUT_MS
                    listOf("urn:schemas-upnp-org:device:MediaRenderer:1", "upnp:rootdevice").forEach { target ->
                        val query = (
                            "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 1\r\n" +
                            "ST: " + target + "\r\n\r\n"
                        ).toByteArray(Charsets.US_ASCII)
                        socket.send(DatagramPacket(query, query.size, InetAddress.getByName("239.255.255.250"), 1900))
                    }
                    val deadline = System.currentTimeMillis() + SSDP_WINDOW_MS
                    while (System.currentTimeMillis() < deadline) {
                        val bytes = ByteArray(8192)
                        val packet = DatagramPacket(bytes, bytes.size)
                        try { socket.receive(packet) } catch (_: java.net.SocketTimeoutException) { continue }
                        val response = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
                        val location = response.header("LOCATION") ?: continue
                        val server = response.header("SERVER").orEmpty()
                        val usn = response.header("USN").orEmpty()
                        val target = response.header("ST").orEmpty()
                        val ip = runCatching { URL(location).host }.getOrNull()
                            ?.takeIf { it.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}")) }
                            ?: packet.address.hostAddress
                        if (ip.contains(':')) continue
                        val kind = inferSsdpType(server, usn, target)
                        add(found, DiscoveredRemoteDevice(
                            ip = ip,
                            name = server.ifBlank { kind.label },
                            type = kind,
                            brand = brandFor(kind, server),
                            services = setOf("SSDP", target, usn).filter { it.isNotBlank() }.toSet()
                        ))
                    }
                }
            }
        }

    private suspend fun scanSubnet(found: ConcurrentHashMap<String, DiscoveredRemoteDevice>) =
        withContext(Dispatchers.IO) {
            val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return@withContext
            @Suppress("DEPRECATION")
            val dhcp = runCatching { wifi.dhcpInfo }.getOrNull() ?: return@withContext
            if (dhcp.ipAddress == 0) return@withContext
            fun octets(value: Int) = IntArray(4) { index -> (value ushr (index * 8)) and 0xFF }
            val local = octets(dhcp.ipAddress)
            val mask = octets(dhcp.netmask)
            val prefix = mask.sumOf { Integer.bitCount(it) }.takeIf { it in 1..32 } ?: 24
            val effectiveMask = if (prefix < 24) intArrayOf(255, 255, 255, 0) else mask
            val base = IntArray(4) { i -> local[i] and effectiveMask[i] }
            val targets = (0..255).mapNotNull { last ->
                val candidate = intArrayOf(base[0], base[1], base[2], last)
                if (candidate.contentEquals(local) || last == 0 || last == 255) return@mapNotNull null
                if ((last and effectiveMask[3]) != base[3]) return@mapNotNull null
                candidate.joinToString(".")
            }
            val gate = Semaphore(MAX_PARALLEL_HOSTS)
            coroutineScope {
                targets.map { ip ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            val open = linkedSetOf<Int>()
                            TCP_PORTS.forEach { port ->
                                val isOpen = runCatching {
                                    Socket().use { socket ->
                                        socket.connect(InetSocketAddress(ip, port), TCP_CONNECT_TIMEOUT_MS)
                                        true
                                    }
                                }.getOrDefault(false)
                                if (isOpen) open += port
                            }
                            if (open.isNotEmpty()) {
                                val kind = inferPortType(open)
                                add(found, DiscoveredRemoteDevice(
                                    ip = ip,
                                    name = kind.label + " · " + ip,
                                    type = kind,
                                    brand = brandFor(kind, null),
                                    ports = open
                                ))
                            }
                        }
                    }
                }.awaitAll()
            }
        }

    private fun String.header(name: String): String? =
        lineSequence().firstOrNull { it.substringBefore(':').trim().equals(name, ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotBlank() }

    private fun typeForService(serviceType: String) = when {
        serviceType.contains("_androidtvremote2._tcp", true) -> RemoteDeviceType.ANDROID_TV
        serviceType.contains("_googlecast._tcp", true) -> RemoteDeviceType.CHROMECAST
        serviceType.contains("_airplay._tcp", true) -> RemoteDeviceType.AIRPLAY
        else -> RemoteDeviceType.UNKNOWN
    }

    private fun inferSsdpType(server: String, usn: String, target: String): RemoteDeviceType {
        val text = "$server $usn $target".lowercase(Locale.ROOT)
        return when {
            "samsung" in text -> RemoteDeviceType.SAMSUNG
            "webos" in text || "lg electronics" in text -> RemoteDeviceType.LG
            "roku" in text -> RemoteDeviceType.ROKU
            "google" in text || "chromecast" in text -> RemoteDeviceType.CHROMECAST
            else -> RemoteDeviceType.DLNA
        }
    }

    private fun inferPortType(ports: Set<Int>) = when {
        6466 in ports || 6467 in ports -> RemoteDeviceType.ANDROID_TV
        8060 in ports -> RemoteDeviceType.ROKU
        8001 in ports || 8002 in ports -> RemoteDeviceType.SAMSUNG
        3000 in ports || 3001 in ports -> RemoteDeviceType.LG
        8008 in ports || 8009 in ports -> RemoteDeviceType.CHROMECAST
        else -> RemoteDeviceType.UNKNOWN
    }

    private fun brandFor(type: RemoteDeviceType, fallback: String?): String? = when (type) {
        RemoteDeviceType.ANDROID_TV, RemoteDeviceType.CHROMECAST -> "Google / Android"
        RemoteDeviceType.SAMSUNG -> "Samsung"
        RemoteDeviceType.LG -> "LG"
        RemoteDeviceType.ROKU -> "Roku"
        RemoteDeviceType.AIRPLAY -> "Apple"
        RemoteDeviceType.DLNA -> fallback?.substringBefore('/')?.takeIf { it.isNotBlank() }
        RemoteDeviceType.UNKNOWN -> null
    }

    private fun add(found: ConcurrentHashMap<String, DiscoveredRemoteDevice>, device: DiscoveredRemoteDevice) {
        if (device.ip.isBlank()) return
        found.compute(device.ip) { _, old ->
            if (old == null) device else {
                val primary = if (rank(device.type) < rank(old.type)) device else old
                primary.copy(
                    name = if ((old.name.contains("·") || old.name.startsWith("Устройство")) && device.name.isNotBlank()) device.name else primary.name,
                    brand = primary.brand ?: old.brand ?: device.brand,
                    model = primary.model ?: old.model ?: device.model,
                    ports = old.ports + device.ports,
                    services = old.services + device.services
                )
            }
        }
    }

    private fun rank(type: RemoteDeviceType) = when (type) {
        RemoteDeviceType.ANDROID_TV -> 0
        RemoteDeviceType.CHROMECAST -> 1
        RemoteDeviceType.SAMSUNG -> 2
        RemoteDeviceType.LG -> 3
        RemoteDeviceType.ROKU -> 4
        RemoteDeviceType.DLNA -> 5
        RemoteDeviceType.AIRPLAY -> 6
        RemoteDeviceType.UNKNOWN -> 7
    }

    private companion object {
        const val MDNS_WINDOW_MS = 2_300L
        const val SSDP_WINDOW_MS = 1_700L
        const val SSDP_READ_TIMEOUT_MS = 240
        const val TCP_CONNECT_TIMEOUT_MS = 180
        const val MAX_PARALLEL_HOSTS = 48
        val TCP_PORTS = listOf(6466, 6467, 8008, 8009, 9080, 8060, 8001, 8002, 3000, 3001)
    }
}
