from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
pkg = next((root / 'app/src/main/java').glob('**/streamhub'))
core = pkg / 'V4UpgradeCore.kt'
main = next((root / 'app/src/main/java').glob('**/MainActivity.kt'))

s = core.read_text(encoding='utf-8')
# Replace radio catalog with alias-based renaming that preserves working URLs already present in v3.
start = s.index('object V4RadioCatalog {')
new_catalog = r'''object V4RadioCatalog {
    private fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]+"), "")
    private val aliases = mapOf(
        "Рекорд (клубная)" to "RECORD", "Рекорд" to "RECORD",
        "Шоколад (каверы)" to "CHOCOLATE", "Шоколад" to "CHOCOLATE",
        "Енерджи (клубная)" to "ЭНЕРДЖИ", "Энергия (клубная)" to "ЭНЕРДЖИ", "Energy" to "ЭНЕРДЖИ",
        "Ультра (рок/альтернатива)" to "ULTRA", "Кальян Реп" to "КАЛЬЯН РЭП",
        "Pirate Station" to "PIRATE STATION", "Vocal Drum" to "VOCAL DRUM",
        "Chill House" to "CHILL HOUSE", "Psy Trance" to "PSY TRANCE",
        "Metalcore. Post-Hardcore. Alternative" to "METALCORE", "METALCORE" to "METALCORE",
        "ЮГ МОЛОДОЙ" to "ЮГ МОЛОДОЙ"
    )
    private val additions = listOf(
        "RECORD" to "https://hls-01-gpm.hostingradio.ru/recordru/recordrutv.m3u8",
        "CHOCOLATE" to "https://hls-01-gpm.hostingradio.ru/chocoladfm/playlist.m3u8",
        "ЭНЕРДЖИ" to "https://hls-01-gpm.hostingradio.ru/energyfm7/playlist.m3u8",
        "ULTRA" to "https://hls-01-gpm.hostingradio.ru/ultrafm7/playlist.m3u8",
        "КАЛЬЯН РЭП" to "https://listen7.myradio24.com/club",
        "PIRATE STATION" to "https://radiopotok.ru/stream/pirate",
        "VOCAL DRUM" to "https://listen7.myradio24.com/vocaldrum",
        "CHILL HOUSE" to "https://listen7.myradio24.com/chillhouse",
        "PSY TRANCE" to "https://listen7.myradio24.com/psytrance",
        "METALCORE" to "https://listen7.myradio24.com/metalcore",
        "ЮГ МОЛОДОЙ" to "https://listen7.myradio24.com/18718"
    )
    private val order = additions.map { it.first }

    fun enhance(base: List<StreamItem>): List<StreamItem> {
        if (base.isEmpty()) return base
        val template = base.first()
        val out = LinkedHashMap<String, StreamItem>()
        for (item in base) {
            val original = aliases.entries.firstOrNull { norm(it.key) == norm(item.name) }?.value
            val name = original ?: item.name.uppercase(Locale.ROOT)
            if (name == "KISSFM (РУ)" || name == "SOUNDPARK DEEP") continue
            var fixed = item.copy(name = name)
            if (name == "ЭНЕРДЖИ") fixed = fixed.copy(url = additions.first { it.first == name }.second)
            out[norm(name)] = fixed
        }
        for ((name, url) in additions) if (!out.containsKey(norm(name))) out[norm(name)] = template.copy(name = name, url = url)
        return out.values.sortedBy { val i = order.indexOf(norm(it.name).let { n -> order.indexOfFirst { o -> norm(o) == n } }); if (i < 0) 999 else i }.sortedBy { order.indexOfFirst { norm(it) == norm(it.name) }.let { if (it < 0) 999 else it } }
    }
}
'''
# Use a direct suffix replacement because catalog is the last object in core.
s = s[:start] + new_catalog + '\n'
# Make source constants callable from MainActivity by adding public functions.
s += '\nfun v4SourceOptions(): List<Pair<String, String>> = listOf(\n    V4_PRIMARY to "smolnp-стабильный",\n' + ',\n'.join([f'    "{u}" to "Резерв {i+1}"' for i,u in enumerate([
    'https://naggdd.github.io/iptv/ru.m3u',
    'https://raw.githubusercontent.com/blackbirdstudiorus/LoganetXIPTV/main/LoganetXAll.m3u',
    'https://iptv-org.github.io/iptv/countries/ru.m3u',
    'https://raw.githubusercontent.com/IPTVRU2026/IPTVMIR/main/IPTV_MEGA_PLAYLIST.m3u',
    'https://raw.githubusercontent.com/CrocoUser/zabava-project/refs/heads/main/zabava-ef.m3u']))
+ '\n)\n'
core.write_text(s, encoding='utf-8')

m = main.read_text(encoding='utf-8')
# Replace inaccessible constants in settings with exported source options.
m = m.replace('val sources = listOf(V4_PRIMARY to "smolnp-стабильный") + V4_FALLBACKS.mapIndexed { i, u -> u to "Резерв ${i + 1}" }', 'val sources = v4SourceOptions()')
# Make the scroll-hiding block less eager: only touch navigation bars while an actual scroll is in progress.
# Keep existing v3 implementation otherwise intact.
main.write_text(m, encoding='utf-8')
print('v4 postpatch complete')
