from pathlib import Path
import re, subprocess, shutil, urllib.request

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
JAVA = next(APP.glob("src/main/java/**/MainActivity.kt"))
PKG_DIR = JAVA.parent
RES = APP / "src/main/res"
DRAW = RES / "drawable"
DRAW.mkdir(parents=True, exist_ok=True)

# v4 build-time assets. The QR resource is deliberately never modified.
ASSETS = {
    "start_tv_logo": "https://avatars.mds.yandex.net/i?id=124c90a0cfd3b3341d7e7592f8eda057e77081cd-4926719-images-thumbs&n=13",
    "start_radio_logo": "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png",
    "radio_record": "https://sun9-58.vkuserphoto.ru/s/v1/ig2/IiddqILI9W20xtBjGASd1Wc2qaE8CtlNMcM4HP7_rOxeHWqZHsTZQrxChaHjZF90iod1cWtN-YKmKEhzRcRW4WNu.jpg?quality=96&as=640x640&from=bu&cs=640x0",
    "radio_chocolate": "https://avatars.mds.yandex.net/i?id=1e60272039bd4313b6db31715603bcbd_l-5207916-images-thumbs&n=13",
    "radio_energy": "https://www.energyfm.ru/favicon.ico",
    "radio_ultra": "https://is1-ssl.mzstatic.com/image/thumb/Purple221/v4/38/a2/31/38a23119-5f63-f992-f243-410ebdbe2e92/AppIcon-1x_U007epad-0-1-85-220-0.png/1200x630wa.jpg",
    "radio_kalyan": "https://dfm.ru/b/d/a9C4t_hQkezt5PerPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=z2mWS9XkQNQR-Eyvb39g-w.webp",
    "radio_pirate": "https://avatars.mds.yandex.net/i?id=716a7e3af7b104f3440332533174c9675b6caf11-7549373-images-thumbs&n=13",
    "radio_vocal": "https://avatars.mds.yandex.net/i?id=32e06202154c0631649a6d0b54f5f6d0_l-5287214-images-thumbs&n=13",
    "radio_chill": "https://avatars.mds.yandex.net/i?id=cdb7307dc06845c2738e64bc70cb8042385cadb1-3986577-images-thumbs&n=13",
    "radio_psy": "https://dfm.ru/b/d/a9C4t_hQkezt4_erPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=cz1cYtES_SBOB1fZ0h832A.webp",
    "radio_metalcore": "https://lh3.ggpht.com/3F-VojAzJppXcdFDGvjZ_55ONQyBo4mlpEqbIS9n5w-kG-W4NxT2MqdQU5qcwsXJ7g=s180",
    "radio_yug": "https://yug-radio.ru/writable/uploads/grafskiy-photos/________________________-mobile.jpg",
}
FALLBACK_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="

def download(url: str, path: Path):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=15) as r:
            path.write_bytes(r.read())
        if path.stat().st_size < 32:
            raise ValueError("tiny asset")
        return True
    except Exception as e:
        print(f"asset fallback: {path.name}: {e}")
        import base64
        path.write_bytes(base64.b64decode(FALLBACK_PNG))
        return False

# Download local logo resources. Existing files are replaced for deterministic v4 builds.
for name, url in ASSETS.items():
    download(url, DRAW / f"{name}.img")
    p = DRAW / f"{name}.img"
    # Android accepts png/jpg/webp/ico poorly depending on aapt, so normalize all downloaded images to PNG.
    out = DRAW / f"{name}.png"
    converted = False
    for cmd in (["magick", str(p), str(out)], ["convert", str(p), str(out)]):
        try:
            subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20)
            converted = True
            break
        except Exception:
            pass
    if not converted:
        shutil.copyfile(p, out)
    p.unlink(missing_ok=True)

# Remove white background from the TV logo where ImageMagick is available.
for tool in ("magick", "convert"):
    if shutil.which(tool):
        try:
            subprocess.run([tool, str(DRAW / "start_tv_logo.png"), "-fuzz", "8%", "-transparent", "white", str(DRAW / "start_tv_logo_transparent.png")], check=True, timeout=20)
            (DRAW / "start_tv_logo.png").unlink(missing_ok=True)
            (DRAW / "start_tv_logo_transparent.png").rename(DRAW / "start_tv_logo.png")
        except Exception as e:
            print("TV transparency fallback:", e)
        break

# Make a stable placeholder resource used by v4 image fallback.
(DRAW / "no_image.png").write_bytes(__import__("base64").b64decode(FALLBACK_PNG))

# Do not overwrite the user's exact QR. v3 already contains it; fail clearly if it is absent.
qr = DRAW / "qr_donate.png"
if not qr.exists():
    raise SystemExit("qr_donate.png is missing from the project; refusing to fabricate a donation QR")

# Patch Gradle version at build time while keeping the v3 branch source history intact.
g = (APP / "build.gradle.kts").read_text(encoding="utf-8")
g = re.sub(r"versionCode\s*=\s*\d+", "versionCode = 40", g, count=1)
g = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "4.0"', g, count=1)
(APP / "build.gradle.kts").write_text(g, encoding="utf-8")

# Add v4 helper core. It deliberately uses only APIs already available in the project.
helper = PKG_DIR / "V4UpgradeCore.kt"
helper.write_text(r'''package com.offex7.streamhub

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val V4_PRIMARY = "https://smolnp.github.io/IPTVru//IPTVstable.m3u8"
private val V4_FALLBACKS = listOf(
    "https://naggdd.github.io/iptv/ru.m3u",
    "https://raw.githubusercontent.com/blackbirdstudiorus/LoganetXIPTV/main/LoganetXAll.m3u",
    "https://iptv-org.github.io/iptv/countries/ru.m3u",
    "https://raw.githubusercontent.com/IPTVRU2026/IPTVMIR/main/IPTV_MEGA_PLAYLIST.m3u",
    "https://raw.githubusercontent.com/CrocoUser/zabava-project/refs/heads/main/zabava-ef.m3u"
)

object V4SourcePrefs {
    const val PRIMARY = V4_PRIMARY
    const val LABEL = "smolnp-стабильный"
    const val COMBINED = "Объединённый"
    private const val PREFS = "v4_playlist_source"
    fun selected(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("url", V4_PRIMARY) ?: V4_PRIMARY
    fun set(context: Context, url: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("url", url).apply() }
    fun reset(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply() }
}

class V4StatsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("v4_usage_seconds", Context.MODE_PRIVATE)
    private fun key(section: Section) = if (section == Section.TV) "tv" else "radio"
    fun seconds(section: Section): Long = prefs.getLong(key(section), 0L)
    fun add(section: Section, seconds: Long = 1L) = prefs.edit().putLong(key(section), seconds(section) + seconds).apply()
    fun reset(section: Section) = prefs.edit().putLong(key(section), 0L).apply()
    fun resetAll() = prefs.edit().clear().apply()
}

class V4PlaylistRepository(private val context: Context) {
    private val cacheFile = File(context.filesDir, "v4_combined_playlist.txt")
    private val parsed = ConcurrentHashMap<String, List<StreamItem>>()
    private val active = ConcurrentHashMap<String, Boolean>()

    suspend fun loadCachedIfFresh(maxAgeMs: Long = 6 * 60 * 60 * 1000L): List<StreamItem>? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists() || System.currentTimeMillis() - cacheFile.lastModified() > maxAgeMs) return@withContext null
        runCatching { decode(cacheFile.readText()) }.getOrNull()
    }

    suspend fun refreshCombined(): Result<List<StreamItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val orderedUrls = listOf(V4SourcePrefs.selected(context), V4_PRIMARY) + V4_FALLBACKS
            val uniqueUrls = orderedUrls.distinct()
            val lists = mutableListOf<List<StreamItem>>()
            var primaryList: List<StreamItem>? = null
            for ((i, src) in uniqueUrls.withIndex()) {
                val list = runCatching { parse(download(src)) }.getOrNull()
                if (!list.isNullOrEmpty()) {
                    parsed[src] = list
                    if (i == 0) primaryList = list
                    lists += list
                    if (i == 0) continue
                }
            }
            if (primaryList.isNullOrEmpty()) throw IllegalStateException("Не удалось загрузить ТВ-плейлист")
            val seen = HashSet<String>()
            val merged = buildList {
                for (list in lists) for (item in list) {
                    val k = normalize(item.name)
                    if (k.isNotEmpty() && seen.add(k)) add(item)
                }
            }
            cacheFile.writeText(encode(merged))
            merged
        }
    }

    suspend fun findFallback(name: String, currentUrl: String): StreamItem? = withContext(Dispatchers.IO) {
        val key = normalize(name)
        for (src in V4_FALLBACKS + listOf(V4_PRIMARY)) {
            val list = parsed[src] ?: runCatching { parse(download(src)).also { parsed[src] = it } }.getOrNull() ?: continue
            val hit = list.firstOrNull { normalize(it.name) == key && it.url != currentUrl }
            if (hit != null) return@withContext hit
        }
        null
    }

    suspend fun scan(urls: List<String>): Set<String> = coroutineScope {
        val sem = Semaphore(2)
        urls.distinct().take(15).map { url -> async(Dispatchers.IO) {
            sem.withPermit { url to head(url) }
        } }.awaitAll().filterNot { it.second }.mapTo(HashSet()) { it.first }.also { bad ->
            bad.forEach { active[it] = false }
            urls.filter { !bad.contains(it) }.forEach { active[it] = true }
        }
    }

    suspend fun check(url: String): Boolean = withContext(Dispatchers.IO) { active[url] ?: head(url).also { active[url] = it } }
    fun isActiveKnown(url: String): Boolean = active[url] ?: true

    private fun download(src: String): String {
        val c = (URL(src).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000; readTimeout = 8000; requestMethod = "GET"; useCaches = false
            setRequestProperty("User-Agent", "TV-Radio-Online/4.0")
        }
        return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    private fun head(src: String): Boolean = runCatching {
        val c = (URL(src).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3000; readTimeout = 3000; requestMethod = "HEAD"; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TV-Radio-Online/4.0")
        }
        c.responseCode in 200..399
    }.getOrDefault(false)

    private fun parse(text: String): List<StreamItem> {
        val result = ArrayList<StreamItem>()
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        var pending: String? = null
        for (line in lines) {
            if (line.startsWith("#EXTINF", true)) pending = line
            else if (!line.startsWith("#") && pending != null) {
                val meta = pending!!
                val comma = meta.indexOf(',')
                val title = if (comma >= 0) meta.substring(comma + 1).trim() else "Без названия"
                val logo = Regex("(?:tvg-logo|logo)=[\\\"]([^\\\"]+)").find(meta)?.groupValues?.getOrNull(1)
                result += makeItem(title, line, logo, null)
                pending = null
            }
        }
        return result
    }

    private fun makeItem(name: String, url: String, logo: String?, epg: String?): StreamItem {
        val ctors = StreamItem::class.java.declaredConstructors.toList().sortedBy { it.parameterTypes.size }
        for (ctor in ctors) {
            runCatching {
                val n = ctor.parameterTypes.size
                val args = when (n) {
                    2 -> arrayOf(name, url)
                    3 -> arrayOf(name, url, logo)
                    4 -> arrayOf(name, url, logo, epg)
                    else -> null
                } ?: return@runCatching null
                ctor.isAccessible = true
                return ctor.newInstance(*args) as StreamItem
            }
        }
        throw IllegalStateException("StreamItem constructor unsupported")
    }

    private fun normalize(s: String): String = s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]+"), "")
    private fun encode(items: List<StreamItem>): String = items.joinToString("\n") { listOf(enc(it.name), enc(it.url), enc(it.logoUrl ?: ""), enc(it.epgLogoUrl ?: "")).joinToString("\t") }
    private fun decode(text: String): List<StreamItem> = text.lineSequence().mapNotNull { row ->
        val p = row.split('\t')
        if (p.size < 2) null else runCatching { makeItem(dec(p[0]), dec(p[1]), p.getOrNull(2)?.let(::dec)?.ifBlank { null }, p.getOrNull(3)?.let(::dec)?.ifBlank { null }) }.getOrNull()
    }.toList()
    private fun enc(s: String) = java.util.Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
    private fun dec(s: String) = String(java.util.Base64.getDecoder().decode(s), Charsets.UTF_8)
}

object V4RadioCatalog {
    private data class Def(val name: String, val url: String)
    private val wanted = listOf(
        Def("RECORD", "https://hls-01-gpm.hostingradio.ru/recordru/recordrutv.m3u8"),
        Def("CHOCOLATE", "https://hls-01-gpm.hostingradio.ru/chocoladfm/playlist.m3u8"),
        Def("ЭНЕРДЖИ", "https://hls-01-gpm.hostingradio.ru/energyfm7/playlist.m3u8"),
        Def("ULTRA", "https://hls-01-gpm.hostingradio.ru/ultrafm7/playlist.m3u8"),
        Def("КАЛЬЯН РЭП", "https://listen7.myradio24.com/club"),
        Def("PIRATE STATION", "https://radiopotok.ru/stream/pirate"),
        Def("VOCAL DRUM", "https://listen7.myradio24.com/vocaldrum"),
        Def("CHILL HOUSE", "https://listen7.myradio24.com/chillhouse"),
        Def("PSY TRANCE", "https://listen7.myradio24.com/psytrance"),
        Def("METALCORE", "https://listen7.myradio24.com/metalcore"),
        Def("ЮГ МОЛОДОЙ", "https://listen7.myradio24.com/18718")
    )
    fun enhance(base: List<StreamItem>): List<StreamItem> {
        val template = base.firstOrNull() ?: return base
        val byNormalized = base.associateBy { norm(it.name) }.toMutableMap()
        for (d in wanted) {
            val old = byNormalized.keys.firstOrNull { it == norm(d.name) }
            if (old != null) {
                val item = byNormalized.remove(old)!!
                byNormalized[norm(d.name)] = item.copy(name = d.name, url = d.url)
            } else {
                byNormalized[norm(d.name)] = template.copy(name = d.name, url = d.url)
            }
        }
        byNormalized.remove(norm("KissFM (ру)"))
        byNormalized.remove(norm("SOUNDPARK DEEP"))
        return byNormalized.values.toList().sortedBy { wanted.indexOfFirst { w -> norm(w.name) == norm(it.name) }.let { i -> if (i < 0) 999 else i } )
    }
    private fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]+"), "")
}
''', encoding="utf-8")

# Replace/augment selected MainActivity functions. The helper functions are intentionally self-contained so a build failure does not corrupt v3 sources.
s = JAVA.read_text(encoding="utf-8")

# Imports used by v4 UI.
anchor = "import androidx.compose.foundation.gestures.detectTransformGestures\n"
imports = anchor + "import androidx.compose.foundation.layout.FlowRow\nimport androidx.compose.foundation.border\n"
s = s.replace(anchor, imports, 1)
s = s.replace("import androidx.compose.material.icons.filled.Favorite\n", "import androidx.compose.material.icons.filled.Favorite\nimport androidx.compose.material.icons.filled.FavoriteBorder\n", 1)
s = s.replace("import androidx.compose.material3.Switch\n", "import androidx.compose.material3.Switch\nimport androidx.compose.material3.SwitchDefaults\n", 1)
s = s.replace("import androidx.compose.runtime.collectAsState\n", "import androidx.compose.runtime.collectAsState\nimport androidx.compose.animation.animateColorAsState\nimport androidx.compose.animation.core.animateFloatAsState\n", 1)

# Settings caller gets access to the real sleep timer and factory reset also clears v4 stats/source.
s = s.replace("onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },\n                    onBack = { settingsOpen = false },", "sleepUntil = sleepUntil,\n                    onSleep = { minutes -> sleepUntil = System.currentTimeMillis() + minutes * 60_000L },\n                    onSleepCancel = { sleepUntil = 0L },\n                    onBack = { settingsOpen = false },", 1)
s = s.replace("scope.launch { store.resetAll() }", "scope.launch { store.resetAll(); V4SourcePrefs.reset(activity); V4StatsStore(activity.applicationContext).resetAll() }", 1)

# Start screen: clean responsive layout, large local logos, version in the top row.
start = re.compile(r'@Composable\nprivate fun PickerScreen\(.*?(?=@Composable\nprivate fun PickerCard)', re.S)
new_start = r'''@Composable
private fun PickerScreen(onSelect: (Section) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.displayCutout)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text("TV_RADIO_ONLINE_V4.0", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
            }
            Spacer(Modifier.height(12.dp))
            val landscape = maxWidth > 560.dp
            if (landscape) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    StartModeCard("ТЕЛЕВИЗОР", R.drawable.start_tv_logo, Modifier.weight(1f)) { onSelect(Section.TV) }
                    StartModeCard("РАДИО", R.drawable.start_radio_logo, Modifier.weight(1f)) { onSelect(Section.RADIO) }
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StartModeCard("ТЕЛЕВИЗОР", R.drawable.start_tv_logo, Modifier.weight(1f)) { onSelect(Section.TV) }
                    StartModeCard("РАДИО", R.drawable.start_radio_logo, Modifier.weight(1f)) { onSelect(Section.RADIO) }
                }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { openUrl(context, TELEGRAM_URL) }, modifier = Modifier.fillMaxWidth()) {
                Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StartModeCard(label: String, logoRes: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(logoRes), label, Modifier.fillMaxWidth(0.58f).weight(1f), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

'''
s, n = start.subn(new_start, s, count=1)
if n != 1: raise SystemExit("PickerScreen patch failed")

# Player/playlist: use v4 repository with working primary+fallback merge.
s = s.replace("val repo = remember { PlaylistRepository(context.applicationContext) }", "val repo = remember { V4PlaylistRepository(context.applicationContext) }", 1)

# Radio screen references the v4 catalog and records usage in real time.
radio_pat = re.compile(r'@Composable\nprivate fun RadioScreen\(.*?(?=@Composable\nprivate fun TvScreen)', re.S)
m = radio_pat.search(s)
if not m: raise SystemExit("RadioScreen not found")
radio_block = m.group(0)
radio_block = radio_block.replace("val scope = rememberCoroutineScope()", "val scope = rememberCoroutineScope()\n    val stations = remember { V4RadioCatalog.enhance(RADIO_STATIONS) }\n    val stats = remember { V4StatsStore(context.applicationContext) }", 1)
radio_block = radio_block.replace("LaunchedEffect(Unit) { while (true) { delay(1000L); sessionSeconds += 1L } }", "LaunchedEffect(Unit) { while (true) { delay(1000L); sessionSeconds += 1L; stats.add(Section.RADIO) } }", 1)
radio_block = radio_block.replace("RADIO_STATIONS", "stations")
# Remove stop control and change state text in the compact top radio player.
radio_block = radio_block.replace('IconButton(onClick = player::stop) { Icon(Icons.Default.Stop, null, tint = Red) }', '')
radio_block = radio_block.replace('Text(if (playing) "Воспроизведение" else "Пауза"', 'Text(if (playing) "Играет" else "Пауза"')
# Save radio scroll position between sessions.
radio_block = radio_block.replace("val networkAvailable = rememberNetworkAvailable(context)", "val networkAvailable = rememberNetworkAvailable(context)\n    val listState = rememberLazyListState()", 1)
radio_block = radio_block.replace("BackHandler(enabled = searchOpen)", "LaunchedEffect(Unit) { val p = store.scrollPosition(Section.RADIO); listState.scrollToItem(p.first, p.second) }\n    LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { p -> store.saveScrollPosition(Section.RADIO, p.first, p.second) } }\n    LaunchedEffect(listState.isScrollInProgress) { val w = context as? ComponentActivity; w?.window?.let { c -> val ctl = WindowInsetsControllerCompat(it, it.decorView); if (listState.isScrollInProgress) ctl.hide(WindowInsetsCompat.Type.navigationBars()) else ctl.show(WindowInsetsCompat.Type.navigationBars()) } }\n\n    BackHandler(enabled = searchOpen)", 1)
radio_block = radio_block.replace('LazyColumn(contentPadding = PaddingValues(10.dp)', 'LazyColumn(state = listState, contentPadding = PaddingValues(10.dp)', 1)
s = s[:m.start()] + radio_block + s[m.end():]

# TV usage counter and navigation-bar hide while scrolling.
s = s.replace("LaunchedEffect(Unit) { while (true) { delay(1000L); sessionSeconds += 1L } }", "LaunchedEffect(Unit) { while (true) { delay(1000L); sessionSeconds += 1L; V4StatsStore(context.applicationContext).add(Section.TV) } }", 1)
s = s.replace("LaunchedEffect(listState) { snapshotFlow {", "LaunchedEffect(listState) { snapshotFlow {", 1)
s = s.replace("store.saveScrollPosition(Section.TV, pos.first, pos.second) } }", "store.saveScrollPosition(Section.TV, pos.first, pos.second) } }\n    LaunchedEffect(listState.isScrollInProgress) { val w = context as? ComponentActivity; w?.window?.let { c -> val ctl = WindowInsetsControllerCompat(it, it.decorView); if (listState.isScrollInProgress) ctl.hide(WindowInsetsCompat.Type.navigationBars()) else ctl.show(WindowInsetsCompat.Type.navigationBars()) } }", 1)

# Animated favorite icon. Existing call sites remain source-compatible because onFavorite is optional.
chan_pat = re.compile(r'@Composable\nprivate fun ChannelRow\(.*?(?=@Composable\nprivate fun LogoImage)', re.S)
new_chan = r'''@Composable
private fun ChannelRow(item: StreamItem, favorite: Boolean, playing: Boolean, offline: Boolean, tv: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, onFavorite: (() -> Unit)? = null) {
    val favColor by animateColorAsState(if (favorite) Red else Color.Gray, label = "favoriteColor")
    val favScale by animateFloatAsState(if (favorite) 1.12f else 1f, label = "favoriteScale")
    Card(Modifier.fillMaxWidth().alpha(if (offline) 0.4f else 1f), colors = CardDefaults.cardColors(containerColor = if (favorite) Color(0x221E1E1E) else Panel), shape = RoundedCornerShape(13.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(item, 48.dp, tv); Spacer(Modifier.width(9.dp)); Text(item.name.uppercase(Locale.ROOT).ifBlank { "БЕЗ НАЗВАНИЯ" }, Modifier.weight(1f), maxLines = 2)
            if (offline) Box(Modifier.size(7.dp).background(Color.Gray, RoundedCornerShape(50)))
            if (playing) Icon(Icons.Default.PlayArrow, "Играет", tint = Red, modifier = Modifier.size(18.dp))
            IconButton(onClick = { (onFavorite ?: onLongClick).invoke() }, modifier = Modifier.graphicsLayer(scaleX = favScale, scaleY = favScale)) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = favColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

'''
s, n = chan_pat.subn(new_chan, s, count=1)
if n != 1: raise SystemExit("ChannelRow patch failed")

# Local radio logos before playlist logos/placeholder. Keep existing URL fallback after local resource.
logo_pat = re.compile(r'@Composable\nprivate fun LogoImage\(.*?(?=@Composable\nprivate fun PlaceholderLogo)', re.S)
new_logo = r'''@Composable
private fun LogoImage(item: StreamItem, size: Dp, tv: Boolean) {
    val local = if (!tv) when (item.name.uppercase(Locale.ROOT)) {
        "RECORD" -> R.drawable.radio_record
        "CHOCOLATE" -> R.drawable.radio_chocolate
        "ЭНЕРДЖИ" -> R.drawable.radio_energy
        "ULTRA" -> R.drawable.radio_ultra
        "КАЛЬЯН РЭП" -> R.drawable.radio_kalyan
        "PIRATE STATION" -> R.drawable.radio_pirate
        "VOCAL DRUM" -> R.drawable.radio_vocal
        "CHILL HOUSE" -> R.drawable.radio_chill
        "PSY TRANCE" -> R.drawable.radio_psy
        "METALCORE" -> R.drawable.radio_metalcore
        "ЮГ МОЛОДОЙ" -> R.drawable.radio_yug
        else -> 0
    } else 0
    if (local != 0) {
        Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(size), color = Skeleton) {
            Image(painterResource(local), item.name, Modifier.fillMaxSize().padding(1.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        return
    }
    val context = LocalContext.current
    var sourceIndex by remember(item.url) { mutableIntStateOf(0) }
    val sources = remember(item.url, item.logoUrl, item.epgLogoUrl, tv) { listOfNotNull(LogoCache.knownLogo(item), item.logoUrl, item.epgLogoUrl).distinct() }
    if (sourceIndex < sources.size) AsyncImage(model = ImageRequest.Builder(context).data(sources[sourceIndex]).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(), contentDescription = item.name, modifier = Modifier.size(size), onError = { sourceIndex += 1 }) else PlaceholderLogo(size)
}

'''
s, n = logo_pat.subn(new_logo, s, count=1)
if n != 1: raise SystemExit("LogoImage patch failed")

# Settings screen: compact timer circles, donation redesign, working stats, persistent PiP/source, factory reset label.
set_pat = re.compile(r'@Composable\nprivate fun SettingsScreen\(.*?(?=@Composable\nprivate fun StatsRow)', re.S)
new_settings = r'''@Composable
private fun SettingsScreen(store: SettingsStore, pipEnabled: Boolean, onPipChange: (Boolean) -> Unit, onBack: () -> Unit, sleepUntil: Long, onSleep: (Long) -> Unit, onSleepCancel: () -> Unit, onStatsReset: (Section) -> Unit, onDisclaimer: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val stats = remember { V4StatsStore(context.applicationContext) }
    var copied by remember { mutableStateOf(false) }
    var timerUntil by remember { mutableLongStateOf(sleepUntil) }
    var countdown by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }
    val tvTotal = stats.seconds(Section.TV)
    val radioTotal = stats.seconds(Section.RADIO)
    val presets = listOf("15 мин" to 15L, "30 мин" to 30L, "1 ч" to 60L, "2 ч" to 120L, "4 ч" to 240L, "8 ч" to 480L, "10 ч" to 600L, "15 ч" to 900L, "24 ч" to 1440L, "36 ч" to 2160L)
    LaunchedEffect(timerUntil) { while (timerUntil > 0) { val left = timerUntil - System.currentTimeMillis(); if (left <= 0) { timerUntil = 0; countdown = 0; break }; countdown = left; delay(1000) } }
    LaunchedEffect(notice) { if (notice != null) { delay(5000); notice = null } }
    LaunchedEffect(sleepUntil) { timerUntil = sleepUntil }

    fun choose(minutes: Long) { val until = System.currentTimeMillis() + minutes * 60_000L; timerUntil = until; onSleep(minutes); notice = "Таймер сна — запущен"; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun cancelTimer() { timerUntil = 0; onSleepCancel(); notice = "Таймер сна — отключён" }

    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars), color = Background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Red) }
                    Text("Настройки", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Таймер сна", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(7.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    presets.forEach { (label, mins) ->
                                        val active = timerUntil > 0 && ((timerUntil - System.currentTimeMillis()) / 60_000L + 1) >= mins - 1 && label.isNotEmpty()
                                        OutlinedButton(onClick = { if (active) cancelTimer() else choose(mins) }, modifier = Modifier.size(width = 72.dp, height = 54.dp), shape = RoundedCornerShape(27.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Red else Color.Gray), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = if (active) Red else Color.Transparent, contentColor = Color.White), contentPadding = PaddingValues(0.dp)) {
                                            Text(if (active && countdown > 0) formatRemaining(countdown) else label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                                if (timerUntil > 0) { Spacer(Modifier.height(8.dp)); Text("Нажмите активный круг для отключения", color = Color.Gray, fontSize = 11.sp) }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Favorite, null, tint = Red); Spacer(Modifier.width(8.dp)); Text("Поддержать проект", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                                Spacer(Modifier.height(7.dp)); Text("USDT TRC20", fontSize = 11.sp, color = Color.LightGray)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth().background(PanelAlt, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text("Адрес кошелька", color = Color.LightGray, fontSize = 11.sp); Text(DONATION_WALLET, fontSize = 12.sp, maxLines = 2) }
                                    TextButton(onClick = { val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("Donation wallet", DONATION_WALLET)); copied = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) { Text(if (copied) "Скопировано" else "Копировать", color = Red, fontSize = 12.sp) }
                                }
                                Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.size(180.dp)) { Image(painterResource(R.drawable.qr_donate), "QR пожертвований", Modifier.fillMaxSize().padding(7.dp)) } }
                            }
                        }
                    }
                    item { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tv, null, tint = Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("PiP при сворачивании", fontWeight = FontWeight.SemiBold); Text("Мини-окно ТВ при уходе из приложения", fontSize = 11.sp, color = Color.LightGray) }; Switch(checked = pipEnabled, onCheckedChange = onPipChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF808080))) } } }
                    item { Text("Статистика", style = MaterialTheme.typography.titleMedium) }
                    item { StatsRow("Общее время использования ТВ", tvTotal * 1000L, "Сбросить счётчик ТВ") { stats.reset(Section.TV); onStatsReset(Section.TV); notice = "Счётчик ТВ сброшен" } }
                    item { StatsRow("Общее время использования Радио", radioTotal * 1000L, "Сбросить счётчик Радио") { stats.reset(Section.RADIO); onStatsReset(Section.RADIO); notice = "Счётчик Радио сброшен" } }
                    item {
                        val sources = listOf(V4_PRIMARY to "smolnp-стабильный") + V4_FALLBACKS.mapIndexed { i, u -> u to "Резерв ${i + 1}" }
                        val current = V4SourcePrefs.selected(context)
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) { Text("Источник ТВ-плейлиста", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(5.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { sources.forEach { (url, label) -> TextButton(onClick = { V4SourcePrefs.set(context, url) }) { Text(if (url == current) "✓ $label" else label, color = if (url == current) Red else Color.LightGray, fontSize = 11.sp) } } }; Text("Автоматический fallback и объединение работают скрыто", fontSize = 11.sp, color = Color.Gray) }
                        }
                    }
                    item { TextButton(onClick = onDisclaimer, Modifier.fillMaxWidth()) { Text("Отказ от ответственности", color = Color.White) } }
                    item { Spacer(Modifier.height(4.dp)); Button(onClick = onReset, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Сбросить настройки до заводских") } }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
            notice?.let { Text(it, Modifier.align(Alignment.BottomCenter).padding(14.dp).background(PanelAlt, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 11.dp), color = Color.White, fontSize = 13.sp) }
        }
    }
}

'''
s, n = set_pat.subn(new_settings, s, count=1)
if n != 1: raise SystemExit("SettingsScreen patch failed")

# Display stats in human-readable hours/minutes with no seconds.
s = s.replace('Text(formatDuration(millis), color = Red, fontSize = 18.sp)', 'Text(formatHoursMinutes(millis), color = Red, fontSize = 18.sp)', 1)
# Add helper formatter near the end.
s = s.replace('private fun formatDuration(ms: Long): String = formatRemaining(ms)\n', 'private fun formatDuration(ms: Long): String = formatRemaining(ms)\nprivate fun formatHoursMinutes(ms: Long): String { val total = (ms / 1000L).coerceAtLeast(0L); val h = total / 3600L; val m = (total % 3600L) / 60L; return if (h > 0) "${h} ч ${m} мин" else "${m} мин" }\n', 1)

# Use local radio logos for the compact top player and list; remove old LogoCache resource preloading from startup if it creates duplicate network load.
s = s.replace("LogoCache.preload(activity)", "", 1)

# Ensure the displayed radio status has no stale Russian phrase.
s = s.replace('"Воспроизведение"', '"Играет"')

# Add a 5-second silent fallback switch for TV channels. It tries the same normalized name on reserve playlists.
needle = 'fun openPlayer(item: StreamItem) { selectedIndex = channels.indexOfFirst { it.url == item.url }; player.play(item.url); fullScreen = true; onFullScreenChanged(true); onSelect(item) }'
replacement = needle + '\n\n    LaunchedEffect(selectedIndex, fullScreen) { if (fullScreen && selectedIndex in channels.indices) { val item = channels[selectedIndex]; delay(5000); if (player.error.value != null || !player.isPlaying.value) { val alt = repo.findFallback(item.name, item.url); if (alt != null) player.play(alt.url) } } }'
s = s.replace(needle, replacement, 1)

JAVA.write_text(s, encoding="utf-8")
print("v4 patch complete", JAVA)
