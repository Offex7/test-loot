package com.offex7.streamhub

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

data class AppUpdateInfo(val versionName: String, val changelog: String, val downloadUrl: String)

class UpdateDownloadControl {
    private enum class State { RUNNING, PAUSED, CANCELLED }
    private val state = MutableStateFlow(State.RUNNING)
    @Volatile private var activeCall: Call? = null
    val isPaused: Boolean get() = state.value == State.PAUSED
    val isCancelled: Boolean get() = state.value == State.CANCELLED

    fun pause() {
        if (state.value == State.RUNNING) {
            state.value = State.PAUSED
            activeCall?.cancel()
        }
    }

    fun resume() {
        if (state.value == State.PAUSED) state.value = State.RUNNING
    }

    fun cancel() {
        state.value = State.CANCELLED
        activeCall?.cancel()
    }

    internal suspend fun awaitRunning() {
        while (true) {
            when (state.value) {
                State.RUNNING -> return
                State.CANCELLED -> throw CancellationException("Update download cancelled")
                State.PAUSED -> state.first { it != State.PAUSED }
            }
        }
    }

    internal fun attach(call: Call) {
        activeCall = call
        if (state.value != State.RUNNING) call.cancel()
    }

    internal fun detach(call: Call) {
        if (activeCall === call) activeCall = null
    }
}

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

    suspend fun downloadApk(context: Context, info: AppUpdateInfo, control: UpdateDownloadControl, onProgress: (Float) -> Unit): Result<File> {
        return try {
            val apk = withContext(Dispatchers.IO) {
                val dir = context.getExternalFilesDir(null) ?: error("External app files directory is unavailable")
                if (!dir.exists()) check(dir.mkdirs() || dir.isDirectory) { "Cannot create update directory" }
                val target = File(dir, "Radio-TV-v" + info.versionName + ".apk")
                val temp = File(dir, target.name + ".part")
                var attempts = 0
                var ready = false
                while (!ready) {
                    currentCoroutineContext().ensureActive()
                    control.awaitRunning()
                    val offset = if (temp.isFile) temp.length() else 0L
                    val builder = Request.Builder().url(info.downloadUrl)
                        .header("User-Agent", "Radio.TV/" + BuildConfig.VERSION_NAME)
                        .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    if (offset > 0L) builder.header("Range", "bytes=" + offset + "-")
                    val call = client.newCall(builder.build())
                    control.attach(call)
                    try {
                        call.execute().use { response ->
                            if (response.code == 416) {
                                val rangeHeader = response.header("Content-Range").orEmpty()
                                val expectedLength = Regex("""bytes \*/(\d+)""").find(rangeHeader)?.groupValues?.get(1)?.toLongOrNull()
                                if (expectedLength != null && temp.isFile && temp.length() == expectedLength) {
                                    validateApk(context, temp, info.versionName)
                                    ready = true
                                } else {
                                    temp.delete()
                                    attempts++
                                }
                            } else {
                                require(response.code == 200 || response.code == 206) { "APK download HTTP " + response.code }
                                val append = response.code == 206 && offset > 0L
                                if (append) {
                                    val rangeHeader = response.header("Content-Range").orEmpty()
                                    val rangeStart = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""").find(rangeHeader)?.groupValues?.get(1)?.toLongOrNull()
                                        ?: throw IOException("Missing or invalid Content-Range")
                                    require(rangeStart == offset) { "Resume offset mismatch" }
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
                                control.awaitRunning()
                                if (total > 0L) require(temp.length() == total) { "Incomplete APK download: " + temp.length() + "/" + total }
                                validateApk(context, temp, info.versionName)
                                ready = true
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (control.isCancelled) throw CancellationException("Update download cancelled")
                        if (control.isPaused) {
                            control.awaitRunning()
                        } else {
                            attempts++
                            if (attempts >= MAX_DOWNLOAD_ATTEMPTS) throw IllegalStateException(DOWNLOAD_FAILED, error)
                        }
                    } finally {
                        control.detach(call)
                    }
                    if (attempts >= MAX_DOWNLOAD_ATTEMPTS && !ready) throw IllegalStateException(DOWNLOAD_FAILED)
                }
                control.awaitRunning()
                check(temp.isFile && temp.length() > 0L) { DOWNLOAD_FAILED }
                if (target.exists()) check(target.delete()) { "Cannot replace previous APK" }
                check(temp.renameTo(target)) { "Cannot finalize APK" }
                validateApk(context, target, info.versionName)
                onProgress(1f)
                target
            }
            Result.success(apk)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(IllegalStateException(DOWNLOAD_FAILED, error))
        }
    }

    fun cancelDownload(context: Context, info: AppUpdateInfo) {
        val dir = context.getExternalFilesDir(null) ?: return
        File(dir, "Radio-TV-v" + info.versionName + ".apk.part").delete()
        File(dir, "Radio-TV-v" + info.versionName + ".apk").delete()
    }

    private fun validateApk(context: Context, apk: File, expectedVersionName: String? = null) {
        require(apk.isFile && apk.length() >= 128L * 1024L) { "APK file is missing, incomplete or too small" }
        ZipFile(apk).use { zip ->
            require(zip.getEntry("AndroidManifest.xml") != null) { "APK has no AndroidManifest.xml" }
            require(zip.entries().asSequence().any { it.name.matches(Regex("""classes(\d*)?\.dex""")) }) { "APK has no dex bytecode" }
        }
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_ACTIVITIES or PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("Downloaded file is not a valid APK")
        require(archive.packageName == context.packageName) { "APK package name mismatch" }
        if (expectedVersionName != null) require(archive.versionName == expectedVersionName) { "APK version mismatch" }
        val installedInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val installedSigners = installedInfo.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        val downloadedSigners = archive.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        require(installedSigners.isNotEmpty() && installedSigners == downloadedSigners) { "APK signing certificate does not match the installed application" }
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
