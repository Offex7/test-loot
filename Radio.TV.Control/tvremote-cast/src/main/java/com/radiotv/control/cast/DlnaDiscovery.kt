package com.radiotv.control.cast

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

data class DlnaDevice(val location: String, val server: String?, val usn: String?)
data class CastMedia(val title: String, val uri: String, val mimeType: String = "video/*")
interface RadioTvMediaProvider { fun currentMedia(): CastMedia? }

/** SSDP renderer discovery starter. AVTransport playback remains a separate implementation step. */
class DlnaCastController(private val context: Context) {
    suspend fun discover(timeoutMs: Long = 2_500): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("Radio.TV.Control-SSDP").apply { setReferenceCounted(false) }
        val found = linkedMapOf<String, DlnaDevice>()
        lock.acquire()
        try {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(0))
                socket.soTimeout = 300
                val query = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"
                ).toByteArray(Charsets.US_ASCII)
                socket.send(DatagramPacket(query, query.size, InetAddress.getByName("239.255.255.250"), 1900))
                val deadline = System.currentTimeMillis() + timeoutMs.coerceAtLeast(0)
                while (System.currentTimeMillis() < deadline) {
                    val data = ByteArray(8192)
                    val packet = DatagramPacket(data, data.size)
                    try { socket.receive(packet) } catch (_: java.net.SocketTimeoutException) { continue }
                    val response = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
                    val location = response.headerValue("LOCATION") ?: continue
                    val device = DlnaDevice(location, response.headerValue("SERVER"), response.headerValue("USN"))
                    found.putIfAbsent(device.usn ?: device.location, device)
                }
            }
        } finally { runCatching { lock.release() } }
        found.values.toList()
    }

    private fun String.headerValue(header: String): String? =
        lineSequence().firstOrNull { it.substringBefore(':').trim().equals(header, ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }
}
