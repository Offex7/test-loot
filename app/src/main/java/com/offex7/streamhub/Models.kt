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

private const val RELAX_LOGO = "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13"

val RADIO_STATIONS = listOf(
    StreamItem("Рекорд (клубная)", "https://radiorecord.hostingradio.ru/rr_main96.aacp"),
    StreamItem("Шоколад (каверы)", "https://choco.hostingradio.ru:10010/fm"),
    StreamItem("Енерджи (клубная)", "https://hls-01-gpm.hostingradio.ru/energyfm7/playlist.m3u8"),
    StreamItem("Ультра (рок/альтернатива)", "https://nashe1.hostingradio.ru:80/ultra-192.mp3?wcid=a1c5abce-19a9-4534-a834-0bc4557727e3&stationId=ultra-hd"),
    StreamItem("Кальян Реп", "https://dfm-kalianrap.hostingradio.ru/kalianrap96.aacp"),
    StreamItem("Pirate Station", "https://radiorecord.hostingradio.ru/ps96.aacp"),
    StreamItem("Vocal Drum", "https://dfm-drum.hostingradio.ru/drum96.aacp"),
    StreamItem("Chill House", "https://radiorecord.hostingradio.ru/chil96.aacp"),
    StreamItem("Psy Trance", "https://dfm-psytrance.hostingradio.ru/psytrance96.aacp"),
    StreamItem("Metalcore. Post-Hardcore. Alternative", "https://srv02.gpmradio.ru:8443/stream/personal/aacp/64/860066"),
    StreamItem("RELAX", "https://srv01.gpmradio.ru/stream/trust/mp3/128/267", RELAX_LOGO),
    StreamItem("COMEDY CLUB", "https://hls-01-gpm.hostingradio.ru/comedyradio495/playlist.m3u8"),
    StreamItem("АВТОРАДИО", "https://hls-01-gpm.hostingradio.ru/avtoradio7/playlist.m3u8"),
    StreamItem("ЮГ МОЛОДОЙ", "https://listen7.myradio24.com/18718")
)

val TV_SOURCES = listOf(
    PlaylistSource("Объединённый (smolnp + NaggDD + DropTV + Zabava)", "combined://tv")
)

val TV_PLAYLIST_URLS = listOf(
    "https://smolnp.github.io/IPTVru//IPTVstable.m3u8",
    "https://naggdd.github.io/iptv/ru.m3u",
    "https://raw.githubusercontent.com/IPTVRU2026/IPTVMIR/main/IPTV_MEGA_PLAYLIST.m3u",
    "https://raw.githubusercontent.com/CrocoUser/zabava-project/refs/heads/main/zabava-ef.m3u"
)
