package com.offex7.streamhub

import android.content.Context
import android.os.Build
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogExporter {
    fun export(context: Context): File {
        val zip = File(context.filesDir, "Radio.TV. bagreport.zip")
        val logLines = synchronized(this) { recentLines.takeLast(1000).toList() }
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            fun entry(name: String, content: String) {
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray(Charsets.UTF_8))
                out.closeEntry()
            }
            entry("version.txt", "Radio.TV ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            entry("device.txt", buildString {
                appendLine("manufacturer=${Build.MANUFACTURER}")
                appendLine("model=${Build.MODEL}")
                appendLine("android=${Build.VERSION.RELEASE}")
                appendLine("sdk=${Build.VERSION.SDK_INT}")
            })
            val systemLog = runCatching {
                ProcessBuilder("logcat", "-d", "-t", "1000", "-v", "time")
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader()
                    .use { it.readText() }
            }.getOrDefault("")
            val finalLogs = (systemLog.ifBlank { logLines.joinToString("\n") })
                .lineSequence()
                .takeLast(1000)
                .joinToString("\n")
            entry("logs.txt", finalLogs)
            entry("settings.txt", "log_export=1")
        }
        return zip
    }

    private val recentLines = ArrayDeque<String>(1000)

    fun log(message: String) {
        synchronized(this) {
            if (recentLines.size >= 1000) recentLines.removeFirst()
            recentLines.addLast("${System.currentTimeMillis()} $message")
        }
    }
}
