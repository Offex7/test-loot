package com.offex7.streamhub

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val changelog: String,
    val downloadUrl: String
)

object AppUpdateManager {
    private const val LATEST_URL = "https://api.github.com/repos/RadioTV-apk/R/releases/latest"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun checkLatest(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Radio.TV/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "GitHub API HTTP ${response.code}" }
                val json = JSONObject(response.body?.string().orEmpty())
                val version = json.optString("tag_name").removePrefix("v").removePrefix("V").trim()
                require(version.isNotBlank()) { "Release tag is empty" }

                if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) {
                    return@use null
                }

                val expected = "Radio-TV-v${version}.apk"
                val assets = json.optJSONArray("assets")
                    ?: throw IllegalStateException("Release has no assets")
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    if (asset.optString("name") == expected) {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
                require(!downloadUrl.isNullOrBlank()) { "APK asset ${expected} not found" }

                AppUpdateInfo(
                    versionName = version,
                    changelog = json.optString("body").trim().ifBlank { "Доступна новая версия." },
                    downloadUrl = downloadUrl!!
                )
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = context.getExternalFilesDir(null)
                ?: throw IllegalStateException("External app files directory is unavailable")
            if (!dir.exists()) dir.mkdirs()

            val target = File(dir, "Radio-TV-v${info.versionName}.apk")
            val temp = File(dir, target.name + ".part")

            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", "Radio.TV/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "APK download HTTP ${response.code}" }
                val body = response.body ?: throw IllegalStateException("Empty APK response")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var written = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0L) onProgress((written.toDouble() / total).toFloat().coerceIn(0f, 1f))
                        }
                    }
                }
            }

            require(temp.exists() && temp.length() > 0L) { "Downloaded APK is empty" }
            if (target.exists()) target.delete()
            check(temp.renameTo(target)) { "Cannot finalize APK" }
            onProgress(1f)
            target
        }
    }

    fun install(context: Context, apk: File) {
        require(apk.exists() && apk.length() > 0L)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun compareVersions(a: String, b: String): Int {
        val av = a.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val bv = b.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(av.size, bv.size)
        for (i in 0 until size) {
            val x = av.getOrElse(i) { 0 }
            val y = bv.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
