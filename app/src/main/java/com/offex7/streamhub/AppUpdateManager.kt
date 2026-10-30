package com.offex7.streamhub

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

data class AppUpdateInfo(val versionName: String, val changelog: String, val downloadUrl: String)

object AppUpdateManager {
    private const val LATEST_URL = "https://api.github.com/repos/RadioTV-apk/R/releases/latest"
    private const val MAX_DOWNLOAD_ATTEMPTS = 3
    private const val DOWNLOAD_FAILED = "Не удалось скачать обновление. Проверьте стабильность соединения и попробуйте позже"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    suspend fun checkLatest(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Radio.TV/" + BuildConfig.VERSION_NAME).build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "GitHub API HTTP " + response.code }
                val json = JSONObject(response.body?.string().orEmpty())
                val version = json.optString("tag_name").removePrefix("v").removePrefix("V").trim()
                require(version.isNotBlank()) { "Release tag is empty" }
                if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) return@use null
                val expected = "Radio-TV-v" + version + ".apk"
                val assets = json.optJSONArray("assets") ?: error("Release has no assets")
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    if (asset.optString("name") == expected) {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
                require(!downloadUrl.isNullOrBlank()) { "APK asset " + expected + " not found" }
                AppUpdateInfo(version, json.optString("body").trim().ifBlank { "Доступна новая версия." }, downloadUrl!!)
            }
        }
    }

    suspend fun downloadApk(context: Context, info: AppUpdateInfo, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: error("External app files directory is unavailable")
            if (!dir.exists()) check(dir.mkdirs() || dir.isDirectory) { "Cannot create update directory" }
            val target = File(dir, "Radio-TV-v" + info.versionName + ".apk")
            val temp = File(dir, target.name + ".part")
            var lastFailure: Exception? = null
            var ready = false
            for (attempt in 1..MAX_DOWNLOAD_ATTEMPTS) {
                if (ready) break
                try {
                    val offset = if (temp.isFile) temp.length() else 0L
                    val requestBuilder = Request.Builder().url(info.downloadUrl)
                        .header("User-Agent", "Radio.TV/" + BuildConfig.VERSION_NAME)
                        .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    if (offset > 0L) requestBuilder.header("Range", "bytes=" + offset + "-")
                    client.newCall(requestBuilder.build()).execute().use { response ->
                        if (response.code == 416) {
                            val expectedLength = Regex("bytes \\*/(\\d+)").find(response.header("Content-Range").orEmpty())?.groupValues?.get(1)?.toLongOrNull()
                            if (expectedLength != null && temp.isFile && temp.length() == expectedLength) {
                                validateApk(context, temp, info.versionName)
                                ready = true
                            } else {
                                temp.delete()
                                throw IOException("Server rejected resume range; restarting from zero")
                            }
                        } else {
                            require(response.code == 200 || response.code == 206) { "APK download HTTP " + response.code }
                            val append = response.code == 206 && offset > 0L
                            if (append) {
                                val range = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)").find(response.header("Content-Range").orEmpty())
                                    ?: throw IOException("Missing or invalid Content-Range")
                                require(range.groupValues[1].toLong() == offset) { "Resume offset mismatch" }
                            }
                            val body = response.body ?: error("Empty APK response")
                            val total = if (append) response.header("Content-Range")?.substringAfterLast("/")?.toLongOrNull() ?: -1L else body.contentLength()
                            body.byteStream().use { input ->
                                FileOutputStream(temp, append).buffered(64 * 1024).use { output ->
                                    val buffer = ByteArray(64 * 1024)
                                    var received = if (append) offset else 0L
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        output.write(buffer, 0, count)
                                        received += count.toLong()
                                        if (total > 0L) onProgress((received.toDouble() / total).toFloat().coerceIn(0f, 0.995f))
                                    }
                                }
                            }
                            if (total > 0L) require(temp.length() == total) { "Incomplete APK download: " + temp.length() + "/" + total }
                            validateApk(context, temp, info.versionName)
                            ready = true
                        }
                    }
                } catch (error: Exception) {
                    lastFailure = error
                    if (attempt < MAX_DOWNLOAD_ATTEMPTS && temp.isFile) onProgress(0f)
                }
            }
            if (!ready) {
                temp.delete()
                throw IllegalStateException(DOWNLOAD_FAILED, lastFailure)
            }
            check(temp.isFile && temp.length() > 0L) { DOWNLOAD_FAILED }
            if (target.exists()) check(target.delete()) { "Cannot replace previous APK" }
            check(temp.renameTo(target)) { "Cannot finalize APK" }
            validateApk(context, target, info.versionName)
            onProgress(1f)
            target
        }.recoverCatching { error -> throw IllegalStateException(DOWNLOAD_FAILED, error) }
    }

    private fun validateApk(context: Context, apk: File, expectedVersionName: String? = null) {
        require(apk.isFile && apk.length() >= 128L * 1024L) { "APK file is missing, incomplete or too small" }
        ZipFile(apk).use { zip ->
            require(zip.getEntry("AndroidManifest.xml") != null) { "APK has no AndroidManifest.xml" }
            require(zip.entries().asSequence().any { it.name.matches(Regex("classes(\\d*)?\\.dex")) }) { "APK has no dex bytecode" }
        }
        val archive = context.packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: error("Downloaded file is not a valid APK")
        require(archive.packageName == context.packageName) { "APK package name mismatch" }
        if (expectedVersionName != null) require(archive.versionName == expectedVersionName) { "APK version mismatch" }

        val installedInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val installedSigners = installedInfo.signingInfo?.apkContentsSigners
            ?.map { it.toCharsString() }?.toSet().orEmpty()
        val downloadedSigners = archive.signingInfo?.apkContentsSigners
            ?.map { it.toCharsString() }?.toSet().orEmpty()
        require(installedSigners.isNotEmpty() && installedSigners == downloadedSigners) {
            "APK signing certificate does not match the installed application"
        }
    }

    fun install(context: Context, apk: File) {
        validateApk(context, apk)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apk)
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