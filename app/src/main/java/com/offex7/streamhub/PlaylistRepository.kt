package com.offex7.streamhub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val context: Context) {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private fun cacheFile(index: Int) = File(context.filesDir, "tv_playlist_$index.m3u")
    private fun stampFile(index: Int) = File(context.filesDir, "tv_playlist_$index.timestamp")
    private val fallbackByName = mutableMapOf<String, List<String>>()
    @Volatile private var warming = false

    suspend fun loadCached(index: Int): List<StreamItem>? = withContext(Dispatchers.IO) {
        val cache = cacheFile(index); val stamp = stampFile(index)
        if (!cache.exists() || !stamp.exists()) return@withContext null
        val ts = stamp.readText().toLongOrNull() ?: return@withContext null
        val age = System.currentTimeMillis() - ts
        if (age !in 0..86_400_000L) null else parse(cache.readText())
    }

    suspend fun loadSource(index: Int): Result<List<StreamItem>> = withContext(Dispatchers.IO) {
        val selected = index.coerceIn(0, TV_SOURCES.lastIndex)
        val candidateIndexes = if (selected == 0) listOf(1, 2, 3, 4, 5, 6) else listOf(selected)
        var lastError: Throwable? = null
        for (sourceIndex in candidateIndexes) {
            try {
                val list = parse(download(TV_SOURCES[sourceIndex].url))
                if (list.isNotEmpty()) {
                    writeCache(sourceIndex, list)
                    return@withContext Result.success(list)
                }
                lastError = IllegalStateException("Плейлист пуст")
            } catch (t: Throwable) { lastError = t }
        }
        Result.failure(lastError ?: IllegalStateException("Не удалось загрузить ТВ-плейлист"))
    }

    suspend fun warmFallbacks() {
        if (warming) return
        synchronized(this) { if (warming) return; warming = true }
        try {
            coroutineScope {
                TV_PLAYLIST_URLS.drop(1).mapIndexed { offset, url ->
                    async(Dispatchers.IO) {
                        runCatching {
                            val list = parse(download(url))
                            writeCache(offset + 2, list)
                            list.forEach { item ->
                                val key = normalizeChannelName(item.name)
                                if (key.isNotBlank()) synchronized(fallbackByName) {
                                    val bucket = fallbackByName[key].orEmpty()
                                    if (item.url !in bucket) fallbackByName[key] = bucket + item.url
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        } finally { warming = false }
    }

    fun fallbackUrlsFor(name: String): List<String> = synchronized(fallbackByName) { fallbackByName[normalizeChannelName(name)].orEmpty() }

    private fun writeCache(index: Int, items: List<StreamItem>) = runCatching { cacheFile(index).writeText(toM3u(items)); stampFile(index).writeText(System.currentTimeMillis().toString()) }
    private fun download(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "TV-Radio-Online/4.0").build()
        return client.newCall(request).execute().use { response -> if (!response.isSuccessful) error("HTTP ${response.code}"); response.body?.string().orEmpty().also { if (it.isBlank()) error("Пустой ответ") } }
    }
    private fun parse(text: String): List<StreamItem> {
        val result = mutableListOf<StreamItem>(); var name: String? = null; var logo: String? = null; var epg: String? = null
        text.lineSequence().map(String::trim).forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> { name = line.substringAfterLast(',', "Без названия").trim().ifBlank { "Без названия" }; logo = attr(line, "tvg-logo"); epg = attr(line, "epg-logo") ?: attr(line, "logo") }
                !line.startsWith("#") && name != null && (line.startsWith("http://") || line.startsWith("https://")) -> { result += StreamItem(name!!, line, logo, epg); name = null; logo = null; epg = null }
            }
        }
        return result
    }
    private fun attr(line: String, key: String): String? = Regex("(?:^|\\s)$key=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    private fun toM3u(items: List<StreamItem>): String = buildString { appendLine("#EXTM3U"); items.forEach { item -> val logo = item.logoUrl?.let { " tvg-logo=\"$it\"" }.orEmpty(); appendLine("#EXTINF:-1$logo,${item.name}"); appendLine(item.url) } }
    private fun normalizeChannelName(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("\\(.*?\\)"), " ").replace(Regex("\\[.*?]"), " ").replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|480p|50fps|60fps)\\b"), " ").filter(Char::isLetterOrDigit)
}
