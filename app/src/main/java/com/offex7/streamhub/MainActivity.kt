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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var variant by remember { mutableIntStateOf(0) }
    val glyphs = listOf("🢁", "🔝", "🔼")

    LaunchedEffect(visible) {
        if (!visible) {
            variant = 0
            return@LaunchedEffect
        }
        while (visible) {
            delay(700L)
            variant = (variant + 1) % glyphs.size
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = .82f, animationSpec = tween(220)),
        exit = fadeOut(tween(220)) + scaleOut(targetScale = .82f, animationSpec = tween(220))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp).background(Red, CircleShape)
        ) {
            androidx.compose.animation.Crossfade(
                targetState = variant,
                animationSpec = tween(220),
                label = "scroll-arrow-glyph"
            ) { i ->
                Text(glyphs[i], color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
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