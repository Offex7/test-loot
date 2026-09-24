package com.offex7.streamhub

import android.content.Context
import android.graphics.BitmapFactory
import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

private val localRadioLogoResources: Map<String, Int> = mapOf(
    "CHILL HOUSE" to R.drawable.radio_logo_chill_house,
    "CHOCOLATE" to R.drawable.radio_logo_chocolate,
    "COMEDY CLUB" to R.drawable.radio_logo_comedy_club,
    "DATASET [AI]" to R.drawable.radio_logo_dataset_ai,
    "METALCORE" to R.drawable.radio_logo_metalcore,
    "PIRATE STATION" to R.drawable.radio_logo_pirate_station,
    "PSY TRANCE" to R.drawable.radio_logo_psy_trance,
    "RECORD" to R.drawable.radio_logo_record,
    "RELAX" to R.drawable.radio_logo_relax,
    "ULTRA" to R.drawable.radio_logo_ultra,
    "VOCAL DRUM" to R.drawable.radio_logo_vocal_drum,
    "АВТОРАДИО" to R.drawable.radio_logo_avtoradio,
    "ГАМАЮН" to R.drawable.radio_logo_gamaun,
    "ЕВРОПА ПЛЮС" to R.drawable.radio_logo_evropa_plus,
    "КАЛЬЯН РЭП" to R.drawable.radio_logo_kalyan_rep,
    "НАШЕ РАДИО" to R.drawable.radio_logo_nashe_radio,
    "РЕТРО FM" to R.drawable.radio_logo_retro_fm,
    "ХИТ FM" to R.drawable.radio_logo_hit_fm,
    "ЭНЕРДЖИ" to R.drawable.radio_logo_energy,
    "ЮГ МОЛОДОЙ" to R.drawable.radio_logo_yug_molodoy
)

private val localRadioLogoCache = mutableMapOf<String, ImageBitmap>()
@Volatile private var localRadioLogoResourcesContext: Resources? = null

fun initLocalRadioLogoResources(context: Context) {
    localRadioLogoResourcesContext = context.applicationContext.resources
}

fun localRadioLogo(name: String): ImageBitmap? {
    val key = name.trim().uppercase(Locale.ROOT)
    val resId = localRadioLogoResources[key] ?: return null
    val resources = localRadioLogoResourcesContext ?: return null
    return synchronized(localRadioLogoCache) {
        localRadioLogoCache[key] ?: runCatching {
            BitmapFactory.decodeResource(resources, resId)?.asImageBitmap()
        }.getOrNull()?.also {
            localRadioLogoCache[key] = it
        }
    }
}
