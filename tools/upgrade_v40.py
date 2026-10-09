#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path("app")
MAIN_PATH = ROOT / "src/main/java/com/offex7/streamhub/MainActivity.kt"
GRADLE_PATH = ROOT / "build.gradle.kts"
LOGO_ASSETS_PATH = ROOT / "src/main/java/com/offex7/streamhub/RadioLogoAssetsV38.kt"
SERVICE_PATH = ROOT / "src/main/java/com/offex7/streamhub/RadioPlaybackService.kt"
ARROW_PATH = ROOT / "src/main/java/com/offex7/streamhub/ScrollTopButtonV37.kt"
RADIO_WIDGET = ROOT / "src/main/res/xml/widget_radio_info.xml"
TV_WIDGET = ROOT / "src/main/res/xml/widget_tv_info.xml"
STRINGS = ROOT / "src/main/res/values/strings.xml"

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    return text.replace(old, new, 1)

gradle = GRADLE_PATH.read_text(encoding="utf-8")
gradle = replace_once(gradle, "versionCode = 12", "versionCode = 13", "versionCode")
gradle = replace_once(gradle, 'versionName = "3.9"', 'versionName = "4.0"', "versionName")
GRADLE_PATH.write_text(gradle, encoding="utf-8")

main = MAIN_PATH.read_text(encoding="utf-8")

recommendation = '''private const val RECOMMEND_MESSAGE = """🎬 Прямой телеэфир и радиовещание без рекламы и подписок? 
Да, такое существует!

Делюсь с тобой приложением RadioTV 🔴

• Визуально — лаконичный минимализм, тёмная тема с яркими 
красными акцентами в интерфейсе. Всё сделано предельно просто: 
открываешь, выбираешь нужную телетрансляцию или радиостанцию 
и сразу смотришь или слушаешь. Никаких всплывающих рекламных 
баннеров и навязчивых подписок...

• Это настоящий «народный проект», который создаётся энтузиастами 
и навсегда останется бесплатным!

📱 Работает на Android 12 и новее.
👉 Переходи в официальную группу и скачивай: 
https://t.me/TvRadioOnline/1

P.S. Сообщение пересылаю лично — это не взлом и не автоматическая 
рассылка. Делюсь от себя, потому что проект реально крутой! 😉"""'''
main, n = re.subn(r'private const val RECOMMEND_MESSAGE = """.*?"""', recommendation, main, count=1, flags=re.S)
if n != 1:
    raise SystemExit("recommendation message: expected exactly one declaration")

old_battery = '''        val powerManager = appContext.getSystemService(android.os.PowerManager::class.java)
        val activityManager = appContext.getSystemService(android.app.ActivityManager::class.java)
        val batteryRestricted = runCatching {
            powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == false
        }.getOrDefault(false)
        val backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            activityManager?.isBackgroundRestricted == true
        if (batteryRestricted || backgroundRestricted) {
            notify("Фоновая работа Radio.TV может быть ограничена ОС. Добавьте приложение в исключения энергосбережения.", 3000L)
        }'''
new_battery = '''        val powerManager = appContext.getSystemService(android.os.PowerManager::class.java)
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
main = replace_once(main, old_battery, new_battery, "battery restriction notice")
main = replace_once(main, 'notify(if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен")', 'notify(if (target == Section.TV) "Счётчик Телевизора сброшен" else "Счётчик Радио сброшен")', "TV counter notice")
main = replace_once(main, '"ПРОВЕРИТЬ ОБНОВЛЕНИЕ",', '"ОБНОВИТЬ ПРИЛОЖЕНИЕ",', "update banner")
main = replace_once(main, '.header("User-Agent", "Radio.TV/3.9")', '.header("User-Agent", "Radio.TV/4.0")', "user-agent v4.0")

pin_hint = '''            Text(
                "ОТСКАНИРУЙТЕ ОТПЕЧАТОК ПАЛЬЦА",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))'''
main = replace_once(main, pin_hint, '            Spacer(Modifier.height(14.dp))', "duplicate biometric hint")

main = replace_once(main, '                        notify("Скопировано", 3000L)', '                        notify("Скопировано", 3000L)\n                        shareText(settingsContext, logText, "Поделиться логами Radio.TV")', "log share intent")

old_logo = '''    val localRadioImage = remember(item.name, isRadio) {
        if (!isRadio) null
        else RadioLogoAssets.image(item.name)
            ?: RadioLogoAssetsV38.image(context, item.name)
    }'''
new_logo = '''    val localRadioImage = remember(item.name, isRadio) {
        if (!isRadio) null
        else if (item.name.equals("РАДИУС FM", ignoreCase = true)) {
            BitmapFactory.decodeResource(context.resources, R.drawable.radius_fm_logo)?.asImageBitmap()
        } else {
            RadioLogoAssets.image(item.name)
                ?: RadioLogoAssetsV38.image(context, item.name)
        }
    }'''
main = replace_once(main, old_logo, new_logo, "local Radius FM artwork")

main = replace_once(main, '    val autoStart by store.autoStartFlow().collectAsState(true)\n', '''    val autoStart by store.autoStartFlow().collectAsState(true)
    val logShareTransition = rememberInfiniteTransition(label = "log-share-pulse")
    val logSharePulse by logShareTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(750), repeatMode = RepeatMode.Reverse),
        label = "log-share-pulse-scale"
    )
    val logShareScale = if (energySaving) 1f else logSharePulse
''', "share pulse state")
main = replace_once(main, '                    Icon(Icons.Default.Share, "Показать логи", tint = Red)', '                    Icon(Icons.Default.Share, "Показать логи", tint = Red, modifier = Modifier.graphicsLayer(scaleX = logShareScale, scaleY = logShareScale))', "share pulse icon")

main = replace_once(main, '    val pinStore = remember(settingsContext) { PinSecurityStore(settingsContext) }\n', '''    val pinStore = remember(settingsContext) { PinSecurityStore(settingsContext) }

    fun toggleFeedback() {
        InteractionFeedback.vibrate(settingsContext, hapticsEnabled, 55L, 180)
        InteractionFeedback.beep(settingsContext, soundEnabled)
    }
''', "settings switch feedback")
main = replace_once(main, '                        onCheckedChange = onPip,', '                        onCheckedChange = { enabled -> toggleFeedback(); onPip(enabled) },', "PiP switch")
main = replace_once(main, '''                            onCheckedChange = { enabled ->
                                scope.launch { store.setEnergySavingMode(if (enabled) "ON" else "OFF") }
                            },''', '''                            onCheckedChange = { enabled ->
                                toggleFeedback()
                                scope.launch { store.setEnergySavingMode(if (enabled) "ON" else "OFF") }
                            },''', "energy-saving switch")
main = replace_once(main, 'onCheckedChange = { enabled -> scope.launch { store.setHapticsEnabled(enabled) } },', 'onCheckedChange = { enabled -> toggleFeedback(); scope.launch { store.setHapticsEnabled(enabled) } },', "haptics switch")
main = replace_once(main, 'onCheckedChange = { enabled -> scope.launch { store.setAutoStartEnabled(enabled) } },', 'onCheckedChange = { enabled -> toggleFeedback(); scope.launch { store.setAutoStartEnabled(enabled) } },', "autostart switch")
main = replace_once(main, 'Switch(checked=soundEnabled,onCheckedChange={enabled->scope.launch{store.setSoundFeedbackEnabled(enabled)}},colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Red,uncheckedThumbColor=Color.White,uncheckedTrackColor=Gray))', 'Switch(checked = soundEnabled, onCheckedChange = { enabled -> toggleFeedback(); scope.launch { store.setSoundFeedbackEnabled(enabled) } }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray))', "sound switch")
main = replace_once(main, '''                        onCheckedChange = { enabled ->
                            if (enabled) pinDialog = true else pinDisableDialog = true
                        },''', '''                        onCheckedChange = { enabled ->
                            toggleFeedback()
                            if (enabled) pinDialog = true else pinDisableDialog = true
                        },''', "PIN switch")
main = replace_once(main, 'Text("Запуск после включения. Экран не будет блокироваться, пока приложение активно",fontSize=11.sp,color=Gray)', 'Text("Экран не будет блокироваться, пока приложение активно", fontSize = 11.sp, color = Gray)', "autostart description")

settings_start = main.index("private fun Settings(")
settings_end = main.index("\n@Composable\nprivate fun ", settings_start + 1)
settings_part = main[settings_start:settings_end]
matches = list(re.finditer(r"(?m)^        item \{", settings_part))
labels = {
    "pip": 'Text("КАРТИНКА В КАРТИНКЕ"',
    "energy": 'Text("ЭНЕРГОСБЕРЕЖЕНИЕ"',
    "haptic": 'Text("ВИБРАЦИОННЫЙ ОТКЛИК"',
    "autostart": 'Text("АВТОЗАПУСК"',
    "sound": 'Text("ЗВУКОВОЙ ОТКЛИК"',
}
indices = {}
for key, marker in labels.items():
    p = settings_part.find(marker)
    if p < 0:
        raise SystemExit(f"missing settings label {marker}")
    indices[key] = next((i for i, m in enumerate(matches) if m.start() <= p and (i + 1 == len(matches) or p < matches[i + 1].start())), None)
    if indices[key] is None:
        raise SystemExit(f"could not locate settings card {key}")
if len(set(indices.values())) != 5:
    raise SystemExit("settings card reordering expected five different cards")
first, last = min(indices.values()), max(indices.values())
if last - first != 4 or indices["pip"] != first or indices["energy"] != first + 1 or indices["haptic"] != first + 2 or indices["autostart"] != first + 3 or indices["sound"] != first + 4:
    raise SystemExit("settings cards are not in expected v3.9 order; safe reorder aborted")
group_start = matches[first].start()
group_end = matches[last + 1].start()
parts = {
    key: settings_part[matches[indices[key]].start():matches[indices[key] + 1].start()]
    for key in labels
}
new_group = "".join(parts[key] for key in ("energy", "autostart", "haptic", "sound", "pip"))
settings_part = settings_part[:group_start] + new_group + settings_part[group_end:]
main = main[:settings_start] + settings_part + main[settings_end:]
MAIN_PATH.write_text(main, encoding="utf-8")

logos = LOGO_ASSETS_PATH.read_text(encoding="utf-8")
method_start = logos.index("    private val cache = HashMap<String, ImageBitmap>()")
methods = '''    private val cache = HashMap<String, ImageBitmap>()
    private val bitmapCache = HashMap<String, android.graphics.Bitmap>()

    fun bitmap(context: Context, stationName: String): android.graphics.Bitmap? {
        val key = stationName.trim().uppercase(Locale.ROOT)
        bitmapCache[key]?.let { return it }
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
        bitmapCache[key] = bitmap
        return bitmap
    }

    fun image(context: Context, stationName: String): ImageBitmap? {
        val key = stationName.trim().uppercase(Locale.ROOT)
        cache[key]?.let { return it }
        val image = bitmap(context, key)?.asImageBitmap() ?: return null
        cache[key] = image
        return image
    }'''
logos = logos[:method_start] + methods + "\n}\n"
LOGO_ASSETS_PATH.write_text(logos, encoding="utf-8")

service = SERVICE_PATH.read_text(encoding="utf-8")
service = replace_once(service, "package com.offex7.streamhub\n\n", '''package com.offex7.streamhub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.util.Locale

''', "MediaSession artwork imports")
service = replace_once(
    service,
    '                        .setArtworkUri(Uri.parse("android.resource://$packageName/${R.drawable.ic_app_icon}"))\n                        .build()',
    '''                        .setArtworkUri(Uri.parse("android.resource://$packageName/${R.drawable.ic_app_icon}"))
                        .setArtworkData(stationArtworkBytes(station.name), androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()''',
    "MediaSession artwork metadata"
)
class_start = service.index("class RadioPlaybackService : MediaSessionService() {")
insert_at = service.index("\n    override fun onCreate()", class_start)
helper = '''
    private fun stationArtworkBytes(stationName: String): ByteArray? {
        val bitmap = if (stationName.trim().uppercase(Locale.ROOT) == "РАДИУС FM") {
            BitmapFactory.decodeResource(resources, R.drawable.radius_fm_logo)
        } else {
            RadioLogoAssets.bitmap(stationName) ?: RadioLogoAssetsV38.bitmap(applicationContext, stationName)
        } ?: return null
        return runCatching {
            ByteArrayOutputStream().use { output ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) output.toByteArray() else null
            }
        }.getOrNull()
    }
'''
service = service[:insert_at] + helper + service[insert_at:]
SERVICE_PATH.write_text(service, encoding="utf-8")

arrow = ARROW_PATH.read_text(encoding="utf-8")
arrow = replace_once(arrow, 'modifier = Modifier.size(31.dp)', 'modifier = Modifier.size(36.dp)', "scroll glyph")
ARROW_PATH.write_text(arrow, encoding="utf-8")

RADIO_WIDGET.write_text('''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="72dp"
    android:minHeight="72dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_radio"
    android:label="@string/widget_radio_label"
    android:previewImage="@drawable/start_radio_v2"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
''', encoding="utf-8")
TV_WIDGET.write_text('''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="72dp"
    android:minHeight="72dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_tv"
    android:label="@string/widget_tv_label"
    android:previewImage="@drawable/start_tv_final"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
''', encoding="utf-8")
strings = STRINGS.read_text(encoding="utf-8")
strings = replace_once(strings, '</resources>', '    <string name="widget_radio_label">Радио</string>\n    <string name="widget_tv_label">Телевизор</string>\n</resources>', "widget strings")
STRINGS.write_text(strings, encoding="utf-8")

print("Radio.TV v4.0 source migration completed.")
