package com.offex7.streamhub

import android.content.Context
import android.util.AtomicFile
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
    val items: List<StreamItem>,
    val epgUrl: String? = null
)

private data class TvResolvedSource(val key: String, val name: String, val url: String)
private data class ParsedTvPlaylist(val items: List<StreamItem>, val epgUrl: String?)

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
        return users + builtins
    }

    suspend fun load(activeSourceKey: String): Result<TvLoadedPlaylist> = withContext(Dispatchers.IO) {
        val source = sources().firstOrNull { it.key == activeSourceKey }
            ?: return@withContext Result.failure(IllegalStateException("Источник не найден"))
        val loaded = runCatching {
            withTimeoutOrNull(15_000L) { loadSingle(source) }
                ?: throw IllegalStateException("Таймаут загрузки источника")
        }.getOrElse { return@withContext Result.failure(it) }

        if (loaded.items.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException(source.name + ": пустой плейлист")
            )
        }

        cache(source, loaded)
        Result.success(TvLoadedPlaylist(source.key, source.name, loaded.items, loaded.epgUrl))
    }

    suspend fun cached(activeSourceKey: String): TvLoadedPlaylist? = withContext(Dispatchers.IO) {
        val source = sources().firstOrNull { it.key == activeSourceKey } ?: return@withContext null
        val file = cacheFile(source)
        // A valid local playlist remains usable even when it is older than 24 hours.
        // The fresh network parse below updates it in the background.
        val parsed = runCatching { parsePlaylist(readCacheAtomically(file), source.url) }.getOrNull()
            ?: return@withContext null
        parsed.items.takeIf { it.isNotEmpty() }?.let {
            TvLoadedPlaylist(source.key, source.name, it, parsed.epgUrl)
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
                    .header("User-Agent", "Radio.TV/4.8")
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

    private fun loadSingle(source: TvResolvedSource): ParsedTvPlaylist {
        val response = client.newCall(
            Request.Builder()
                .url(source.url)
                .header("User-Agent", "Radio.TV/4.8")
                .header("Accept", "application/vnd.apple.mpegurl, audio/x-mpegurl, application/x-mpegURL, text/plain, */*")
                .build()
        ).execute()

        return response.use { r ->
            if (!r.isSuccessful) error("HTTP " + r.code)
            val body = r.body?.string().orEmpty().removePrefix("\uFEFF")
            if (body.isBlank()) error("Пустой ответ")
            parsePlaylist(body, r.request.url.toString())
        }
    }

    private fun cache(source: TvResolvedSource, playlist: ParsedTvPlaylist) {
        runCatching {
            writeCacheAtomically(cacheFile(source), buildString {
                append("#EXTM3U")
                playlist.epgUrl?.takeIf { it.isNotBlank() }
                    ?.let { append(" x-tvg-url=\"" + escape(it) + "\"") }
                appendLine()
                playlist.items.forEach { item ->
                    append("#EXTINF:-1")
                    item.tvgId?.takeIf { it.isNotBlank() }
                        ?.let { append(" tvg-id=\"" + escape(it) + "\"") }
                    item.groupTitle?.takeIf { it.isNotBlank() }
                        ?.let { append(" group-title=\"" + escape(it) + "\"") }
                    item.logoUrl?.takeIf { it.isNotBlank() }
                        ?.let { append(" tvg-logo=\"" + escape(it) + "\"") }
                    append("," + item.name)
                    appendLine()
                    appendLine(item.url)
                }
            })
            writeCacheAtomically(timestampFile(source), System.currentTimeMillis().toString())
        }
    }

    private fun readCacheAtomically(file: File): String =
        AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun writeCacheAtomically(file: File, content: String) {
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(content.toByteArray(Charsets.UTF_8))
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun parsePlaylist(text: String, baseUrl: String): ParsedTvPlaylist {
        val result = ArrayList<StreamItem>()
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null
        var epgUrl: String? = null

        text.lineSequence().map { it.trim().removePrefix("\uFEFF") }.forEach { line ->
            if (line.isBlank()) return@forEach

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val rawEpgUrl = attr(line, "x-tvg-url") ?: attr(line, "url-tvg")
                    epgUrl = rawEpgUrl
                        ?.split(',')
                        ?.firstOrNull()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { resolveUrl(baseUrl, it) }
                }

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.indexOf(",")
                    val title = if (comma >= 0) line.substring(comma + 1).trim() else "Без названия"
                    currentName = attr(line, "tvg-name")?.trim()?.takeIf { it.isNotBlank() }
                        ?: title.ifBlank { "Без названия" }
                    currentTvgId = attr(line, "tvg-id")?.trim()?.takeIf { it.isNotBlank() }
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
                            groupTitle = currentGroup,
                            tvgId = currentTvgId
                        )
                    }
                    currentName = null
                    currentLogo = null
                    currentGroup = null
                    currentTvgId = null
                }
            }
        }

        return ParsedTvPlaylist(result.distinctBy { it.url }, epgUrl)
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
