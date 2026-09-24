package com.offex7.streamhub

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

private val localRadioLogoCache = mutableMapOf<String, ImageBitmap>()
private val localRadioLogoData: Map<String, String> by lazy {
    buildMap {
        putAll(localRadioLogoPart1)
        putAll(localRadioLogoPart2)
        putAll(localRadioLogoPart3)
        putAll(localRadioLogoPart4)
    }
}

fun localRadioLogo(name: String): ImageBitmap? {
    val key = name.trim().uppercase(Locale.ROOT)
    val source = localRadioLogoData.entries.firstOrNull { it.key.uppercase(Locale.ROOT) == key } ?: return null
    return synchronized(localRadioLogoCache) {
        localRadioLogoCache[source.key] ?: runCatching {
            val bytes = Base64.decode(source.value, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()?.also { localRadioLogoCache[source.key] = it }
    }
}
