package com.offex7.streamhub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.AtomicFile
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

private const val TV_EPG_TAG = "TvEpgRepository"
private const val EPG_DAY_MS = 24L * 60L * 60L * 1000L
private const val EPG_CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1000L
private const val EPG_MAX_UNCOMPRESSED_BYTES = 120L * 1024L * 1024L
private const val EPG_MAX_PROGRAMMES = 250_000

data class TvProgramme(
    val startMillis: Long,
    val stopMillis: Long,
    val title: String,
    val description: String
)

enum class TvEpgUnavailableReason {
    NO_LINK,
    MOBILE_RESTRICTED,
    PHONE_STATE_PERMISSION_REQUIRED,
    NO_NETWORK,
    INVALID_OR_EMPTY
}

data class TvEpgResult(
    val programmes: List<TvProgramme> = emptyList(),
    val unavailableReason: TvEpgUnavailableReason? = null,
    val isLoading: Boolean = false
) {
    val requiresPhoneStatePermission: Boolean
        get() = unavailableReason == TvEpgUnavailableReason.PHONE_STATE_PERMISSION_REQUIRED
}

private data class TvEpgCatalog(
    val channelIds: Set<String>,
    val programmesByChannel: Map<String, List<TvProgramme>>
)

private enum class EpgNetworkPolicy {
    ALLOWED,
    MOBILE_RESTRICTED,
    PHONE_PERMISSION_REQUIRED,
    NO_NETWORK
}

private data class CatalogLoad(
    val catalog: TvEpgCatalog?,
    val reason: TvEpgUnavailableReason?
)

/** XMLTV parsing and network I/O always run on Dispatchers.IO. */
class TvEpgRepository(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val mutex = Mutex()
    private val catalogs = mutableMapOf<String, TvEpgCatalog>()

    suspend fun preload(epgUrl: String?) {
        if (epgUrl.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            try {
                mutex.withLock { loadCatalogLocked(epgUrl.trim()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TV_EPG_TAG, "EPG preload failed", error)
            }
        }
    }

    suspend fun programmes(epgUrl: String?, tvgId: String?): TvEpgResult =
        withContext(Dispatchers.IO) {
            if (epgUrl.isNullOrBlank() || tvgId.isNullOrBlank()) {
                return@withContext TvEpgResult(unavailableReason = TvEpgUnavailableReason.NO_LINK)
            }
            try {
                val loaded = mutex.withLock { loadCatalogLocked(epgUrl.trim()) }
                val catalog = loaded.catalog
                    ?: return@withContext TvEpgResult(unavailableReason = loaded.reason)
                if (tvgId !in catalog.channelIds) {
                    return@withContext TvEpgResult(unavailableReason = TvEpgUnavailableReason.INVALID_OR_EMPTY)
                }
                val now = System.currentTimeMillis()
                val upcoming = catalog.programmesByChannel[tvgId]
                    .orEmpty()
                    .asSequence()
                    .filter { it.stopMillis > now && it.startMillis < now + EPG_DAY_MS }
                    .sortedBy { it.startMillis }
                    .toList()
                if (upcoming.isEmpty()) {
                    TvEpgResult(unavailableReason = TvEpgUnavailableReason.INVALID_OR_EMPTY)
                } else {
                    TvEpgResult(programmes = upcoming)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TV_EPG_TAG, "EPG parsing failed", error)
                TvEpgResult(unavailableReason = TvEpgUnavailableReason.INVALID_OR_EMPTY)
            }
        }

    private suspend fun loadCatalogLocked(url: String): CatalogLoad {
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
            return CatalogLoad(null, TvEpgUnavailableReason.NO_LINK)
        }

        val cache = cacheFile(url)
        val cacheExists = cache.isFile && cache.length() > 0L
        val cacheFresh = cacheExists &&
            System.currentTimeMillis() - cache.lastModified() < EPG_CACHE_MAX_AGE_MS

        var cachedCatalog = catalogs[url]
        if (cachedCatalog == null && cacheExists) {
            cachedCatalog = try {
                parseXmlTv(cache)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TV_EPG_TAG, "Cached XMLTV is damaged; will try to refresh it", error)
                null
            }
            if (cachedCatalog != null) catalogs[url] = cachedCatalog
        }

        if (cachedCatalog != null && cacheFresh) return CatalogLoad(cachedCatalog, null)

        when (networkPolicy()) {
            EpgNetworkPolicy.MOBILE_RESTRICTED ->
                return if (cachedCatalog != null) CatalogLoad(cachedCatalog, null)
                else CatalogLoad(null, TvEpgUnavailableReason.MOBILE_RESTRICTED)
            EpgNetworkPolicy.PHONE_PERMISSION_REQUIRED ->
                return if (cachedCatalog != null) CatalogLoad(cachedCatalog, null)
                else CatalogLoad(null, TvEpgUnavailableReason.PHONE_STATE_PERMISSION_REQUIRED)
            EpgNetworkPolicy.NO_NETWORK ->
                return if (cachedCatalog != null) CatalogLoad(cachedCatalog, null)
                else CatalogLoad(null, TvEpgUnavailableReason.NO_NETWORK)
            EpgNetworkPolicy.ALLOWED -> Unit
        }

        return try {
            downloadXmlTv(url, cache)
            val parsed = parseXmlTv(cache)
            if (parsed.channelIds.isEmpty() || parsed.programmesByChannel.isEmpty()) {
                throw IllegalStateException("XMLTV contains no channels or programmes")
            }
            catalogs[url] = parsed
            CatalogLoad(parsed, null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TV_EPG_TAG, "XMLTV download or parsing failed", error)
            if (cachedCatalog != null) CatalogLoad(cachedCatalog, null)
            else CatalogLoad(null, TvEpgUnavailableReason.INVALID_OR_EMPTY)
        }
    }

    private fun networkPolicy(): EpgNetworkPolicy {
        val manager = connectivityManager ?: return EpgNetworkPolicy.NO_NETWORK
        val network = manager.activeNetwork ?: return EpgNetworkPolicy.NO_NETWORK
        val capabilities = manager.getNetworkCapabilities(network) ?: return EpgNetworkPolicy.NO_NETWORK
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return EpgNetworkPolicy.NO_NETWORK
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) return EpgNetworkPolicy.ALLOWED

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return EpgNetworkPolicy.NO_NETWORK
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_BASIC_PHONE_STATE
        } else {
            Manifest.permission.READ_PHONE_STATE
        }
        if (appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            return EpgNetworkPolicy.PHONE_PERMISSION_REQUIRED
        }

        val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return EpgNetworkPolicy.MOBILE_RESTRICTED
        val networkType = try {
            telephony.dataNetworkType
        } catch (error: SecurityException) {
            Log.w(TV_EPG_TAG, "Cannot read cellular network type", error)
            return EpgNetworkPolicy.PHONE_PERMISSION_REQUIRED
        } catch (_: UnsupportedOperationException) {
            return EpgNetworkPolicy.MOBILE_RESTRICTED
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE,
            19, // NETWORK_TYPE_LTE_CA (LTE Carrier Aggregation).
            TelephonyManager.NETWORK_TYPE_NR -> EpgNetworkPolicy.ALLOWED
            else -> EpgNetworkPolicy.MOBILE_RESTRICTED
        }
    }

    private fun cacheFile(url: String): File {
        val directory = File(appContext.filesDir, "epg_cache")
        if (!directory.exists()) directory.mkdirs()
        return File(directory, "xmltv_" + sha256(url).take(24) + ".xml")
    }

    private fun downloadXmlTv(url: String, destination: File) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Radio.TV/4.16")
            .header("Accept", "application/xml, text/xml, application/gzip, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("XMLTV HTTP " + response.code)
            val body = response.body ?: error("Empty XMLTV response body")
            val raw = BufferedInputStream(body.byteStream())
            raw.mark(2)
            val first = raw.read()
            val second = raw.read()
            raw.reset()
            val decoded: InputStream =
                if (first == 0x1f && second == 0x8b) GZIPInputStream(raw) else raw
            val atomicFile = AtomicFile(destination)
            val output = atomicFile.startWrite()
            try {
                decoded.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= EPG_MAX_UNCOMPRESSED_BYTES) {
                            "XMLTV file exceeds the safe size limit"
                        }
                        output.write(buffer, 0, count)
                    }
                }
                output.fd.sync()
                atomicFile.finishWrite(output)
            } catch (error: Throwable) {
                atomicFile.failWrite(output)
                throw error
            }
        }
    }

    private fun parseXmlTv(file: File): TvEpgCatalog {
        require(file.isFile && file.length() > 0L) { "XMLTV cache is empty" }
        val channelIds = LinkedHashSet<String>()
        val programmes = HashMap<String, MutableList<TvProgramme>>()
        var totalProgrammes = 0
        val now = System.currentTimeMillis()
        val windowEnd = now + EPG_DAY_MS

        data class PendingProgramme(
            val channelId: String,
            val startMillis: Long?,
            val stopMillis: Long?,
            val depth: Int,
            var title: String = "",
            var description: String = ""
        )
        var pending: PendingProgramme? = null
        var textField: String? = null
        var textDepth = -1
        var textBuffer: StringBuilder? = null

        FileInputStream(file).buffered().use { input ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                        "channel" -> parser.getAttributeValue(null, "id")
                            ?.trim()?.takeIf { it.isNotEmpty() }?.let(channelIds::add)
                        "programme" -> {
                            pending = PendingProgramme(
                                channelId = parser.getAttributeValue(null, "channel")?.trim().orEmpty(),
                                startMillis = parseXmlTvDate(parser.getAttributeValue(null, "start")),
                                stopMillis = parseXmlTvDate(parser.getAttributeValue(null, "stop")),
                                depth = parser.depth
                            )
                        }
                        "title", "desc" -> if (pending != null && textField == null) {
                            textField = parser.name.lowercase(Locale.ROOT)
                            textDepth = parser.depth
                            textBuffer = StringBuilder()
                        }
                    }

                    XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                        val target = textBuffer
                        if (textField != null && target != null && target.length < 8_000) {
                            target.append(parser.text.orEmpty())
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.lowercase(Locale.ROOT)
                        if (textField == tag && parser.depth == textDepth) {
                            val value = textBuffer?.toString()?.trim().orEmpty()
                            when (tag) {
                                "title" -> if (pending?.title.isNullOrBlank()) pending?.title = value.take(300)
                                "desc" -> if (pending?.description.isNullOrBlank()) pending?.description = value.take(4_000)
                            }
                            textField = null
                            textDepth = -1
                            textBuffer = null
                        }
                        if (tag == "programme" && pending?.depth == parser.depth) {
                            val item = pending
                            val start = item?.startMillis
                            val stop = item?.stopMillis
                            if (item != null && item.channelId.isNotBlank() &&
                                start != null && stop != null && stop > start &&
                                stop > now && start < windowEnd &&
                                item.title.isNotBlank() && totalProgrammes < EPG_MAX_PROGRAMMES
                            ) {
                                programmes.getOrPut(item.channelId) { ArrayList() }.add(
                                    TvProgramme(start, stop, item.title, item.description)
                                )
                                totalProgrammes++
                            }
                            pending = null
                            textField = null
                            textDepth = -1
                            textBuffer = null
                        }
                    }
                }
                event = parser.next()
            }
        }

        val sorted = programmes.mapValues { (_, items) ->
            items.distinctBy { listOf(it.startMillis, it.stopMillis, it.title) }
                .sortedBy { it.startMillis }
        }
        return TvEpgCatalog(channelIds, sorted)
    }

    private fun parseXmlTvDate(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        val match = Regex("""^(\d{14})(?:\s*([+-]\d{2}:?\d{2}|Z))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(value) ?: return null
        val zoneToken = match.groupValues.getOrNull(2).orEmpty()
        val zone = when {
            zoneToken.equals("Z", true) -> TimeZone.getTimeZone("UTC")
            zoneToken.isBlank() -> TimeZone.getDefault()
            else -> {
                val compact = zoneToken.replace(":", "")
                val sign = if (compact.startsWith("-")) -1 else 1
                val hours = compact.substring(1, 3).toIntOrNull() ?: return null
                val minutes = compact.substring(3, 5).toIntOrNull() ?: return null
                if (hours > 23 || minutes > 59) return null
                SimpleTimeZone(sign * (hours * 60 + minutes) * 60 * 1000, "XMLTV")
            }
        }
        return try {
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                isLenient = false
                timeZone = zone
            }.parse(match.groupValues[1])?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
