package com.radiotv.tvremote.cast

import android.content.Context
import android.net.wifi.WifiManager
import com.radiotv.tvremote.core.CastController
import com.radiotv.tvremote.core.CastMedia
import com.radiotv.tvremote.core.CastRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID

class DlnaCastController(private val context: Context) : CastController {
    override suspend fun discover(): List<CastRenderer> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = runCatching {
            wifi.createMulticastLock("radiotv-dlna-ssdp").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()

        try {
            val request = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: 239.255.255.250:1900\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("MX: 2\r\n")
                append("ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n")
            }
            val found = linkedMapOf<String, CastRenderer>()
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val bytes = request.toByteArray()
                repeat(3) {
                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("239.255.255.250"), 1900))
                }
                val deadline = System.currentTimeMillis() + 3500
                val buffer = ByteArray(8192)
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.soTimeout = maxOf(100, (deadline - System.currentTimeMillis()).toInt())
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length)
                        val location = text.lines().firstOrNull { it.startsWith("location:", true) }
                            ?.substringAfter(":")?.trim()
                        if (!location.isNullOrBlank()) {
                            val key = location.lowercase()
                            found[key] = CastRenderer(key, extractFriendlyName(location) ?: "DLNA Renderer", location)
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    }
                }
            }
            found.values.toList()
        } finally {
            runCatching { lock?.release() }
        }
    }

    override suspend fun play(renderer: CastRenderer, media: CastMedia) = withContext(Dispatchers.IO) {
        val description = httpGet(renderer.descriptionUrl)
        val control = findAvTransportControlUrl(description.first, description.second)
            ?: throw IllegalStateException("DLNA renderer does not expose AVTransport")
        val metadata = didl(media)
        soap(
            control,
            "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>" + esc(media.url) +
                "</CurrentURI><CurrentURIMetaData>" + esc(metadata) + "</CurrentURIMetaData>"
        )
        soap(control, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    override suspend fun stop(renderer: CastRenderer) = withContext(Dispatchers.IO) {
        val description = httpGet(renderer.descriptionUrl)
        val control = findAvTransportControlUrl(description.first, description.second) ?: return@withContext
        soap(control, "Stop", "<InstanceID>0</InstanceID>")
    }

    private fun httpGet(url: String): Pair<String, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 4000
        c.readTimeout = 5000
        val body = c.inputStream.bufferedReader().use { it.readText() }
        return url to body
    }

    private fun findAvTransportControlUrl(base: String, xml: String): String? {
        val match = Regex(
            "<service>.*?<serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>.*?<controlURL>(.*?)</controlURL>.*?</service>",
            RegexOption.DOT_MATCHES_ALL
        ).find(xml) ?: return null
        val control = match.groupValues[1]
        return try { URI(base).resolve(control).toString() } catch (_: Exception) { control }
    }

    private fun soap(controlUrl: String, action: String, args: String) {
        val body =
            "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:" +
                action + " xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" + args +
                "</u:" + action + "></s:Body></s:Envelope>"
        val c = URL(controlUrl).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 5000
        c.readTimeout = 5000
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        c.setRequestProperty("SOAPAction", "\"urn:schemas-upnp-org:service:AVTransport:1#" + action + "\"")
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray()) }
        if (c.responseCode !in 200..299) {
            throw IllegalStateException("DLNA SOAP " + action + " failed: HTTP " + c.responseCode)
        }
    }

    private fun didl(media: CastMedia): String {
        val itemClass = if (media.mimeType.startsWith("audio/", true)) {
            "object.item.audioItem.musicTrack"
        } else {
            "object.item.videoItem"
        }
        return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
            "<item id=\"" + UUID.randomUUID() + "\" parentID=\"0\" restricted=\"1\">" +
            "<dc:title>" + esc(media.title) + "</dc:title><upnp:class>" + itemClass + "</upnp:class>" +
            "<res protocolInfo=\"http-get:*:" + esc(media.mimeType) + ":*\">" + esc(media.url) +
            "</res></item></DIDL-Lite>"
    }

    private fun esc(s: String): String = s.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun extractFriendlyName(url: String): String? = runCatching {
        val html = httpGet(url).second
        Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
    }.getOrNull()
}
