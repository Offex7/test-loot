package com.radiotv.tvremote.core

data class CastRenderer(val id: String, val name: String, val descriptionUrl: String)
data class CastMedia(val url: String, val mimeType: String, val title: String = "Radio.TV")
interface CastController {
    suspend fun discover(): List<CastRenderer>
    suspend fun play(renderer: CastRenderer, media: CastMedia)
    suspend fun stop(renderer: CastRenderer)
}
interface RadioTvMediaProvider {
    fun currentMedia(): CastMedia?
}
