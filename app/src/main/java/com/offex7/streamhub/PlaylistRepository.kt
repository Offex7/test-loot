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
        val f = cacheFile(index); val s = stampFile(index)
        if (!f.exists() || !s.exists()) return@withContext null
        val ts = s.readText().toLongOrNull() ?: return@withContext null
        if (System.currentTimeMillis() - ts in 0..86_400_000L) parse(f.readText()) else null
    }

    suspend fun loadSource(index: Int): Result<List<StreamItem>> = withContext(Dispatchers.IO) {
        val selected = index.coerceIn(0, TV_SOURCES.lastIndex)
        val candidates = if (selected == 0) (0..TV_SOURCES.lastIndex).toList() else listOf(selected)
        var last: Throwable? = null
        for (i in candidates) {
            try {
                val list = parse(download(TV_SOURCES[i].url))
                if (list.isNotEmpty()) { writeCache(i, list); return@withContext Result.success(list) }
                last = IllegalStateException("Плейлист пуст")
            } catch (t: Throwable) { last = t }
        }
        Result.failure(last ?: IllegalStateException("Не удалось загрузить ТВ-плейлист"))
    }

    suspend fun warmFallbacks() {
        if (warming) return
        synchronized(this) { if (warming) return; warming = true }
        try {
            coroutineScope {
                TV_PLAYLIST_URLS.drop(1).mapIndexed { idx, url ->
                    async(Dispatchers.IO) {
                        runCatching {
                            val list = parse(download(url)); writeCache(idx + 1, list)
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
        } finally { warming = false }
    }

    fun fallbackUrlsFor(name: String): List<String> = synchronized(fallbackByName) { fallbackByName[normalizeChannelName(name)].orEmpty() }

    private fun writeCache(index: Int, items: List<StreamItem>) = runCatching { cacheFile(index).writeText(toM3u(items)); stampFile(index).writeText(System.currentTimeMillis().toString()) }
    private fun download(url: String): String {
        val response = client.newCall(Request.Builder().url(url).header("User-Agent", "TV-Radio-Online/4.0").build()).execute()
        return response.use { r -> if (!r.isSuccessful) error("HTTP ${r.code}"); r.body?.string().orEmpty().also { if (it.isBlank()) error("Пустой ответ") } }
    }
    private fun parse(text: String): List<StreamItem> {
        val result = mutableListOf<StreamItem>(); var name: String? = null; var logo: String? = null; var epg: String? = null
        text.lineSequence().map(String::trim).forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> { name = line.substringAfterLast(',', "Без названия").trim().ifBlank { "Без названия" }; logo = attr(line,"tvg-logo"); epg = attr(line,"epg-logo") ?: attr(line,"logo") }
                !line.startsWith("#") && name != null && (line.startsWith("http://") || line.startsWith("https://")) -> { result += StreamItem(name!!, line, logo, epg); name=null; logo=null; epg=null }
            }
        }
        return result
    }
    private fun attr(line:String,key:String):String? = Regex("(?:^|\\s)$key=\\\"([^\\\"]+)\\\"",RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)?.takeIf{it.startsWith("http://")||it.startsWith("https://")}
    private fun toM3u(items:List<StreamItem>)=buildString{appendLine("#EXTM3U");items.forEach{val l=it.logoUrl?.let{" tvg-logo=\"$it\""}.orEmpty();appendLine("#EXTINF:-1$l,${it.name}");appendLine(it.url)}}
    private fun normalizeChannelName(value:String)=value.lowercase(Locale.ROOT).replace(Regex("\\(.*?\\)")," ").replace(Regex("\\[.*?]")," ").replace(Regex("\\b(fhd|uhd|hd|sd|4k|1080p|720p|576p|480p|50fps|60fps)\\b")," ").filter(Char::isLetterOrDigit)
}
