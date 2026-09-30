package com.offex7.streamhub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlaylistRepository(
    private val context: Context,
    private val store: SettingsStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private data class ResolvedSource(val key: String, val name: String, val url: String)
    private data class DownloadedPlaylist(val text: String, val finalUrl: String)

    private val fallbackByName = mutableMapOf<String, List<String>>()
    @Volatile private var warming = false

    private suspend fun allSources(): List<ResolvedSource> {
        val builtins = TV_SOURCES.mapIndexed { index, source ->
            ResolvedSource(builtinSourceKey(index), source.name, source.url)
        }
        val users = store.userPlaylists().map { playlist ->
            ResolvedSource(playlist.key, playlist.name, playlist.url)
        }
        return builtins + users
    }

    private fun cacheFile(source: ResolvedSource): File {
        val id = if (source.key.startsWith(BUILTIN_SOURCE_PREFIX)) {
            "tv_playlist_${source.key.removePrefix(BUILTIN_SOURCE_PREFIX)}"
        } else {
            "tv_playlist_user_${sha256(source.url).take(20)}"
        }
        return File(context.filesDir, "$id.m3u")
    }

    private fun stampFile(source: ResolvedSource): File =
        File(context.filesDir, "${cacheFile(source).name}.timestamp")

    suspend fun loadCached(sourceKey: String): List<StreamItem>? = withContext(Dispatchers.IO) {
        val source = allSources().firstOrNull { it.key == sourceKey } ?: return@withContext null
        val playlist = cacheFile(source)
        val stamp = stampFile(source)
        if (!playlist.exists() || !stamp.exists()) return@withContext null
        val ts = stamp.readText().toLongOrNull() ?: return@withContext null
        if (System.currentTimeMillis() - ts in 0..86_400_000L) {
            parse(playlist.readText(), source.url)
        } else null
    }

    suspend fun loadSource(sourceKey: String): Result<List<StreamItem>> = withContext(Dispatchers.IO) {
        val source = allSources().firstOrNull { it.key == sourceKey }
            ?: return@withContext Result.failure(IllegalStateException("Источник не найден"))
        val downloaded = withTimeoutOrNull(15_000L) {
            runCatching { download(source.url) }.getOrNull()
        } ?: return@withContext Result.failure(IllegalStateException("Таймаут загрузки источника"))
        runCatching {
            val items = parse(downloaded.text, downloaded.finalUrl)
            if (items.isEmpty()) error("Пустой плейлист")
            writeCache(source, items)
            items
        }
    }

    suspend fun warmFallbacks() {
        if (warming) return
        synchronized(this) {
            if (warming) return
            warming = true
        }
        try {
            coroutineScope {
                allSources().map { source ->
                    async(Dispatchers.IO) {
                        runCatching {
                            val list = loadCached(source.key) ?: run {
                                val downloaded = download(source.url)
                                parse(downloaded.text, downloaded.finalUrl).also { writeCache(source, it) }
                            }
                            list.forEach { item ->
                                val key = normalizeChannelName(item.name)
                                if (key.isNotBlank()) synchronized(fallbackByName) {
                                    val old = fallbackByName[key].orEmpty()
                                    if (item.url !in old) fallbackByName[key] = old + item.url
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        } finally {
            warming = false
        }
    }

    fun fallbackUrlsFor(name: String): List<String> =
        synchronized(fallbackByName) { fallbackByName[normalizeChannelName(name)].orEmpty() }

    private fun writeCache(source: ResolvedSource, items: List<StreamItem>) {
        runCatching {
            cacheFile(source).writeText(toM3u(items))
            stampFile(source).writeText(System.currentTimeMillis().toString())
        }
    }

    private fun download(url: String): DownloadedPlaylist {
        val response = client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "TV-Radio-Online/7.0")
                .build()
        ).execute()
        return response.use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            val body = r.body?.string().orEmpty().removePrefix("\uFEFF")
            if (body.isBlank()) error("Пустой ответ")
            DownloadedPlaylist(body, r.request.url.toString())
        }
    }

    private fun parse(text: String, baseUrl: String): List<StreamItem> {
        val result = mutableListOf<StreamItem>()
        var name: String? = null
        var logo: String? = null
        var epg: String? = null

        text.lineSequence().map { it.trim().removePrefix("\uFEFF") }.forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> {
                    val fallbackName = line.substringAfterLast(",", "Без названия").trim().ifBlank { "Без названия" }
                    name = attr(line, "tvg-name", baseUrl)?.trim()?.takeIf { it.isNotBlank() } ?: fallbackName
                    logo = attr(line, "tvg-logo", baseUrl)
                    epg = attr(line, "epg-logo", baseUrl) ?: attr(line, "logo", baseUrl)
                }
                !line.startsWith("#") && !line.startsWith(";") && name != null && line.isNotBlank() -> {
                    val streamUrl = resolveUrl(baseUrl, line)
                    if (streamUrl.startsWith("http://", true) || streamUrl.startsWith("https://", true)) {
                        result += StreamItem(name!!, streamUrl, logo, epg)
                        name = null
                        logo = null
                        epg = null
                    }
                }
            }
        }
        return result
    }

    private fun attr(line: String, key: String, baseUrl: String): String? {
        val regex = Regex(
            """(?:^|\s)$key\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s]+))""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(line) ?: return null
        val raw = listOf(
            match.groupValues.getOrNull(1),
            match.groupValues.getOrNull(2),
            match.groupValues.getOrNull(3)
        ).firstOrNull { !it.isNullOrBlank() } ?: return null
        return resolveUrl(baseUrl, raw)
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String =
        runCatching { URI(baseUrl).resolve(candidate).toString() }.getOrElse { candidate }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun toM3u(items: List<StreamItem>): String = buildString {
        appendLine("#EXTM3U")
        items.forEach {
            val logo = it.logoUrl?.let { value -> " tvg-logo=\"$value\"" }.orEmpty()
            appendLine("#EXTINF:-1$logo,${it.name}")
            appendLine(it.url)
        }
    }

    private fun normalizeChannelName(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("\\[.*?]"), " ")
            .replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|480p|50fps|60fps)\\b"), " ")
            .filter(Char::isLetterOrDigit)
}