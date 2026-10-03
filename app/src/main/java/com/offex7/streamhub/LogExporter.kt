package com.offex7.streamhub

import android.content.Context
import android.os.Build
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogExporter {
    suspend fun export(context: Context, store: SettingsStore): File {
        val appExportDir = File(context.filesDir, "exports")
        appExportDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val zip = File(appExportDir, "radio-tv-logs-$timestamp.zip")
        val logLines = synchronized(this) {
            val all = recentLines.toList()
            all.subList((all.size - 1000).coerceAtLeast(0), all.size)
        }
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
            val finalLines = (systemLog.ifBlank { logLines.joinToString("\n") })
                .lineSequence()
                .toList()
            val start = (finalLines.size - 1000).coerceAtLeast(0)
            val finalLogs = finalLines.subList(start, finalLines.size).joinToString("\n")
            entry("logs.txt", finalLogs)
            val eq = store.radioEqualizerSettings()
            val settingsSnapshot = buildString {
                appendLine("app_version=" + BuildConfig.VERSION_NAME)
                appendLine("app_version_code=" + BuildConfig.VERSION_CODE)
                appendLine("energy_saving_mode=" + store.energySavingMode())
                appendLine("haptics_enabled=" + store.hapticsEnabled())
                appendLine("autostart_enabled=" + store.autoStartEnabled())
                appendLine("pip_enabled=" + store.pipEnabled())
                appendLine("active_tv_source=" + store.activeSourceKey())
                appendLine("hidden_channels_count=" + store.hiddenChannels().size)
                appendLine("tv_favorites_count=" + store.favorites(Section.TV).size)
                appendLine("radio_favorites_count=" + store.favorites(Section.RADIO).size)
                appendLine("radio_eq_bass=" + eq.bass)
                appendLine("radio_eq_mid=" + eq.mid)
                appendLine("radio_eq_treble=" + eq.treble)
                appendLine("radio_eq_preset=" + eq.preset)
                appendLine("radio_normalize=" + eq.normalize)
            }
            entry("settings.txt", settingsSnapshot.trimEnd())
        }

        val shareDir = context.getExternalFilesDir(null) ?: context.cacheDir
        shareDir.mkdirs()
        val sharedZip = File(shareDir, "radio-tv-logs-$timestamp-share.zip")
        zip.copyTo(sharedZip, overwrite = true)
        return sharedZip
    }

    private val recentLines = ArrayDeque<String>(1000)

    fun log(message: String) {
        synchronized(this) {
            if (recentLines.size >= 1000) recentLines.removeFirst()
            recentLines.addLast("${System.currentTimeMillis()} $message")
        }
    }
}
