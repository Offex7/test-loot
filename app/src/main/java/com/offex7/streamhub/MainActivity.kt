@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)
package com.offex7.streamhub

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

private val Red = Color(0xFFE53935)
private val Orange = Color(0xFFFF9800)
private val Pink = Color(0xFFFF4081)
private val Bg = Color(0xFF121212)
private val Panel = Color(0xFF1A1A1A)
private val PanelAlt = Color(0xFF232323)
private val Gray = Color(0xFF808080)

private const val TELEGRAM = "https://t.me/TvRadioOnline"
private const val TELEGRAM_DONATION = "https://t.me/TvRadioOnline/9"
private const val RECOMMEND_MESSAGE = """Приветствую! Слушай, хочу поделиться находкой, которая реально заслуживает внимания — приложением Radio.TV. Представь: куча ТВ-каналов, Радио и всё, что нужно для досуга, в одном месте. И всё это абсолютно бесплатно! Дизайн минималистичный и приятный, ничего лишнего. Приложение активно развивается, так что скучать не придётся. Работает на Android 12 и новее. Я уже пользуюсь — и мне очень зашло. Держи ссылку на блог разработчика, устанавливай скорее: [https://t.me/TvRadioOnline](https://t.me/TvRadioOnline). Не пожалеешь! И да, я пишу это осознанно: это не спам, а искренняя рекомендация.  """
private const val WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"
private const val RESTORE_WINDOW = 10 * 60 * 1000L
private const val ERROR_COOLDOWN = 5 * 60 * 1000L
private val APP_VERSION = BuildConfig.VERSION_NAME
val LocalEnergySaving = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
private fun rememberSystemPowerSave(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val pm = context.getSystemService(android.os.PowerManager::class.java)
        fun read() { enabled = pm?.isPowerSaveMode == true }
        read()
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) read()
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return enabled
}
private val zoomByChannel = mutableMapOf<String, Float>()
private val logoHttpClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).callTimeout(7, TimeUnit.SECONDS).build()
private fun shareText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private val SleepOptions = listOf(
    5L to "5 мин", 10L to "10 мин", 15L to "15 мин", 30L to "30 мин", 60L to "1 ч", 120L to "2 ч",
    240L to "4 ч", 480L to "8 ч", 600L to "10 ч", 900L to "15 ч", 1440L to "24 ч", 2160L to "36 ч"
)

class MainActivity : FragmentActivity() {
    private lateinit var store: SettingsStore
    private lateinit var tv: PlayerController
    private lateinit var radio: RadioMediaController
    internal var pipEnabled = true
    internal var tvViewing by mutableStateOf(false)
    internal var pipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AColor.TRANSPARENT
        window.navigationBarColor = AColor.TRANSPARENT
        window.setNavigationBarDividerColor(AColor.TRANSPARENT)
        store = SettingsStore(applicationContext)
        tv = PlayerController(applicationContext)
        radio = RadioMediaController(applicationContext)
        setContent { AppTheme(store) { App(store, tv, radio, this) } }
    }

    override fun onStop() {
        lifecycleScope.launch { store.setLastExitTime(System.currentTimeMillis()) }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        if (tvViewing && pipEnabled && Build.VERSION.SDK_INT >= 26 && !isInPictureInPictureMode) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        }
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        tv.release()
        radio.release()
        super.onDestroy()
    }
}

@Composable
private fun AppTheme(store: SettingsStore, content: @Composable () -> Unit) {
    val systemPowerSave = rememberSystemPowerSave()
    val energyMode by store.energySavingModeFlow().collectAsState(initial = "AUTO")
    val energySaving = energyMode == "ON" || (energyMode == "AUTO" && systemPowerSave)
    val bg = if (energySaving) Color.Black else Bg
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Red,
            secondary = Red,
            background = bg,
            surface = if (energySaving) Color.Black else Panel,
            surfaceVariant = if (energySaving) Color(0xFF101010) else PanelAlt
        )
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalEnergySaving provides energySaving) {
            content()
        }
    }
}

@Composable
private fun App(
    store: SettingsStore,
    tv: PlayerController,
    radio: RadioMediaController,
    activity: MainActivity
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val energySaving = LocalEnergySaving.current
    val hapticsEnabled by store.hapticsFlow().collectAsState(true)
    val soundEnabled by store.soundFeedbackFlow().collectAsState(true)
    val autoStartEnabled by store.autoStartFlow().collectAsState(false)
    val pinStore = remember(appContext) { PinSecurityStore(appContext) }
    var pinUnlocked by rememberSaveable { mutableStateOf(!pinStore.isEnabled()) }
    DisposableEffect(autoStartEnabled, activity.tvViewing) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val active = activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
                if (active && (autoStartEnabled || activity.tvViewing)) {
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    handler.postDelayed(this, 8_000L)
                } else {
                    activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
        runnable.run()
        onDispose {
            handler.removeCallbacks(runnable)
            if (!activity.tvViewing) {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    if (!pinUnlocked) {
        PinGate(activity = activity, store = pinStore, onUnlocked = { pinUnlocked = true })
        return
    }
    val radioTimerStore = remember(appContext) { RadioTimerStore(appContext) }
    var notification by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationDuration by remember { mutableLongStateOf(5000L) }
    var notificationToken by remember { mutableLongStateOf(0L) }
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var disclaimer by rememberSaveable { mutableStateOf(false) }
    var exit by rememberSaveable { mutableStateOf(false) }
    var quickLockSetup by rememberSaveable { mutableStateOf(false) }
    var restore by remember { mutableStateOf<StreamItem?>(null) }
    var pip by remember { mutableStateOf(true) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableLongStateOf(0L) }
    var sleepMinutes by remember { mutableLongStateOf(0L) }
    var sleepWarningFor by remember { mutableLongStateOf(0L) }
    fun notify(message: String, durationMs: Long = 5000L) {
        notification = message
        notificationDuration = durationMs.coerceAtLeast(250L)
        notificationToken += 1L
    }

    LaunchedEffect(notificationToken) {
        if (notification == null || notificationToken == 0L) return@LaunchedEffect
        val token = notificationToken
        delay(notificationDuration)
        if (token == notificationToken) notification = null
    }

    LaunchedEffect(Unit) {
        if (!store.disclaimerShown()) {
            disclaimer = true
            store.setDisclaimerShown(true)
        }
    }

    LaunchedEffect(Unit) {
        pip = store.pipEnabled()
        activity.pipEnabled = pip
        val last = store.lastExitTime()
        val savedSection = store.lastSection()
        if (savedSection != null && last > 0L && System.currentTimeMillis() - last < RESTORE_WINDOW) {
            section = savedSection
            restore = store.lastStream(savedSection)
        }
    }

    fun armSleep(minutes: Long) {
        tv.cancelFadeOut()
        radio.cancelFadeOut()
        sleepMinutes = minutes.coerceAtLeast(1L)
        sleepUntil = System.currentTimeMillis() + sleepMinutes * 60_000L
        sleepWarningFor = 0L
        notify("Таймер сна — запущен")
    }

    fun cancelSleep() {
        sleepUntil = 0L
        sleepRemaining = 0L
        sleepMinutes = 0L
        sleepWarningFor = 0L
        tv.cancelFadeOut()
        radio.cancelFadeOut()
        notify("Таймер сна — отключён")
    }

    LaunchedEffect(sleepUntil) {
        tv.cancelFadeOut()
        radio.cancelFadeOut()
        if (sleepUntil <= 0L) {
            sleepRemaining = 0L
            return@LaunchedEffect
        }
        var warningShown = false
        while (true) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 15_000L && !warningShown) {
                warningShown = true
                sleepWarningFor = sleepUntil
                notify("Таймер сна сработает через 15 секунд", 3_000L)
                val fadeDuration = left.coerceAtLeast(1L)
                tv.fadeOut(fadeDuration)
                radio.fadeOut(fadeDuration)
            }
            if (left <= 0L) {
                sleepRemaining = 0L
                sleepMinutes = 0L
                tv.pause()
                radio.pause()
                radioTimerStore.pause(System.currentTimeMillis())
                sleepUntil = 0L
                delay(250L)
                activity.finishAndRemoveTask()
                break
            }
            sleepRemaining = left
            delay(250L)
        }
    }
    fun leaveSection() {
        section = null
        settings = false
        disclaimer = false
        restore = null
        activity.tvViewing = false
        tv.stop()
        radio.stop()
        scope.launch { store.clearSection(); radioTimerStore.reset() }
    }

    BackHandler {
        when {
            disclaimer -> disclaimer = false
            settings -> settings = false
            section != null -> leaveSection()
            else -> exit = true
        }
    }

    Scaffold(containerColor = if (energySaving) Color.Black else Bg, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
            )
        ) {
            when {
                disclaimer -> Disclaimer(onBack = { disclaimer = false })
                settings -> Settings(
                    store = store,
                    pip = pip,
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepMinutes = sleepMinutes,
                    onBack = { settings = false },
                    onPip = {
                        pip = it
                        activity.pipEnabled = it
                        scope.launch { store.setPipEnabled(it) }
                    },
                    onSleep = {
                        sleepMinutes = it
                        armSleep(it)
                    },
                    onCancelSleep = {
                        cancelSleep()
                    },
                    onResetStats = { target ->
                        scope.launch {
                            runCatching { store.resetUsage(target) }
                                .onSuccess { notify(if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен") }
                                .onFailure { notify("Не удалось сбросить счётчик") }
                        }
                    },
                    onSource = { key ->
                        scope.launch {
                            store.setActiveSourceKey(key)
                            notify("Источник сохранён")
                        }
                    },
                    onDisclaimer = { disclaimer = true },
                    notify = ::notify,
                    onResetAll = {
                        scope.launch {
                            runCatching {
                                store.resetAll()
                                radioTimerStore.reset()
                            }.onSuccess {
                                    sleepUntil = 0L
                                    sleepRemaining = 0L
                                    sleepMinutes = 0L
                                    pip = true
                                    activity.pipEnabled = true
                                    section = null
                                    settings = false
                                    disclaimer = false
                                    restore = null
                                    notify("Настройки сброшены")
                                }
                                .onFailure { notify("Не удалось сбросить настройки") }
                        }
                    }
                )
                section == null -> Home(
                    activity = activity,
                    hapticsEnabled = hapticsEnabled,
                    onHaptic = { InteractionFeedback.click(appContext,hapticsEnabled,soundEnabled,false) },
                    open = {
                        section = it
                        scope.launch { store.setSection(it) }
                    },
                    settings = { settings = true }
                )
                section == Section.TV -> TvV8Screen(
                    player = tv,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = {
                        restore = null
                        scope.launch { store.saveLastStream(Section.TV, it) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true },
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepMinutes = sleepMinutes,
                    onSleep = {
                        sleepMinutes = it
                        sleepUntil = System.currentTimeMillis() + it * 60_000L
                        notify("Таймер сна — запущен")
                    },
                    onCancelSleep = {
                        sleepUntil = 0L
                        sleepRemaining = 0L
                        sleepMinutes = 0L
                        notify("Таймер сна — отключён")
                    },
                    notify = ::notify,
                    onViewingChanged = { activity.tvViewing = it },
                    pipMode = activity.pipMode,
                    hapticsEnabled = hapticsEnabled,
                    soundEnabled = soundEnabled,
                    radioPlaying = radio.isPlaying.collectAsState(false).value
                )
                else -> Radio(
                    player = radio,
                    store = store,
                    restore = restore,
                    dismissRestore = { restore = null },
                    saveLast = {
                        restore = null
                        scope.launch { store.saveLastStream(Section.RADIO, it) }
                    },
                    back = ::leaveSection,
                    settings = { settings = true },
                    notify = ::notify,
                    sleepRemaining = sleepRemaining,
                    sleepUntil = sleepUntil,
                    sleepMinutes = sleepMinutes,
                    onSleep = ::armSleep,
                    onCancelSleep = ::cancelSleep,
                    hapticsEnabled = hapticsEnabled,
                    soundEnabled = soundEnabled,
                    onQuickLock = { if(pinStore.isEnabled()){pinUnlocked=false;activity.moveTaskToBack(true)}else quickLockSetup=true }
                )
            }

            AnimatedVisibility(
                visible = notification != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut()
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelAlt),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            notification.orEmpty(),
                            Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { notification = null },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, "Закрыть уведомление", tint = Red)
                        }
                    }
                }
            }
        }
    }

    if(quickLockSetup){PinSetupDialog(onDismiss={quickLockSetup=false},onSave={pin,hint->if(pinStore.enable(pin,hint)){quickLockSetup=false;pinUnlocked=false;activity.moveTaskToBack(true)}})}

    if (exit) {
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Выйти из приложения?") },
            confirmButton = { TextButton(onClick = { activity.finishAndRemoveTask() }) { Text("Выйти", color = Red) } },
            dismissButton = { TextButton(onClick = { exit = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun Home(
    activity: MainActivity,
    hapticsEnabled: Boolean,
    onHaptic: () -> Unit,
    open: (Section) -> Unit,
    settings: () -> Unit
) {
    val context = LocalContext.current
    val energySaving = LocalEnergySaving.current
    val transition = rememberInfiniteTransition(label = "home-animations")
    val gearPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (energySaving) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home-gear-pulse"
    )
    val gearColorAnimated by transition.animateColor(
        initialValue = Color.White,
        targetValue = Color.White,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                Color.White at 0
                Color(0xFF42A5F5) at 1000
                Red at 2000
                Color.White at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "home-gear-color"
    )
    val gearColor=if(energySaving)Color.White else gearColorAnimated
    var gearTurns by remember { mutableIntStateOf(0) }
    val gearRotation by animateFloatAsState(
        targetValue = if (energySaving) 0f else gearTurns * 90f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "home-gear-rotation"
    )
    val tvOutlineColor = Color.White
    val radioOutlineColor = Color.White

    val recommendPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (energySaving) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recommend-text-pulse"
    )

    val widthClass = calculateWindowSizeClass(activity).widthSizeClass

    BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Radio.TV ") }
                        withStyle(SpanStyle(color = Color(0xFF4CAF50))) { append(APP_VERSION) }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        onHaptic()
                        gearTurns += 1
                        settings()
                    }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        "Настройки",
                        tint = gearColor,
                        modifier = Modifier.graphicsLayer(
                            rotationZ = gearRotation,
                            scaleX = gearPulse,
                            scaleY = gearPulse
                        )
                    )
                }
            }

            when (widthClass) {
                WindowWidthSizeClass.Compact -> {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv_final, Modifier.fillMaxWidth().weight(1f), tvOutlineColor) { onHaptic(); open(Section.TV) }
                        HomeCard("РАДИО", R.drawable.start_radio_v2, Modifier.fillMaxWidth().weight(1f), radioOutlineColor) { onHaptic(); open(Section.RADIO) }
                    }
                }
                else -> {
                    Row(
                        Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HomeCard("ТЕЛЕВИЗОР", R.drawable.start_tv_final, Modifier.weight(1f), tvOutlineColor) { onHaptic(); open(Section.TV) }
                        HomeCard("РАДИО", R.drawable.start_radio_v2, Modifier.weight(1f), radioOutlineColor) { onHaptic(); open(Section.RADIO) }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                onClick = {
                    InteractionFeedback.click(context, hapticsEnabled, false, allowSound = false)
                    shareText(context, RECOMMEND_MESSAGE, "Рекомендовать Radio.TV")
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "рекомендовать друзьям",
                        color = Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer(scaleX = recommendPulse, scaleY = recommendPulse)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    logo: Int,
    modifier: Modifier,
    borderColor: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val iconModifier = Modifier.fillMaxHeight(0.82f).aspectRatio(1f)
                Box(iconModifier) {
                    val painter = painterResource(logo)
                    val outlineOffsets = listOf(
                        Offset(-1.5f, 0f), Offset(1.5f, 0f),
                        Offset(0f, -1.5f), Offset(0f, 1.5f),
                        Offset(-1.05f, -1.05f), Offset(1.05f, -1.05f),
                        Offset(-1.05f, 1.05f), Offset(1.05f, 1.05f)
                    )
                    outlineOffsets.forEach { shift ->
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().offset(x = shift.x.dp, y = shift.y.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(borderColor)
                        )
                    }
                    Image(painter = painter, contentDescription = title, modifier = Modifier.matchParentSize())
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ScrollActionButton(
    visible: Boolean,
    direction: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.8f, animationSpec = tween(180)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.8f, animationSpec = tween(180))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Red, CircleShape)
        ) {
            Icon(
                if (direction == 1) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                if (direction == 1) "Вниз" else "Вверх",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun onHapticFeedback(context: Context,hapticsEnabled:Boolean,soundEnabled:Boolean){InteractionFeedback.click(context,hapticsEnabled,soundEnabled,false)}
private fun performRadioTvHaptic(view: android.view.View, enabled: Boolean, constant: Int) {
    if (enabled) view.performHapticFeedback(constant)
}

internal fun performFavoriteHaptic(view: android.view.View, enabled: Boolean, adding: Boolean) {
    if (!enabled) return
    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    if (adding) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (view.isAttachedToWindow) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }, 70L)
    }
}

internal fun fuzzyMatch(query: String, text: String): Boolean {
    val q = query.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    if (q.isBlank()) return true
    val normalizedText = text.lowercase(Locale.ROOT)
    if (normalizedText.contains(q)) return true
    val aliases = when (q.removeSuffix("ы").removeSuffix("и")) {
        "спорт" -> listOf("спорт", "sport")
        "фильм", "кино" -> listOf("фильм", "кино", "movie")
        "музык" -> listOf("музык", "music", "радио")
        "новост" -> listOf("новост", "news")
        "детск" -> listOf("детск", "children", "kids")
        "познавател" -> listOf("познавател", "документ", "science")
        "развлекател" -> listOf("развлекател", "entertainment")
        else -> emptyList()
    }
    if (aliases.any { normalizedText.contains(it) }) return true
    return normalizedText.split(Regex("[^\\p{L}\\p{Nd}]+"))
        .filter { it.isNotBlank() }
        .any { levenshtein(q, it) <= maxOf(1, q.length / 4) }
}

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    for (i in a.indices) {
        val cur = IntArray(b.length + 1)
        cur[0] = i + 1
        for (j in b.indices) {
            cur[j + 1] = minOf(prev[j + 1] + 1, cur[j] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
        }
        prev = cur
    }
    return prev[b.length]
}

internal fun fuzzyFilter(items: List<StreamItem>, query: String): List<StreamItem> =
    if (query.isBlank()) items else items.filter { fuzzyMatch(query, it.name) || fuzzyMatch(query, it.groupTitle.orEmpty()) }

@Composable
fun SearchHistoryPanel(
    history: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit
) {
    if (history.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("ПОСЛЕДНИЕ ПОИСКИ", color = Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        history.forEach { q ->
            TextButton(
                onClick = { onPick(q) },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) { Text(q, Modifier.fillMaxWidth(), textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        TextButton(
            onClick = onClear,
            modifier = Modifier.align(Alignment.End).height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) { Text("Очистить историю", color = Red, fontSize = 11.sp) }
    }
}

@Composable
private fun Header(
    title: String,
    timer: String,
    back: () -> Unit,
    settings: (() -> Unit)?,
    searchOpen: Boolean,
    query: String,
    onSearchOpen: () -> Unit,
    onQuery: (String) -> Unit,
    onSearchClose: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    extraAction: (@Composable () -> Unit)? = null,
    showSearch: Boolean = true,
    showTimer: Boolean = true
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (showTimer && timer.isNotBlank()) Text(timer, fontSize = 11.sp, color = Red)
            if (!searchOpen) {
                onRefresh?.let { IconButton(onClick = it) { Icon(Icons.Default.Refresh, "Обновить", tint = Red) } }
                if (showSearch) IconButton(onClick = onSearchOpen) { Icon(Icons.Default.Search, "Поиск", tint = Red) }
                extraAction?.invoke()
                settings?.let { IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) } }
            } else if (showSearch) {
                IconButton(onClick = onSearchClose) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
            }
        }
        if (searchOpen && showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                singleLine = true,
                maxLines = 1,
                placeholder = { Text("Поиск…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = onSearchClose) { Icon(Icons.Default.Close, null) } }
            )
        }
    }
}

@Composable
private fun SearchAutoClose(open: Boolean, query: String, stamp: Long, close: () -> Unit) {
    LaunchedEffect(open, query, stamp) {
        if (!open) return@LaunchedEffect
        val current = stamp
        delay(if (query.isBlank()) 5000L else 15000L)
        if (open && current == stamp) close()
    }
}

@Composable
private fun Radio(
    player: RadioMediaController,
    store: SettingsStore,
    restore: StreamItem?,
    dismissRestore: () -> Unit,
    saveLast: (StreamItem) -> Unit,
    back: () -> Unit,
    settings: () -> Unit,
    notify: (String) -> Unit,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    onQuickLock: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sleepMenu by remember { mutableStateOf(false) }
    val index by player.currentIndex.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val error by player.error.collectAsState()
    val connected by player.connected.collectAsState()
    val network by rememberNetworkState()
    val list = rememberLazyListState()
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var favoriteTimes by remember { mutableStateOf(emptyMap<String, Long>()) }
    var timerElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var timerStartedAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var timerNowMs by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var scrollDirection by remember { mutableIntStateOf(0) } // 1=up, -1=down
    var showScrollAction by remember { mutableStateOf(false) }
    var scrollActivityToken by remember { mutableLongStateOf(0L) }
    var availability by remember { mutableStateOf<Map<String, AvailabilityStatus>>(emptyMap()) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchStamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var searchHistory by remember { mutableStateOf(emptyList<String>()) }
    var radioBumpNonce by remember { mutableIntStateOf(0) }
    var eqDialog by remember { mutableStateOf(false) }
    val eqSettings by store.radioEqualizerFlow().collectAsState(SettingsStore.RadioEqualizerSettings())
    val energySaving = LocalEnergySaving.current
    val radioTimerContext = LocalContext.current.applicationContext
    val timerStore = remember(radioTimerContext) { RadioTimerStore(radioTimerContext) }

    KeepSystemBarsVisible()

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.RADIO)
        favoriteTimes = store.favoriteAddedAt(Section.RADIO)
        searchHistory = store.searchHistory(Section.RADIO)
        val saved = store.scrollPosition(Section.RADIO)
        list.scrollToItem(saved.first.coerceAtMost(RADIO_STATIONS.lastIndex), saved.second)
        val timer = timerStore.state()
        timerElapsedMs = timer.elapsedMs
        timerStartedAtMs = timer.startedAtMs
        timerNowMs = System.currentTimeMillis()
    }

    LaunchedEffect(connected) {
        if (connected) {
            val timer = timerStore.state()
            timerElapsedMs = timer.elapsedMs
            timerStartedAtMs = timer.startedAtMs
            timerNowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        availability = scanRadioAvailability(RADIO_STATIONS)
    }

    LaunchedEffect(energySaving) {
        if (energySaving) return@LaunchedEffect
        while (true) {
            delay(40 * 60 * 1000L)
            availability = availability + scanRadioAvailability(RADIO_STATIONS)
        }
    }

    LaunchedEffect(playing, timerStartedAtMs, energySaving) {
        while (playing && timerStartedAtMs > 0L) {
            timerNowMs = System.currentTimeMillis()
            delay(if (energySaving) 2000L else 1000L)
        }
        timerNowMs = System.currentTimeMillis()
    }

    LaunchedEffect(error) {
        if (error != null && index >= 0) {
            val now = System.currentTimeMillis()
            val state = timerStore.pause(now)
            timerElapsedMs = state.elapsedMs
            timerStartedAtMs = state.startedAtMs
            timerNowMs = now
            RADIO_STATIONS.getOrNull(index)?.let { availability = availability + (it.url to AvailabilityStatus.OFFLINE) }
            notify("Возникла проблема. Обсуждаем решения в Telegram.")
        }
    }

    LaunchedEffect(playing, player.currentIndex.value, energySaving) {
        if (!playing || energySaving) return@LaunchedEffect
        val stationKey = RADIO_STATIONS.getOrNull(player.currentIndex.value)?.key ?: return@LaunchedEffect
        while (playing && !energySaving) {
            delay(Random.nextLong(30 * 60 * 1000L, 50 * 60 * 1000L + 1L))
            if (playing && RADIO_STATIONS.getOrNull(player.currentIndex.value)?.key == stationKey) radioBumpNonce += 1
        }
    }

    LaunchedEffect(searchOpen, query, searchStamp) {
        if (!searchOpen) return@LaunchedEffect
        val token = searchStamp
        delay(if (query.isBlank()) 5000L else 15000L)
        if (token == searchStamp) {
            if (query.isNotBlank()) store.rememberSearch(Section.RADIO, query)
            searchOpen = false
            query = ""
        }
    }

    LaunchedEffect(Unit) {
        UsageTicker(
            store = store,
            section = Section.RADIO,
            channelIdProvider = { RADIO_STATIONS.getOrNull(player.currentIndex.value)?.name },
            activeProvider = { player.isPlaying.value && player.currentIndex.value >= 0 }
        ).run()
    }

    LaunchedEffect(list) {
        var previous = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        var previousTime = System.currentTimeMillis()
        snapshotFlow {
            Triple(
                list.firstVisibleItemIndex,
                list.firstVisibleItemScrollOffset,
                list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            )
        }.collect { current ->
            val now = System.currentTimeMillis()
            store.saveScrollPosition(Section.RADIO, current.first, current.second)
            if (current.first != previous.first || current.second != previous.second) {
                val deltaIndex = current.first - previous.first
                val deltaPx = current.second - previous.second
                val elapsed = (now - previousTime).coerceAtLeast(1L)
                val movingUp = deltaIndex < 0 || (deltaIndex == 0 && deltaPx < 0)
                val movingDown = deltaIndex > 0 || (deltaIndex == 0 && deltaPx > 0)
                val fast = kotlin.math.abs(deltaIndex) >= 2 ||
                    (elapsed <= 140L && kotlin.math.abs(deltaPx) >= 96)
                val total = RADIO_STATIONS.size
                val lastVisible = current.third
                val canGoTop = current.first > maxOf(5, favorites.size)
                val canGoBottom = total > 0 && lastVisible >= 0 && lastVisible < total - 6
                when {
                    current.first <= 0 && current.second <= 0 -> showScrollAction = false
                    total > 0 && lastVisible >= total - 1 -> showScrollAction = false
                    fast && movingDown && canGoTop -> {
                        scrollDirection = -1
                        showScrollAction = true
                        scrollActivityToken += 1L
                    }
                    fast && movingUp && canGoBottom -> {
                        scrollDirection = 1
                        showScrollAction = true
                        scrollActivityToken += 1L
                    }
                }
                previous = current.first to current.second
                previousTime = now
            }
        }
    }

    LaunchedEffect(scrollActivityToken) {
        if (scrollActivityToken == 0L) return@LaunchedEffect
        val token = scrollActivityToken
        delay(5000L)
        if (token == scrollActivityToken) showScrollAction = false
    }

    fun updateTimerState(state: RadioTimerState, now: Long) {
        timerElapsedMs = state.elapsedMs
        timerStartedAtMs = state.startedAtMs
        timerNowMs = now
    }

    suspend fun startNewStationTimer() {
        val now = System.currentTimeMillis()
        updateTimerState(timerStore.start(now), now)
    }

    suspend fun pauseCurrentTimer() {
        val now = System.currentTimeMillis()
        updateTimerState(timerStore.pause(now), now)
    }

    suspend fun resumeCurrentTimer() {
        val now = System.currentTimeMillis()
        val cutoff = store.lastExitTime()
        updateTimerState(timerStore.resume(now, cutoff), now)
    }

    suspend fun playNewStation(i: Int, station: StreamItem) {
        if (i !in RADIO_STATIONS.indices) return
        startNewStationTimer()
        player.play(i)
        availability = availability + (station.url to AvailabilityStatus.ONLINE)
        saveLast(station)
    }

    suspend fun toggleActiveStation(i: Int, station: StreamItem) {
        if (i !in RADIO_STATIONS.indices) return
        if (player.isPlaying.value) {
            pauseCurrentTimer()
            player.pause()
        } else {
            resumeCurrentTimer()
            player.play(i)
            availability = availability + (station.url to AvailabilityStatus.ONLINE)
            saveLast(station)
        }
    }

    val current = RADIO_STATIONS.getOrNull(index)
    val orderedStations = remember(favorites, favoriteTimes) {
        RADIO_STATIONS.sortedWith(
            compareByDescending<StreamItem> { favorites.contains(it.key) }
                .thenByDescending { favoriteTimes[it.key] ?: 0L }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    val filteredStations = fuzzyFilter(orderedStations, query)

    val displayedTimerMs = (
        timerElapsedMs +
            if (playing && timerStartedAtMs > 0L) {
                (timerNowMs - timerStartedAtMs).coerceAtLeast(0L)
            } else 0L
        ).coerceAtLeast(0L)

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            if (!network) NetworkBanner()

            Header(
                title = "РАДИО",
                timer = "",
                back = back,
                settings = settings,
                extraAction = {
                    Row(verticalAlignment=Alignment.CenterVertically){
                        IconButton(onClick={InteractionFeedback.vibrate(context,hapticsEnabled,42L,105);eqDialog=true}){Icon(Icons.Default.Equalizer,"Эквалайзер",tint=Red)}
                        IconButton(onClick={InteractionFeedback.vibrate(context,hapticsEnabled,42L,105);onQuickLock()}){Icon(Icons.Default.Lock,"Блокировка",tint=Red)}
                        IconButton(onClick={InteractionFeedback.vibrate(context,hapticsEnabled,42L,105);sleepMenu=!sleepMenu}){Icon(Icons.Default.AccessTime,"Таймер сна",tint=Red)}
                    }
                },
                searchOpen = false,
                query = query,
                onSearchOpen = {
                    searchOpen = true
                    searchStamp = System.currentTimeMillis()
                    scope.launch { searchHistory = store.searchHistory(Section.RADIO) }
                },
                onQuery = {
                    query = it
                    searchStamp = System.currentTimeMillis()
                },
                onSearchClose = {
                    if (query.isNotBlank()) scope.launch { store.rememberSearch(Section.RADIO, query) }
                    searchOpen = false
                    query = ""
                    searchStamp = System.currentTimeMillis()
                },
                showSearch = false,
                showTimer = false
            )

            if (searchOpen && query.isBlank()) {
                SearchHistoryPanel(
                    history = searchHistory,
                    onPick = {
                        query = it
                        searchStamp = System.currentTimeMillis()
                    },
                    onClear = {
                        scope.launch {
                            store.clearSearchHistory(Section.RADIO)
                            searchHistory = emptyList()
                        }
                    }
                )
            }

            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        scope.launch {
                            val i = RADIO_STATIONS.indexOfFirst { station ->
                                station.url == item.url || station.name.equals(item.name, ignoreCase = true)
                            }
                            if (i >= 0) {
                                val now = System.currentTimeMillis()
                                updateTimerState(timerStore.resume(now, store.lastExitTime()), now)
                                player.play(i)
                                saveLast(RADIO_STATIONS[i])
                            } else {
                                notify("Сохранённая станция больше не найдена")
                            }
                        }
                    },
                    onClose = dismissRestore
                )
            }

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(filteredStations, key = { it.key }) { station ->
                        val favorite = favorites.contains(station.key)
                        val active = station.url == current?.url
                        ChannelRow(
                            item = station,
                            favorite = favorite,
                            playing = active && playing,
                            offline = availability[station.url] == AvailabilityStatus.OFFLINE,
                            onPlay = {
                                val i = RADIO_STATIONS.indexOf(station)
                                if (i >= 0) {
                                    scope.launch {
                                        if (active) {
                                            toggleActiveStation(i, station)
                                        } else {
                                            if (index >= 0 && player.isPlaying.value) {
                                                timerStore.pause(System.currentTimeMillis())
                                            }
                                            playNewStation(i, station)
                                        }
                                    }
                                }
                            },
                            onFavorite = {
                                scope.launch {
                                    runCatching {
                                        store.setFavorite(Section.RADIO, station.key, !favorite)
                                        favorites = store.favorites(Section.RADIO)
                                        favoriteTimes = store.favoriteAddedAt(Section.RADIO)
                                        notify(if (!favorite) "Добавлено в избранное" else "Удалено из избранного")
                                    }.onFailure {
                                        notify("Не удалось обновить избранное")
                                    }
                                }
                            },
                            logoSize = 96.dp,
                            isRadio = true,
                            hapticsEnabled = hapticsEnabled,
                            soundEnabled = soundEnabled,
                            activeRadio = active,
                            radioStatus = if (active) {
                                when {
                                    error != null -> "ошибка"
                                    playing -> "играет"
                                    else -> "пауза"
                                }
                            } else null,
                            radioTimer = if (active) formatShortTimer(displayedTimerMs / 1000L) else null,
                            radioBump = if (active && playing) radioBumpNonce else 0,
                            radioPlaying = active && playing,
                            onToggle = if (active) {
                                {
                                    val activeIndex = RADIO_STATIONS.indexOf(station)
                                    if (activeIndex >= 0) scope.launch { toggleActiveStation(activeIndex, station) }
                                }
                            } else null
                        )
                    }
                }
                if(sleepMenu){
                    Popup(alignment=Alignment.TopEnd,onDismissRequest={sleepMenu=false},properties=PopupProperties(focusable=true,dismissOnClickOutside=true,dismissOnBackPress=true)){
                        androidx.compose.material3.Surface(color=Color.Black.copy(alpha=.88f),shape=RoundedCornerShape(14.dp),modifier=Modifier.padding(top=52.dp,end=8.dp).width(240.dp)){
                            Column(Modifier.padding(10.dp).heightIn(max=380.dp).verticalScroll(rememberScrollState())){
                                Text("Таймер сна",color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                                    SleepOptions.forEach{(minutes,label)->
                                        val active=minutes==sleepMinutes&&sleepUntil>System.currentTimeMillis()
                                        Card(onClick={InteractionFeedback.vibrate(context,hapticsEnabled,42L,105);if(active)onCancelSleep()else onSleep(minutes);sleepMenu=false},modifier=Modifier.size(68.dp),colors=CardDefaults.cardColors(containerColor=if(active)Red else PanelAlt),shape=CircleShape,border=BorderStroke(1.dp,if(active)Red else Gray)){
                                            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(active)formatTimerCircle(sleepRemaining)else label,color=Color.White,fontSize=11.sp,textAlign=TextAlign.Center)}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (eqDialog) {
                    RadioEqualizerDialog(
                        settings = eqSettings,
                        onDismiss = { eqDialog = false },
                        onSave = { updated ->
                            scope.launch { store.setRadioEqualizer(updated) }
                            eqDialog = false
                        }
                    )
                }

                ScrollActionButton(
                    visible = showScrollAction,
                    direction = scrollDirection,
                    onClick = {
                        scope.launch {
                            showScrollAction = false
                            if (scrollDirection == -1) {
                                list.animateScrollToItem(0)
                            } else {
                                list.animateScrollToItem((orderedStations.lastIndex).coerceAtLeast(0))
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                )
            }

        }
    }
}

@Composable
private fun Tv(
    player: PlayerController,
    store: SettingsStore,
    restore: StreamItem?,
    dismissRestore: () -> Unit,
    saveLast: (StreamItem) -> Unit,
    back: () -> Unit,
    settings: () -> Unit,
    fullChanged: (Boolean) -> Unit,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = remember(context, store) { PlaylistRepository(context.applicationContext, store) }
    val list = rememberLazyListState()
    val network by rememberNetworkState()
    val error by player.error.collectAsState()
    val waiting by player.waitingForNetwork.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    var channels by remember { mutableStateOf(emptyList<StreamItem>()) }
    var favorites by remember { mutableStateOf(emptySet<String>()) }
    var health by remember { mutableStateOf(emptyMap<String, AvailabilityStatus>()) }
    var full by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var fallbackMessage by remember { mutableStateOf<String?>(null) }
    var playbackJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    KeepSystemBarsVisible()

    suspend fun loadSource() {
        val sourceKey = store.activeSourceKey()
        repo.loadCached(sourceKey)?.let { channels = it; loading = false }
        repo.loadSource(sourceKey)
            .onSuccess { channels = it; loading = false; repo.warmFallbacks() }
            .onFailure { loading = false; notify("Возникла проблема. Обсуждаем решения в Telegram.") }
    }

    LaunchedEffect(Unit) {
        favorites = store.favorites(Section.TV)
        val saved = store.scrollPosition(Section.TV)
        list.scrollToItem(saved.first.coerceAtLeast(0), saved.second)
        loadSource()
    }
    LaunchedEffect(Unit) {
        UsageTicker(
            store,
            Section.TV,
            channelIdProvider = { channels.getOrNull(selected)?.name },
            activeProvider = { full && player.isPlaying.value }
        ).run()
    }
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
            .collect { (first, offset) -> store.saveScrollPosition(Section.TV, first, offset) }
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) health = scanAvailability(channels.take(18), health)
    }
    LaunchedEffect(list.firstVisibleItemIndex, channels, health) {
        if (channels.isEmpty()) return@LaunchedEffect
        val first = list.firstVisibleItemIndex.coerceIn(0, channels.lastIndex)
        val end = minOf(channels.size, first + 20)
        if (end > first) {
            val pending = channels.subList(first, end).filter { health[it.url] == null }
            if (pending.isNotEmpty()) health = scanAvailability(pending, health)
        }
    }
    LaunchedEffect(error) { if (error != null && full) notify("Возникла проблема. Обсуждаем решения в Telegram.") }
    SearchAutoClose(searchOpen, query, stamp) {
        query = ""
        searchOpen = false
    }
    BackHandler(enabled = searchOpen && !full) {
        query = ""
        searchOpen = false
        stamp = System.currentTimeMillis()
    }
    LaunchedEffect(full) { fullChanged(full) }

    fun candidates(item: StreamItem): List<String> = listOf(item.url) + repo.fallbackUrlsFor(item.name)

    fun previousIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start
        repeat(channels.size - 1) {
            cursor = (cursor - 1 + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun nextIndex(start: Int): Int {
        if (channels.isEmpty()) return -1
        var cursor = start
        repeat(channels.size - 1) {
            cursor = (cursor + 1 + channels.size) % channels.size
            if (health[channels[cursor].url] != AvailabilityStatus.OFFLINE) return cursor
        }
        return -1
    }

    fun startPlayback(startIndex: Int) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            if (channels.isEmpty()) return@launch
            var cursor = startIndex.coerceIn(0, channels.lastIndex)
            repeat(channels.size) {
                selected = cursor
                fallbackMessage = null
                val result = player.playWithFallback(candidates(channels[cursor]))
                if (result >= 0) {
                    saveLast(channels[cursor])
                    return@launch
                }
                fallbackMessage = "Канал временно недоступен — переключаю на следующий"
                delay(3000L)
                cursor = nextIndex(cursor)
                if (cursor < 0) {
                    full = false
                    fallbackMessage = null
                    return@launch
                }
            }
            full = false
            fallbackMessage = null
        }
    }

    if (full && selected in channels.indices) {
        TvPlayer(
            player = player,
            channel = channels[selected],
            error = if (fallbackMessage == null) error else null,
            waiting = waiting || !network,
            isPlaying = isPlaying,
            noticeMessage = fallbackMessage,
            onBack = { playbackJob?.cancel(); full = false },
            onPrev = {
                val i = previousIndex(selected)
                if (i >= 0) startPlayback(i)
            },
            onNext = {
                val i = nextIndex(selected)
                if (i >= 0) startPlayback(i)
            },
            onPause = { player.toggle() },
            onResetZoom = { zoomByChannel.remove(channels[selected].key) },
            sleepRemaining = sleepRemaining,
            sleepUntil = sleepUntil,
            sleepMinutes = sleepMinutes,
            onSleep = onSleep,
            onCancelSleep = onCancelSleep
        )
        return
    }

    val ordered = channels.sortedWith(compareByDescending<StreamItem> { favorites.contains(it.key) }.thenBy { it.name.lowercase(Locale.ROOT) })
    val filtered = fuzzyFilter(ordered, query)

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            Header(
                title = "ТЕЛЕВИЗОР",
                timer = "",
                back = back,
                settings = settings,
                searchOpen = searchOpen,
                query = query,
                onSearchOpen = { searchOpen = true; stamp = System.currentTimeMillis() },
                onQuery = { query = it; stamp = System.currentTimeMillis() },
                onSearchClose = { query = ""; searchOpen = false; stamp = System.currentTimeMillis() },
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        repo.loadSource(store.activeSourceKey())
                            .onSuccess { channels = it; health = emptyMap(); repo.warmFallbacks() }
                            .onFailure { notify("Возникла проблема. Обсуждаем решения в Telegram.") }
                        refreshing = false
                    }
                }
            )
            restore?.let { item ->
                RestoreBanner(
                    item = item,
                    onContinue = {
                        val i = channels.indexOfFirst { it.url == item.url || it.name.equals(item.name, ignoreCase = true) }
                        if (i >= 0) { selected = i; full = true; startPlayback(i) }
                        else notify("Сохранённый канал больше не найден")
                    },
                    onClose = dismissRestore
                )
            }
            when {
                loading -> SkeletonList()
                filtered.isEmpty() -> if (query.isBlank()) EmptyPlaylistState() else EmptySearchState()
                else -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            repo.loadSource(store.activeSourceKey())
                                .onSuccess { channels = it; health = emptyMap(); repo.warmFallbacks() }
                                .onFailure { notify("Возникла проблема. Обсуждаем решения в Telegram.") }
                            refreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.key }) { channel ->
                            val offline = health[channel.url] == AvailabilityStatus.OFFLINE
                            val favorite = favorites.contains(channel.key)
                            ChannelRow(
                                item = channel,
                                favorite = favorite,
                                playing = false,
                                offline = offline,
                                onPlay = {
                                    val i = channels.indexOfFirst { it.key == channel.key }
                                    if (i >= 0) { selected = i; full = true; startPlayback(i) }
                                },
                                onFavorite = {
                                    scope.launch {
                                        runCatching {
                                            store.setFavorite(Section.TV, channel.key, !favorite)
                                            favorites = store.favorites(Section.TV)
                                            notify(if (!favorite) "Добавлено в избранное" else "Удалено из избранного")
                                        }.onFailure { notify("Не удалось обновить избранное") }
                                    }
                                },
                                logoSize = 56.dp,
                                preferRemoteLogo = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TvPlayer(
    player: PlayerController,
    channel: StreamItem,
    error: String?,
    waiting: Boolean,
    isPlaying: Boolean,
    noticeMessage: String?,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPause: () -> Unit,
    onResetZoom: () -> Unit,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit
) {
    val activity = LocalContext.current as? ComponentActivity
    val portrait = LocalConfiguration.current.screenWidthDp < LocalConfiguration.current.screenHeightDp
    val haptic = LocalHapticFeedback.current
    var controls by remember(channel.key) { mutableStateOf(true) }
    var taps by remember(channel.key) { mutableIntStateOf(0) }
    var zoom by remember(channel.key) { mutableFloatStateOf(zoomByChannel[channel.key] ?: 1f) }
    var sleepMenu by remember(channel.key) { mutableStateOf(false) }
    var sleepMenuToken by remember(channel.key) { mutableIntStateOf(0) }
    val controller = remember(activity) { activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) } }

    fun showBars() { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    fun hideBars() { controller?.hide(WindowInsetsCompat.Type.systemBars()) }

    LaunchedEffect(Unit) { hideBars() }
    LaunchedEffect(controls) {
        if (controls) {
            showBars()
            delay(5000L)
            controls = false
            hideBars()
        } else hideBars()
    }
    LaunchedEffect(sleepMenu, sleepMenuToken) {
        if (sleepMenu) {
            delay(5000L)
            sleepMenu = false
        }
    }
    DisposableEffect(Unit) { onDispose { showBars() } }
    BackHandler { if (sleepMenu) sleepMenu = false else onBack() }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(channel.key) {
                detectTapGestures(onTap = {
                    controls = true
                    taps++
                    if (taps >= 3) {
                        zoom = 1f
                        zoomByChannel.remove(channel.key)
                        onResetZoom()
                        taps = 0
                    }
                    showBars()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                })
            }
            .pointerInput(channel.key) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                    zoomByChannel[channel.key] = zoom
                    controls = true
                    showBars()
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player.player
                    setShutterBackgroundColor(AColor.BLACK)
                    setKeepContentOnPlayerReset(true)
                }
            }
        )
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring()) + fadeOut()
        ) {
            Box(Modifier.fillMaxSize().padding(top = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = { showBars(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = Red, modifier = Modifier.size(28.dp))
                    }
                    Row {
                        IconButton(onClick = { zoom = 1f; zoomByChannel.remove(channel.key); showBars(); onResetZoom() }) {
                            Icon(Icons.Default.Refresh, "Сбросить зум", tint = Red, modifier = Modifier.size(26.dp))
                        }
                        IconButton(onClick = { showBars(); sleepMenu = !sleepMenu; sleepMenuToken++ }) {
                            Icon(Icons.Default.AccessTime, "Таймер сна", tint = Red, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    IconButton(onClick = { showBars(); onPrev() }, Modifier.size(58.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Предыдущий канал", tint = Red, modifier = Modifier.size(42.dp))
                    }
                    IconButton(onClick = { showBars(); onPause() }, Modifier.size(78.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Пауза", tint = Red, modifier = Modifier.size(60.dp))
                    }
                    IconButton(onClick = { showBars(); onNext() }, Modifier.size(58.dp)) {
                        Icon(Icons.Default.SkipNext, "Следующий канал", tint = Red, modifier = Modifier.size(42.dp))
                    }
                }
                Text(
                    channel.name,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    color = Color.White,
                    fontSize = if (portrait) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (sleepMenu) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { sleepMenu = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(top = 56.dp, end = 8.dp).width(240.dp)
                ) {
                    Column(Modifier.padding(10.dp).heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                        Text("Таймер сна", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SleepOptions.forEach { (minutes, label) ->
                                val active = minutes == sleepMinutes && sleepUntil > System.currentTimeMillis()
                                Card(
                                    onClick = {
                                        if (active) onCancelSleep() else onSleep(minutes)
                                        sleepMenu = false
                                    },
                                    modifier = Modifier.size(68.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (active) Red else PanelAlt),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if (active) Red else Gray)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (active) formatTimerCircle(sleepRemaining) else label,
                                            color = if (active) Color(0xFFFFD6D6) else Color.White,
                                            fontSize = if (active) 11.sp else 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        if (sleepUntil > System.currentTimeMillis()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Нажмите активный таймер, чтобы отменить", color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        noticeMessage?.let { Text(it, Modifier.align(Alignment.Center).padding(horizontal = 24.dp), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
        if (waiting) Text("Сигнал утерян, перепроверьте подключение к сети", Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (error != null && noticeMessage == null) Text("Возникла проблема. Обсуждаем решения в Telegram.", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White, textAlign = TextAlign.Center)
    }
}
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Settings(
    store: SettingsStore,
    pip: Boolean,
    sleepRemaining: Long,
    sleepUntil: Long,
    sleepMinutes: Long,
    onBack: () -> Unit,
    onPip: (Boolean) -> Unit,
    onSleep: (Long) -> Unit,
    onCancelSleep: () -> Unit,
    onResetStats: (Section) -> Unit,
    onSource: (String) -> Unit,
    onDisclaimer: () -> Unit,
    notify: (String, Long) -> Unit,
    onResetAll: () -> Unit
) {
    val tvUsage by store.usageFlow(Section.TV).collectAsState(0L)
    val radioUsage by store.usageFlow(Section.RADIO).collectAsState(0L)
    val tvChannels by store.channelUsageFlow(Section.TV).collectAsState(emptyMap())
    val radioStations by store.channelUsageFlow(Section.RADIO).collectAsState(emptyMap())
    val hiddenChannels by store.hiddenChannelsFlow().collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    val settingsContext = LocalContext.current.applicationContext
    val tvRepo = remember(settingsContext, store) { TvPlaylistRepositoryV8(settingsContext, store) }
    var tvCatalog by remember { mutableStateOf(emptyList<StreamItem>()) }
    var activeSourceKey by remember { mutableStateOf(builtinSourceKey(0)) }
    var userPlaylists by remember { mutableStateOf(emptyList<UserPlaylist>()) }
    var sourceDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<UserPlaylist?>(null) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    val energyMode by store.energySavingModeFlow().collectAsState("AUTO")
    val systemPowerSave = rememberSystemPowerSave()
    val energySaving = energyMode == "ON" || (energyMode == "AUTO" && systemPowerSave)
    val hapticsEnabled by store.hapticsFlow().collectAsState(true)
    val soundEnabled by store.soundFeedbackFlow().collectAsState(true)
    val autoStart by store.autoStartFlow().collectAsState(false)
    var pinEnabled by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var pinDisableDialog by remember { mutableStateOf(false) }
    val pinStore = remember(settingsContext) { PinSecurityStore(settingsContext) }

    LaunchedEffect(Unit) { pinEnabled = pinStore.isEnabled() }

    KeepSystemBarsVisible()

    LaunchedEffect(Unit) {
        activeSourceKey = store.activeSourceKey()
        userPlaylists = store.userPlaylists()
    }
    LaunchedEffect(hiddenChannels, activeSourceKey) {
        if (hiddenChannels.isEmpty()) {
            tvCatalog = emptyList()
        } else {
            val cached = runCatching { tvRepo.cached(activeSourceKey) }.getOrNull()
            tvCatalog = cached?.items.orEmpty()
            if (tvCatalog.isEmpty()) {
                tvCatalog = runCatching { tvRepo.load(activeSourceKey).getOrNull()?.items.orEmpty() }.getOrDefault(emptyList())
            }
        }
    }

    val selectedSourceName = when {
        activeSourceKey.startsWith(BUILTIN_SOURCE_PREFIX) ->
            activeSourceKey.removePrefix(BUILTIN_SOURCE_PREFIX).toIntOrNull()
                ?.let { TV_SOURCES.getOrNull(it)?.name }
                ?: TV_SOURCES.firstOrNull()?.name.orEmpty()
        activeSourceKey.startsWith(USER_SOURCE_PREFIX) ->
            userPlaylists.firstOrNull { it.key == activeSourceKey }?.name ?: "Мой плейлист"
        else -> TV_SOURCES.firstOrNull()?.name.orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
                Text("НАСТРОЙКИ", fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("ТАЙМЕР СНА", color = Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SleepGrid(sleepRemaining, sleepUntil, sleepMinutes, onSleep, onCancelSleep)
                }
            }
        }
        item { DonationCard() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("СТАТИСТИКА", color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(8.dp))
                    Text("ТЕЛЕВИЗОР", color = Pink, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    UsageLine("Общее время просмотра Телевизора", tvUsage)
                    Spacer(Modifier.height(4.dp))
                    Text("ТОП-3 Активных канала:", color = Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TopStats(tvChannels)
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onResetStats(Section.TV) },
                        modifier = Modifier.height(40.dp).align(Alignment.Start),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)
                    ) { Text("Сбросить счётчик просмотров", fontSize = 13.sp, maxLines = 1, softWrap = false) }

                    Spacer(Modifier.height(12.dp))
                    Text("РАДИО", color = Pink, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    UsageLine("Общее время прослушивания Радио", radioUsage)
                    Spacer(Modifier.height(4.dp))
                    Text("ТОП-3 Активных станции:", color = Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TopStats(radioStations)
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onResetStats(Section.RADIO) },
                        modifier = Modifier.height(40.dp).align(Alignment.Start),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)
                    ) { Text("Сбросить счётчик прослушивания", fontSize = 13.sp, maxLines = 1, softWrap = false) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("ИСТОЧНИК ТВ-ПЛЕЙЛИСТА", color = Red, fontWeight = FontWeight.Bold)
                    Text(selectedSourceName, color = Orange, fontSize = 12.sp, maxLines = 2)
                    Button(
                        onClick = { sourceDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("ВЫБРАТЬ ИСТОЧНИК") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("КАРТИНКА В КАРТИНКЕ", fontSize = 16.sp)
                        Text("Сохраняется после перезапуска", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = pip,
                        onCheckedChange = onPip,
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
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ЭНЕРГОСБЕРЕЖЕНИЕ", color = if (energySaving) Red else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("AMOLED-чёрный фон и облегчённые анимации", fontSize = 11.sp, color = Gray)
                        }
                        Switch(
                            checked = energySaving,
                            onCheckedChange = { enabled ->
                                scope.launch { store.setEnergySavingMode(if (enabled) "ON" else "OFF") }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Режим: " + when (energyMode) {
                                "ON" -> "ВКЛ"
                                "OFF" -> "ВЫКЛ"
                                else -> "АВТО"
                            },
                            color = Gray, fontSize = 10.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { scope.launch { store.setEnergySavingMode("AUTO") } }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("АВТО", color = Red, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ТАКТИЛЬНАЯ ОБРАТНАЯ СВЯЗЬ", fontSize = 16.sp)
                        Text("Общий виброотклик для кнопок и действий",fontSize=11.sp,color=Gray)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { enabled -> scope.launch { store.setHapticsEnabled(enabled) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray)
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("АВТОЗАПУСК", fontSize = 16.sp)
                        Text("Запуск после включения. Экран не будет блокироваться, пока приложение активно",fontSize=11.sp,color=Gray)
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = { enabled -> scope.launch { store.setAutoStartEnabled(enabled) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray)
                    )
                }
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(14.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){Text("ЗВУКОВОЙ ОТКЛИК",fontSize=16.sp);Text("Короткий сигнал на ТВ/устройствах без вибрации",fontSize=11.sp,color=Gray)}
                    Switch(checked=soundEnabled,onCheckedChange={enabled->scope.launch{store.setSoundFeedbackEnabled(enabled)}},colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Red,uncheckedThumbColor=Color.White,uncheckedTrackColor=Gray))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ЗАЩИТА ПРИЛОЖЕНИЯ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PIN-код на вход", color = if (pinEnabled) Red else Color.White)
                            Text(if (pinEnabled) "PIN + отпечаток/лицо" else "Запрос не используется", fontSize = 11.sp, color = Gray)
                        }
                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) pinDialog = true else pinDisableDialog = true
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red, uncheckedThumbColor = Color.White, uncheckedTrackColor = Gray)
                        )
                    }
                }
            }
        }
        item {
            val sharePulse by rememberInfiniteTransition(label = "share-log-pulse").animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "share-log-pulse-value"
            )
            Card(
                onClick = {
                    scope.launch {
                        runCatching {
                            val file = LogExporter.export(settingsContext, store)
                            if (!file.exists() || file.length() <= 0L) {
                                error("ZIP-файл логов не создан")
                            }
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                settingsContext,
                                settingsContext.packageName + ".fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Radio.TV — логи приложения")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = ClipData.newRawUri("Radio.TV logs", uri)
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            settingsContext.startActivity(
                                Intent.createChooser(intent, "Поделиться логами")
                            )
                        }.onFailure {
                            Toast.makeText(
                                settingsContext,
                                "Не удалось экспортировать лог",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
                    Column(Modifier.weight(1f)) {
                        Text("ЭКСПОРТ ЛОГОВ", fontSize = 16.sp)
                        Text("ZIP-файл для отправки", fontSize = 11.sp, color = Gray)
                    }
                    Icon(
                        Icons.Default.Share,
                        "Поделиться логами",
                        tint = Red,
                        modifier = Modifier.graphicsLayer(
                            scaleX = sharePulse,
                            scaleY = sharePulse
                        )
                    )
                }
            }
        }
        if (hiddenChannels.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("СКРЫТЫЕ КАНАЛЫ", color = Red, fontWeight = FontWeight.Bold)
                        hiddenChannels.sorted().forEach { key ->
                            val item = tvCatalog.firstOrNull { it.key == key }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))) {
                                    if (!item?.logoUrl.isNullOrBlank()) {
                                        RemoteLogoImage(
                                            item!!.logoUrl!!,
                                            item.name,
                                            Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            Modifier.fillMaxSize().background(PanelAlt, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) { Text("NO", color = Gray, fontSize = 8.sp) }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item?.name ?: "Скрытый канал",
                                    Modifier.weight(1f),
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { scope.launch { store.removeHiddenChannel(key); notify("Канал восстановлен",3000L) } },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("Восстановить", color = Red, fontSize = 11.sp) }
                            }
                        }
                        Button(
                            onClick = { scope.launch { store.clearHiddenChannels(); notify("Канал восстановлен",3000L) } },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)
                        ) { Text("ВОССТАНОВИТЬ ВСЕ", fontSize = 12.sp) }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onResetAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PanelAlt)
            ) { Text("СБРОСИТЬ НАСТРОЙКИ ДО ЗАВОДСКИХ") }
        }
        item {
            OutlinedButton(onClick = onDisclaimer, modifier = Modifier.fillMaxWidth()) {
                Text("ОТКАЗ ОТ ОТВЕТСТВЕННОСТИ")
            }
        }
    }

    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false },
            title = { Text("ИСТОЧНИК ТВ-ПЛЕЙЛИСТА") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        item {
                            Text("ВСТРОЕННЫЕ ИСТОЧНИКИ", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(TV_SOURCES) { source ->
                            val i = TV_SOURCES.indexOf(source)
                            val key = builtinSourceKey(i)
                            Row(
                                Modifier.fillMaxWidth().combinedClickable(onClick = {
                                    activeSourceKey = key
                                    sourceDialog = false
                                    onSource(key)
                                }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = activeSourceKey == key,
                                    onClick = {
                                        activeSourceKey = key
                                        sourceDialog = false
                                        onSource(key)
                                    }
                                )
                                Text(
                                    source.name,
                                    Modifier.padding(6.dp).weight(1f),
                                    color = if (activeSourceKey == key) Orange else Color.White
                                )
                            }
                        }
                        if (userPlaylists.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("МОИ ПЛЕЙЛИСТЫ", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            items(userPlaylists, key = { it.key }) { playlist ->
                                Row(
                                    Modifier.fillMaxWidth().combinedClickable(onClick = {
                                        activeSourceKey = playlist.key
                                        sourceDialog = false
                                        onSource(playlist.key)
                                    }),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = activeSourceKey == playlist.key,
                                        onClick = {
                                            activeSourceKey = playlist.key
                                            sourceDialog = false
                                            onSource(playlist.key)
                                        }
                                    )
                                    Text(
                                        playlist.name,
                                        Modifier.padding(6.dp).weight(1f),
                                        color = if (activeSourceKey == playlist.key) Orange else Color.White,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { deleteCandidate = playlist }) {
                                        Icon(Icons.Default.Delete, "Удалить плейлист", tint = Red)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            newName = ""
                            newUrl = ""
                            sourceDialog = false
                            addDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) { Text("ДОБАВИТЬ СВОЙ ПЛЕЙЛИСТ") }
                }
            },
            confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("ЗАКРЫТЬ") } }
        )
    }

    if (addDialog) {
        val urlOk = newUrl.trim().startsWith("http://", true) || newUrl.trim().startsWith("https://", true)
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("ДОБАВИТЬ СВОЙ ПЛЕЙЛИСТ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название плейлиста") })
                    OutlinedTextField(value = newUrl, onValueChange = { newUrl = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL плейлиста") }, placeholder = { Text("https://.../playlist.m3u") })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        val url = newUrl.trim()
                        val valid = name.isNotBlank() && (url.startsWith("http://", true) || url.startsWith("https://", true))
                        if (!valid) return@TextButton
                        scope.launch {
                            val added = runCatching { store.addUserPlaylist(name, url) }.getOrDefault(false)
                            if (added) {
                                userPlaylists = store.userPlaylists()
                                addDialog = false
                            }
                        }
                    },
                    enabled = newName.isNotBlank() && urlOk
                ) { Text("ДОБАВИТЬ", color = Red) }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("ОТМЕНА") } }
        )
    }

    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Удаление плейлиста") },
            text = { Text("Удалить плейлист «${item.name}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            store.removeUserPlaylist(item.url)
                            userPlaylists = store.userPlaylists()
                            if (activeSourceKey == item.key) {
                                activeSourceKey = builtinSourceKey(0)
                                onSource(activeSourceKey)
                            }
                            deleteCandidate = null
                        }
                    }
                ) { Text("Удалить", color = Red) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Отмена") } }
        )
    }

    if (pinDialog) {
        PinSetupDialog(
            onDismiss = { pinDialog = false },
            onSave = { pin, hint ->
                if (pinStore.enable(pin, hint)) {
                    pinEnabled = true
                    pinDialog = false
                }
            }
        )
    }

    if (pinDisableDialog) {
        PinDisableDialog(
            onDismiss = { pinDisableDialog = false },
            onDisable = {
                pinStore.disable()
                pinEnabled = false
                pinDisableDialog = false
            }
        )
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN-код на вход") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("4 цифры") }
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it.take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Подсказка (до 15 символов)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin, hint) }, enabled = pin.length == 4) {
                Text("Сохранить", color = Red)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun PinDisableDialog(
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { PinSecurityStore(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отключить PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(4); error = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Текущий PIN") },
                isError = error
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (store.verify(pin)) onDisable() else error = true
                },
                enabled = pin.length == 4
            ) { Text("Отключить", color = Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun PinGate(
    activity: MainActivity,
    store: PinSecurityStore,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var biometricTried by rememberSaveable { mutableStateOf(false) }
    var biometricAvailable by rememberSaveable { mutableStateOf(false) }
    val feedbackContext=LocalContext.current
    val pinSettingsStore=remember(feedbackContext){SettingsStore(feedbackContext.applicationContext)}
    val pinHapticsEnabled by pinSettingsStore.hapticsFlow().collectAsState(true)
    val pinSoundEnabled by pinSettingsStore.soundFeedbackFlow().collectAsState(true)

    LaunchedEffect(Unit) {
        if (!biometricTried) {
            biometricTried = true
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            biometricAvailable=BiometricManager.from(context).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS
            if(biometricAvailable){
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }
                })
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Разблокировать")
                        .setSubtitle("Подтвердите вход")
                        .setNegativeButtonText("PIN-код")
                        .build()
                )
            }
        }
    }

    val redFlash by animateColorAsState(if(error)Color(0xFF5A1111).copy(alpha=.72f)else Color.Transparent,animationSpec=tween(220),label="pin-error-flash")
    LaunchedEffect(error){if(error){delay(1200L);error=false}}
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = Red, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text("Введите PIN-код", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { value ->
                    pin=value.filter(Char::isDigit).take(4);error=false
                    if(pin.length==4){
                        if(store.verify(pin))onUnlocked()else{error=true;InteractionFeedback.error(feedbackContext,pinHapticsEnabled);InteractionFeedback.beep(feedbackContext,pinSoundEnabled)}
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                label = { Text("4 цифры") },
                isError = error
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { Toast.makeText(context, "Подсказка: " + store.hint(), Toast.LENGTH_LONG).show() }
            ) { Text("Забыли PIN?", color = Red) }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={if(store.verify(pin))onUnlocked()else{error=true;InteractionFeedback.error(feedbackContext,pinHapticsEnabled);InteractionFeedback.beep(feedbackContext,pinSoundEnabled)}},enabled=pin.length==4,modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("ВОЙТИ")}
                if(biometricAvailable)IconButton(onClick={
                    val executor=ContextCompat.getMainExecutor(context)
                    val prompt=BiometricPrompt(activity,executor,object:BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){onUnlocked()}})
                    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Разблокировать").setSubtitle("Подтвердите вход").setNegativeButtonText("PIN-код").build())
                }){Icon(Icons.Default.Fingerprint,"Вход по отпечатку или лицу",tint=Red,modifier=Modifier.size(30.dp))}
            }
        }
        Box(Modifier.fillMaxSize().background(redFlash))
    }
}

@Composable
private fun UsageLine(label: String, seconds: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.LightGray, modifier = Modifier.weight(1f), maxLines = 2)
        Text(formatUsage(seconds), color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TopStats(stats: Map<String, Long>) {
    val top = stats.entries.filter { it.value > 0L }.sortedByDescending { it.value }.take(3)
    if (top.isEmpty()) {
        Text("Пока нет данных", color = Color.Gray, fontSize = 12.sp)
    } else {
        val rankColors = listOf(Color(0xFF00E676), Color(0xFFB388FF), Color(0xFFFFD740))
        top.forEachIndexed { i, entry ->
            val rankColor = rankColors.getOrElse(i) { Color.White }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = rankColor, fontWeight = FontWeight.Bold)) { append((i + 1).toString() + ". ") }
                        withStyle(SpanStyle(color = Color.LightGray)) { append(entry.key) }
                    },
                    Modifier.weight(1f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(formatUsage(entry.value), color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SleepGrid(
    remaining: Long,
    until: Long,
    duration: Long,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val options = SleepOptions
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (minutes, label) ->
            val active = minutes == duration && until > System.currentTimeMillis()
            val background by animateColorAsState(if (active) Red else PanelAlt, label = "sleep-$minutes")
            Card(
                onClick = { if (active) onCancel() else onSelect(minutes) },
                modifier = Modifier.size(66.dp),
                colors = CardDefaults.cardColors(containerColor = background),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (active) Red else Gray)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (active) formatTimerCircle(remaining) else label,
                        color = if (active) Color(0xFFFFD6D6) else Color.White,
                        fontSize = if (active) 11.sp else 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DonationCard() {
    val context = LocalContext.current
    val energySaving = LocalEnergySaving.current
    val hapticsEnabled by SettingsStore(context.applicationContext).hapticsFlow().collectAsState(true)
    val transition = rememberInfiniteTransition(label = "donation-heart-transition")
    val heartColorAnimated by transition.animateColor(
        initialValue = Color.White,
        targetValue = Color.White,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                Color.White at 0
                Color(0xFF42A5F5) at 1000
                Color(0xFFF06292) at 2000
                Color.White at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "donation-heart-color"
    )
    val heartColor=if(energySaving)Color.White else heartColorAnimated
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if(energySaving)1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "donation-heart-pulse"
    )
    val heartScale by animateFloatAsState(
        targetValue = pulse,
        animationSpec = tween(80, easing = FastOutSlowInEasing),
        label = "donation-heart-scale"
    )
    val starTransition = rememberInfiniteTransition(label = "donation-star")
    val starPulse by starTransition.animateFloat(
        initialValue = 1f,
        targetValue = if(energySaving)1f else 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "donation-star-pulse"
    )

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Red.copy(alpha = .4f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Favorite,
                    null,
                    tint = heartColor,
                    modifier = Modifier.graphicsLayer(scaleX = heartScale, scaleY = heartScale)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Red)) { append("ПОДДЕРЖАТЬ ПРОЕКТ ") }
                        withStyle(SpanStyle(color = Color(0xFFA5D6A7))) { append("USDT (TRC20)") }
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(WALLET, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Wallet", WALLET))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Копировать") }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.size(180.dp), color = Color.White, shape = RoundedCornerShape(8.dp)) {
                DonationQrImage(
                    contentDescription = "QR-код для пожертвований",
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    onClick = { InteractionFeedback.click(context,hapticsEnabled,false,false); openUrl(context, TELEGRAM_DONATION) },
                    modifier = Modifier.height(46.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF229ED9)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Telegram", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "★",
                            color = Color(0xFFFFD740),
                            fontSize = 22.sp,
                            modifier = Modifier.graphicsLayer(scaleX = starPulse, scaleY = starPulse)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationQrImage(
    contentDescription: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("qr_donate.png").use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
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
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun Disclaimer(onBack: () -> Unit) {
    val context = LocalContext.current
    var interactionToken by remember { mutableLongStateOf(0L) }
    var fingerDown by remember { mutableStateOf(false) }

    LaunchedEffect(interactionToken, fingerDown) {
        if (fingerDown) return@LaunchedEffect
        val token = interactionToken
        delay(10_000L)
        if (!fingerDown && token == interactionToken) onBack()
    }

    val scrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): Offset {
                interactionToken += 1L
                return Offset.Zero
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        fingerDown = true
                        interactionToken += 1L
                        tryAwaitRelease()
                        fingerDown = false
                        interactionToken += 1L
                    }
                )
            },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
                Text("Отказ от ответственности", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text(
                """Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.

При использовании сторонних M3U/M3U8-плейлистов и других внешних источников возможны изменения, блокировки, недоступность, ошибки воспроизведения и прекращение отдельных трансляций. Пользователь самостоятельно принимает решение об использовании таких источников и несёт ответственность за свои действия и соблюдение применимых правил и законодательства.

Приложение не хранит, не распространяет и не модифицирует транслируемый контент. Технически приложение только получает данные и пытается воспроизвести поток, предоставленный сторонним источником. Разработчик не контролирует содержание сторонних трансляций, их доступность, качество и стабильность и не гарантирует бесперебойную работу источников.

Функциональность, внешний вид, источники, способы загрузки и другие возможности приложения могут изменяться или отключаться без предварительного уведомления. Используя приложение, пользователь подтверждает, что понимает эти ограничения.

Пользование приложением разрешено только совершеннолетним. Используя приложение, вы подтверждаете свой возраст. Если вам нет 18 лет — позовите родителей.""",
                color = Color.LightGray,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
        item {
            Card(
                onClick = { openUrl(context, TELEGRAM) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Send, "Telegram", tint = Color(0xFF229ED9), modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Telegram", color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RadioEqualizerDialog(
    settings: SettingsStore.RadioEqualizerSettings,
    onDismiss: () -> Unit,
    onSave: (SettingsStore.RadioEqualizerSettings) -> Unit
) {
    var bass by remember(settings) { mutableFloatStateOf(settings.bass.toFloat()) }
    var mid by remember(settings) { mutableFloatStateOf(settings.mid.toFloat()) }
    var treble by remember(settings) { mutableFloatStateOf(settings.treble.toFloat()) }
    var preset by remember(settings) { mutableStateOf(settings.preset) }
    val presets = listOf("Flat","Rock","Pop","Jazz","Classical","Dance","Bass Boost","Treble Boost","Vocal","Hip-Hop","Lounge","Live","Virtualizer")
    fun applyPreset(name: String) {
        preset = name
        when (name) {
            "Flat" -> { bass = 0f; mid = 0f; treble = 0f }
            "Bass Boost" -> { bass = 1100f; mid = 0f; treble = 350f }
            "Rock" -> { bass = 900f; mid = 150f; treble = 800f }
            "Pop" -> { bass = 500f; mid = -100f; treble = 500f }
            "Jazz" -> { bass=400f;mid=-150f;treble=350f }
            "Classical" -> { bass=250f;mid=0f;treble=450f }
            "Dance" -> { bass=900f;mid=100f;treble=700f }
            "Treble Boost" -> { bass=100f;mid=0f;treble=950f }
            "Vocal" -> { bass=-150f;mid=550f;treble=250f }
            "Hip-Hop" -> { bass=850f;mid=50f;treble=500f }
            "Lounge" -> { bass=300f;mid=100f;treble=250f }
            "Live" -> { bass=650f;mid=150f;treble=500f }
            "Virtualizer" -> { bass=0f;mid=0f;treble=0f }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ЭКВАЛАЙЗЕР РАДИО") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bass", color = Gray, fontSize = 11.sp)
                Slider(value = bass, onValueChange = { bass = it; preset = "Flat" }, valueRange = -1500f..1500f)
                Text("Mid", color = Gray, fontSize = 11.sp)
                Slider(value = mid, onValueChange = { mid = it; preset = "Flat" }, valueRange = -1500f..1500f)
                Text("Treble", color = Gray, fontSize = 11.sp)
                Slider(value = treble, onValueChange = { treble = it; preset = "Flat" }, valueRange = -1500f..1500f)
                Text("Пресет", color = Gray, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    presets.forEach { name ->
                        AssistChip(onClick={applyPreset(name)},label={Text(name,color=if(preset==name)Color(0xFFB6FF5C)else Color.White,fontSize=9.sp)})
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(SettingsStore.RadioEqualizerSettings(bass.toInt(), mid.toInt(), treble.toInt(), preset, false))
            }) { Text("СОХРАНИТЬ", color = Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА") } }
    )
}

@Composable
private fun ChannelRow(
    item: StreamItem,
    favorite: Boolean,
    playing: Boolean,
    offline: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    logoSize: Dp,
    isRadio: Boolean = false,
    activeRadio: Boolean = false,
    radioStatus: String? = null,
    radioTimer: String? = null,
    onToggle: (() -> Unit)? = null,
    preferRemoteLogo: Boolean = false,
    radioBump: Int = 0,
    radioPlaying: Boolean = activeRadio,
    hapticsEnabled: Boolean = true,
    soundEnabled: Boolean = true
) {
    val energySaving=LocalEnergySaving.current
    val iconColorAnimated by animateColorAsState(if (favorite) Red else Color.White, label = "favorite-color")
    val iconColor=if(energySaving)if(favorite)Red else Color.White else iconColorAnimated
    val scaleAnimated by animateFloatAsState(if(favorite)1.14f else 1f,animationSpec=spring(),label="favorite-scale")
    val scale=if(energySaving)1f else scaleAnimated
    val feedbackContext=LocalContext.current
    Card(
        onClick = { InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,false); onPlay() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (favorite) Red.copy(alpha = .08f) else Panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoImage(item = item, size = logoSize, dimmed = offline, preferRemote = preferRemoteLogo, overlayText = radioTimer, activeRadio = activeRadio, isRadio = isRadio, radioBump = radioBump, radioPlaying = radioPlaying)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name.uppercase(Locale.ROOT),
                    color = if (offline) Gray else Color.White,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                radioStatus?.let {
                    Text(it, color = if (it == "ошибка") Red else Orange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                if (offline && !isRadio) Text("• временно недоступен", color = Gray, fontSize = 10.sp)
            }
            if (isRadio && activeRadio && onToggle != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,false); onFavorite() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Избранное",
                            tint = iconColor,
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    }
                    IconButton(
                        onClick = { InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,false); onToggle() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Старт / пауза", tint = Orange, modifier = Modifier.size(22.dp))
                    }
                }
            } else {
                IconButton(
                    onClick = { InteractionFeedback.click(feedbackContext,hapticsEnabled,soundEnabled,false); onFavorite() },
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                ) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное", tint = iconColor)
                }
            }
        }
    }
}
@Composable
private fun LogoImage(
    item: StreamItem,
    size: Dp,
    dimmed: Boolean = false,
    preferRemote: Boolean = false,
    overlayText: String? = null,
    activeRadio: Boolean = false,
    isRadio: Boolean = false,
    radioBump: Int = 0,
    radioPlaying: Boolean = activeRadio
) {
    val context = LocalContext.current
    val energySaving = LocalEnergySaving.current
    val resourceName = localLogoName(item.name)
    val resourceId = remember(resourceName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    val localRadioImage = remember(item.name, isRadio) {
        if (isRadio) RadioLogoAssets.image(item.name) else null
    }

    val pulseTransition = rememberInfiniteTransition(label = "radio-logo-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radio-logo-pulse-value"
    )

    var bumping by remember(item.name) { mutableStateOf(false) }
    LaunchedEffect(radioBump) {
        if (radioBump > 0 && radioPlaying) {
            bumping = true
            delay(170L)
            bumping = false
        }
    }
    val targetScale = if (activeRadio && !energySaving) 1.05f + (if (radioPlaying) (0.03f * pulse) else 0f) + if (bumping) 0.08f else 0f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(),
        label = "active-logo-scale"
    )
    val borderColor = when {
        !activeRadio -> Color.Transparent
        radioPlaying && !energySaving -> Red.copy(alpha = 0.45f + (0.25f * pulse))
        else -> Red.copy(alpha = 0.70f)
    }

    Box(
        Modifier.size(size).graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.BottomCenter
    ) {
        val logoContainerModifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (activeRadio) Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
        Box(logoContainerModifier, contentAlignment = Alignment.Center) {
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(if (activeRadio) 3.dp else 2.dp)
                .clip(RoundedCornerShape(9.dp))
                .alpha(if (dimmed) .4f else 1f)
            val remoteUrl = item.logoUrl ?: item.epgLogoUrl
            if (localRadioImage != null) {
                Image(
                    bitmap = localRadioImage,
                    contentDescription = item.name,
                    modifier = contentModifier,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else if (isRadio) {
                Box(
                    contentModifier.background(Color(0xFF2A2A2A), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            } else if (preferRemote && !remoteUrl.isNullOrBlank()) {
                RemoteLogoImage(remoteUrl, item.name, contentModifier)
            } else if (resourceId != 0) {
                Image(
                    painter = painterResource(resourceId),
                    contentDescription = item.name,
                    modifier = contentModifier,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Box(
                    contentModifier.background(Color(0xFF2A2A2A), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NO Image", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }

        overlayText?.let {
            val energySaving = LocalEnergySaving.current
            val timerText = it
            Text(
                timerText,
                color = Red,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                style = TextStyle(
                    shadow = Shadow(Color.Black.copy(alpha = if (energySaving) .35f else .55f), blurRadius = 2.2f)
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

private fun averageTimerOutline(name: String): Color {
    val bitmap = RadioLogoAssets.bitmap(name) ?: return Color.White
    val startY = (bitmap.height * 0.62f).toInt().coerceAtLeast(0)
    var sum = 0.0
    var count = 0
    val stepX = (bitmap.width / 24).coerceAtLeast(1)
    val stepY = (bitmap.height / 12).coerceAtLeast(1)
    var y = startY
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val c = bitmap.getPixel(x, y)
            sum += 0.2126 * android.graphics.Color.red(c) +
                0.7152 * android.graphics.Color.green(c) +
                0.0722 * android.graphics.Color.blue(c)
            count++
            x += stepX
        }
        y += stepY
    }
    return if (count > 0 && sum / count > 170.0) Color.Black else Color.White
}


private suspend fun scanRadioAvailability(items: List<StreamItem>): Map<String, AvailabilityStatus> = coroutineScope {
    val result = ConcurrentHashMap<String, AvailabilityStatus>()
    val semaphore = Semaphore(4)
    items.map { item ->
        launch(Dispatchers.IO) {
            semaphore.withPermit {
                val status = runCatching {
                    logoHttpClient.newCall(
                        Request.Builder()
                            .url(item.url)
                            .header("Range", "bytes=0-1024")
                            .header("User-Agent", "Radio.TV/3.3")
                            .build()
                    ).execute().use { response ->
                        if (response.isSuccessful || response.code == 206 || response.code == 416) AvailabilityStatus.ONLINE
                        else AvailabilityStatus.OFFLINE
                    }
                }.getOrDefault(AvailabilityStatus.OFFLINE)
                result[item.url] = status
            }
        }
    }.forEach { it.join() }
    result.toMap()
}

@Composable
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
}

private fun localLogoName(name: String): String = when (name.uppercase(Locale.ROOT)) {
    "RECORD" -> "logo_record"
    "CHOCOLATE" -> "logo_chocolate"
    "ЭНЕРДЖИ" -> "logo_energy"
    "ULTRA" -> "logo_ultra"
    "КАЛЬЯН РЭП" -> "logo_kalyan"
    "PIRATE STATION" -> "logo_pirate"
    "VOCAL DRUM" -> "logo_vocal"
    "CHILL HOUSE" -> "logo_chill"
    "PSY TRANCE" -> "logo_psy"
    "METALCORE" -> "logo_metalcore"
    "RELAX" -> "logo_relax"
    "COMEDY CLUB" -> "logo_comedy"
    "АВТОРАДИО" -> "logo_autoradio"
    "ЮГ МОЛОДОЙ" -> "logo_yug"
    "ЕВРОПА ПЛЮС" -> "logo_europa"
    "РЕТРО FM" -> "logo_retro"
    "ХИТ FM" -> "logo_hit"
    "НАШЕ РАДИО" -> "logo_nashe"
    "DATASET [AI]" -> "logo_dataset_ai"
    "ГАМАЮН" -> "logo_gamaun"
    else -> "logo_fallback"
}

@Composable
private fun RestoreBanner(item: StreamItem, onContinue: () -> Unit, onClose: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PanelAlt),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Продолжить [${item.name}]?", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onContinue, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                    Text("Продолжить", maxLines = 1)
                }
                OutlinedButton(onClick = onClose, Modifier.weight(1f)) { Text("Закрыть", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun NetworkBanner() {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF2B1B1B)).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WifiOff, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Сигнал утерян, перепроверьте подключение к сети", color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
private fun NoticeBanner(notice: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = notice != null,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring()) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring()) + fadeOut()
    ) {
        Card(
            Modifier.fillMaxWidth().padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = PanelAlt),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(notice.orEmpty(), Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { openUrl(context, TELEGRAM); onDismiss() }) { Text("Перейти", color = Red) }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text("Ничего не найдено", color = Color.Gray)
    }
}

@Composable
private fun EmptyPlaylistState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text("Плейлист пуст", color = Color.Gray)
    }
}

@Composable
private fun SkeletonList() {
    LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(8) {
            Box(Modifier.fillMaxWidth().height(82.dp).background(PanelAlt, RoundedCornerShape(12.dp)))
        }
    }
}

@Composable
private fun KeepSystemBarsVisible() {
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity) {
        val controller = activity?.let { WindowInsetsControllerCompat(it.window, it.window.decorView) }
        controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun formatShortTimer(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    return "%02d:%02d".format(total / 60L, total % 60L)
}

private fun formatTime(seconds: Long): String {
    return if (seconds < 3600L) {
        "%02d:%02d".format(seconds / 60L, seconds % 60L)
    } else {
        "%02d:%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
    }
}

private fun formatUsage(seconds: Long): String = "${seconds / 3600L} ч ${(seconds / 60L) % 60L} мин"

private fun formatTimerCircle(milliseconds: Long): String {
    val total = max(0L, milliseconds / 1000L)
    val hours = total / 3600L
    val minutes = (total / 60L) % 60L
    val seconds = total % 60L
    return if (hours > 0L) "%02d:%02d".format(hours, minutes) else "%02d:%02d".format(minutes, seconds)
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun networkNow(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    return manager.allNetworks.any { network ->
        manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
}

@Composable
private fun rememberNetworkState(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(initialValue = networkNow(context)) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        value = networkNow(context)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = networkNow(context) }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        awaitDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}

private suspend fun scanAvailability(
    items: List<StreamItem>,
    current: Map<String, AvailabilityStatus>
): Map<String, AvailabilityStatus> {
    if (items.isEmpty()) return current
    val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
    val semaphore = Semaphore(3)
    val output = ConcurrentHashMap<String, AvailabilityStatus>()
    kotlinx.coroutines.coroutineScope {
        items.map { item ->
            launch(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    output[item.url] = runCatching {
                        client.newCall(
                            Request.Builder().url(item.url)
                                .header("Range", "bytes=0-1")
                                .header("User-Agent", "TV-Radio-Online/5.0")
                                .get().build()
                        ).execute().use {
                            if (it.isSuccessful || it.code in 300..399 || it.code == 416) AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE
                        }
                    }.getOrDefault(AvailabilityStatus.OFFLINE)
                } finally { semaphore.release() }
            }
        }.forEach { it.join() }
    }
    return current + output
}
internal class UsageTicker(
    private val store: SettingsStore,
    private val section: Section,
    private val channelIdProvider: () -> String?,
    private val activeProvider: () -> Boolean
) {
    suspend fun run() {
        val pending = mutableMapOf<String, Long>()
        var totalPending = 0L
        var lastFlush = System.currentTimeMillis()
        try {
            while (true) {
                delay(1000L)
                if (activeProvider()) {
                    val id = channelIdProvider()?.trim().orEmpty()
                    if (id.isNotBlank()) {
                        pending[id] = (pending[id] ?: 0L) + 1L
                        totalPending++
                    }
                }
                if (System.currentTimeMillis() - lastFlush >= 5000L) {
                    if (totalPending > 0L) {
                        store.addUsageSeconds(section, totalPending)
                        store.addChannelUsage(section, pending.toMap())
                    }
                    pending.clear()
                    totalPending = 0L
                    lastFlush = System.currentTimeMillis()
                }
            }
        } finally {
            if (totalPending > 0L) {
                store.addUsageSeconds(section, totalPending)
                store.addChannelUsage(section, pending.toMap())
            }
        }
    }
}