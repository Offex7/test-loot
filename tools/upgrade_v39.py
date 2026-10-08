#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path("app")
MAIN = ROOT / "src/main/java/com/offex7/streamhub/MainActivity.kt"
GRADLE = ROOT / "build.gradle.kts"

def read(path):
    return path.read_text(encoding="utf-8")

def write(path, content):
    path.write_text(content, encoding="utf-8")

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)

# ---------- Version ----------
g = read(GRADLE)
g = replace_once(g, "versionCode = 11", "versionCode = 12", "versionCode")
g = replace_once(g, 'versionName = "3.8"', 'versionName = "3.9"', "versionName")
write(GRADLE, g)

# ---------- MainActivity ----------
m = read(MAIN)

if "import androidx.compose.foundation.clickable\n" not in m:
    m = replace_once(
        m,
        "import androidx.compose.foundation.combinedClickable\n",
        "import androidx.compose.foundation.combinedClickable\nimport androidx.compose.foundation.clickable\n",
        "clickable import"
    )

m = replace_once(
    m,
    "class MainActivity : FragmentActivity() {\n    private lateinit var store: SettingsStore",
    """class MainActivity : FragmentActivity() {
    internal var requestedSection by mutableStateOf<Section?>(null)

    companion object {
        const val EXTRA_WIDGET_SECTION = "widget_section"
    }

    private lateinit var store: SettingsStore""",
    "MainActivity widget state"
)

m = replace_once(
    m,
    """        store = SettingsStore(applicationContext)
        tv = PlayerController(applicationContext)""",
    """        store = SettingsStore(applicationContext)
        requestedSection = when (intent.getStringExtra(EXTRA_WIDGET_SECTION)) {
            Section.RADIO.name -> Section.RADIO
            Section.TV.name -> Section.TV
            else -> null
        }
        tv = PlayerController(applicationContext)""",
    "widget section onCreate"
)

m = replace_once(
    m,
    """    override fun onStop() {
        lifecycleScope.launch { store.setLastExitTime(System.currentTimeMillis()) }""",
    """    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedSection = when (intent.getStringExtra(EXTRA_WIDGET_SECTION)) {
            Section.RADIO.name -> Section.RADIO
            Section.TV.name -> Section.TV
            else -> null
        }
    }

    override fun onStop() {
        lifecycleScope.launch { store.setLastExitTime(System.currentTimeMillis()) }""",
    "widget section onNewIntent"
)

m = replace_once(
    m,
    """    LaunchedEffect(Unit) {
        if (!store.disclaimerShown()) {""",
    """    LaunchedEffect(activity.requestedSection, pinUnlocked) {
        val requested = activity.requestedSection ?: return@LaunchedEffect
        if (!pinUnlocked) return@LaunchedEffect
        tv.stop()
        radio.stop()
        activity.tvViewing = false
        section = requested
        settings = false
        disclaimer = false
        restore = null
        scope.launch { store.setSection(requested) }
        activity.requestedSection = null
    }

    LaunchedEffect(Unit) {
        if (!store.disclaimerShown()) {""",
    "widget navigation effect"
)

m = replace_once(
    m,
    """    LaunchedEffect(notificationToken) {
        if (notification == null || notificationToken == 0L) return@LaunchedEffect
        val token = notificationToken
        delay(notificationDuration)
        if (token == notificationToken) notification = null
    }

    LaunchedEffect(Unit) {
        pip = store.pipEnabled()""",
    """    LaunchedEffect(notificationToken) {
        if (notification == null || notificationToken == 0L) return@LaunchedEffect
        val token = notificationToken
        delay(notificationDuration)
        if (token == notificationToken) notification = null
    }

    LaunchedEffect(Unit) {
        delay(250L)
        val powerManager = appContext.getSystemService(android.os.PowerManager::class.java)
        val activityManager = appContext.getSystemService(android.app.ActivityManager::class.java)
        val batteryRestricted = runCatching {
            powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == false
        }.getOrDefault(false)
        val backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            activityManager?.isBackgroundRestricted == true
        if (batteryRestricted || backgroundRestricted) {
            notify("Фоновая работа Radio.TV может быть ограничена ОС. Добавьте приложение в исключения энергосбережения.", 3000L)
        }
    }

    LaunchedEffect(Unit) {
        pip = store.pipEnabled()""",
    "energy restriction notification"
)

m = replace_once(
    m,
    """                    onHaptic = { InteractionFeedback.click(appContext,hapticsEnabled,soundEnabled,false) },
                    open = {
                        section = it
                        scope.launch { store.setSection(it) }
                    },
                    settings = { settings = true }
                )""",
    """                    onHaptic = { InteractionFeedback.click(appContext,hapticsEnabled,soundEnabled,false) },
                    open = {
                        section = it
                        scope.launch { store.setSection(it) }
                    },
                    settings = { settings = true },
                    onCheckUpdate = { scope.launch { checkForUpdates(manual = true) } }
                )""",
    "Home update callback"
)

m = replace_once(
    m,
    """private fun Home(
    activity: MainActivity,
    hapticsEnabled: Boolean,
    onHaptic: () -> Unit,
    open: (Section) -> Unit,
    settings: () -> Unit
) {""",
    """private fun Home(
    activity: MainActivity,
    hapticsEnabled: Boolean,
    onHaptic: () -> Unit,
    open: (Section) -> Unit,
    settings: () -> Unit,
    onCheckUpdate: () -> Unit
) {""",
    "Home signature"
)

m = replace_once(
    m,
    """                    Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Radio.TV ") }
                        withStyle(SpanStyle(color = Color(0xFF4CAF50))) { append(APP_VERSION) }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )""",
    """                    Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Radio.TV ") }
                        withStyle(SpanStyle(color = Color(0xFF4CAF50))) { append(APP_VERSION) }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.clickable { onCheckUpdate() }
                )""",
    "Home version tap"
)

# Move update check banner directly below support card.
start_marker = """        item {
            val updatePulseTransition = rememberInfiniteTransition(label = "settings-update-pulse")"""
end_marker = """        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("КАРТИНКА В КАРТИНКЕ", fontSize = 16.sp)"""
start = m.find(start_marker)
end = m.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("update banner move markers not found")
m = m[:start] + m[end:]

donation_marker = "        item { DonationCard() }\n"
banner_block = """        item {
            Card(
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = if (updateAvailable) Color(0xFF2E7D32) else Red),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "ПРОВЕРИТЬ ОБНОВЛЕНИЕ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
"""
m = replace_once(m, donation_marker, donation_marker + banner_block, "settings update banner placement")

m = replace_once(
    m,
    """                    Text("СТАТИСТИКА", color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)""",
    """                    Text("СТАТИСТИКА", color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)""",
    "statistics centered"
)

m = replace_once(
    m,
    """                    Text("ТАЙМЕР СНА", color = Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SleepGrid(sleepRemaining, sleepUntil, sleepMinutes, onSleep, onCancelSleep)""",
    """                    Text("ТАЙМЕР СНА", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    SleepGrid(
                        remaining = sleepRemaining,
                        until = sleepUntil,
                        duration = sleepMinutes,
                        onSelect = onSleep,
                        onCancel = onCancelSleep,
                        hapticsEnabled = hapticsEnabled,
                        soundEnabled = soundEnabled
                    )""",
    "sleep grid settings"
)

m = replace_once(
    m,
    """                        Text("Сохраняется после перезапуска", fontSize = 11.sp, color = Color.Gray)
""",
    "",
    "pip description"
)

m = replace_once(
    m,
    """                        Text("ТАКТИЛЬНАЯ ОБРАТНАЯ СВЯЗЬ", fontSize = 16.sp)
                        Text("Общий виброотклик для кнопок и действий",fontSize=11.sp,color=Gray)""",
    """                        Text("ВИБРАЦИОННЫЙ ОТКЛИК", fontSize = 16.sp)""",
    "haptic setting label"
)

m = replace_once(
    m,
    """                    Column(Modifier.weight(1f)){Text("ЗВУКОВОЙ ОТКЛИК",fontSize=16.sp);Text("Короткий сигнал на ТВ/устройствах без вибрации",fontSize=11.sp,color=Gray)}""",
    """                    Column(Modifier.weight(1f)){Text("ЗВУКОВОЙ ОТКЛИК",fontSize=16.sp)}""",
    "sound description"
)

# Simplify app protection card to a single row.
prot_start = """        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ЗАЩИТА ПРИЛОЖЕНИЯ\""""
ps = m.find(prot_start)
pe = m.find("""        item {
            val sharePulse""", ps)
if ps < 0 or pe < 0:
    # fallback for exact source spelling
    ps = m.find("""        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ЗАЩИТА ПРИЛОЖЕНИЯ", fontSize = 16.sp, fontWeight = FontWeight.Bold)""")
    pe = m.find("""        item {
            val sharePulse""", ps)
if ps < 0 or pe < 0:
    raise SystemExit("protection card markers not found")
protection = """        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Pin-код на вход", color = if (pinEnabled) Red else Color.White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) pinDialog = true else pinDisableDialog = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Red,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Gray
                        )
                    )
                }
            }
        }
"""
m = m[:ps] + protection + m[pe:]

# Replace export block with dialog-backed clipboard snapshot.
ex_start = m.find("""        item {
            val sharePulse""")
ex_end = m.find("""        if (hiddenChannels.isNotEmpty()) {""", ex_start)
if ex_start < 0 or ex_end < 0:
    raise SystemExit("export block markers not found")
export_block = """        item {
            Card(
                onClick = {
                    scope.launch {
                        runCatching { LogExporter.buildText(settingsContext, store) }
                            .onSuccess { logDialogText = it }
                            .onFailure { notify("Не удалось подготовить логи", 3000L) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ЭКСПОРТ ЛОГОВ", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Share, "Показать логи", tint = Red)
                }
            }
        }
"""
m = m[:ex_start] + export_block + m[ex_end:]

# Add log dialog state.
m = replace_once(
    m,
    """    var pinDisableDialog by remember { mutableStateOf(false) }
    val pinStore""",
    """    var pinDisableDialog by remember { mutableStateOf(false) }
    var logDialogText by remember { mutableStateOf<String?>(null) }
    val pinStore""",
    "log dialog state"
)

# Add log dialog before source chooser dialog.
log_dialog = """    logDialogText?.let { logText ->
        AlertDialog(
            onDismissRequest = { logDialogText = null },
            title = { Text("ЭКСПОРТ ЛОГОВ") },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(logText, color = Color.LightGray, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = settingsContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Radio.TV logs", logText))
                        notify("Скопировано", 3000L)
                    }
                ) { Text("СКОПИРОВАТЬ", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { logDialogText = null }) { Text("ЗАКРЫТЬ") }
            }
        )
    }

"""
m = replace_once(m, "    if (sourceDialog) {", log_dialog + "    if (sourceDialog) {", "log dialog insertion")

# Radio timer button/card feedback.
m = replace_once(
    m,
    """                        IconButton(onClick={InteractionFeedback.vibrate(context,hapticsEnabled,42L,105);sleepMenu=!sleepMenu}) {""",
    """                        IconButton(onClick={InteractionFeedback.click(context,hapticsEnabled,soundEnabled);sleepMenu=!sleepMenu}) {""",
    "radio sleep icon feedback"
)
m = replace_once(
    m,
    """                                                Card(onClick={
                                                    InteractionFeedback.vibrate(context,hapticsEnabled,42L,105)
                                                    if(active)onCancelSleep()else onSleep(minutes)""",
    """                                                Card(onClick={
                                                    InteractionFeedback.click(context,hapticsEnabled,soundEnabled)
                                                    if(active)onCancelSleep()else onSleep(minutes)""",
    "radio sleep option feedback"
)

# TV player timer feedback and stores.
m = replace_once(
    m,
    """    val activity = LocalContext.current as? ComponentActivity
    val portrait""",
    """    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settingsStore = remember(context) { SettingsStore(context.applicationContext) }
    val hapticsEnabled by settingsStore.hapticsFlow().collectAsState(true)
    val soundEnabled by settingsStore.soundFeedbackFlow().collectAsState(true)
    val portrait""",
    "tv timer feedback stores"
)
m = replace_once(
    m,
    """                        IconButton(onClick = { showBars(); sleepMenu = !sleepMenu; sleepMenuToken++ }) {""",
    """                        IconButton(onClick = { InteractionFeedback.click(context,hapticsEnabled,soundEnabled); showBars(); sleepMenu = !sleepMenu; sleepMenuToken++ }) {""",
    "tv sleep icon feedback"
)
m = replace_once(
    m,
    """                                    onClick = {
                                        if (active) onCancelSleep() else onSleep(minutes)
                                        sleepMenu = false
                                    },""",
    """                                    onClick = {
                                        InteractionFeedback.click(context,hapticsEnabled,soundEnabled)
                                        if (active) onCancelSleep() else onSleep(minutes)
                                        sleepMenu = false
                                    },""",
    "tv sleep option feedback"
)

# PIN biometric + portrait lock.
m = replace_once(
    m,
    """@Composable
private fun PinGate(
    activity: MainActivity,
    store: PinSecurityStore,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current""",
    """@Composable
private fun PinGate(
    activity: MainActivity,
    store: PinSecurityStore,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(activity) {
        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = previousOrientation }
    }""",
    "PIN portrait"
)

m = replace_once(
    m,
    """        if (!biometricAvailable || lockoutRemaining > 0L) return""",
    """        if (!biometricAvailable) return""",
    "biometric lockout gate"
)
m = replace_once(
    m,
    """                .setSubtitle("Разблокировать приложение при помощи биометрии или введите пароль вручную")""",
    """                .setSubtitle("ОТСКАНИРУЙТЕ ОТПЕЧАТОК ПАЛЬЦА")""",
    "biometric subtitle"
)
m = replace_once(
    m,
    """                "Разблокировать приложение при помощи биометрии или введите пароль вручную",""",
    """                "ОТСКАНИРУЙТЕ ОТfечаток ПАЛЬЦА",""",
    "pin screen placeholder"
)
# Correct the intentional normalized replacement to the exact requested string.
m = m.replace("ОТСКАНИРУЙТЕ ОТfечаток ПАЛЬЦА", "ОТСКАНИРУЙТЕ ОТПЕЧАТОК ПАЛЬЦА")
m = replace_once(
    m,
    """                    enabled = biometricAvailable && lockoutRemaining <= 0L,""",
    """                    enabled = biometricAvailable,""",
    "biometric button lockout"
)

# Ensure notification close uses configured feedback.
m = replace_once(
    m,
    """                            onClick = { notification = null },""",
    """                            onClick = {
                                InteractionFeedback.click(appContext,hapticsEnabled,soundEnabled)
                                notification = null
                            },""",
    "notification close feedback"
)

# Sleep grid: active circle pulse and feedback.
sleep_start = m.find("""private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit
) {""")
if sleep_start < 0:
    raise SystemExit("SleepGrid signature not found")
old_sig = """private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit
) {"""
new_sig = """private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean
) {"""
m = replace_once(m, old_sig, new_sig, "SleepGrid signature")

m = replace_once(
    m,
    """    val options = SleepOptions
    FlowRow(""",
    """    val options = SleepOptions
    val context = LocalContext.current
    val energySaving = LocalEnergySaving.current
    FlowRow(""",
    "SleepGrid context"
)

m = replace_once(
    m,
    """            val active = minutes == duration && until > System.currentTimeMillis()
            val background by animateColorAsState(if (active) Red else PanelAlt, label = "sleep-$minutes")
            Card(
                onClick = { if (active) onCancel() else onSelect(minutes) },
                modifier = Modifier.size(66.dp),""",
    """            val active = minutes == duration && until > System.currentTimeMillis()
            val background by animateColorAsState(if (active) Red else PanelAlt, label = "sleep-$minutes")
            val pulseScale by animateFloatAsState(
                targetValue = if (active && !energySaving) 1.035f else 1f,
                animationSpec = if (active && !energySaving) {
                    infiniteRepeatable(
                        animation = tween(500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                } else tween(120),
                label = "sleep-$minutes-pulse"
            )
            Card(
                onClick = {
                    InteractionFeedback.click(context,hapticsEnabled,soundEnabled)
                    if (active) onCancel() else onSelect(minutes)
                },
                modifier = Modifier.size(66.dp).graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),""",
    "SleepGrid pulse"
)

# Cache remote logos on disk.
old_remote = """@Composable
private fun RemoteLogoImage(
    url: String,
    contentDescription: String,
    modifier: Modifier
) {
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                logoHttpClient.newCall(
                    Request.Builder().url(url).get().build()
                ).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use(BitmapFactory::decodeStream)?.asImageBitmap()
                    } else null
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } ?: Box(
        modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}"""
new_remote = """@Composable
private fun RemoteLogoImage(
    url: String,
    contentDescription: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    val cache = remember(context.applicationContext) { TvLogoCache(context.applicationContext) }
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = cache.loadImageBitmap(url)
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } ?: Box(
        modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}"""
m = replace_once(m, old_remote, new_remote, "remote logo disk cache")

m = replace_once(
    m,
    """.header("User-Agent", "Radio.TV/3.8")""",
    """.header("User-Agent", "Radio.TV/3.9")""",
    "radio User-Agent"
)

write(MAIN, m)

# ---------- LogExporter: clipboard snapshot, no file/share ----------
log_exporter = r'''package com.offex7.streamhub

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

object LogExporter {
    suspend fun buildText(context: Context, store: SettingsStore): String {
        val timestamp = System.currentTimeMillis()
        val metrics = context.resources.displayMetrics
        val language = Locale.getDefault().toLanguageTag()

        val capturedLogcat = runCatching {
            ProcessBuilder("logcat", "-d", "-t", "100", "-v", "time")
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")

        val fallbackLines = synchronized(this) { recentLines.toList() }
        val logLines = (capturedLogcat.ifBlank { fallbackLines.joinToString("\n") })
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(100)

        val settingsSnapshot = buildString {
            appendLine("energy_saving_mode=" + store.energySavingMode())
            appendLine("haptics_enabled=" + store.hapticsEnabled())
            appendLine("sound_feedback_enabled=" + store.soundFeedbackFlow().first())
            appendLine("autostart_enabled=" + store.autoStartEnabled())
            appendLine("pip_enabled=" + store.pipEnabled())
            appendLine("active_tv_source=" + store.activeSourceKey().let { key ->
                if (key.startsWith(USER_SOURCE_PREFIX)) "user_playlist" else key
            })
            appendLine("user_playlists_count=" + store.userPlaylists().size)
            appendLine("hidden_channels_count=" + store.hiddenChannels().size)
            appendLine("tv_favorites_count=" + store.favorites(Section.TV).size)
            appendLine("radio_favorites_count=" + store.favorites(Section.RADIO).size)
            val eq = store.radioEqualizerSettings()
            appendLine("radio_eq_bass=" + eq.bass)
            appendLine("radio_eq_mid=" + eq.mid)
            appendLine("radio_eq_treble=" + eq.treble)
            appendLine("radio_eq_preset=" + eq.preset)
            appendLine("radio_normalize=" + eq.normalize)
        }

        return buildString {
            appendLine("Radio.TV log export")
            appendLine("created_at_epoch_ms=" + timestamp)
            appendLine("created_at_local=" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp)))
            appendLine()
            appendLine("[app]")
            appendLine("version_name=" + BuildConfig.VERSION_NAME)
            appendLine("version_code=" + BuildConfig.VERSION_CODE)
            appendLine()
            appendLine("[device]")
            appendLine("manufacturer=" + Build.MANUFACTURER)
            appendLine("model=" + Build.MODEL)
            appendLine("android_version=" + Build.VERSION.RELEASE)
            appendLine("sdk=" + Build.VERSION.SDK_INT)
            appendLine("display=" + Build.DISPLAY)
            appendLine("fingerprint=" + Build.FINGERPRINT)
            appendLine("screen_width_px=" + metrics.widthPixels)
            appendLine("screen_height_px=" + metrics.heightPixels)
            appendLine("density=" + metrics.density)
            appendLine("density_dpi=" + metrics.densityDpi)
            appendLine("language=" + language)
            appendLine()
            appendLine("[settings_without_personal_data]")
            append(settingsSnapshot)
            appendLine()
            appendLine("[last_100_logcat_lines]")
            if (logLines.isEmpty()) appendLine("No logcat lines available")
            else logLines.forEach(::appendLine)
        }
    }

    private val recentLines = ArrayDeque<String>(1000)

    fun log(message: String) {
        synchronized(this) {
            if (recentLines.size >= 1000) recentLines.removeFirst()
            recentLines.addLast(System.currentTimeMillis().toString() + " " + message)
        }
    }
}
'''
write(ROOT / "src/main/java/com/offex7/streamhub/LogExporter.kt", log_exporter)

# ---------- Scroll-top behavior ----------
scroll = r'''package com.offex7.streamhub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScrollTopButtonV37(
    listState: androidx.compose.foundation.lazy.LazyListState,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var scrollSession by remember { mutableLongStateOf(0L) }
    var lastScrollActivityAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (index, offset, scrolling) ->
            val awayFromTop = index > 0 || offset > 0
            if (!awayFromTop) {
                visible = false
                scrollSession = 0L
                lastScrollActivityAt = 0L
                wasScrolling = scrolling
                return@collect
            }

            if (scrolling) {
                lastScrollActivityAt = System.currentTimeMillis()
                if (!wasScrolling) scrollSession += 1L
            }
            wasScrolling = scrolling
        }
    }

    LaunchedEffect(scrollSession) {
        if (scrollSession <= 0L) return@LaunchedEffect
        val session = scrollSession
        delay(2000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (session == scrollSession && awayFromTop) {
            visible = true
        }
    }

    LaunchedEffect(lastScrollActivityAt) {
        if (lastScrollActivityAt <= 0L) return@LaunchedEffect
        val activityAt = lastScrollActivityAt
        delay(5000L)
        val awayFromTop =
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        if (activityAt == lastScrollActivityAt && awayFromTop) {
            visible = !listState.canScrollForward
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "scroll-top-v39")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scroll-top-v39-pulse"
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { width -> width },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(
            targetOffsetX = { width -> width },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(220))
    ) {
        IconButton(
            onClick = {
                InteractionFeedback.vibrate(context, hapticsEnabled, 52L, 165)
                if (soundEnabled) InteractionFeedback.beep(context, true)
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Вверх", tint = Color.White, modifier = Modifier.size(31.dp))
        }
    }
}
'''
write(ROOT / "src/main/java/com/offex7/streamhub/ScrollTopButtonV37.kt", scroll)

# ---------- Widgets ----------
widget_package = "com.offex7.streamhub"

widget_provider = r'''package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

private fun widgetPendingIntent(
    context: Context,
    appWidgetId: Int,
    section: Section
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_WIDGET_SECTION, section.name)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    return PendingIntent.getActivity(context, appWidgetId, intent, flags)
}

class RadioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_radio)
            views.setImageViewResource(R.id.widget_icon, R.drawable.start_radio_v2)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                widgetPendingIntent(context, id, Section.RADIO)
            )
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

class TvWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_tv)
            views.setImageViewResource(R.id.widget_icon, R.drawable.start_tv_final)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                widgetPendingIntent(context, id, Section.TV)
            )
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
'''
write(ROOT / "src/main/java/com/offex7/streamhub/RadioWidgetProvider.kt", widget_provider)
write(ROOT / "src/main/java/com/offex7/streamhub/TvWidgetProvider.kt", 'package com.offex7.streamhub

// Marker file kept intentionally tiny; the provider implementation is generated with RadioWidgetProvider.kt.
')

# fix split provider files: keep each class in its own file for clean Android component lookup.
radio_provider = r'''package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

private fun radioWidgetPendingIntent(
    context: Context,
    appWidgetId: Int
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_WIDGET_SECTION, Section.RADIO.name)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    return PendingIntent.getActivity(context, appWidgetId, intent, flags)
}

class RadioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_radio)
            views.setImageViewResource(R.id.widget_icon, R.drawable.start_radio_v2)
            views.setOnClickPendingIntent(R.id.widget_root, radioWidgetPendingIntent(context, id))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
'''
tv_provider = r'''package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

private fun tvWidgetPendingIntent(
    context: Context,
    appWidgetId: Int
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_WIDGET_SECTION, Section.TV.name)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    return PendingIntent.getActivity(context, appWidgetId, intent, flags)
}

class TvWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_tv)
            views.setImageViewResource(R.id.widget_icon, R.drawable.start_tv_final)
            views.setOnClickPendingIntent(R.id.widget_root, tvWidgetPendingIntent(context, id))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
'''
write(ROOT / "src/main/java/com/offex7/streamhub/RadioWidgetProvider.kt", radio_provider)
write(ROOT / "src/main/java/com/offex7/streamhub/TvWidgetProvider.kt", tv_provider)

widget_layout = '''<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#121212"
    android:padding="8dp">
    <ImageView
        android:id="@+id/widget_icon"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:contentDescription="@null"
        android:scaleType="fitCenter" />
</FrameLayout>
'''
write(ROOT / "src/main/res/layout/widget_radio.xml", widget_layout)
write(ROOT / "src/main/res/layout/widget_tv.xml", widget_layout)

widget_info = '''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="72dp"
    android:minHeight="72dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_radio"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
'''
write(ROOT / "src/main/res/xml/widget_radio_info.xml", widget_info)

widget_info_tv = widget_info.replace('@layout/widget_radio', '@layout/widget_tv')
write(ROOT / "src/main/res/xml/widget_tv_info.xml", widget_info_tv)

manifest = read(ROOT / "src/main/AndroidManifest.xml")
manifest = replace_once(
    manifest,
    'android:exported="true"\n            android:supportsPictureInPicture="true">',
    'android:exported="true"\n            android:launchMode="singleTop"\n            android:supportsPictureInPicture="true">',
    "singleTop"
)
receiver_block = '''        <receiver
            android:name=".RadioWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_radio_info" />
        </receiver>
        <receiver
            android:name=".TvWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_tv_info" />
        </receiver>
'''
manifest = replace_once(
    manifest,
    """        <receiver
        android:name=".PipActionReceiver"
        android:exported="false" />""",
    """        <receiver
        android:name=".PipActionReceiver"
        android:exported="false" />

""" + receiver_block.rstrip(),
    "widget manifest receivers"
)
write(ROOT / "src/main/AndroidManifest.xml", manifest)

# ---------- Remove all literal v3.8 User-Agent remnants ----------
for path in ROOT.rglob("*"):
    if path.is_file() and path.suffix in {".kt", ".kts", ".xml", ".yml", ".yaml"}:
        text = path.read_text(encoding="utf-8")
        if "Radio.TV/3.8" in text:
            path.write_text(text.replace("Radio.TV/3.8", "Radio.TV/3.9"), encoding="utf-8")

print("v3.9 source transformation: OK")

# Remove this temporary patcher from the final source tree, then commit the real source changes.
script_path = Path(__file__).resolve()
script_path.unlink()

import subprocess
def run(*args):
    subprocess.run(args, check=True)

run("git", "config", "user.name", "Radio.TV Builder")
run("git", "config", "user.email", "actions@github.com")
run("git", "add", "-A")
run("git", "commit", "-m", "feat(v3.9): apply Radio.TV v3.9 changes")
run("git", "push", "origin", "HEAD:main")
print("v3.9 source committed to main")
