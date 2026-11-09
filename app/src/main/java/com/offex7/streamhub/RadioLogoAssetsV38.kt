package com.offex7.streamhub

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale
import java.util.zip.ZipInputStream

object RadioLogoAssetsV38 {
    private const val ZIP = "radio_logo2.zip"
    private const val NEW_LOGOS_BASE64 = "radio_logo3.zip.b64"

    private val filenames = mapOf(
        "ШАНСОН" to "shanson.jfif",
        "МАЯК" to "mayak.jfif",
        "LOVE" to "love.jfif",
        "ВЕСТИ FM" to "vestifm.jfif",
        "МАРУСЯ FM" to "marusyafm.jfif",
        "РАДИО ДАЧА" to "radiodacha.jfif",
        "RAP CLASSIC" to "rapclassic.jfif",
        "REMIX FM" to "remixfm.jfif",
        "НОВОЕ РАДИО" to "novoeradio.jfif",
        "ЮМОР FM" to "umor_fm.jfif",
        "ДЕТСКОЕ РАДИО" to "detskoe_radio.jfif",
        "БОРНЕО" to "BORNEO.webp",
        "ГАРАЖ FM" to "GARAGE_FM.webp",
        "НЕСТАНДАРТ" to "NESTANDART.webp"
    )
    private val newLogoStations = setOf("БОРНЕО", "ГАРАЖ FM", "НЕСТАНДАРТ")

    private val cache = HashMap<String, ImageBitmap>()
    private val bitmapCache = HashMap<String, android.graphics.Bitmap>()

    fun bitmap(context: Context, stationName: String): android.graphics.Bitmap? {
        val key = stationName.trim().uppercase(Locale.ROOT)
        bitmapCache[key]?.let { return it }
        val filename = filenames[key] ?: return null
        val bitmap = runCatching {
            var result: android.graphics.Bitmap? = null
            val archiveInput = if (key in newLogoStations) {
                val encoded = context.assets.open(NEW_LOGOS_BASE64).bufferedReader().use { it.readText() }
                ByteArrayInputStream(Base64.decode(encoded, Base64.DEFAULT))
            } else {
                context.assets.open(ZIP)
            }
            archiveInput.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name == filename) {
                            result = BitmapFactory.decodeStream(zip)
                            break
                        }
                    }
                }
            }
            result
        }.getOrNull() ?: return null
        bitmapCache[key] = bitmap
        return bitmap
    }

    fun image(context: Context, stationName: String): ImageBitmap? {
        val key = stationName.trim().uppercase(Locale.ROOT)
        cache[key]?.let { return it }
        val image = bitmap(context, key)?.asImageBitmap() ?: return null
        cache[key] = image
        return image
    }
}
