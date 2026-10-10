#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path("app")
MAIN = ROOT / "src/main/java/com/offex7/streamhub/MainActivity.kt"
PLAYER = ROOT / "src/main/java/com/offex7/streamhub/PlayerController.kt"
TV = ROOT / "src/main/java/com/offex7/streamhub/TvV8Screen.kt"
STORE = ROOT / "src/main/java/com/offex7/streamhub/Storage.kt"
GRADLE = ROOT / "build.gradle.kts"
MANIFEST = ROOT / "src/main/AndroidManifest.xml"
STRINGS = ROOT / "src/main/res/values/strings.xml"
SHORTCUTS = ROOT / "src/main/res/xml/shortcuts.xml"

def once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one exact match, found {count}")
    return text.replace(old, new, 1)

def remove_once(text, old, label):
    return once(text, old, "", label)

# Versioning: preserve the existing package and signing setup.
gradle = GRADLE.read_text(encoding="utf-8")
gradle = once(gradle, "versionCode = 13", "versionCode = 14", "versionCode")
gradle = once(gradle, 'versionName = "4.0"', 'versionName = "4.1"', "versionName")
GRADLE.write_text(gradle, encoding="utf-8")

# Storage: session timestamps and the 92-hour battery-warning cooldown live in DataStore.
store = STORE.read_text(encoding="utf-8")
store = once(
    store,
    'private val radioStationUsageKey = stringPreferencesKey("usage_radio_stations")',
    '''private val radioStationUsageKey = stringPreferencesKey("usage_radio_stations")
    private val energyWarningLastShownKey = longPreferencesKey("energy_warning_last_shown_at_v1")
    private val tvUsageSessionChannelKey = stringPreferencesKey("usage_session_tv_channel_v1")
    private val tvUsageSessionStartedKey = longPreferencesKey("usage_session_tv_started_at_v1")
    private val tvUsageSessionBaseTotalKey = longPreferencesKey("usage_session_tv_base_total_v1")
    private val tvUsageSessionBaseChannelKey = longPreferencesKey("usage_session_tv_base_channel_v1")
    private val radioUsageSessionChannelKey = stringPreferencesKey("usage_session_radio_channel_v1")
    private val radioUsageSessionStartedKey = longPreferencesKey("usage_session_radio_started_at_v1")
    private val radioUsageSessionBaseTotalKey = longPreferencesKey("usage_session_radio_base_total_v1")
    private val radioUsageSessionBaseChannelKey = longPreferencesKey("usage_session_radio_base_channel_v1")''',
    "usage and warning DataStore keys"
)
store = once(
    store,
    '''    suspend fun resetUsage(section: Section) {
        context.dataStore.edit {
            it[if (section == Section.TV) tvUsageKey else radioUsageKey] = 0L
            it[if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey] = "{}"
        }
    }''',
    '''    suspend fun resetUsage(section: Section) {
        val now = System.currentTimeMillis()
        val session = usageSession(section)
        context.dataStore.edit { prefs ->
            prefs[if (section == Section.TV) tvUsageKey else radioUsageKey] = 0L
            prefs[if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey] = "{}"
            val channelKey = if (section == Section.TV) tvUsageSessionChannelKey else radioUsageSessionChannelKey
            val startedKey = if (section == Section.TV) tvUsageSessionStartedKey else radioUsageSessionStartedKey
            val baseTotalKey = if (section == Section.TV) tvUsageSessionBaseTotalKey else radioUsageSessionBaseTotalKey
            val baseChannelKey = if (section == Section.TV) tvUsageSessionBaseChannelKey else radioUsageSessionBaseChannelKey
            if (session != null) {
                prefs[channelKey] = session.channelId
                prefs[startedKey] = now
                prefs[baseTotalKey] = 0L
                prefs[baseChannelKey] = 0L
            } else {
                prefs.remove(channelKey)
                prefs.remove(startedKey)
                prefs.remove(baseTotalKey)
                prefs.remove(baseChannelKey)
            }
        }
    }

    suspend fun usageTotalSeconds(section: Section): Long =
        context.dataStore.data.first()[if (section == Section.TV) tvUsageKey else radioUsageKey] ?: 0L

    suspend fun channelUsageSnapshot(section: Section): Map<String, Long> {
        val prefs = context.dataStore.data.first()
        return decodeUsageMap(prefs[if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey])
    }

    suspend fun usageSession(section: Section): UsageSession? {
        val prefs = context.dataStore.data.first()
        val channelKey = if (section == Section.TV) tvUsageSessionChannelKey else radioUsageSessionChannelKey
        val startedKey = if (section == Section.TV) tvUsageSessionStartedKey else radioUsageSessionStartedKey
        val baseTotalKey = if (section == Section.TV) tvUsageSessionBaseTotalKey else radioUsageSessionBaseTotalKey
        val baseChannelKey = if (section == Section.TV) tvUsageSessionBaseChannelKey else radioUsageSessionBaseChannelKey
        val channelId = prefs[channelKey]?.takeIf { it.isNotBlank() } ?: return null
        val startedAt = prefs[startedKey]?.takeIf { it > 0L } ?: return null
        return UsageSession(
            channelId = channelId,
            startedAtMs = startedAt,
            baseTotalSeconds = prefs[baseTotalKey] ?: 0L,
            baseChannelSeconds = prefs[baseChannelKey] ?: 0L
        )
    }

    suspend fun saveUsageSession(section: Section, session: UsageSession) {
        context.dataStore.edit { prefs ->
            prefs[if (section == Section.TV) tvUsageSessionChannelKey else radioUsageSessionChannelKey] = session.channelId
            prefs[if (section == Section.TV) tvUsageSessionStartedKey else radioUsageSessionStartedKey] = session.startedAtMs
            prefs[if (section == Section.TV) tvUsageSessionBaseTotalKey else radioUsageSessionBaseTotalKey] = session.baseTotalSeconds
            prefs[if (section == Section.TV) tvUsageSessionBaseChannelKey else radioUsageSessionBaseChannelKey] = session.baseChannelSeconds
        }
    }

    suspend fun clearUsageSession(section: Section) {
        context.dataStore.edit { prefs ->
            prefs.remove(if (section == Section.TV) tvUsageSessionChannelKey else radioUsageSessionChannelKey)
            prefs.remove(if (section == Section.TV) tvUsageSessionStartedKey else radioUsageSessionStartedKey)
            prefs.remove(if (section == Section.TV) tvUsageSessionBaseTotalKey else radioUsageSessionBaseTotalKey)
            prefs.remove(if (section == Section.TV) tvUsageSessionBaseChannelKey else radioUsageSessionBaseChannelKey)
        }
    }

    suspend fun canShowEnergyWarning(now: Long = System.currentTimeMillis()): Boolean {
        val last = context.dataStore.data.first()[energyWarningLastShownKey] ?: 0L
        return last <= 0L || (now >= last && now - last >= 92L * 60L * 60L * 1000L)
    }

    suspend fun markEnergyWarningShown(now: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[energyWarningLastShownKey] = now }
    }''',
    "stats sessions, reset handling and energy warning cooldown"
)
# Put the immutable session model outside SettingsStore.
store = once(
    store,
    'private val Context.dataStore by preferencesDataStore("streamhub_settings")',
    '''private val Context.dataStore by preferencesDataStore("streamhub_settings")

internal data class UsageSession(
    val channelId: String,
    val startedAtMs: Long,
    val baseTotalSeconds: Long,
    val baseChannelSeconds: Long
)''',
    "usage session model"
)
STORE.write_text(store, encoding="utf-8")

# Main app: user-agent, actual cooldown, local station logo and proper chooser flags.
main = MAIN.read_text(encoding="utf-8")
main = once(main, '.header("User-Agent", "Radio.TV/4.0")', '.header("User-Agent", "Radio.TV/4.1")', "User-Agent")
battery_old = '''        val powerManager = appContext.getSystemService(android.os.PowerManager::class.java)
        val activityManager = appContext.getSystemService(android.app.ActivityManager::class.java)
        val ignoringBatteryOptimizations = runCatching {
            powerManager?.isIgnoringBatteryOptimizations(appContext.packageName)
        }.getOrNull()
        val backgroundRestricted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) activityManager?.isBackgroundRestricted else null
        }.getOrNull()
        // A confirmed battery-optimization exemption always suppresses the warning.
        // Unknown status is not treated as restricted unless ActivityManager confirms it.
        val shouldWarnAboutBackgroundLimits = when (ignoringBatteryOptimizations) {
            true -> false
            false -> true
            null -> backgroundRestricted == true
        }
        if (shouldWarnAboutBackgroundLimits) {
            notify("Фоновая работа Radio.TV может быть ограничена ОС. Добавьте приложение в исключения энергосбережения.", 3000L)
        }'''
battery_new = '''        val powerManager = appContext.getSystemService(android.os.PowerManager::class.java)
        val activityManager = appContext.getSystemService(android.app.ActivityManager::class.java)
        val ignoringBatteryOptimizations = runCatching {
            powerManager?.isIgnoringBatteryOptimizations(appContext.packageName)
        }.getOrNull()
        val backgroundRestricted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) activityManager?.isBackgroundRestricted else null
        }.getOrNull()
        // Exemption suppresses the warning. Unknown status is not treated as a restriction.
        val shouldWarnAboutBackgroundLimits = when (ignoringBatteryOptimizations) {
            true -> false
            false -> true
            null -> backgroundRestricted == true
        }
        if (shouldWarnAboutBackgroundLimits && store.canShowEnergyWarning()) {
            store.markEnergyWarningShown()
            notify("Фоновая работа Radio.TV может быть ограничена ОС. Добавьте приложение в исключения энергосбережения.", 3000L)
        }'''
main = once(main, battery_old, battery_new, "92-hour energy notice guard")

old_logo = '''    val localRadioImage = remember(item.name, isRadio) {
        if (!isRadio) null
        else if (item.name.equals("РАДИУС FM", ignoreCase = true)) {
            BitmapFactory.decodeResource(context.resources, R.drawable.radius_fm_logo)?.asImageBitmap()
        } else {
            RadioLogoAssets.image(item.name)
                ?: RadioLogoAssetsV38.image(context, item.name)
        }
    }'''
new_logo = '''    val localRadioImage = remember(item.name, isRadio) {
        if (!isRadio) null
        else when {
            item.name.equals("РАДИУС FM", ignoreCase = true) ->
                BitmapFactory.decodeResource(context.resources, R.drawable.radius_fm_logo)?.asImageBitmap()
            item.name.equals("ЮНИСТАР", ignoreCase = true) ->
                BitmapFactory.decodeResource(context.resources, R.drawable.unistar_logo)?.asImageBitmap()
            else -> RadioLogoAssets.image(item.name)
                ?: RadioLogoAssetsV38.image(context, item.name)
        }
    }'''
main = once(main, old_logo, new_logo, "local Unistar logo")

share_old = '''private fun shareText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }.onFailure {
        Toast.makeText(context, "Не удалось открыть меню «Поделиться»", Toast.LENGTH_LONG).show()
    }
}'''
share_new = '''private fun shareText(context: Context, text: String, chooserTitle: String): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(sendIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching {
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}'''
main = once(main, share_old, share_new, "Share Intent chooser task flag")
main = once(
    main,
    '''                        notify("Скопировано", 3000L)
                        shareText(settingsContext, logText, "Поделиться логами Radio.TV")''',
    '''                        val shared = shareText(settingsContext, logText, "Поделиться логами Radio.TV")
                        notify("Скопировано", if (shared) 3000L else 2000L)''',
    "copy remains successful if the system chooser cannot be opened"
)

# Centralize playback usage tracking so it continues while Settings is open and the stream remains active.
radio_ticker = '''    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.RADIO,
            channelIdProvider = { RADIO_STATIONS.getOrNull(player.currentIndex.value)?.name },
            activeProvider = { player.isPlaying.value && player.currentIndex.value >= 0 }
        ).run()
    }

'''
tv_ticker = '''    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.TV,
            channelIdProvider = { channels.getOrNull(selectedIndex)?.name },
            activeProvider = { fullscreen && selectedIndex in channels.indices && player.isPlaying.value }
        ).run()
    }

'''
main = remove_once(main, radio_ticker, "screen-local radio usage ticker")
# The TV ticker appears in TvV8Screen.kt; remove it below.
tracker_marker = '    LaunchedEffect(activity.requestedSection, pinUnlocked) {'
tracker_code = '''    LaunchedEffect(section, settings) {
        val trackedSections = if (settings) {
            listOf(Section.TV, Section.RADIO)
        } else {
            listOfNotNull(section)
        }
        if (trackedSections.isEmpty()) return@LaunchedEffect
        coroutineScope {
            trackedSections.forEach { target ->
                launch {
                    when (target) {
                        Section.TV -> UsageTicker(
                            store = store,
                            section = Section.TV,
                            channelIdProvider = { tv.activeChannelId.value },
                            activeProvider = { tv.isPlaying.value }
                        ).run()
                        Section.RADIO -> UsageTicker(
                            store = store,
                            section = Section.RADIO,
                            channelIdProvider = { RADIO_STATIONS.getOrNull(radio.currentIndex.value)?.name },
                            activeProvider = { radio.isPlaying.value && radio.currentIndex.value >= 0 }
                        ).run()
                    }
                }
            }
        }
    }

'''
main = once(main, tracker_marker, tracker_code + tracker_marker, "central usage tracking while Settings is visible")

# Settings: reset confirmation state, move the update banner to the bottom, and update the reset label.
main = once(main, '    var logDialogText by remember { mutableStateOf<String?>(null) }', '''    var logDialogText by remember { mutableStateOf<String?>(null) }
    var resetAllConfirm by remember { mutableStateOf(false) }''', "factory reset confirmation state")
update_item = '''        item {
            Card(
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = if (updateAvailable) Color(0xFF2E7D32) else Red),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "ОБНОВИТЬ ПРИЛОЖЕНИЕ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
'''
main = remove_once(main, update_item, "remove update banner from upper settings")
disclaimer_item = '''        item {
            OutlinedButton(onClick = onDisclaimer, modifier = Modifier.fillMaxWidth()) {
                Text("ОТКАЗ ОТ ОТВЕТСТВЕННОСТИ")
            }
        }'''
main = once(main, disclaimer_item, disclaimer_item + "\n" + update_item.rstrip(), "place update banner at bottom")
main = once(main, 'onClick = onResetAll,', 'onClick = { resetAllConfirm = true },', "factory reset confirmation click")
main = once(main, 'Text("СБРОСИТЬ НАСТРОЙКИ ДО ЗАВОДСКИХ")', 'Text("ВОССТАНОВИТЬ ПО УМОЛЧАНИЮ")', "factory reset label")
dialog_marker = '    logDialogText?.let { logText ->'
reset_dialog = '''    if (resetAllConfirm) {
        AlertDialog(
            onDismissRequest = { resetAllConfirm = false },
            text = { Text("Восстановить настройки по умолчанию?") },
            confirmButton = {
                TextButton(onClick = {
                    resetAllConfirm = false
                    onResetAll()
                }) { Text("Да", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { resetAllConfirm = false }) { Text("Нет") }
            }
        )
    }

'''
main = once(main, dialog_marker, reset_dialog + dialog_marker, "factory reset confirmation dialog")

# Landscape-only equalizer fit; portrait layout retains its current sizing and spacing.
eq_old = '''            val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val contentModifier = if (landscape) {
                Modifier
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Bass", color = Gray, fontSize = 11.sp)
                Slider(value = bass, onValueChange = { bass = it; preset = "Flat" }, valueRange = -1500f..1500f)
                Text("Mid", color = Gray, fontSize = 11.sp)
                Slider(value = mid, onValueChange = { mid = it; preset = "Flat" }, valueRange = -1500f..1500f)
                Text("Treble", color = Gray, fontSize = 11.sp)
                Slider(value = treble, onValueChange = { treble = it; preset = "Flat" }, valueRange = -1500f..1500f)'''
eq_new = '''            val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp - 150).coerceAtLeast(140).dp
            val contentModifier = if (landscape) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxHeight)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(if (landscape) 3.dp else 6.dp)
            ) {
                Text("Bass", color = Gray, fontSize = if (landscape) 10.sp else 11.sp)
                Slider(value = bass, onValueChange = { bass = it; preset = "Flat" }, valueRange = -1500f..1500f, modifier = Modifier.fillMaxWidth())
                Text("Mid", color = Gray, fontSize = if (landscape) 10.sp else 11.sp)
                Slider(value = mid, onValueChange = { mid = it; preset = "Flat" }, valueRange = -1500f..1500f, modifier = Modifier.fillMaxWidth())
                Text("Treble", color = Gray, fontSize = if (landscape) 10.sp else 11.sp)
                Slider(value = treble, onValueChange = { treble = it; preset = "Flat" }, valueRange = -1500f..1500f, modifier = Modifier.fillMaxWidth())'''
main = once(main, eq_old, eq_new, "landscape equalizer scrolling and sizing")
# Settings reads the live Store flows on each open and projects the active timestamp only while this screen exists.
stats_marker = '    val hiddenChannels by store.hiddenChannelsFlow().collectAsState(emptySet())'
stats_add = '''    var statsNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tvSession by remember { mutableStateOf<UsageSession?>(null) }
    var radioSession by remember { mutableStateOf<UsageSession?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            statsNow = System.currentTimeMillis()
            tvSession = runCatching { store.usageSession(Section.TV) }.getOrNull()
            radioSession = runCatching { store.usageSession(Section.RADIO) }.getOrNull()
            delay(10_000L)
        }
    }
    val tvProjectedUsage = tvUsage + ((statsNow - (tvSession?.startedAtMs ?: statsNow)).coerceAtLeast(0L) / 1000L)
    val radioProjectedUsage = radioUsage + ((statsNow - (radioSession?.startedAtMs ?: statsNow)).coerceAtLeast(0L) / 1000L)
    val tvChannelsLive = remember(tvChannels, tvSession, statsNow) {
        tvChannels.toMutableMap().apply {
            tvSession?.let { session ->
                val elapsed = ((statsNow - session.startedAtMs).coerceAtLeast(0L) / 1000L)
                if (elapsed > 0L) this[session.channelId] = (this[session.channelId] ?: 0L) + elapsed
            }
        }
    }
    val radioStationsLive = remember(radioStations, radioSession, statsNow) {
        radioStations.toMutableMap().apply {
            radioSession?.let { session ->
                val elapsed = ((statsNow - session.startedAtMs).coerceAtLeast(0L) / 1000L)
                if (elapsed > 0L) this[session.channelId] = (this[session.channelId] ?: 0L) + elapsed
            }
        }
    }'''
main = once(main, stats_marker, stats_marker + "\n" + stats_add, "live statistics projection state")
main = once(main, 'UsageLine("Общее время просмотра Телевизора", tvUsage)', 'UsageLine("Общее время просмотра Телевизора", tvProjectedUsage)', "live TV total")
main = once(main, 'TopStats(tvChannels)', 'TopStats(tvChannelsLive)', "live TV Top 3")
main = once(main, 'UsageLine("Общее время прослушивания Радио", radioUsage)', 'UsageLine("Общее время прослушивания Радио", radioProjectedUsage)', "live radio total")
main = once(main, 'TopStats(radioStations)', 'TopStats(radioStationsLive)', "live radio Top 3")

MAIN.write_text(main, encoding="utf-8")

# PIN prompt: no OS subtitle/title text, keypad shown only after "Ввести PIN" or when biometric is unavailable.
pin = MAIN.read_text(encoding="utf-8")
pin = remove_once(
    pin,
    '''    DisposableEffect(activity) {
        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = previousOrientation }
    }
''',
    "remove portrait lock from PIN screen"
)
pin = once(pin, '    var biometricAvailable by rememberSaveable { mutableStateOf(false) }', '''    var biometricAvailable by rememberSaveable { mutableStateOf(false) }
    var showPinEntry by rememberSaveable { mutableStateOf(false) }''', "PIN entry visibility state")
pin = once(
    pin,
    '''                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    store.registerSuccess()
                    failedAttempts = 0
                    onUnlocked()
                }''',
    '''                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    store.registerSuccess()
                    failedAttempts = 0
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    showPinEntry = true
                }''',
    "biometric prompt negative/back handling"
)
pin = once(
    pin,
    '''        if (biometricAvailable) promptBiometric()
    }''',
    '''        if (biometricAvailable) promptBiometric() else showPinEntry = true
    }''',
    "show keypad when biometrics unavailable"
)
pin = once(
    pin,
    '''            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Radio.TV")
                .setSubtitle("ОТСКАНИРУЙТЕ ОТПЕЧАТОК ПАЛЬЦА")
                .setNegativeButtonText("Ввести PIN")
                .build()''',
    '''            BiometricPrompt.PromptInfo.Builder()
                .setTitle("\\u00a0")
                .setSubtitle("\\u00a0")
                .setNegativeButtonText("Ввести PIN")
                .build()''',
    "remove biometric prompt title and subtitle"
)
pin_start = pin.index('    Surface(Modifier.fillMaxSize(), color = Color.Black) {', pin.index('private fun PinGate('))
pin_end_marker = '        Box(Modifier.fillMaxSize().background(redFlash))\n    }\n}'
pin_end = pin.index(pin_end_marker, pin_start) + len(pin_end_marker)
pin_ui = '''    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val pinButtonSize = if (landscape) 52.dp else 74.dp
    val pinContent = Modifier
        .fillMaxSize()
        .padding(horizontal = if (landscape) 12.dp else 20.dp, vertical = if (landscape) 4.dp else 18.dp)
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            Column(
                if (landscape) pinContent.verticalScroll(rememberScrollState()) else pinContent,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (landscape) Arrangement.Top else Arrangement.Center
            ) {
                Icon(Icons.Default.Fingerprint, "Биометрическая проверка", tint = Red, modifier = Modifier.size(if (landscape) 34.dp else 46.dp))
                Spacer(Modifier.height(if (landscape) 4.dp else 10.dp))
                if (!showPinEntry) {
                    TextButton(onClick = { showPinEntry = true }) {
                        Text("Ввести PIN", color = Red, fontSize = 14.sp)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(if (landscape) 12.dp else 15.dp)
                                    .background(if (i < pin.length) Red else PanelAlt, CircleShape)
                                    .border(1.dp, if (error) Red else Gray, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(if (landscape) 6.dp else 12.dp))
                    if (lockoutRemaining > 0L) {
                        val seconds = (lockoutRemaining + 999L) / 1000L
                        Text(
                            "Ввод заблокирован на 5 минут • %02d:%02d".format(seconds / 60L, seconds % 60L),
                            color = Red,
                            fontSize = if (landscape) 10.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else if (failedAttempts > 0) {
                        Text("Неверных попыток: $failedAttempts/5", color = Red, fontSize = if (landscape) 10.sp else 11.sp)
                    }
                    Spacer(Modifier.height(if (landscape) 4.dp else 10.dp))
                    val rows = listOf(
                        listOf('1', '2', '3'),
                        listOf('4', '5', '6'),
                        listOf('7', '8', '9')
                    )
                    rows.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 10.dp, Alignment.CenterHorizontally)
                        ) {
                            row.forEach { digit ->
                                Button(
                                    onClick = {
                                        InteractionFeedback.click(context, hapticsEnabled, soundEnabled, allowSound = false)
                                        appendDigit(digit)
                                    },
                                    enabled = lockoutRemaining <= 0L && pin.length < 4,
                                    modifier = Modifier.size(pinButtonSize),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Panel,
                                        contentColor = Color.White,
                                        disabledContainerColor = PanelAlt
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(digit.toString(), fontSize = if (landscape) 19.sp else 24.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(if (landscape) 4.dp else 10.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 10.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = {
                                InteractionFeedback.click(context, hapticsEnabled, soundEnabled, allowSound = false)
                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                error = false
                            },
                            enabled = lockoutRemaining <= 0L && pin.isNotEmpty(),
                            modifier = Modifier.size(pinButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Panel, disabledContainerColor = PanelAlt),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Удалить последнюю цифру", tint = Color.White, modifier = Modifier.size(if (landscape) 20.dp else 25.dp))
                        }
                        Button(
                            onClick = {
                                InteractionFeedback.click(context, hapticsEnabled, soundEnabled, allowSound = false)
                                appendDigit('0')
                            },
                            enabled = lockoutRemaining <= 0L && pin.length < 4,
                            modifier = Modifier.size(pinButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Panel,
                                contentColor = Color.White,
                                disabledContainerColor = PanelAlt
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("0", fontSize = if (landscape) 19.sp else 24.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { promptBiometric() },
                            enabled = biometricAvailable,
                            modifier = Modifier.size(pinButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Panel, disabledContainerColor = PanelAlt),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, "Переключиться на биометрию", tint = Red, modifier = Modifier.size(if (landscape) 23.dp else 28.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        Toast.makeText(context, "Подсказка: " + store.hint(), Toast.LENGTH_LONG).show()
                    }) {
                        Text("Забыли PIN?", color = Red, fontSize = if (landscape) 10.sp else 12.sp)
                    }
                }
            }
            Box(Modifier.matchParentSize().background(redFlash))
        }
    }
}'''
pin = pin[:pin_start] + pin_ui + pin[pin_end:]
MAIN.write_text(pin, encoding="utf-8")

# Persisted playback sessions provide current DataStore totals plus elapsed time from the saved timestamp.
main = MAIN.read_text(encoding="utf-8")
main = once(main, "import kotlinx.coroutines.Dispatchers", "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.NonCancellable", "NonCancellable import")
ticker_start = main.index("internal class UsageTicker(")
ticker_new = """internal class UsageTicker(
    private val store: SettingsStore,
    private val section: Section,
    private val channelIdProvider: () -> String?,
    private val activeProvider: () -> Boolean
) {
    private suspend fun flushSession(local: UsageSession, now: Long, keepSession: Boolean): UsageSession? {
        val persisted = store.usageSession(section)
        val session = if (
            persisted != null &&
            persisted.channelId == local.channelId &&
            persisted.startedAtMs > local.startedAtMs
        ) persisted else local
        // Display = previous total + (now - saved playback-start timestamp).
        val elapsedSeconds = ((now - session.startedAtMs).coerceAtLeast(0L) / 1000L)
        if (elapsedSeconds > 0L) store.addPlaybackUsage(section, mapOf(session.channelId to elapsedSeconds))
        if (!keepSession) {
            store.clearUsageSession(section)
            return null
        }
        if (elapsedSeconds == 0L) {
            store.saveUsageSession(session)
            return session
        }
        val rebased = UsageSession(
            channelId = session.channelId,
            startedAtMs = now,
            baseTotalSeconds = store.usageTotalSeconds(section),
            baseChannelSeconds = store.channelUsageSnapshot(section)[session.channelId] ?: 0L
        )
        store.saveUsageSession(rebased)
        return rebased
    }

    suspend fun run() {
        var session: UsageSession? = null
        var inactiveSessionChecked = false
        var lastActiveTickMs = 0L
        try {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val activeId = if (runCatching { activeProvider() }.getOrDefault(false)) {
                    runCatching { channelIdProvider()?.trim()?.takeIf { it.isNotBlank() } }.getOrNull()
                } else null
                val current = session
                if (activeId == null) {
                    if (current != null) {
                        val stoppedAt = lastActiveTickMs.takeIf { it >= current.startedAtMs } ?: current.startedAtMs
                        session = flushSession(current, stoppedAt, keepSession = false)
                    } else if (!inactiveSessionChecked) {
                        if (store.usageSession(section) != null) store.clearUsageSession(section)
                        inactiveSessionChecked = true
                    }
                    lastActiveTickMs = 0L
                    continue
                }
                inactiveSessionChecked = false
                if (current == null || current.channelId != activeId) {
                    if (current != null) {
                        val switchedAt = lastActiveTickMs.takeIf { it >= current.startedAtMs } ?: current.startedAtMs
                        flushSession(current, switchedAt, keepSession = false)
                    }
                    val saved = store.usageSession(section)
                    val reusable = saved?.takeIf {
                        it.channelId == activeId && now >= it.startedAtMs && now - it.startedAtMs <= 15_000L
                    }
                    val started = reusable ?: UsageSession(
                        channelId = activeId,
                        startedAtMs = now,
                        baseTotalSeconds = store.usageTotalSeconds(section),
                        baseChannelSeconds = store.channelUsageSnapshot(section)[activeId] ?: 0L
                    )
                    store.saveUsageSession(started)
                    session = started
                    lastActiveTickMs = now
                } else {
                    lastActiveTickMs = now
                    if (now - current.startedAtMs >= 10_000L) {
                        session = flushSession(current, now, keepSession = true)
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                val current = session
                val currentId = runCatching {
                    if (activeProvider()) channelIdProvider()?.trim()?.takeIf { it.isNotBlank() } else null
                }.getOrNull()
                if (current != null) {
                    val stoppedAt = lastActiveTickMs.takeIf { it >= current.startedAtMs } ?: current.startedAtMs
                    flushSession(current, stoppedAt, keepSession = currentId == current.channelId)
                } else if (currentId == null && store.usageSession(section) != null) {
                    store.clearUsageSession(section)
                }
            }
        }
    }
}
"""
main = main[:ticker_start] + ticker_new
MAIN.write_text(main, encoding="utf-8")

# The source uses an OS-provided share chooser, not a URI; keep the copy and close controls intact.
# Version/build constants elsewhere derive from BuildConfig.VERSION_NAME, so the Gradle bump updates their UI.

# Static App Shortcuts are routed through the same EXTRA already used by the two widgets.
manifest = MANIFEST.read_text(encoding="utf-8")
manifest = once(
    manifest,
    '''                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>''',
    '''                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
        </activity>''',
    "static app shortcuts metadata"
)
MANIFEST.write_text(manifest, encoding="utf-8")
strings = STRINGS.read_text(encoding="utf-8")
strings = once(
    strings,
    '</resources>',
    '''    <string name="shortcut_radio_short">Радио</string>
    <string name="shortcut_radio_long">Открыть раздел Радио</string>
    <string name="shortcut_tv_short">Телевизор</string>
    <string name="shortcut_tv_long">Открыть раздел Телевизор</string>
</resources>''',
    "shortcut labels"
)
STRINGS.write_text(strings, encoding="utf-8")
SHORTCUTS.write_text('''<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="open_radio"
        android:enabled="true"
        android:icon="@drawable/start_radio_v2"
        android:shortcutShortLabel="@string/shortcut_radio_short"
        android:shortcutLongLabel="@string/shortcut_radio_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.offex7.streamhub"
            android:targetClass="com.offex7.streamhub.MainActivity">
            <extra android:name="widget_section" android:value="RADIO" />
        </intent>
    </shortcut>
    <shortcut
        android:shortcutId="open_tv"
        android:enabled="true"
        android:icon="@drawable/start_tv_final"
        android:shortcutShortLabel="@string/shortcut_tv_short"
        android:shortcutLongLabel="@string/shortcut_tv_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.offex7.streamhub"
            android:targetClass="com.offex7.streamhub.MainActivity">
            <extra android:name="widget_section" android:value="TV" />
        </intent>
    </shortcut>
</shortcuts>
''', encoding="utf-8")

# PlayerController: recovery thresholds and silent fast-switch mode.
player = PLAYER.read_text(encoding="utf-8")
player = once(player, 'const val BUFFER_TIMEOUT_MS = 15_000L', 'const val BUFFER_TIMEOUT_MS = 10_000L', "TV max buffer duration")
player = once(player, 'const val WEAK_NETWORK_NOTICE_MS = 4_000L', 'const val WEAK_NETWORK_NOTICE_MS = 3_000L', "TV buffer notice delay")
player = once(
    player,
    '''    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()''',
    '''    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeChannelId = MutableStateFlow<String?>(null)
    val activeChannelId: StateFlow<String?> = _activeChannelId.asStateFlow()

    fun setActiveChannelId(channelId: String?) {
        _activeChannelId.value = channelId?.trim()?.takeIf { it.isNotBlank() }
    }''',
    "active TV channel id for statistics"
)
player = once(
    player,
    'suspend fun playWithFallback(urls: List<String>): Int {',
    'suspend fun playWithFallback(urls: List<String>, attemptTimeoutMs: Long = BUFFER_TIMEOUT_MS, suppressRecovery: Boolean = false): Int {',
    "TV fallback configurable timeout"
)
player = once(
    player,
    '''        internalRetryEnabled = false
        userPaused = false
        userStopped = false
        resumeAfterInterruption = false
        reconnectAttempts = 0
        cancelFadeOut(restore = false)''',
    '''        internalRetryEnabled = false
        suppressBufferRecovery = suppressRecovery
        userPaused = false
        userStopped = false
        resumeAfterInterruption = false
        reconnectAttempts = 0
        cancelFadeOut(restore = false)''',
    "set silent fallback recovery mode"
)
player = once(player, '    private var internalRetryEnabled = true', '    private var internalRetryEnabled = true\n    private var suppressBufferRecovery = false', "silent recovery flag")
player = once(player, 'val until = System.currentTimeMillis() + BUFFER_TIMEOUT_MS', 'val until = System.currentTimeMillis() + attemptTimeoutMs.coerceAtLeast(300L)', "per-channel attempt timeout")
player = once(
    player,
    '''        } finally {
            if (generation == playbackGeneration) internalRetryEnabled = true
        }

        if (generation == playbackGeneration) {''',
    '''        } finally {
            if (generation == playbackGeneration) {
                internalRetryEnabled = true
                suppressBufferRecovery = false
            }
        }

        if (generation == playbackGeneration) {''',
    "restore recovery flag after fallback"
)
# Also clear silent mode for invalid candidates and stop().
player = once(player, '        if (candidates.isEmpty()) {\n            _error.value = "Поток недоступен"', '        if (candidates.isEmpty()) {\n            suppressBufferRecovery = false\n            _error.value = "Поток недоступен"', "empty fallback cleanup")
player = once(
    player,
    '''                System.currentTimeMillis() - bufferingStartedAt >= WEAK_NETWORK_NOTICE_MS
            ) {''',
    '''                System.currentTimeMillis() - bufferingStartedAt >= WEAK_NETWORK_NOTICE_MS &&
                !suppressBufferRecovery
            ) {''',
    "do not lower quality or notify in fast switching"
)
player = once(
    player,
    '_error.value = "Буферизация не завершилась за 15 секунд. Переключите канал."',
    '_error.value = "Буферизация не завершилась за 10 секунд. Переключите канал."',
    "TV buffer timeout text"
)
player = once(player, 'LogExporter.log("TV buffering timeout after 15s")', 'LogExporter.log("TV buffering timeout after 10s")', "TV buffer timeout log")
player = once(player, '        lastUrl = null\n        reconnectAttempts = 0', '        lastUrl = null\n        _activeChannelId.value = null\n        suppressBufferRecovery = false\n        reconnectAttempts = 0', "clear active TV channel on stop")
PLAYER.write_text(player, encoding="utf-8")

# TV screen: three explicit switching modes: intentional choice, silent fast zap, and timed recovery failover.
tv = TV.read_text(encoding="utf-8")
# Remove screen-local usage ticker; the App coordinates it so Settings can keep tracking a background stream.
tv_ticker = '''    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.TV,
            channelIdProvider = { channels.getOrNull(selectedIndex)?.name },
            activeProvider = { fullscreen && selectedIndex in channels.indices && player.isPlaying.value }
        ).run()
    }

'''
tv = remove_once(tv, tv_ticker, "screen-local TV usage ticker")
tv = once(
    tv,
    '    var isSwitching by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }',
    '''    var isSwitching by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var silentSwitching by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var suppressChannelNotice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var playbackRequestToken by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var autoRecoveryTargets by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }''',
    "TV switching state"
)
tv = once(
    tv,
    'fun startPlayback(start: Int, fromFavorites: Boolean? = null) {',
    'fun startPlayback(start: Int, fromFavorites: Boolean? = null, fastSwitch: Boolean = false, direction: Int = 1, automaticRecovery: Boolean = false) {',
    "TV switching function signature"
)
tv = once(
    tv,
    '''            navigationSession = buildNavigationSession(
                start,
                navigationMode == TvNavigationMode.FAVORITES_THEN_GENERAL
            )
        }
        playbackJob?.cancel()''',
    '''            navigationSession = buildNavigationSession(
                start,
                navigationMode == TvNavigationMode.FAVORITES_THEN_GENERAL
            )
            if (!fastSwitch && !automaticRecovery && start !in navigationSession) {
                navigationSession = listOf(start) + navigationSession
            }
        }
        playbackJob?.cancel()''',
    "include consciously selected channel even if health cache marked it offline"
)

tv = once(
    tv,
    '''                    if (!isNavigable(cursor)) {
                        cursor = adjacentIndex(cursor, 1)
                        if (cursor < 0) break
                        continue
                    }''',
    '''                    val forcedInitialSelection = cursor == start && !fastSwitch && !automaticRecovery &&
                        cursor in channels.indices && !hiddenChannels.contains(channels[cursor].key)
                    if (!isNavigable(cursor) && !forcedInitialSelection) {
                        cursor = adjacentIndex(cursor, step)
                        if (cursor < 0) break
                        continue
                    }''',
    "allow deliberate selection to try a channel marked offline"
)

tv = once(
    tv,
    '''        playbackJob?.cancel()
        selectedIndex = start
        fullscreen = true
        notice = null
        isSwitching = true
        playbackJob = scope.launch {
            var cursor = start
            var attempts = 0
            try {''',
    '''        if (!automaticRecovery) autoRecoveryTargets = emptySet()
        playbackJob?.cancel()
        val requestToken = playbackRequestToken + 1L
        playbackRequestToken = requestToken
        selectedIndex = start
        fullscreen = true
        notice = null
        isSwitching = true
        silentSwitching = fastSwitch
        suppressChannelNotice = fastSwitch
        playbackJob = scope.launch {
            var cursor = start
            var attempts = 0
            val step = if (fastSwitch) if (direction < 0) -1 else 1 else 1
            try {''',
    "TV switching request setup"
)
tv = once(
    tv,
    '''                    val candidate = channels[cursor]
                    val result = player.playWithFallback(listOf(candidate.url))
                    if (result >= 0) {''',
    '''                    val candidate = channels[cursor]
                    player.setActiveChannelId(candidate.name)
                    val result = player.playWithFallback(
                        listOf(candidate.url),
                        attemptTimeoutMs = if (fastSwitch) 1_200L else 10_000L,
                        suppressRecovery = fastSwitch
                    )
                    if (result >= 0) {''',
    "TV per-candidate play timeout"
)
tv = once(
    tv,
    '''                        val neighbors = listOf(
                            adjacentIndex(cursor, -1),
                            adjacentIndex(cursor, 1)
                        ).filter { it >= 0 }.distinct()''',
    '''                        val neighbors = (1..3)
                            .mapNotNull { offset -> channels.getOrNull(adjacentIndex(cursor, step * offset)) }
                            .filterNot { it.url == candidate.url }
                            .distinctBy { it.url }''',
    "pre-check next several TV channels"
)
tv = once(
    tv,
    '''                    health = health + (candidate.url to AvailabilityStatus.OFFLINE)
                    cursor = adjacentIndex(cursor, 1)
                    if (cursor < 0) break''',
    '''                    health = health + (candidate.url to AvailabilityStatus.OFFLINE)
                    if (!fastSwitch && !automaticRecovery) {
                        notify("Не удалось воспроизвести канал, переключаю на следующий", 3000L)
                    }
                    cursor = adjacentIndex(cursor, step)
                    if (cursor < 0) break''',
    "TV failure path and directional fallback"
)
tv = once(
    tv,
    '''                isSwitching = false
                fullscreen = false
                notify("Не удалось найти рабочий канал", 2500L)
            } finally {
                isSwitching = false
            }
        }
    }
    fun closePlayer() {
        playbackJob?.cancel()
        player.stop()
        notice = null
        fullscreen = false
    }''',
    '''                if (requestToken == playbackRequestToken) {
                    isSwitching = false
                    silentSwitching = false
                    fullscreen = false
                    player.setActiveChannelId(null)
                    if (!fastSwitch) notify("Не удалось найти рабочий канал", 2500L)
                }
            } finally {
                if (requestToken == playbackRequestToken) {
                    isSwitching = false
                    silentSwitching = false
                }
            }
        }
    }

    LaunchedEffect(error, fullscreen, isSwitching, selectedIndex) {
        if (error == null || !fullscreen || isSwitching || selectedIndex !in channels.indices) return@LaunchedEffect
        val failed = channels[selectedIndex]
        if (failed.url in autoRecoveryTargets) {
            fullscreen = false
            player.stop()
            notify("Не удалось найти рабочий канал", 3000L)
            return@LaunchedEffect
        }
        autoRecoveryTargets = autoRecoveryTargets + failed.url
        health = health + (failed.url to AvailabilityStatus.OFFLINE)
        val next = adjacentIndex(selectedIndex, 1)
        if (next >= 0 && next !in channels.indices.filter { channels[it].url in autoRecoveryTargets }) {
            notify("Не удалось воспроизвести канал, переключаю на следующий", 3000L)
            startPlayback(next, automaticRecovery = true)
        } else {
            fullscreen = false
            player.stop()
            notify("Не удалось найти рабочий канал", 3000L)
        }
    }

    fun closePlayer() {
        playbackJob?.cancel()
        playbackRequestToken += 1L
        isSwitching = false
        silentSwitching = false
        suppressChannelNotice = false
        player.stop()
        notice = null
        fullscreen = false
    }''',
    "TV automatic recovery and protected job finalization"
)
# Use silent switching only for the player controls' next/previous buttons; list and favorite selection remain intentional.
tv = once(
    tv,
    '''            onPrev = {
                val i = adjacentIndex(selectedIndex, -1)
                if (i >= 0) startPlayback(i)
            },
            onNext = {
                val i = adjacentIndex(selectedIndex, 1)
                if (i >= 0) startPlayback(i)
            },''',
    '''            onPrev = {
                val i = adjacentIndex(selectedIndex, -1)
                if (i >= 0) startPlayback(i, fastSwitch = player.isPlaying.value, direction = -1)
            },
            onNext = {
                val i = adjacentIndex(selectedIndex, 1)
                if (i >= 0) startPlayback(i, fastSwitch = player.isPlaying.value, direction = 1)
            },''',
    "TV remote next/previous fast-switch route"
)
tv = once(
    tv,
    '''            switching = isSwitching,
            hapticsEnabled = hapticsEnabled,''',
    '''            switching = isSwitching,
            silentSwitching = silentSwitching,
            suppressChannelNotice = suppressChannelNotice,
            hapticsEnabled = hapticsEnabled,''',
    "pass silent-switch state into player"
)
tv = once(
    tv,
    '''    switching: Boolean,
    hapticsEnabled: Boolean,''',
    '''    switching: Boolean,
    silentSwitching: Boolean,
    suppressChannelNotice: Boolean,
    hapticsEnabled: Boolean,''',
    "player component fast-switch flags"
)
tv = once(
    tv,
    '''    androidx.compose.runtime.LaunchedEffect(channel.key, switching) {
        channelNotice = false
        if (switching) return@LaunchedEffect
        player.resetWeakNetworkSession()
        channelNotice = true
        delay(3000L)
        channelNotice = false
    }''',
    '''    androidx.compose.runtime.LaunchedEffect(channel.key, switching, suppressChannelNotice) {
        channelNotice = false
        if (switching || suppressChannelNotice) return@LaunchedEffect
        player.resetWeakNetworkSession()
        channelNotice = true
        delay(3000L)
        channelNotice = false
    }''',
    "suppress channel notice during fast switch"
)
tv = once(
    tv,
    '''                    val showBuffering = !pipMode &&
                        !switching &&''',
    '''                    val showBuffering = !pipMode &&
                        !silentSwitching &&
                        !switching &&''',
    "hide buffering overlay during fast switch"
)
tv = once(
    tv,
    '''                    val showLoading = switching || (!waiting && error == null && !isPlaying && !buffering)

                    if (!pipMode && noticeMessage == null && (showLoading || waiting || error != null)) {''',
    '''                    val showLoading = !silentSwitching && (switching || (!waiting && error == null && !isPlaying && !buffering))

                    if (!pipMode && !silentSwitching && noticeMessage == null && (showLoading || waiting || error != null)) {''',
    "hide loading and service text during fast switch"
)
TV.write_text(tv, encoding="utf-8")

# PIN-screen rotation remains responsive in landscape. App Shortcuts reuse widget_section already handled by MainActivity.
print("Radio.TV v4.1 migration completed.")
