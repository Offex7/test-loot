package com.offex7.streamhub

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale
import java.util.zip.ZipInputStream

object RadioLogoAssetsV38 {
    private const val ZIP = "radio_logo2.zip"

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
        "ДЕТСКОЕ РАДИО" to "detskoe_radio.jfif"
    )

    private val cache = HashMap<String, ImageBitmap>()

    fun image(context: Context, stationName: String): ImageBitmap? {
        val key = stationName.trim().uppercase(Locale.ROOT)
        cache[key]?.let { return it }
        val filename = filenames[key] ?: return null

        val bitmap = runCatching {
            var result: android.graphics.Bitmap? = null
            context.assets.open(ZIP).use { input ->
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

        val image = bitmap.asImageBitmap()
        cache[key] = image
        return image
    }
}
