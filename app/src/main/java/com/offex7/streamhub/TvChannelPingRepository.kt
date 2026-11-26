package com.offex7.streamhub

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class TvChannelPingResult(
    val latencyMillis: Long?,
    val expiresAtMillis: Long
)

/** Measures TCP connection latency, never opens the media stream, and limits network concurrency. */
class TvChannelPingRepository {
    private val semaphore = Semaphore(4)
    private val cache = ConcurrentHashMap<String, TvChannelPingResult>()

    suspend fun ping(url: String): TvChannelPingResult = semaphore.withPermit {
        val now = System.currentTimeMillis()
        cache[url]?.takeIf { it.expiresAtMillis > now }?.let { return@withPermit it }
        val latency = withContext(Dispatchers.IO) {
            runCatching {
                val uri = URI(url)
                val host = uri.host?.takeIf { it.isNotBlank() } ?: error("Stream URL has no host")
                val port = when {
                    uri.port > 0 -> uri.port
                    uri.scheme.equals("https", ignoreCase = true) -> 443
                    else -> 80
                }
                val started = SystemClock.elapsedRealtime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2_500)
                }
                (SystemClock.elapsedRealtime() - started).coerceAtLeast(0L)
            }.getOrNull()
        }
        val ttl = Random.nextLong(30L, 221L) * 1_000L
        TvChannelPingResult(latency, System.currentTimeMillis() + ttl).also { cache[url] = it }
    }
}
