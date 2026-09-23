package com.offex7.streamhub

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TvLogoCache(context: Context) {
    private val directory = File(context.applicationContext.cacheDir, "tv_logos").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun loadImageBitmap(url: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        val file = fileFor(url)
        decode(file)?.also { touch(file) }?.let { return@withContext it }

        runCatching {
            download(url, file)
            decode(file)?.also { touch(file) }
        }.getOrNull()
    }

    suspend fun prefetch(urls: List<String>) = withContext(Dispatchers.IO) {
        coroutineScope {
            val semaphore = Semaphore(4)
            urls.asSequence()
                .filter { it.isNotBlank() }
                .distinct()
                .take(400)
                .map { url ->
                    launch {
                        semaphore.withPermit {
                            runCatching {
                                val file = fileFor(url)
                                if (!file.exists()) download(url, file) else touch(file)
                            }
                        }
                    }
                }
                .forEach { it.join() }
        }
    }

    private fun download(url: String, file: File) {
        client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "Radio.TV/9.0")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) error("HTTP " + response.code)
            val bytes = response.body?.bytes().orEmpty()
            if (bytes.isEmpty() || bytes.size > 1_500_000) error("Logo is too large")
            file.outputStream().use { it.write(bytes) }
            trim()
        }
    }

    private fun fileFor(url: String): File =
        File(directory, sha256(url) + ".img")

    private fun decode(file: File): ImageBitmap? {
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    private fun touch(file: File) {
        runCatching { file.setLastModified(System.currentTimeMillis()) }
    }

    private fun trim() {
        val files = directory.listFiles()?.filter { it.isFile }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= 50L * 1024L * 1024L) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= 45L * 1024L * 1024L) break
            total -= file.length()
            runCatching { file.delete() }
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
