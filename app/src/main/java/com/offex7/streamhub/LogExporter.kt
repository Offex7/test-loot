package com.offex7.streamhub

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

object LogExporter {
    suspend fun export(context: Context, store: SettingsStore): File {
        val exportDir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("External app files directory is unavailable")
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw IllegalStateException("Cannot create external app files directory")
        }

        val timestamp = System.currentTimeMillis()
        val file = File(exportDir, "radio-tv-log-${timestamp}.txt")
        val metrics = context.resources.displayMetrics
        val language = Locale.getDefault().toLanguageTag()

        val capturedLogcat = runCatching {
            ProcessBuilder("logcat", "-d", "-t", "100", "-v", "time")
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")

        val fallbackLines = synchronized(this) {
            recentLines.toList()
        }
        val logLines = (capturedLogcat.ifBlank { fallbackLines.joinToString("\n") })
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(100)

        val settingsSnapshot = buildString {
            appendLine("energy_saving_mode=" + store.energySavingMode())
            appendLine("haptics_enabled=" + store.hapticsEnabled())
            appendLine("sound_feedback_enabled=" + store.soundFeedbackFlow().first())
            appendLine("autostart_enabled=" + store.autoStartEnabled())
            appendLine("pip_enabled=" + store.pipEnabled())
            appendLine("active_tv_source=" + store.activeSourceKey().let { key ->
                if (key.startsWith(USER_SOURCE_PREFIX)) "user_playlist" else key
            })
            appendLine("user_playlists_count=" + store.userPlaylists().size)
            appendLine("hidden_channels_count=" + store.hiddenChannels().size)
            appendLine("tv_favorites_count=" + store.favorites(Section.TV).size)
            appendLine("radio_favorites_count=" + store.favorites(Section.RADIO).size)
            val eq = store.radioEqualizerSettings()
            appendLine("radio_eq_bass=" + eq.bass)
            appendLine("radio_eq_mid=" + eq.mid)
            appendLine("radio_eq_treble=" + eq.treble)
            appendLine("radio_eq_preset=" + eq.preset)
            appendLine("radio_normalize=" + eq.normalize)
        }

        file.writeText(
            buildString {
                appendLine("Radio.TV log export")
                appendLine("created_at_epoch_ms=" + timestamp)
                appendLine("created_at_local=" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp)))
                appendLine()
                appendLine("[app]")
                appendLine("version_name=" + BuildConfig.VERSION_NAME)
                appendLine("version_code=" + BuildConfig.VERSION_CODE)
                appendLine()
                appendLine("[device]")
                appendLine("manufacturer=" + Build.MANUFACTURER)
                appendLine("model=" + Build.MODEL)
                appendLine("android_version=" + Build.VERSION.RELEASE)
                appendLine("sdk=" + Build.VERSION.SDK_INT)
                appendLine("display=" + Build.DISPLAY)
                appendLine("fingerprint=" + Build.FINGERPRINT)
                appendLine("screen_width_px=" + metrics.widthPixels)
                appendLine("screen_height_px=" + metrics.heightPixels)
                appendLine("density=" + metrics.density)
                appendLine("density_dpi=" + metrics.densityDpi)
                appendLine("language=" + language)
                appendLine()
                appendLine("[settings_without_personal_data]")
                append(settingsSnapshot)
                appendLine()
                appendLine("[last_100_logcat_lines]")
                if (logLines.isEmpty()) {
                    appendLine("No logcat lines available")
                } else {
                    logLines.forEach(::appendLine)
                }
            },
            Charsets.UTF_8
        )
        require(file.exists() && file.length() > 0L) { "TXT log file was not created" }
        return file
    }

    private val recentLines = ArrayDeque<String>(1000)

    fun log(message: String) {
        synchronized(this) {
            if (recentLines.size >= 1000) recentLines.removeFirst()
            recentLines.addLast(System.currentTimeMillis().toString() + " " + message)
        }
    }
}
