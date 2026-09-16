package com.offex7.streamhub

data class StreamItem(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val epgLogoUrl: String? = null,
    val isOffline: Boolean = false
) {
    val key: String get() = url
}

data class PlaylistSource(val name: String, val url: String)

enum class Section { TV, RADIO }

enum class AvailabilityStatus { UNKNOWN, ONLINE, OFFLINE }

val RADIO_STATIONS = listOf(
    StreamItem("Рекорд (клубная)", "https://radiorecord.hostingradio.ru/rr_main96.aacp"),
    StreamItem("Шоколад (каверы)", "https://choco.hostingradio.ru:10010/fm"),
    StreamItem("Енерджи (клубная)", "https://pub0301.101.ru:8443/stream/air/mp3/256/99"),
    StreamItem("КиссФМ (ру)", "https://a7.radioheart.ru:9015/RH73674?ver=410278"),
    StreamItem("Ультра (рок/альтернатива)", "https://nashe1.hostingradio.ru:80/ultra-192.mp3?wcid=a1c5abce-19a9-4534-a834-0bc4557727e3&stationId=ultra-hd"),
    StreamItem("Кальян Реп", "https://dfm-kalianrap.hostingradio.ru/kalianrap96.aacp"),
    StreamItem("Pirate Station", "https://radiorecord.hostingradio.ru/ps96.aacp"),
    StreamItem("Vocal Drum", "https://dfm-drum.hostingradio.ru/drum96.aacp"),
    StreamItem("Chill House", "https://radiorecord.hostingradio.ru/chil96.aacp"),
    StreamItem("Psy Trance", "https://dfm-psytrance.hostingradio.ru/psytrance96.aacp"),
    StreamItem("SOUNDPARK DEEP", "https://relay1.radiotoolkit.com/spdeep"),
    StreamItem("Metalcore. Post-Hardcore. Alternative", "https://srv02.gpmradio.ru:8443/stream/personal/aacp/64/860066")
)

val TV_SOURCES = listOf(
    PlaylistSource("naggdd — основной", "https://naggdd.github.io/iptv/ru.m3u"),
    PlaylistSource("smolnp — стабильный", "https://smolnp.github.io/IPTVru//IPTVstable.m3u8"),
    PlaylistSource("kittv — самообновляемый", "https://kittv.ru/rus.m3u"),
    PlaylistSource("DropTV — еженедельный", "https://raw.githubusercontent.com/IPTVRU2026/IPTVMIR/main/IPTV_MEGA_PLAYLIST.m3u"),
    PlaylistSource("zabava — Ростелеком", "https://raw.githubusercontent.com/CrocoUser/zabava-project/refs/heads/main/zabava-ef.m3u")
)
