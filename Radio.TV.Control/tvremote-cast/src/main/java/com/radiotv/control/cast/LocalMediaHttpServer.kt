package com.radiotv.control.cast

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import fi.iki.elonen.NanoHTTPD
import java.io.FilterInputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

/**
 * Shares one user-selected content:// URI over the local network so a DLNA renderer can fetch it.
 * The stream is read on demand (and reopened per request) rather than copying large files to cache.
 */
class LocalMediaHttpServer(context: Context) : NanoHTTPD("0.0.0.0", 0), AutoCloseable {
    private val appContext = context.applicationContext
    @Volatile private var selectedUri: Uri? = null
    @Volatile private var selectedMime: String = "application/octet-stream"
    @Volatile private var selectedTitle: String = "media"
    @Volatile private var selectedLength: Long = -1L

    @Synchronized
    fun start(uri: Uri): CastMedia {
        val info = readMetadata(uri)
        val extension = info.title.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeLower = info.mime.lowercase(Locale.ROOT)
        require(extension !in setOf("m3u", "m3u8", "mpd", "wvm") &&
            !mimeLower.contains("mpegurl") &&
            !mimeLower.contains("dash+xml") &&
            !mimeLower.contains("oma.drm") &&
            !mimeLower.contains("drm.content")) {
            "DRM/HLS/DASH-контент напрямую не поддерживается. Выберите обычный локальный медиафайл."
        }
        if (isAlive) stop()
        selectedUri = uri
        selectedMime = info.mime
        selectedTitle = info.title
        selectedLength = info.length
        try {
            start(SOCKET_READ_TIMEOUT, false)
            val address = resolveLocalIpv4()
            return CastMedia(info.title, "http://$address:$listeningPort/media", info.mime)
        } catch (error: Exception) {
            runCatching { stop() }
            throw IllegalStateException(
                "Не удалось открыть локальную HTTP-раздачу. Убедитесь, что телефон и телевизор в одной Wi‑Fi сети и разрешён доступ к локальной сети.",
                error
            )
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = selectedUri ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Медиа не выбрано.")
        val mime = selectedMime
        val total = selectedLength
        val rangeHeader = session.headers["range"]
        val range = parseRange(rangeHeader, total)
        if (rangeHeader != null && range == null) {
            val response = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Неподдерживаемый диапазон.")
            if (total > 0L) response.addHeader("Content-Range", "bytes */$total")
            return response
        }
        return try {
            val start = range?.first ?: 0L
            val end = range?.last ?: if (total > 0L) total - 1 else -1L
            val contentLength = if (total > 0L) (end - start + 1).coerceAtLeast(0L) else -1L
            val raw = appContext.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Файл недоступен.")
            val stream = raw.createInputStream()
            skipFully(stream, start)
            val bounded = if (contentLength >= 0) LimitedInputStream(stream, contentLength) else stream
            val response = if (range != null && contentLength >= 0) {
                newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, bounded, contentLength)
            } else if (contentLength >= 0) {
                newFixedLengthResponse(Response.Status.OK, mime, bounded, contentLength)
            } else {
                newChunkedResponse(Response.Status.OK, mime, bounded)
            }
            response.addHeader("Accept-Ranges", "bytes")
            if (range != null && total > 0L) response.addHeader("Content-Range", "bytes $start-$end/$total")
            response.addHeader("Cache-Control", "no-store")
            response
        } catch (error: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, error.message ?: "Ошибка чтения файла.")
        }
    }

    private fun readMetadata(uri: Uri): Metadata {
        var title = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "media"
        var length = -1L
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) title = cursor.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: title
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) length = cursor.getLong(sizeIndex)
            }
        }
        val mime = appContext.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(title.substringAfterLast('.', "").lowercase(Locale.ROOT))
            ?: "application/octet-stream"
        return Metadata(title, mime, length)
    }

    private fun resolveLocalIpv4(): String {
        val candidates = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .flatMap { network ->
                Collections.list(network.inetAddresses)
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && it.isSiteLocalAddress }
                    .map { network.name to it.hostAddress }
            }
        return candidates.sortedBy { (name, _) -> if (name.startsWith("wlan", true) || name.startsWith("wifi", true)) 0 else 1 }
            .firstOrNull()?.second
            ?: error("Не удалось определить IPv4-адрес Wi‑Fi. Подключите телефон к локальной сети.")
    }

    private fun parseRange(header: String?, total: Long): LongRange? {
        if (header == null) return null
        if (total <= 0L) return null
        val match = Regex("""bytes=(\d*)-(\d*)""").matchEntire(header.trim()) ?: return null
        val first = match.groupValues[1]
        val last = match.groupValues[2]
        val start = first.toLongOrNull() ?: 0L
        val requestedEnd = last.toLongOrNull() ?: (total - 1)
        if (start !in 0 until total || requestedEnd < start) return null
        return start..minOf(requestedEnd, total - 1)
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var left = bytes
        while (left > 0L) {
            val skipped = stream.skip(left)
            if (skipped > 0L) left -= skipped
            else {
                if (stream.read() == -1) error("Невозможно прочитать выбранный файл.")
                left--
            }
        }
    }

    private data class Metadata(val title: String, val mime: String, val length: Long)

    private class LimitedInputStream(input: InputStream, limit: Long) : FilterInputStream(input) {
        private var remaining = limit
        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = super.read()
            if (value >= 0) remaining--
            return value
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0L) return -1
            val count = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (count > 0) remaining -= count.toLong()
            return count
        }
        override fun skip(count: Long): Long {
            val skipped = super.skip(minOf(count, remaining))
            remaining -= skipped
            return skipped
        }
    }

    override fun close() {
        selectedUri = null
        if (isAlive) stop()
    }
}