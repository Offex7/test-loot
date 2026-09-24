package com.offex7.streamhub

import android.content.Context
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.util.Locale

private val localRadioLogoResourceNames: Map<String, String> = mapOf(
    "CHILL HOUSE" to "radio_logo_chill_house_b64",
    "CHOCOLATE" to "radio_logo_chocolate_b64",
    "COMEDY CLUB" to "radio_logo_comedy_club_b64",
    "DATASET [AI]" to "radio_logo_dataset_ai_b64",
    "METALCORE" to "radio_logo_metalcore_b64",
    "PIRATE STATION" to "radio_logo_pirate_station_b64",
    "PSY TRANCE" to "radio_logo_psy_trance_b64",
    "RECORD" to "radio_logo_record_b64",
    "RELAX" to "radio_logo_relax_b64",
    "ULTRA" to "radio_logo_ultra_b64",
    "VOCAL DRUM" to "radio_logo_vocal_drum_b64",
    "АВТОРАДИО" to "radio_logo_avtoradio_b64",
    "ГАМАЮН" to "radio_logo_gamaun_b64",
    "ЕВРОПА ПЛЮС" to "radio_logo_evropa_plus_b64",
    "КАЛЬЯН РЭП" to "radio_logo_kalyan_rep_b64",
    "НАШЕ РАДИО" to "radio_logo_nashe_radio_b64",
    "РЕТРО FM" to "radio_logo_retro_fm_b64",
    "ХИТ FM" to "radio_logo_hit_fm_b64",
    "ЭНЕРДЖИ" to "radio_logo_energy_b64",
    "ЮГ МОЛОДОЙ" to "radio_logo_yug_molodoy_b64"
)

private val localRadioLogoCache = mutableMapOf<String, ImageBitmap>()
private var localRadioLogoContext: Context? = null

fun initLocalRadioLogoResources(context: Context) {
    localRadioLogoContext = context.applicationContext
}

fun localRadioLogo(name: String): ImageBitmap? {
    val key = name.trim().uppercase(Locale.ROOT)
    val resourceName = localRadioLogoResourceNames[key] ?: return null
    val context = localRadioLogoContext ?: return null
    return synchronized(localRadioLogoCache) {
        localRadioLogoCache[key] ?: runCatching {
            val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            if (resId == 0) return@runCatching null
            val encoded = context.resources.openRawResource(resId).bufferedReader(Charsets.US_ASCII).use { it.readText().trim() }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()?.also {
            localRadioLogoCache[key] = it
        }
    }
}
