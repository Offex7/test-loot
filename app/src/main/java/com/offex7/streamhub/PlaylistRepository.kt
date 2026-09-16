package com.offex7.streamhub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import okhttp3.ResponseBody
import java.io.File
import java.util.concurrent.TimeUnit

private interface PlaylistApi { @GET suspend fun getPlaylist(@Url url: String): Response<ResponseBody> }

class PlaylistRepository(private val context: Context) {
    private val api = Retrofit.Builder().baseUrl("https://example.com/").build().create(PlaylistApi::class.java)
    private val headClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val cacheFile = File(context.filesDir, "tv_playlist.m3u")
    private val stampFile = File(context.filesDir, "tv_playlist.timestamp")

    suspend fun loadCachedIfFresh(): List<StreamItem>? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists() || !stampFile.exists()) return@withContext null
        val age = System.currentTimeMillis() - (stampFile.readText().toLongOrNull() ?: Long.MAX_VALUE)
        if (age > 24L * 60L * 60L * 1000L) null else parse(cacheFile.readText())
    }

    suspend fun refreshInOrder(preferredIndex: Int): Result<Pair<Int, List<StreamItem>>> = withContext(Dispatchers.IO) {
        val safeIndex = preferredIndex.coerceIn(0, TV_SOURCES.lastIndex)
        val indexes = listOf(safeIndex) + TV_SOURCES.indices.filter { it != safeIndex }
        var lastError: Throwable? = null
        for (index in indexes) try {
            val response = api.getPlaylist(TV_SOURCES[index].url)
            val body = response.body()?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) throw IllegalStateException("HTTP ${response.code()}")
            val parsed = parse(body)
            if (parsed.isEmpty()) throw IllegalStateException("Плейлист пуст")
            cacheFile.writeText(body)
            stampFile.writeText(System.currentTimeMillis().toString())
            return@withContext Result.success(index to parsed)
        } catch (t: Throwable) { lastError = t }
        Result.failure(lastError ?: IllegalStateException("Все источники недоступны"))
    }

    suspend fun checkAvailability(items: List<StreamItem>): Map<String, AvailabilityStatus> = withContext(Dispatchers.IO) {
        coroutineScope {
            items.map { item ->
                async {
                    val request = Request.Builder().url(item.url).head().build()
                    val status = runCatching { headClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE
                    } }.getOrDefault(AvailabilityStatus.OFFLINE)
                    item.url to status
                }
            }.awaitAll().toMap()
        }
    }

    private fun parse(text: String): List<StreamItem> {
        val result = mutableListOf<StreamItem>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingEpgLogo: String? = null
        for (line in text.lines().map(String::trim).filter(String::isNotEmpty)) {
            if (line.startsWith("#EXTINF", true)) {
                pendingName = line.substringAfter(',', "").trim().ifBlank { "Без названия" }
                pendingLogo = attr(line, "tvg-logo")
                pendingEpgLogo = attr(line, "epg-logo")
            } else if (!line.startsWith("#") && pendingName != null &&
                (line.startsWith("http://") || line.startsWith("https://"))) {
                result += StreamItem(pendingName!!, line, pendingLogo, pendingEpgLogo)
                pendingName = null
                pendingLogo = null
                pendingEpgLogo = null
            }
        }
        return result.distinctBy { it.url }
    }

    private fun attr(line: String, name: String): String? {
        val regex = Regex("(?:^|\\s)$name=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
