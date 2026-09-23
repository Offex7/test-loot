package com.offex7.streamhub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class TvLoadedPlaylist(
    val sourceKey: String,
    val sourceName: String,
    val items: List<StreamItem>
)

private data class TvResolvedSource(val key: String, val name: String, val url: String)

class TvPlaylistRepositoryV8(context: Context, private val store: SettingsStore) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun cacheFile(source: TvResolvedSource): File =
        File(appContext.filesDir, "tv_v8_" + sha256(source.url).take(20) + ".m3u")

    private fun timestampFile(source: TvResolvedSource): File =
        File(appContext.filesDir, "tv_v8_" + sha256(source.url).take(20) + ".timestamp")

    private suspend fun sources(): List<TvResolvedSource> {
        val builtins = TV_SOURCES.mapIndexed { index, source ->
            TvResolvedSource(builtinSourceKey(index), source.name, source.url)
        }
        val users = store.userPlaylists().map { playlist ->
            TvResolvedSource(playlist.key, playlist.name, playlist.url)
        }
        return builtins + users
    }

    suspend fun load(activeSourceKey: String): Result<TvLoadedPlaylist> = withContext(Dispatchers.IO) {
        val all = sources()
        if (all.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Нет источников ТВ-плейлиста"))
        }

        val start = all.indexOfFirst { it.key == activeSourceKey }.let { if (it >= 0) it else 0 }
        var lastError: Throwable? = null

        for (offset in all.indices) {
            val source = all[(start + offset) % all.size]
            val loaded = runCatching {
                withTimeoutOrNull(15_000L) { loadSingle(source) }
                    ?: throw IllegalStateException("Таймаут загрузки источника")
            }.getOrElse {
                lastError = it
                null
            }

            if (!loaded.isNullOrEmpty()) {
                cache(source, loaded)
                return@withContext Result.success(TvLoadedPlaylist(source.key, source.name, loaded))
            }
            if (loaded != null) {
                lastError = IllegalStateException(source.name + ": пустой плейлист")
            }
        }

        Result.failure(lastError ?: IllegalStateException("Не удалось загрузить ТВ-плейлист"))
    }

    suspend fun cached(activeSourceKey: String): TvLoadedPlaylist? = withContext(Dispatchers.IO) {
        val source = sources().firstOrNull { it.key == activeSourceKey } ?: return@withContext null
        val file = cacheFile(source)
        val stamp = timestampFile(source)
        if (!file.exists() || !stamp.exists()) return@withContext null

        val ts = stamp.readText().toLongOrNull() ?: return@withContext null
        if (System.currentTimeMillis() - ts !in 0L..86_400_000L) return@withContext null

        val items = runCatching { parse(file.readText(), source.url) }.getOrNull().orEmpty()
        items.takeIf { it.isNotEmpty() }?.let {
            TvLoadedPlaylist(source.key, source.name, it)
        }
    }

    suspend fun checkAvailability(url: String): AvailabilityStatus = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext AvailabilityStatus.OFFLINE
        runCatching {
            client.newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .header("Range", "bytes=0-2047")
                    .header("User-Agent", "Radio.TV/9.0")
                    .build()
            ).execute().use { response ->
                when {
                    response.isSuccessful -> AvailabilityStatus.ONLINE
                    response.code in 300..399 -> AvailabilityStatus.ONLINE
                    response.code == 416 -> AvailabilityStatus.ONLINE
                    else -> AvailabilityStatus.OFFLINE
                }
            }
        }.getOrDefault(AvailabilityStatus.OFFLINE)
    }

    private fun loadSingle(source: TvResolvedSource): List<StreamItem> {
        val response = client.newCall(
            Request.Builder()
                .url(source.url)
                .header("User-Agent", "Radio.TV/9.0")
                .header("Accept", "application/vnd.apple.mpegurl, audio/x-mpegurl, application/x-mpegURL, text/plain, */*")
                .build()
        ).execute()

        return response.use { r ->
            if (!r.isSuccessful) error("HTTP " + r.code)
            val body = r.body?.string().orEmpty().removePrefix("\uFEFF")
            if (body.isBlank()) error("Пустой ответ")
            parse(body, r.request.url.toString())
        }
    }

    private fun cache(source: TvResolvedSource, items: List<StreamItem>) {
        runCatching {
            cacheFile(source).writeText(buildString {
                appendLine("#EXTM3U")
                items.forEach { item ->
                    append("#EXTINF:-1")
                    item.groupTitle?.takeIf { it.isNotBlank() }?.let { append(" group-title=\"" + escape(it) + "\"") }
                    item.logoUrl?.takeIf { it.isNotBlank() }?.let { append(" tvg-logo=\"" + escape(it) + "\"") }
                    append("," + item.name)
                    appendLine()
                    appendLine(item.url)
                }
            })
            timestampFile(source).writeText(System.currentTimeMillis().toString())
        }
    }

    private fun parse(text: String, baseUrl: String): List<StreamItem> {
        val result = ArrayList<StreamItem>()
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null

        text.lineSequence().map { it.trim().removePrefix("\uFEFF") }.forEach { line ->
            if (line.isBlank()) return@forEach

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.indexOf(",")
                    val title = if (comma >= 0) line.substring(comma + 1).trim() else "Без названия"
                    currentName = attr(line, "tvg-name")?.trim()?.takeIf { it.isNotBlank() }
                        ?: title.ifBlank { "Без названия" }
                    currentLogo = attr(line, "tvg-logo")?.let { resolveUrl(baseUrl, it) }
                    currentGroup = attr(line, "group-title")?.trim()
                }

                line.startsWith("#EXTGRP", ignoreCase = true) -> {
                    currentGroup = line.substringAfter(":", "").trim().ifBlank { currentGroup }
                }

                !line.startsWith("#") && currentName != null -> {
                    val candidate = line.trim().trim('"')
                    val streamUrl = resolveUrl(baseUrl, candidate)
                    if (streamUrl.startsWith("http://", true) || streamUrl.startsWith("https://", true)) {
                        result += StreamItem(
                            name = currentName.orEmpty(),
                            url = streamUrl,
                            logoUrl = currentLogo,
                            epgLogoUrl = null,
                            groupTitle = currentGroup
                        )
                    }
                    currentName = null
                    currentLogo = null
                    currentGroup = null
                }
            }
        }

        return result.distinctBy { it.url }
    }

    private fun attr(line: String, key: String): String? {
        val regex = Regex("""(?:^|\s)$key\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s]+))""", RegexOption.IGNORE_CASE)
        val match = regex.find(line) ?: return null
        return listOf(
            match.groups[1]?.value,
            match.groups[2]?.value,
            match.groups[3]?.value
        ).firstOrNull { !it.isNullOrBlank() }
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String =
        runCatching { URI(baseUrl).resolve(candidate).toString() }.getOrElse { candidate }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
