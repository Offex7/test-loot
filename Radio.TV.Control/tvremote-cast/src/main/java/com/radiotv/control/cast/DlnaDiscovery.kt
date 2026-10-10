package com.radiotv.control.cast

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL

data class DlnaDevice(
    val location: String,
    val server: String?,
    val usn: String?,
    val friendlyName: String? = null,
    val controlUrl: String? = null,
    val serviceType: String? = null
)
data class CastMedia(val title: String, val uri: String, val mimeType: String = "video/*")
interface RadioTvMediaProvider { fun currentMedia(): CastMedia? }

/** SSDP discovery + UPnP AVTransport SOAP controls. */
class DlnaCastController(private val context: Context) {
    suspend fun discover(timeoutMs: Long = 2_500): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@withContext emptyList()
        val lock = wifi.createMulticastLock("Radio.TV.Control-SSDP").apply { setReferenceCounted(false) }
        val found = linkedMapOf<String, DlnaDevice>()
        runCatching { lock.acquire() }
        try {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(0))
                socket.soTimeout = 300
                listOf(
                    "urn:schemas-upnp-org:device:MediaRenderer:1",
                    "urn:schemas-upnp-org:service:AVTransport:1",
                    "upnp:rootdevice"
                ).forEach { target ->
                    val query = (
                        "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: $target\r\n\r\n"
                    ).toByteArray(Charsets.US_ASCII)
                    socket.send(DatagramPacket(query, query.size, InetAddress.getByName("239.255.255.250"), 1900))
                }
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
        } finally {
            runCatching { lock.release() }
        }
        found.values.toList()
    }

    /** Resolves friendly name and absolute AVTransport control URL from each device description. */
    suspend fun discoverRenderers(timeoutMs: Long = 2_500): List<DlnaDevice> = withContext(Dispatchers.IO) {
        discover(timeoutMs).mapNotNull { resolveRenderer(it) }
            .distinctBy { it.controlUrl }
    }

    suspend fun setAvTransportUri(renderer: DlnaDevice, media: CastMedia) = withContext(Dispatchers.IO) {
        val controlUrl = checkNotNull(renderer.controlUrl) { "У устройства нет AVTransport control URL." }
        require(media.uri.startsWith("http://", true) || media.uri.startsWith("https://", true)) {
            "DLNA-приёмнику нужен абсолютный HTTP(S)-URL медиафайла."
        }
        val didl = buildDidl(media)
        soap(
            controlUrl,
            renderer.serviceType ?: AVTRANSPORT_SERVICE,
            "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>${xmlEscape(media.uri)}</CurrentURI>" +
                "<CurrentURIMetaData>${xmlEscape(didl)}</CurrentURIMetaData>"
        )
    }

    suspend fun play(renderer: DlnaDevice, media: CastMedia) {
        setAvTransportUri(renderer, media)
        play(renderer)
    }

    suspend fun play(renderer: DlnaDevice) = withContext(Dispatchers.IO) {
        val controlUrl = checkNotNull(renderer.controlUrl) { "У устройства нет AVTransport control URL." }
        soap(controlUrl, renderer.serviceType ?: AVTRANSPORT_SERVICE, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    suspend fun pause(renderer: DlnaDevice) = withContext(Dispatchers.IO) {
        val controlUrl = checkNotNull(renderer.controlUrl) { "У устройства нет AVTransport control URL." }
        soap(controlUrl, renderer.serviceType ?: AVTRANSPORT_SERVICE, "Pause", "<InstanceID>0</InstanceID>")
    }

    /** Relative seek uses GetPositionInfo; unsupported renderers return a user-visible error. */
    suspend fun seek(renderer: DlnaDevice, offsetSeconds: Long) = withContext(Dispatchers.IO) {
        require(offsetSeconds != 0L) { "Смещение для перемотки не задано." }
        val controlUrl = checkNotNull(renderer.controlUrl) { "У устройства нет AVTransport control URL." }
        val service = renderer.serviceType ?: AVTRANSPORT_SERVICE
        val positionXml = soap(controlUrl, service, "GetPositionInfo", "<InstanceID>0</InstanceID>")
        val timeText = Regex("<RelTime>([^<]+)</RelTime>").find(positionXml)?.groupValues?.get(1)
            ?: error("Приёмник не сообщил текущую позицию для перемотки.")
        val fields = timeText.split(":")
        check(fields.size == 3) { "Приёмник вернул некорректную позицию: $timeText" }
        val currentSeconds = fields[0].toLong() * 3600 + fields[1].toLong() * 60 + fields[2].toLong()
        val targetSeconds = (currentSeconds + offsetSeconds).coerceAtLeast(0L)
        val hours = targetSeconds / 3600
        val minutes = (targetSeconds % 3600) / 60
        val seconds = targetSeconds % 60
        val target = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        soap(
            controlUrl,
            service,
            "Seek",
            "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>$target</Target>"
        )
    }

    suspend fun stop(renderer: DlnaDevice) = withContext(Dispatchers.IO) {
        val controlUrl = checkNotNull(renderer.controlUrl) { "У устройства нет AVTransport control URL." }
        soap(controlUrl, renderer.serviceType ?: AVTRANSPORT_SERVICE, "Stop", "<InstanceID>0</InstanceID>")
    }

    private fun resolveRenderer(raw: DlnaDevice): DlnaDevice? {
        val connection = URL(raw.location).openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        return try {
            if (connection.responseCode !in 200..299) return null
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            connection.inputStream.use { parser.setInput(it, null); parseDescription(raw, parser) }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDescription(raw: DlnaDevice, parser: XmlPullParser): DlnaDevice? {
        var baseUrl = raw.location
        var friendlyName: String? = null
        var controlPath: String? = null
        var serviceType: String? = null
        var insideService = false
        var candidateType: String? = null
        var candidateControl: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "URLBase" -> baseUrl = parser.nextText().ifBlank { raw.location }
                    "friendlyName" -> friendlyName = parser.nextText().takeIf { it.isNotBlank() }
                    "service" -> {
                        insideService = true
                        candidateType = null
                        candidateControl = null
                    }
                    "serviceType" -> if (insideService) candidateType = parser.nextText()
                    "controlURL" -> if (insideService) candidateControl = parser.nextText()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "service") {
                if (candidateType?.contains("AVTransport", ignoreCase = true) == true &&
                    !candidateControl.isNullOrBlank()
                ) {
                    serviceType = candidateType
                    controlPath = candidateControl
                }
                insideService = false
            }
            event = parser.next()
        }
        val path = controlPath ?: return null
        val controlUrl = runCatching { URL(URL(baseUrl), path).toString() }.getOrNull() ?: return null
        return raw.copy(
            friendlyName = friendlyName ?: raw.server ?: raw.location,
            controlUrl = controlUrl,
            serviceType = serviceType ?: AVTRANSPORT_SERVICE
        )
    }

    private fun soap(controlUrl: String, serviceType: String, action: String, innerXml: String): String {
        val body = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body><u:$action xmlns:u="$serviceType">$innerXml</u:$action></s:Body></s:Envelope>"""
        val connection = URL(controlUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        connection.setRequestProperty("SOAPAction", "\"$serviceType#$action\"")
        val bytes = body.toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(bytes.size)
        try {
            connection.outputStream.use { it.write(bytes) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                error("DLNA $action завершился с HTTP $code. ${detail.take(300)}")
            }
            connection.inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }

    private fun buildDidl(media: CastMedia): String {
        val itemClass = if (media.mimeType.startsWith("audio/")) "object.item.audioItem.musicTrack" else "object.item.videoItem"
        return """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns:dlna="urn:schemas-dlna-org:metadata-1-0/">
<item id="0" parentID="-1" restricted="1"><dc:title>${xmlEscape(media.title)}</dc:title><upnp:class>$itemClass</upnp:class><res protocolInfo="http-get:*:${xmlEscape(media.mimeType)}:*">${xmlEscape(media.uri)}</res></item></DIDL-Lite>"""
    }

    private fun String.headerValue(header: String): String? =
        lineSequence().firstOrNull { it.substringBefore(':').trim().equals(header, ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private companion object {
        const val HTTP_TIMEOUT_MS = 5_000
        const val AVTRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"
    }
}
