package com.offex7.streamhub

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.zip.ZipInputStream

private val localRadioLogoFiles: Map<String, String> = mapOf(
    "CHILL HOUSE" to "radio_logo_chill_house.jpg",
    "CHOCOLATE" to "radio_logo_chocolate.jpg",
    "COMEDY CLUB" to "radio_logo_comedy_club.jpg",
    "DATASET [AI]" to "radio_logo_dataset_ai.jpg",
    "METALCORE" to "radio_logo_metalcore.jpg",
    "PIRATE STATION" to "radio_logo_pirate_station.jpg",
    "PSY TRANCE" to "radio_logo_psy_trance.jpg",
    "RECORD" to "radio_logo_record.jpg",
    "RELAX" to "radio_logo_relax.jpg",
    "ULTRA" to "radio_logo_ultra.jpg",
    "VOCAL DRUM" to "radio_logo_vocal_drum.jpg",
    "АВТОРАДИО" to "radio_logo_avtoradio.jpg",
    "ГАМАЮН" to "radio_logo_gamaun.jpg",
    "ЕВРОПА ПЛЮС" to "radio_logo_evropa_plus.jpg",
    "КАЛЬЯН РЭП" to "radio_logo_kalyan_rep.jpg",
    "НАШЕ РАДИО" to "radio_logo_nashe_radio.jpg",
    "РЕТРО FM" to "radio_logo_retro_fm.jpg",
    "ХИТ FM" to "radio_logo_hit_fm.jpg",
    "ЭНЕРДЖИ" to "radio_logo_energy.jpg",
    "ЮГ МОЛОДОЙ" to "radio_logo_yug_molodoy.jpg"
)

private val localRadioLogoCache = mutableMapOf<String, ImageBitmap>()
private var localRadioLogoContext: Context? = null

fun initLocalRadioLogoResources(context: Context) {
    localRadioLogoContext = context.applicationContext
}

fun localRadioLogo(name: String): ImageBitmap? {
    val key = name.trim().uppercase(Locale.ROOT)
    val wantedFile = localRadioLogoFiles[key] ?: return null
    val context = localRadioLogoContext ?: return null

    return synchronized(localRadioLogoCache) {
        localRadioLogoCache[key] ?: runCatching {
            val resId = context.resources.getIdentifier(
                "radio_logos_payload_b64",
                "raw",
                context.packageName
            )
            if (resId == 0) return@runCatching null
            val encoded = context.resources.openRawResource(resId)
                .bufferedReader(Charsets.US_ASCII)
                .use { it.readText().trim() }
            val zipBytes = Base64.decode(encoded, Base64.DEFAULT)

            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == wantedFile) {
                        val bytes = zis.readBytes()
                        return@runCatching BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                    entry = zis.nextEntry
                }
                null
            }
        }.getOrNull()?.also { localRadioLogoCache[key] = it }
    }
}
