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
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val cacheFile = File(context.filesDir, "tv_playlist_combined.m3u")
    private val stampFile = File(context.filesDir, "tv_playlist_combined.timestamp")

    suspend fun loadCachedIfFresh(): List<StreamItem>? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists() || !stampFile.exists()) return@withContext null
        val stamp = stampFile.readText().toLongOrNull() ?: return@withContext null
        val age = System.currentTimeMillis() - stamp
        if (age !in 0..(24L * 60L * 60L * 1000L)) null else parse(cacheFile.readText())
    }

    suspend fun refreshCombined(): Result<List<StreamItem>> = withContext(Dispatchers.IO) {
        val downloaded = coroutineScope {
            TV_PLAYLIST_URLS.map { url ->
                async { url to runCatching { download(url) }.getOrNull() }
            }.awaitAll().toMap()
        }
        val parsed = TV_PLAYLIST_URLS.map { url -> downloaded[url].orEmpty() }.map(::parse)
        val base = parsed.firstOrNull().orEmpty()
        if (base.isEmpty() && parsed.drop(1).all { it.isEmpty() }) {
            return@withContext Result.failure(IllegalStateException("Не удалось загрузить объединённый плейлист"))
        }

        val merged = LinkedHashMap<String, StreamItem>()
        val urls = HashSet<String>()
        val orderedLists = if (base.isNotEmpty()) parsed else parsed.drop(1)
        orderedLists.forEach { list ->
            list.forEach { item ->
                val nameKey = normalizeChannelName(item.name)
                if (nameKey.isBlank()) return@forEach
                if (merged.containsKey(nameKey) || !urls.add(item.url)) return@forEach
                merged[nameKey] = item
            }
        }
        val result = merged.values.toList()
        if (result.isEmpty()) return@withContext Result.failure(IllegalStateException("Плейлист пуст"))
        cacheFile.writeText(toM3u(result))
        stampFile.writeText(System.currentTimeMillis().toString())
        Result.success(result)
    }

    private fun download(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "TV-Radio-Online/3.0").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string().orEmpty().also { if (it.isBlank()) error("Пустой ответ") }
        }
    }

    private fun parse(text: String): List<StreamItem> {
        val result = mutableListOf<StreamItem>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingEpgLogo: String? = null
        text.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> {
                    pendingName = line.substringAfterLast(',', "Без названия").trim().ifBlank { "Без названия" }
                    pendingLogo = attr(line, "tvg-logo")
                    pendingEpgLogo = attr(line, "epg-logo") ?: attr(line, "logo")
                }
                !line.startsWith("#") && pendingName != null && (line.startsWith("http://") || line.startsWith("https://")) -> {
                    result += StreamItem(pendingName!!, line, pendingLogo, pendingEpgLogo)
                    pendingName = null; pendingLogo = null; pendingEpgLogo = null
                }
            }
        }
        return result
    }

    private fun attr(line: String, name: String): String? {
        val regex = Regex("(?:^|\\s)$name=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun toM3u(items: List<StreamItem>): String = buildString {
        appendLine("#EXTM3U")
        items.forEach { item ->
            val logo = item.logoUrl?.let { " tvg-logo=\"$it\"" }.orEmpty()
            appendLine("#EXTINF:-1$logo,${item.name}")
            appendLine(item.url)
        }
    }

    private fun normalizeChannelName(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("\\(.*?\\)"), " ")
        .replace(Regex("\\[.*?]"), " ")
        .replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|480p)\\b"), " ")
        .filter(Char::isLetterOrDigit)
}
