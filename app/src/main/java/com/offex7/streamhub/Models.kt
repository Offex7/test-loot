package com.offex7.streamhub

data class StreamItem(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val epgLogoUrl: String? = null,
    val isOffline: Boolean = false,
    val sourceUrls: List<String> = emptyList()
) { val key: String get() = url }

data class PlaylistSource(val name: String, val url: String)

data class UserPlaylist(
    val name: String,
    val url: String
) {
    val key: String get() = userPlaylistKey(url)
}

const val BUILTIN_SOURCE_PREFIX = "builtin:"
const val USER_SOURCE_PREFIX = "user:"
fun builtinSourceKey(index: Int): String = BUILTIN_SOURCE_PREFIX + index.coerceIn(0, TV_SOURCES.lastIndex)
fun userPlaylistKey(url: String): String = USER_SOURCE_PREFIX + url.trim()

enum class Section { TV, RADIO }
enum class AvailabilityStatus { UNKNOWN, ONLINE, OFFLINE }

private const val RELAX_LOGO = "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13"

val RADIO_STATIONS = listOf(
    StreamItem("RECORD", "https://radiorecord.hostingradio.ru/rr_main96.aacp"),
    StreamItem("CHOCOLATE", "https://choco.hostingradio.ru:10010/fm"),
    StreamItem("ЭНЕРДЖИ", "https://hls-01-gpm.hostingradio.ru/energyfm7/playlist.m3u8"),
    StreamItem("ULTRA", "https://nashe1.hostingradio.ru:80/ultra-192.mp3?wcid=a1c5abce-19a9-4534-a834-0bc4557727e3&stationId=ultra-hd"),
    StreamItem("КАЛЬЯН РЭП", "https://dfm-kalianrap.hostingradio.ru/kalianrap96.aacp"),
    StreamItem("PIRATE STATION", "https://radiorecord.hostingradio.ru/ps96.aacp"),
    StreamItem("VOCAL DRUM", "https://dfm-drum.hostingradio.ru/drum96.aacp"),
    StreamItem("CHILL HOUSE", "https://radiorecord.hostingradio.ru/chil96.aacp"),
    StreamItem("PSY TRANCE", "https://dfm-psytrance.hostingradio.ru/psytrance96.aacp"),
    StreamItem("METALCORE", "https://srv02.gpmradio.ru:8443/stream/personal/aacp/64/860066"),
    StreamItem("RELAX", "https://srv01.gpmradio.ru/stream/trust/mp3/128/267", RELAX_LOGO),
    StreamItem("COMEDY CLUB", "https://hls-01-gpm.hostingradio.ru/comedyradio495/playlist.m3u8"),
    StreamItem("АВТОРАДИО", "https://hls-01-gpm.hostingradio.ru/avtoradio7/playlist.m3u8"),
    StreamItem("ЮГ МОЛОДОЙ", "https://listen7.myradio24.com/18718")
)

val RADIO_LOGO_URLS = mapOf(
    "RECORD" to "https://sun9-58.vkuserphoto.ru/s/v1/ig2/IiddqILI9W20xtBjGASd1Wc2qaE8CtlNMcM4HP7_rOxeHWqZHsTZQrxChaHjZF90iod1cWtN-YKmKEhzRcRW4WNu.jpg?quality=96&cs=640x0",
    "CHOCOLATE" to "https://avatars.mds.yandex.net/i?id=1e60272039bd4313b6db31715603bcbd_l-5207916-images-thumbs&n=13",
    "ЭНЕРДЖИ" to "https://www.energyfm.ru/favicon.ico",
    "ULTRA" to "https://is1-ssl.mzstatic.com/image/thumb/Purple221/v4/38/a2/31/38a23119-5f63-f992-f243-410ebdbe2e92/AppIcon-1x_U007epad-0-1-85-220-0.png/1200x630wa.jpg",
    "КАЛЬЯН РЭП" to "https://dfm.ru/b/d/a9C4t_hQkezt5PerPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=z2mWS9XkQNQR-Eyvb39g-w.webp",
    "PIRATE STATION" to "https://avatars.mds.yandex.net/i?id=716a7e3af7b104f3440332533174c9675b6caf11-7549373-images-thumbs&n=13",
    "VOCAL DRUM" to "https://avatars.mds.yandex.net/i?id=32e06202154c0631649a6d0b54f5f6d0_l-5287214-images-thumbs&n=13",
    "CHILL HOUSE" to "https://avatars.mds.yandex.net/i?id=cdb7307dc06845c2738e64bc70cb8042385cadb1-3986577-images-thumbs&n=13",
    "PSY TRANCE" to "https://dfm.ru/b/d/a9C4t_hQkezt4_erPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=cz1cYtES_SBOB1fZ0h832A.webp",
    "METALCORE" to "https://lh3.ggpht.com/3F-VojAzJppXcdFDGvjZ_55ONQyBo4mlpEqbIS9n5w-kG-W4NxT2MqdQU5qcwsXJ7g=s180",
    "ЮГ МОЛОДОЙ" to "https://yug-radio.ru/writable/uploads/grafskiy-photos/________________________-mobile.jpg"
)
const val RADIO_LOGO_FALLBACK_URL = "https://avatars.mds.yandex.net/i?id=0a9808b1a359810ad95a4407bb71fa8b_l-5146492-images-thumbs&n=13"

val TV_SOURCES = listOf(
    PlaylistSource("Источник 1 — smolnp", "https://smolnp.github.io/IPTVru//IPTVru.m3u"),
    PlaylistSource("Источник 2 — NaggDD", "https://naggdd.github.io/iptv/ru.m3u"),
    PlaylistSource("Источник 3 — Zabava", "https://raw.githubusercontent.com/CrocoUser/zabava-project/refs/heads/main/zabava-ef.m3u"),
    PlaylistSource("Источник 4 — World IP TV", "https://romaxa55.github.io/world_ip_tv/output/index.m3u")
)
val TV_PLAYLIST_URLS = TV_SOURCES.map { it.url }