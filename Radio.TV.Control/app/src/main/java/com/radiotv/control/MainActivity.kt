package com.radiotv.control

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.provider.Settings
import android.net.Uri
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.radiotv.control.ui.RadioTvPalette
import com.radiotv.control.ui.RemoteFeatureAction
import com.radiotv.control.ui.radioTvColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiotv.control.companion.CompanionConnectionStatus
import com.radiotv.control.companion.RadioTvCompanionClient
import com.radiotv.control.cast.CastMedia
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.cast.DlnaDevice
import com.radiotv.control.cast.LocalNetworkDeviceDiscovery
import com.radiotv.control.cast.LocalMediaHttpServer
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.AdbRemoteDevice
import com.radiotv.control.core.AdbRemoteStatus
import com.radiotv.control.core.AndroidTvVoiceInputController
import com.radiotv.control.core.VoiceInputStatus
import com.radiotv.control.core.AirMouseStatus
import com.radiotv.control.core.GyroAirMouseController
import com.radiotv.control.core.BluetoothHidController
import com.radiotv.control.core.BluetoothHidStatus
import com.radiotv.control.core.DiscoveredRemoteDevice
import com.radiotv.control.core.RemoteDeviceType
import com.radiotv.control.core.RemoteStatus
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.core.InstalledTvApp
import com.radiotv.control.core.OtherTvRemoteController
import com.radiotv.control.core.OtherTvRemoteStatus
import com.radiotv.control.ui.TouchpadSurface
import com.radiotv.control.ui.TvRemotePad
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {
    private var adbSetupReceiver: BroadcastReceiver? = null
    private var adbSetupDialog: AlertDialog? = null
    private var promptAfterDeviceInfo = false
    private val remote: AndroidTvRemoteV2Transport by inject()
    private val adbRemote: AdbRemoteDevice by inject()
    private val bluetoothHid: BluetoothHidController by inject()
    private val airMouse: GyroAirMouseController by inject()
    private val voiceInput: AndroidTvVoiceInputController by inject()
    private val otherTvRemote: OtherTvRemoteController by inject()
    private val dlnaCast: DlnaCastController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = if (isTelevision() || resources.configuration.smallestScreenWidthDp >= 600) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val initialUiScreen = intent.getStringExtra("ui-screen") ?: "remote"
        setContent { RadioTvControlScreen(remote, adbRemote, bluetoothHid, airMouse, voiceInput, otherTvRemote, dlnaCast, initialUiScreen) }
        startTvCompanionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (promptAfterDeviceInfo && isDeveloperOptionsEnabled()) {
            promptAfterDeviceInfo = false
            window.decorView.post { showAdbSetupDialog("Отладка ADB нужна для отправки команд телевизору.") }
        }
    }

    private fun startTvCompanionIfNeeded() {
        if (!isTelevision()) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                showAdbSetupDialog(intent?.getStringExtra(RadioTvCompanionService.EXTRA_REASON).orEmpty())
            }
        }
        adbSetupReceiver = receiver
        val filter = IntentFilter(RadioTvCompanionService.ACTION_ADB_REQUIRED)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver, filter)
        runCatching { startForegroundService(Intent(this, RadioTvCompanionService::class.java)) }
            .onFailure { showAdbSetupDialog(it.message.orEmpty()) }
    }

    private fun isTelevision(): Boolean {
        val uiMode = getSystemService(UiModeManager::class.java)
        return uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    private fun isDeveloperOptionsEnabled(): Boolean =
        runCatching { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 }
            .getOrDefault(false)

    private fun showAdbSetupDialog(reason: String) {
        if (isFinishing || isDestroyed || adbSetupDialog?.isShowing == true) return
        val developerOptionsEnabled = isDeveloperOptionsEnabled()
        val message = buildString {
            append("Для управления телевизором без пульта необходимо включить отладку. Это безопасно и не требует Root-прав.")
            if (!developerOptionsEnabled) append("\n\nСначала включите режим разработчика: откройте сведения об устройстве и нажмите 7 раз на «Номер сборки».")
            else append("\n\nВ параметрах разработчика включите доступную на вашем телевизоре отладку ADB.")
            if (reason.isNotBlank()) append("\n\nПодробности: ").append(reason.take(180))
        }
        adbSetupDialog = AlertDialog.Builder(this)
            .setTitle("Настройка ADB")
            .setMessage(message)
            .setNegativeButton("Позже", null)
            .setPositiveButton("Перейти в настройки") { _, _ ->
                if (developerOptionsEnabled) {
                    runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                        .onFailure { startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
                } else {
                    promptAfterDeviceInfo = true
                    runCatching { startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
                }
            }
            .create()
        adbSetupDialog?.show()
    }

    override fun onDestroy() {
        adbSetupReceiver?.let { receiver -> runCatching { unregisterReceiver(receiver) } }
        adbSetupReceiver = null
        adbSetupDialog?.dismiss()
        adbSetupDialog = null
        if (isFinishing) {
            remote.close()
            adbRemote.close()
            bluetoothHid.close()
            airMouse.close()
            voiceInput.close()
            otherTvRemote.close()
        }
        super.onDestroy()
    }
}

@Composable
internal fun RadioTvControlScreen(remote: AndroidTvRemoteV2Transport, adbRemote: AdbRemoteDevice, bluetoothHid: BluetoothHidController, airMouse: GyroAirMouseController, voiceInput: AndroidTvVoiceInputController, otherTvRemote: OtherTvRemoteController, dlnaCast: DlnaCastController, initialUiScreen: String) {
    val context = LocalContext.current
    val companionClient = remember(context) { RadioTvCompanionClient(context) }
    val companionStatus by companionClient.status.collectAsStateWithLifecycle(initialValue = CompanionConnectionStatus.Searching)
    val companionConnected = companionStatus is CompanionConnectionStatus.Connected
    var companionWasConnected by remember { mutableStateOf(false) }
    LaunchedEffect(companionClient) { companionClient.start() }
    DisposableEffect(companionClient) { onDispose { companionClient.close() } }
    LaunchedEffect(companionStatus) {
        val active = companionStatus as? CompanionConnectionStatus.Connected
        if (active != null) context.setLastConnectedDevice(active.host, "Radio.TV приложение", "COMPANION")
    }
    val hapticFeedback = LocalHapticFeedback.current
    val vibrator = remember(context) { context.getSystemService(VibratorManager::class.java)?.defaultVibrator }
    val feedbackSettings by context.radioTvFeedbackSettings.collectAsStateWithLifecycle(initialValue = RadioTvFeedbackSettings())
    val clickTone = remember(context) { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }
    DisposableEffect(clickTone) { onDispose { clickTone.release() } }
    val sendUiFeedback: (String) -> Unit = { label ->
        if (feedbackSettings.vibrationEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            runCatching {
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }
        if (feedbackSettings.clickSoundEnabled) {
            clickTone.startTone(ToneGenerator.TONE_PROP_BEEP, 22)
        }
    }
    val status by remote.status.collectAsStateWithLifecycle()
    val adbStatus by adbRemote.status.collectAsStateWithLifecycle()
    val bluetoothStatus by bluetoothHid.status.collectAsStateWithLifecycle()
    val airMouseStatus by airMouse.status.collectAsStateWithLifecycle()
    val voiceStatus by voiceInput.status.collectAsStateWithLifecycle()
    var installedApps by remember { mutableStateOf<List<InstalledTvApp>>(emptyList()) }
    val otherRemoteStatus by otherTvRemote.status.collectAsStateWithLifecycle()
    LaunchedEffect(otherRemoteStatus) {
        val active = otherRemoteStatus as? OtherTvRemoteStatus.Connected
        if (active != null) {
            context.setLastConnectedDevice(active.ip, active.type.label, active.type.name)
            installedApps = runCatching { otherTvRemote.listInstalledApps() }.getOrDefault(emptyList())
        } else {
            installedApps = emptyList()
        }
    }
    LaunchedEffect(adbStatus) {
        val active = adbStatus as? AdbRemoteStatus.Connected
        if (active != null) {
            context.setLastConnectedDevice(active.host, "Kivi TV (ADB)", RemoteDeviceType.KIVI_ADB.name)
            installedApps = runCatching { adbRemote.listInstalledApps() }.getOrDefault(emptyList())
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var snackbarRequestId by remember { mutableStateOf(0) }
    LaunchedEffect(feedbackSettings.notificationsEnabled) {
        if (!feedbackSettings.notificationsEnabled) {
            snackbarRequestId += 1
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    val showNotification: (String) -> Unit = { message ->
        if (feedbackSettings.notificationsEnabled && message.isNotBlank()) {
            snackbarRequestId += 1
            val requestId = snackbarRequestId
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val snackbarJob = launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                        duration = androidx.compose.material3.SnackbarDuration.Indefinite
                    )
                }
                delay(3_000L)
                if (snackbarRequestId == requestId) snackbarHostState.currentSnackbarData?.dismiss()
                snackbarJob.cancel()
            }
        }
    }
    val discovery = remember(context) { LocalNetworkDeviceDiscovery(context) }
    var host by rememberSaveable { mutableStateOf("") }
    var pairedHost by rememberSaveable { mutableStateOf(remote.pairedHost().orEmpty()) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Телефон и телевизор должны быть в одной сети Wi‑Fi.") }
    var devices by remember { mutableStateOf<List<DiscoveredRemoteDevice>>(emptyList()) }
    var selectedDeviceName by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(status) {
        val active = status as? RemoteStatus.Connected
        if (active != null) {
            pairedHost = remote.pairedHost().orEmpty().ifBlank { active.host }
            context.setLastConnectedDevice(active.host, selectedDeviceName.ifBlank { "Android TV" }, RemoteDeviceType.ANDROID_TV.name)
        }
    }
    var scanning by remember { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    var showTouchpad by rememberSaveable { mutableStateOf(false) }
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var showDigitPad by rememberSaveable { mutableStateOf(false) }
    var keyboardText by rememberSaveable { mutableStateOf("") }
    var muteActive by rememberSaveable { mutableStateOf(false) }
    val keyboardFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var castMessage by rememberSaveable { mutableStateOf("Нажмите «Обновить», чтобы найти DLNA-приёмники.") }
    var duplicationExpanded by rememberSaveable { mutableStateOf(initialUiScreen == "cast") }
    var castScanning by remember { mutableStateOf(false) }
    var castDevices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var selectedCastRenderer by remember { mutableStateOf<DlnaDevice?>(null) }
    var selectedTab by rememberSaveable {
        mutableStateOf(when (initialUiScreen) {
            "pairing" -> RadioTvTab.SETTINGS
            "settings" -> RadioTvTab.SETTINGS
            "digital" -> RadioTvTab.REMOTE
            "cast" -> RadioTvTab.CONTROL
            else -> RadioTvTab.REMOTE
        })
    }
    LaunchedEffect(initialUiScreen) {
        if (initialUiScreen == "digital") showDigitPad = true
    }
    BackHandler(enabled = showKeyboard || showTouchpad || showDigitPad || selectedTab != RadioTvTab.REMOTE) {
        when {
            showKeyboard -> { showKeyboard = false; keyboardController?.hide() }
            showDigitPad -> showDigitPad = false
            showTouchpad -> showTouchpad = false
            else -> selectedTab = RadioTvTab.REMOTE
        }
    }
    var castMedia by remember { mutableStateOf<CastMedia?>(null) }
    var castPlaybackStatus by rememberSaveable { mutableStateOf("Остановлено") }
    var pendingLocalNetworkAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPickedUri by remember { mutableStateOf<Uri?>(null) }
    val localMediaServer = remember(context) { LocalMediaHttpServer(context) }
    DisposableEffect(localMediaServer) {
        onDispose { localMediaServer.close() }
    }

    // Picker callbacks only stage the content URI. The local-network permission is requested
    // afterwards, because merely opening the system file picker does not need LAN access.
    val handlePickedMedia: (Uri?) -> Unit = { uri ->
        if (uri == null) {
            castMessage = "Выбор файла отменён."
        } else {
            pendingPickedUri = uri
        }
    }

    val processPickedMedia: (Uri) -> Unit = { uri ->
        scope.launch {
            castMessage = "Готовим файл для передачи на телевизор…"
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val media = withContext(Dispatchers.IO) { localMediaServer.start(uri) }
                val extension = media.title.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                val mimeType = media.mimeType.substringBefore(';').lowercase(java.util.Locale.ROOT)
                val isPlaylist = extension in setOf("m3u8", "mpd") ||
                    mimeType.contains("mpegurl") || mimeType.contains("dash+xml")
                if (isPlaylist) {
                    withContext(Dispatchers.IO) { localMediaServer.close() }
                    error("Плейлисты HLS/DASH и DRM-защищённый контент не поддерживаются. Выберите локальный незащищённый файл.")
                }
                media
            }.onSuccess { media ->
                castMedia = media
                castMessage = "Подготовлено: ${media.title}. Выберите DLNA-приёмник."
                val renderer = selectedCastRenderer
                if (renderer != null) {
                    castMessage = "Подготовлено: ${media.title}. Начинаем трансляцию…"
                    runCatching { dlnaCast.play(renderer, media) }
                        .onSuccess {
                            castPlaybackStatus = "Воспроизведение"
                            castMessage = "Передаём «${media.title}» на ${renderer.friendlyName ?: renderer.server ?: renderer.location}."
                        }
                        .onFailure {
                            castPlaybackStatus = "Ошибка"
                            castMessage = it.message ?: "Не удалось начать DLNA-воспроизведение."
                        }
                } else if (castDevices.isEmpty()) {
                    castMessage = "Файл подготовлен, но DLNA-приёмник не найден. Убедитесь, что телефон и телевизор в одной Wi-Fi сети и ТВ поддерживает DLNA/UPnP."
                } else {
                    castMessage = "Подготовлено: ${media.title}. Выберите телевизор в списке DLNA-приёмников."
                }
            }.onFailure {
                castPlaybackStatus = "Ошибка"
                castMessage = it.message ?: "Не удалось подготовить выбранный файл."
            }
        }
    }

    // Photo/video and arbitrary files use ACTION_OPEN_DOCUMENT; audio uses ACTION_GET_CONTENT.
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedMedia(uri) }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> handlePickedMedia(uri) }
    val codeRequested = status is RemoteStatus.AwaitingCode
    val wifiConnected = status is RemoteStatus.Connected
    val otherConnected = otherRemoteStatus is OtherTvRemoteStatus.Connected
    val adbConnected = adbStatus is AdbRemoteStatus.Connected
    val connected = wifiConnected || otherConnected || adbConnected || companionConnected
    val hidConnected = bluetoothStatus is BluetoothHidStatus.Connected
    // Two protocol sessions may remain connected at once. Route commands to the device selected
    // in Settings instead of accidentally sending keys to an older Android TV session.
    val connectedWifiHost = (status as? RemoteStatus.Connected)?.host
    val connectedOtherIp = (otherRemoteStatus as? OtherTvRemoteStatus.Connected)?.ip
    val connectedAdbHost = (adbStatus as? AdbRemoteStatus.Connected)?.host
    val selectedWifiConnected = wifiConnected &&
        (host.isBlank() || connectedWifiHost.equals(host, ignoreCase = true))
    val selectedOtherConnected = otherConnected &&
        (host.isBlank() || connectedOtherIp.equals(host, ignoreCase = true))
    val selectedAdbConnected = adbConnected &&
        (host.isBlank() || connectedAdbHost.equals(host, ignoreCase = true))

    val startPairing: (String) -> Unit = { target ->
        scope.launch {
            host = target
            statusMessage = "Подключаемся к сервису сопряжения…"
            runCatching { remote.beginPairing(target) }
                .onSuccess { statusMessage = "Код появится на телевизоре. Введите его ниже." }
                .onFailure { statusMessage = it.message ?: "Не удалось начать сопряжение." }
        }
    }

    val runDiscovery: () -> Unit = {
        scope.launch {
            scanning = true
            statusMessage = "Ищем устройства в локальной сети…"
            runCatching { discovery.discover() }
                .onSuccess { result ->
                    devices = result
                    statusMessage = if (result.isEmpty()) {
                        "Устройства пока не найдены. Проверьте Wi‑Fi и повторите поиск."
                    } else {
                        "Найдено устройств: " + result.size
                    }
                }
                .onFailure { statusMessage = it.message ?: "Ошибка поиска устройств." }
            scanning = false
        }
    }

    val discoverCastDevices: () -> Unit = {
        scope.launch {
            castScanning = true
            castMessage = "Ищем DLNA / UPnP renderers…"
            runCatching { dlnaCast.discoverRenderers() }
                .onSuccess { result ->
                    castDevices = result
                    if (selectedCastRenderer?.location !in result.map { it.location }) selectedCastRenderer = null
                    castMessage = if (result.isEmpty()) {
                        "DLNA-приёмники с AVTransport не найдены. ТВ должен поддерживать UPnP MediaRenderer."
                    } else {
                        "Найдено DLNA-приёмников: " + result.size
                    }
                }
                .onFailure { castMessage = it.message ?: "Ошибка DLNA-поиска." }
            castScanning = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingAction = pendingLocalNetworkAction
        pendingLocalNetworkAction = null
        if (granted) {
            companionClient.start()
            val uiMode = context.getSystemService(UiModeManager::class.java)
            val isTv = uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            if (isTv) runCatching {
                context.startService(Intent(context, RadioTvCompanionService::class.java).setAction(RadioTvCompanionService.ACTION_RETRY_MDNS))
            }
            (pendingAction ?: runDiscovery).invoke()
        } else {
            statusMessage = "Для поиска и трансляции в локальной сети разрешите доступ или введите IP вручную."
            if (pendingAction != null) {
                castMessage = "Для DLNA-трансляции разрешите доступ к локальной сети."
                pendingPickedUri = null
            }
        }
    }

    val runWithLocalNetworkPermission: ((() -> Unit) -> Unit) = { action ->
        val needsPermission = Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingLocalNetworkAction = action
            permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        } else {
            action()
        }
    }

    val refreshCastDevices: () -> Unit = {
        runWithLocalNetworkPermission { discoverCastDevices() }
    }

    val launchMediaPicker: (Array<String>) -> Unit = { mimeTypes ->
        mediaPicker.launch(mimeTypes)
    }

    val launchAudioPicker: () -> Unit = {
        audioPicker.launch("audio/*")
    }

    androidx.compose.runtime.LaunchedEffect(pendingPickedUri) {
        val uri = pendingPickedUri ?: return@LaunchedEffect
        runWithLocalNetworkPermission {
            pendingPickedUri = null
            scope.launch {
                castScanning = true
                castMessage = "Ищем DLNA-приёмники…"
                runCatching { dlnaCast.discoverRenderers() }
                    .onSuccess { result ->
                        castDevices = result
                        if (selectedCastRenderer?.location !in result.map { it.location }) {
                            selectedCastRenderer = null
                        }
                        if (selectedCastRenderer == null && result.size == 1) {
                            selectedCastRenderer = result.single()
                        }
                        castScanning = false
                        processPickedMedia(uri)
                    }
                    .onFailure { error ->
                        castScanning = false
                        castDevices = emptyList()
                        selectedCastRenderer = null
                        castMessage = error.message ?: "Не удалось найти DLNA-приёмники. Повторите поиск в разделе «Контроль»."
                        processPickedMedia(uri)
                    }
            }
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                runCatching { voiceInput.start() }
                    .onSuccess { statusMessage = it }
                    .onFailure { statusMessage = it.message ?: "Не удалось включить микрофон." }
            }
        } else {
            statusMessage = "Для голосового ввода разрешите доступ к микрофону."
        }
    }

    val discoverabilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        statusMessage = if (result.resultCode == Activity.RESULT_OK) {
            "Телефон обнаруживаем по Bluetooth. На телевизоре откройте Bluetooth → добавить устройство → Radio.TV.Control."
        } else {
            "Разрешите обнаружение телефона, чтобы телевизор мог подключиться по Bluetooth HID."
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bluetoothHid.start()
            discoverabilityLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            )
        } else {
            statusMessage = "Bluetooth не включён."
        }
    }

    val startBluetoothSession: () -> Unit = {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        when {
            adapter == null -> statusMessage = "На этом устройстве отсутствует Bluetooth."
            !runCatching { adapter.isEnabled }.getOrDefault(false) ->
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> {
                bluetoothHid.start()
                discoverabilityLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                )
            }
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.BLUETOOTH_CONNECT] == true &&
            grants[Manifest.permission.BLUETOOTH_SCAN] == true &&
            grants[Manifest.permission.BLUETOOTH_ADVERTISE] == true
        ) {
            startBluetoothSession()
        } else {
            statusMessage = "Для Bluetooth HID разрешите Bluetooth Connect, Scan и Advertise."
        }
    }

    val beginBluetoothSession: () -> Unit = {
        val required = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
        val missing = required.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing) bluetoothPermissionLauncher.launch(required) else startBluetoothSession()
    }

    val refreshDevices: () -> Unit = {
        runWithLocalNetworkPermission { runDiscovery() }
    }

    LaunchedEffect(companionStatus) {
        when (companionStatus) {
            is CompanionConnectionStatus.Connected -> companionWasConnected = true
            is CompanionConnectionStatus.Unavailable -> {
                if (companionWasConnected) {
                    companionWasConnected = false
                    if (!wifiConnected && !otherConnected && !adbConnected) refreshDevices()
                }
            }
            else -> Unit
        }
    }

    val connectPairedHost: (String) -> Unit = { target ->
        host = target
        selectedDeviceName = "Android TV"
        scope.launch {
            statusMessage = "Подключаемся к ранее сопряжённому Android TV…"
            runCatching { remote.connect(target) }
                .onSuccess { statusMessage = "Подключено к сохранённому телевизору: $target"; selectedDeviceName = "Android TV"; selectedTab = RadioTvTab.REMOTE }
                .onFailure { statusMessage = it.message ?: "Не удалось подключиться к сохранённому телевизору."; refreshDevices() }
        }
    }


    fun chooseDevice(device: DiscoveredRemoteDevice) {
        selectedDeviceName = friendlyDeviceName(device)
        when (device.type) {
            RemoteDeviceType.ANDROID_TV -> {
                host = device.ip
                if (device.ip == pairedHost) connectPairedHost(device.ip) else startPairing(device.ip)
            }
            RemoteDeviceType.SAMSUNG, RemoteDeviceType.LG, RemoteDeviceType.ROKU -> {
                scope.launch {
                    host = device.ip
                    statusMessage = "Подключаемся к " + device.type.label + "…"
                    runCatching { otherTvRemote.connect(device) }
                        .onSuccess {
                            statusMessage = when (val current = otherTvRemote.status.value) {
                                is OtherTvRemoteStatus.Pairing -> current.message
                                is OtherTvRemoteStatus.Connected -> "Подключено: " + current.type.label + " · " + current.ip
                                is OtherTvRemoteStatus.Error -> current.message
                                else -> "Соединение инициализировано."
                            }
                        }
                        .onFailure { statusMessage = it.message ?: "Не удалось подключить телевизор." }
                }
            }
            RemoteDeviceType.KIVI_ADB -> {
                host = device.ip
                selectedDeviceName = "Kivi TV (ADB)"
                scope.launch {
                    statusMessage = "Подключаемся к Kivi через ADB…"
                    runCatching { adbRemote.connect(device.ip, device.ports.firstOrNull { it == 5555 } ?: 5555) }
                        .onSuccess {
                            selectedTab = RadioTvTab.REMOTE
                            statusMessage = "Kivi TV подключён. Команды отправляются через ADB."
                        }
                        .onFailure {
                            statusMessage = it.message ?: "Для управления Kivi включите отладку по ADB в настройках телевизора."
                        }
                }
            }
            RemoteDeviceType.CHROMECAST -> {
                host = device.ip
                statusMessage = "Найден Google Cast по адресу " + device.ip + ". Управляющий Cast-адаптер добавляется отдельно."
            }
            RemoteDeviceType.UNKNOWN -> {
                host = device.ip
                scope.launch {
                    statusMessage = "Пробуем подключение ADB на порту 5555…"
                    val adbFailure = runCatching { adbRemote.connect(device.ip, 5555) }.exceptionOrNull()
                    if (adbFailure == null) {
                        selectedDeviceName = "Kivi TV (ADB)"
                        selectedTab = RadioTvTab.REMOTE
                        statusMessage = "Устройство подключено через ADB."
                    } else {
                        runCatching { remote.beginPairing(device.ip) }
                            .onSuccess { statusMessage = "ADB не ответил. Начато стандартное сопряжение Android TV." }
                            .onFailure {
                                statusMessage = "Не удалось определить протокол. Для управления Kivi включите отладку по ADB в настройках телевизора."
                            }
                    }
                }
            }
            else -> {
                host = device.ip
                statusMessage = "Определено: " + device.type.label + " (" + device.ip + "). Для этого типа нужен отдельный протокол."
            }
        }
    }

    val connectManualHost: () -> Unit = {
        val target = host.trim()
        scope.launch {
            statusMessage = "Определяем протокол по IP-адресу…"
            runCatching { discovery.detectHost(target) }
                .onSuccess { detected ->
                    statusMessage = "Определён тип: " + detected.type.label + ". Подключаемся…"
                    chooseDevice(detected)
                }
                .onFailure { error ->
                    statusMessage = error.message ?: "Не удалось определить устройство."
                }
        }
    }

    LaunchedEffect(Unit) {
        if (connected || otherConnected) return@LaunchedEffect
        val remembered = context.radioTvLastDevice.first()
        // Prefer the Companion channel when one is installed; only then fall back to saved legacy protocols.
        val foundCompanion = kotlinx.coroutines.withTimeoutOrNull(2800L) {
            companionClient.status.first { it is CompanionConnectionStatus.Connected }
        }
        if (foundCompanion != null) return@LaunchedEffect
        when {
            remembered.host.isNotBlank() && remembered.type == RemoteDeviceType.ANDROID_TV.name -> {
                host = remembered.host
                selectedDeviceName = remembered.name.ifBlank { "Android TV" }
                connectPairedHost(remembered.host)
            }
            remembered.host.isNotBlank() && remembered.type == RemoteDeviceType.KIVI_ADB.name -> {
                host = remembered.host
                selectedDeviceName = "Kivi TV (ADB)"
                statusMessage = "Восстанавливаем подключение к Kivi через ADB…"
                runCatching { adbRemote.connect(remembered.host, 5555) }
                    .onSuccess { statusMessage = "Kivi TV подключён через ADB."; selectedTab = RadioTvTab.REMOTE }
                    .onFailure { statusMessage = it.message ?: "Для управления Kivi включите отладку по ADB в настройках телевизора." }
            }
            remembered.host.isNotBlank() -> {
                host = remembered.host
                statusMessage = "Восстанавливаем сохранённое подключение…"
                runCatching { discovery.detectHost(remembered.host) }
                    .onSuccess { device -> chooseDevice(device.copy(name = remembered.name.ifBlank { device.name })) }
                    .onFailure { statusMessage = "Не удалось восстановить подключение. Поиск устройств запущен."; refreshDevices() }
            }
            pairedHost.isNotBlank() -> connectPairedHost(pairedHost)
            else -> refreshDevices()
        }
    }

    val handleFeatureAction: (RemoteFeatureAction) -> Unit = { action ->
        val feedbackMessage = when (action) {
            RemoteFeatureAction.VOICE_INPUT -> {
                if (voiceStatus is VoiceInputStatus.Recording || voiceStatus is VoiceInputStatus.Starting) {
                    scope.launch {
                        runCatching { voiceInput.stop() }
                            .onSuccess { statusMessage = it }
                            .onFailure { statusMessage = it.message ?: "Не удалось остановить запись." }
                    }
                    "Останавливаем голосовой ввод…"
                } else if (!selectedWifiConnected) {
                    if (selectedOtherConnected) {
                        "Голосовой ввод доступен только через Android TV Remote v2. Выберите Android TV и подключитесь по Wi-Fi."
                    } else {
                        "Сначала подключитесь к Android TV по Wi-Fi."
                    }
                } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    scope.launch {
                        runCatching { voiceInput.start() }
                            .onSuccess { statusMessage = it }
                            .onFailure { statusMessage = it.message ?: "Не удалось включить микрофон." }
                    }
                    "Подключаем голосовую сессию. Индикатор станет красным после начала записи."
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    "Запрашиваем разрешение на микрофон…"
                }
            }
            RemoteFeatureAction.APP_YOUTUBE -> {
                "Запуск YouTube напрямую пока не поддерживается выбранным протоколом. Откройте приложение на телевизоре."
            }
            RemoteFeatureAction.APP_NETFLIX -> {
                "Запуск Netflix напрямую пока не поддерживается выбранным протоколом. Откройте приложение на телевизоре."
            }
            RemoteFeatureAction.KEYBOARD -> {
                showKeyboard = true
                "Открыто окно ввода текста на телевизор."
            }
            RemoteFeatureAction.AIR_MOUSE -> {
                when {
                    airMouseStatus is AirMouseStatus.Active -> airMouse.toggle()
                    !airMouse.hasGyroscope -> airMouse.toggle()
                    !hidConnected -> {
                        beginBluetoothSession()
                        "Запущен режим сопряжения Bluetooth. Выберите Radio.TV.Control в настройках Bluetooth телевизора, затем включите аэромышь ещё раз."
                    }
                    else -> airMouse.toggle()
                }
            }
            RemoteFeatureAction.TOUCHPAD -> {
                showTouchpad = true
                "Открыто окно тачпада."
            }
            RemoteFeatureAction.CAST -> {
                // Show the system picker first; ask for LAN access only after content is selected.
                selectedTab = RadioTvTab.CONTROL
                duplicationExpanded = true
                mediaPicker.launch(arrayOf("*/*"))
                "Выберите фото, видео, аудио или файл для трансляции на DLNA-совместимый телевизор. DRM/HLS/DASH не поддерживается."
            }
        }
        sendUiFeedback(when (action) {
            RemoteFeatureAction.VOICE_INPUT -> "Микрофон"
            RemoteFeatureAction.KEYBOARD -> "Клавиатура"
            RemoteFeatureAction.AIR_MOUSE -> "Аэромышь"
            RemoteFeatureAction.TOUCHPAD -> "Тачпад"
            RemoteFeatureAction.CAST -> "Трансляция"
            RemoteFeatureAction.APP_YOUTUBE -> "YouTube"
            RemoteFeatureAction.APP_NETFLIX -> "Netflix"
        })
        statusMessage = feedbackMessage
        showNotification(feedbackMessage)
    }

    val dispatchRemoteKey: (RemoteKey) -> Unit = { key ->
        sendUiFeedback(remoteKeyFeedbackLabel(key))
        scope.launch {
            runCatching {
                if (companionConnected) {
                    runCatching { companionClient.sendKeyCode(remoteKeyToAndroidKeyCode(key)) }
                        .recoverCatching {
                            when {
                                selectedAdbConnected -> adbRemote.sendKey(key)
                                selectedOtherConnected -> otherTvRemote.sendKey(key)
                                selectedWifiConnected -> remote.sendKey(key)
                                hidConnected -> bluetoothHid.sendRemoteKey(key)
                                else -> throw it
                            }
                        }.getOrThrow()
                } else when {
                    selectedAdbConnected -> adbRemote.sendKey(key)
                    selectedOtherConnected -> otherTvRemote.sendKey(key)
                    selectedWifiConnected -> remote.sendKey(key)
                    hidConnected -> bluetoothHid.sendRemoteKey(key)
                    else -> error("Выбранный телевизор не подключён. Выберите устройство в настройках.")
                }
            }.onSuccess { if (key == RemoteKey.MUTE) muteActive = !muteActive }
                .onFailure {
                    val message = it.message ?: "Не удалось отправить команду."
                    statusMessage = message
                    showNotification(message)
                }
        }
    }

    MaterialTheme(colorScheme = radioTvColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = RadioTvPalette.Black) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(RadioTvPalette.Black)) {
                val isLandscape = maxWidth > maxHeight
                // Landscape/tablet letterbox uses a centered fixed-width column; portrait phones fill safe width.
                val panelWidth = if (isLandscape) 460.dp else maxWidth
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = panelWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(RadioTvPalette.Black)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 7 }) togetherWith
                                    (fadeOut(tween(120)) + slideOutVertically(tween(180)) { -it / 10 })
                            },
                            label = "main-screen-slide-up"
                        ) { screen ->
                            when (screen) {
                            RadioTvTab.REMOTE -> {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    TvRemotePad(
                                        enabled = connected || otherConnected || hidConnected,
                                        modifier = Modifier.widthIn(max = if (maxWidth > maxHeight) 420.dp else 470.dp).fillMaxWidth().fillMaxHeight().padding(horizontal = 4.dp, vertical = 2.dp),
                                        onKey = dispatchRemoteKey,
                                        onFeatureAction = handleFeatureAction,
                                        touchpadActive = showTouchpad,
                                        airMouseActive = airMouseStatus is AirMouseStatus.Active,
                                        muteActive = muteActive,
voiceActive = voiceStatus is VoiceInputStatus.Recording,
                                        keyboardActive = showKeyboard,
                                        featureMessage = null,
                                        showFeatureActions = false,
                                        deviceName = when {
                                            companionConnected -> (companionStatus as? CompanionConnectionStatus.Connected)?.serviceName?.substringAfter(" - ")?.takeIf { it.isNotBlank() } ?: "Телевизор"
                                            selectedAdbConnected -> "Kivi TV (ADB)"
                                            selectedOtherConnected -> selectedDeviceName.ifBlank { "Телевизор" }
                                            selectedWifiConnected -> selectedDeviceName.ifBlank { "Android TV" }
                                            hidConnected -> selectedDeviceName.ifBlank { "Bluetooth HID" }
                                            else -> "Начать поиск"
                                        },
                                        onStartSearch = { sendUiFeedback("Поиск устройств"); refreshDevices(); selectedTab = RadioTvTab.SETTINGS },
                                         onSettings = { sendUiFeedback("Настройки"); selectedTab = RadioTvTab.SETTINGS; refreshDevices() },
                                        onBack = {
                                             when {
                                                 showKeyboard -> { showKeyboard = false; keyboardController?.hide() }
                                                 showDigitPad -> showDigitPad = false
                                                 showTouchpad -> showTouchpad = false
                                                 selectedTab != RadioTvTab.REMOTE -> selectedTab = RadioTvTab.REMOTE
                                                 else -> dispatchRemoteKey(RemoteKey.BACK)
                                             }
                                         }
                                    )
                                }
                            }
                            RadioTvTab.CONTROL -> {
                                DigitalRemotePanel(modifier = Modifier.fillMaxSize(), deviceName = selectedDeviceName.ifBlank { "Нет подключения" },
                                    installedApps = installedApps,
                                    onLaunchApp = { app ->
                                        sendUiFeedback(app.name)
                                        scope.launch {
                                            runCatching {
                                                if (companionConnected) {
                                                    runCatching { companionClient.launchApp(app.id) }
                                                        .recoverCatching {
                                                            when {
                                                                selectedAdbConnected -> adbRemote.launchApp(app.id)
                                                                selectedOtherConnected -> otherTvRemote.launchInstalledApp(app)
                                                                else -> throw it
                                                            }
                                                        }.getOrThrow()
                                                } else if (selectedAdbConnected) adbRemote.launchApp(app.id)
                                                else otherTvRemote.launchInstalledApp(app)
                                            }.onFailure { statusMessage = it.message ?: "Не удалось запустить приложение на телевизоре." }
                                        }
                                    },
                                    enabled = connected || otherConnected || hidConnected, micRecording = voiceStatus is VoiceInputStatus.Recording,
                                    airMouseActive = airMouseStatus is AirMouseStatus.Active, keyboardActive = showKeyboard,
                                    onKey = dispatchRemoteKey, onFeatureAction = handleFeatureAction,
                                    onSettings = { sendUiFeedback("Настройки"); selectedTab = RadioTvTab.SETTINGS; refreshDevices() },
                                    onReturnRemote = { sendUiFeedback("Пульт"); selectedTab = RadioTvTab.REMOTE },
                                    duplicationExpanded = duplicationExpanded,
                                    onToggleDuplication = { sendUiFeedback("Дублирование"); duplicationExpanded = !duplicationExpanded; if (duplicationExpanded) refreshCastDevices() },
                                    duplicationContent = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { sendUiFeedback("Фото и видео"); launchMediaPicker(arrayOf("image/*", "video/*")) }, modifier = Modifier.weight(1f)) { Text("Фото / видео") }
                                            Button(onClick = { sendUiFeedback("Музыка и аудио"); launchAudioPicker() }, modifier = Modifier.weight(1f)) { Text("Музыка / аудио") }
                                        }
                                        OutlinedButton(onClick = { sendUiFeedback("Выбрать файл"); launchMediaPicker(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ВЫБРАТЬ ЛЮБОЙ ФАЙЛ") }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("DLNA-приёмники", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); OutlinedButton(onClick = { sendUiFeedback("Обновить DLNA"); refreshCastDevices() }, enabled = !castScanning) { Text(if (castScanning) "ПОИСК…" else "ОБНОВИТЬ") } }
                                        Text(castMedia?.let { "Медиа: " + it.title } ?: "Медиа ещё не выбрано.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        Text("Состояние: " + castPlaybackStatus, color = if (castPlaybackStatus == "Воспроизведение") RadioTvPalette.Red else RadioTvPalette.Muted)
                                        castDevices.forEach { renderer ->
                                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)) {
                                                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(renderer.friendlyName?.takeIf { it.isNotBlank() } ?: "DLNA-устройство", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                    OutlinedButton(onClick = { sendUiFeedback("Выбрать DLNA"); selectedCastRenderer = renderer; val media = castMedia; if (media != null) scope.launch { runCatching { dlnaCast.play(renderer, media) }.onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Передаём медиа на выбранное устройство." }.onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось запустить медиа." } } }) { Text(if (selectedCastRenderer?.location == renderer.location) "ВЫБРАНО" else "ВЫБРАТЬ") }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { sendUiFeedback("Воспроизведение"); val renderer = selectedCastRenderer; val media = castMedia; if (renderer != null && media != null) scope.launch { runCatching { dlnaCast.play(renderer, media) }.onSuccess { castPlaybackStatus = "Воспроизведение"; castMessage = "Команда Play отправлена." }.onFailure { castPlaybackStatus = "Ошибка"; castMessage = it.message ?: "Не удалось начать воспроизведение." } } }, enabled = selectedCastRenderer != null && castMedia != null, modifier = Modifier.weight(1f)) { Text("▶ Play") }
                                            OutlinedButton(onClick = { sendUiFeedback("Пауза"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.pause(renderer) }.onSuccess { castPlaybackStatus = "Пауза"; castMessage = "Команда Pause отправлена." }.onFailure { castMessage = it.message ?: "Pause не поддерживается." } } } }, enabled = selectedCastRenderer != null) { Text("Ⅱ") }
                                            OutlinedButton(onClick = { sendUiFeedback("Стоп"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.stop(renderer) }.onSuccess { castPlaybackStatus = "Остановлено"; castMessage = "Команда Stop отправлена." }.onFailure { castMessage = it.message ?: "Не удалось остановить." } } } }, enabled = selectedCastRenderer != null) { Text("■") }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { sendUiFeedback("Назад 30 секунд"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.seek(renderer, -30) }.onSuccess { castMessage = "Перемотка назад отправлена." }.onFailure { castMessage = it.message ?: "Перемотка не поддерживается." } } } }, enabled = selectedCastRenderer != null, modifier = Modifier.weight(1f)) { Text("−30 с") }
                                            OutlinedButton(onClick = { sendUiFeedback("Вперёд 30 секунд"); selectedCastRenderer?.let { renderer -> scope.launch { runCatching { dlnaCast.seek(renderer, 30) }.onSuccess { castMessage = "Перемотка вперёд отправлена." }.onFailure { castMessage = it.message ?: "Перемотка не поддерживается." } } } }, enabled = selectedCastRenderer != null, modifier = Modifier.weight(1f)) { Text("+30 с") }
                                        }
                                        Text(castMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                )
                            }
                            RadioTvTab.SETTINGS -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                                    .background(Color(0xFF2A2A2A))
                                                    .clickable { sendUiFeedback("Назад"); selectedTab = RadioTvTab.REMOTE }
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад к пульту", tint = Color.White) }
                                            Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    item {
                                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                            Text("СПОСОБ ПОДКЛЮЧЕНИЯ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                listOf(
                                                    CONNECTION_AUTO to "Авто",
                                                    CONNECTION_MANUAL to "Ручное",
                                                    CONNECTION_BLUETOOTH to "Bluetooth"
                                                ).forEach { (method, title) ->
                                                    val selected = feedbackSettings.connectionMethod == method
                                                    Surface(
                                                        modifier = Modifier.weight(1f).height(42.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                sendUiFeedback(title)
                                                                scope.launch { context.setConnectionMethod(method) }
                                                                when (method) {
                                                                    CONNECTION_AUTO -> { manualEntry = false; refreshDevices() }
                                                                    CONNECTION_MANUAL -> { manualEntry = true }
                                                                    CONNECTION_BLUETOOTH -> { manualEntry = false; beginBluetoothSession() }
                                                                }
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (selected) RadioTvPalette.Red else RadioTvPalette.Raised
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                        }
                                                    }
                                                }
                                            }
                                            Text(
                                                when {
                                                    companionConnected -> "Подключено к Radio.TV приложению"
                                                    connected -> selectedDeviceName.ifBlank { "Телевизор сопряжён" }
                                                    else -> "Для управления телефоном и ТВ используйте одну локальную сеть."
                                                },
                                                color = if (connected || companionConnected) RadioTvPalette.Red else RadioTvPalette.Muted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .background(RadioTvPalette.Raised).padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("ПОИСК УСТРОЙСТВ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                OutlinedButton(onClick = { sendUiFeedback("Поиск устройств"); refreshDevices() }, enabled = !scanning) {
                                                    Text(if (scanning) "ПОИСК…" else "ИСКАТЬ")
                                                }
                                            }
                                            if (scanning) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                                    Text("Ищем Companion и телевизоры…", color = RadioTvPalette.Muted)
                                                }
                                            } else if (devices.isEmpty()) {
                                                Text("Устройства не найдены. Запустите поиск ещё раз или введите IP вручную.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (devices.isNotEmpty()) Text("Найдено устройств: ${devices.size}", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                            if (pairedHost.isNotBlank()) {
                                                Text("Сохранённый телевизор доступен для повторного подключения.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                OutlinedButton(onClick = { sendUiFeedback("Подключить сохранённый ТВ"); connectPairedHost(pairedHost) }, modifier = Modifier.fillMaxWidth()) { Text("ПОДКЛЮЧИТЬ СОХРАНЁННЫЙ ТВ") }
                                            }
                                            if (manualEntry || feedbackSettings.connectionMethod == CONNECTION_MANUAL) {
                                                OutlinedTextField(
                                                    value = host,
                                                    onValueChange = { host = it },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text("IP-адрес телевизора") },
                                                    placeholder = { Text("192.168.1.100") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                                                )
                                                Button(onClick = { sendUiFeedback("Подключить по IP"); connectManualHost() }, modifier = Modifier.fillMaxWidth(), enabled = host.isNotBlank()) {
                                                    Text("ПОДКЛЮЧИТЬ ПО IP")
                                                }
                                            }
                                            if (codeRequested) {
                                                OutlinedTextField(
                                                    value = pairingCode,
                                                    onValueChange = { pairingCode = it.uppercase().filter { ch -> ch in "0123456789ABCDEF" }.take(6) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text("Код сопряжения с экрана ТВ") },
                                                    placeholder = { Text("A1B2C3") },
                                                    singleLine = true
                                                )
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            runCatching { remote.completePairing(pairingCode) }
                                                                .onSuccess { pairedHost = remote.pairedHost().orEmpty(); selectedTab = RadioTvTab.REMOTE }
                                                                .onFailure { statusMessage = it.message ?: "Не удалось завершить сопряжение." }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = pairingCode.length == 6
                                                ) { Text("ПОДТВЕРДИТЬ КОД") }
                                            }
                                            if (statusMessage.isNotBlank()) Text(statusMessage, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    items(devices, key = { it.ip }) { device ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(friendlyDeviceName(device), fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text(device.ip, color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                OutlinedButton(onClick = { sendUiFeedback("Подключить ТВ"); chooseDevice(device) }) { Text("Подключить") }
                                            }
                                        }
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .background(RadioTvPalette.Raised).padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("BLUETOOTH HID", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                                            Text(bluetoothStatus.asUserLabel(), color = if (hidConnected) RadioTvPalette.Red else RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                            Button(onClick = { sendUiFeedback("Bluetooth HID"); beginBluetoothSession() }, modifier = Modifier.fillMaxWidth()) {
                                                Text(if (hidConnected) "HID ПОДКЛЮЧЁН" else "ПОДКЛЮЧИТЬ BLUETOOTH HID")
                                            }
                                            Text("На телевизоре выберите Radio.TV.Control в списке Bluetooth-устройств.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .background(RadioTvPalette.Raised).padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("ОТКЛИК КНОПОК", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Вибрация", fontWeight = FontWeight.SemiBold)
                                                    Text("Короткий тактильный отклик", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Switch(checked = feedbackSettings.vibrationEnabled, onCheckedChange = { enabled ->
                                                    sendUiFeedback("Вибрация")
                                                    scope.launch { context.setVibrationEnabled(enabled) }
                                                })
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Звук", fontWeight = FontWeight.SemiBold)
                                                    Text(if (feedbackSettings.clickSoundEnabled) "Включён" else "Выключен", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Switch(checked = feedbackSettings.clickSoundEnabled, onCheckedChange = { enabled ->
                                                    sendUiFeedback("Звук")
                                                    scope.launch { context.setClickSoundEnabled(enabled) }
                                                })
                                            }
                                        }
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .background(RadioTvPalette.Raised).padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("УВЕДОМЛЕНИЯ", color = RadioTvPalette.Red, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Показывать уведомления", fontWeight = FontWeight.SemiBold)
                                                    Text("Всплывают сверху и скрываются через 3 секунды.", color = RadioTvPalette.Muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Switch(checked = feedbackSettings.notificationsEnabled, onCheckedChange = { enabled ->
                                                    sendUiFeedback("Уведомления")
                                                    scope.launch { context.setNotificationsEnabled(enabled) }
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
                if (showKeyboard) {
                    Dialog(onDismissRequest = {
                        showKeyboard = false
                        keyboardController?.hide()
                    }) {
                        Surface(shape = RoundedCornerShape(18.dp), color = RadioTvPalette.Raised, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Текст на телевизор", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                OutlinedTextField(
                                    value = keyboardText,
                                    onValueChange = { keyboardText = it },
                                    modifier = Modifier.fillMaxWidth().focusRequester(keyboardFocusRequester),
                                    placeholder = { Text("Введите текст…") },
                                    label = { Text("Сообщение") },
                                    singleLine = false,
                                    minLines = 2,
                                    maxLines = 5,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = {
                                        showKeyboard = false
                                        keyboardController?.hide()
                                    }, modifier = Modifier.weight(1f)) { Text("Закрыть") }
                                    Button(
                                        onClick = {
                                            val textToSend = keyboardText
                                            if (textToSend.isNotBlank()) scope.launch {
                                                runCatching {
                                                    if (companionConnected) {
                                                        runCatching { companionClient.sendText(textToSend) }
                                                            .recoverCatching {
                                                                when {
                                                                    selectedAdbConnected -> adbRemote.sendText(textToSend)
                                                                    selectedOtherConnected -> otherTvRemote.sendText(textToSend)
                                                                    selectedWifiConnected -> remote.sendText(textToSend)
                                                                    hidConnected -> bluetoothHid.sendText(textToSend)
                                                                    else -> throw it
                                                                }
                                                            }.getOrThrow()
                                                    } else when {
                                                        selectedAdbConnected -> adbRemote.sendText(textToSend)
                                                        selectedOtherConnected -> otherTvRemote.sendText(textToSend)
                                                        selectedWifiConnected -> remote.sendText(textToSend)
                                                        hidConnected -> bluetoothHid.sendText(textToSend)
                                                        else -> error("Сначала подключитесь к телевизору.")
                                                    }
                                                }.onSuccess {
                                                    keyboardText = ""
                                                    showKeyboard = false
                                                    keyboardController?.hide()
                                                    statusMessage = "Текст отправлен."
                                                }.onFailure {
                                                    val message = it.message ?: "Не удалось отправить текст на телевизор."
                                                    statusMessage = message
                                                    showNotification(message)
                                                }
                                            }
                                        },
                                        enabled = keyboardText.isNotBlank() && (companionConnected || selectedAdbConnected || selectedOtherConnected || selectedWifiConnected || hidConnected),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Отправить") }
                                }
                                LaunchedEffect(Unit) {
                                    delay(180)
                                    keyboardFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                        }
                    }
                }
                if (showTouchpad) {
                    Dialog(onDismissRequest = { showTouchpad = false }) {
                        Surface(shape = RoundedCornerShape(18.dp), color = RadioTvPalette.Raised, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Тачпад / Аэромышь", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                TouchpadSurface(
                                    enabled = hidConnected,
                                    modifier = Modifier.fillMaxWidth().height(236.dp),
                                    onMove = { dx, dy -> scope.launch { runCatching { bluetoothHid.moveMouse(dx, dy) }.onFailure { val message = it.message ?: "Не удалось переместить курсор."; statusMessage = message; showNotification(message) } } },
                                    onTap = { sendUiFeedback("Клик тачпада"); scope.launch { runCatching { bluetoothHid.clickMouse(1) }.onFailure { val message = it.message ?: "Не удалось нажать мышь."; statusMessage = message; showNotification(message) } } },
                                    onLongPress = { sendUiFeedback("Контекстный клик"); scope.launch { runCatching { bluetoothHid.clickMouse(2) }.onFailure { val message = it.message ?: "Не удалось отправить контекстный клик."; statusMessage = message; showNotification(message) } } }
                                )
                                Button(
                                    onClick = {
                                        when {
                                            airMouseStatus is AirMouseStatus.Active -> airMouse.toggle()
                                            !airMouse.hasGyroscope -> airMouse.toggle()
                                            !hidConnected -> beginBluetoothSession()
                                            else -> airMouse.toggle()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (airMouseStatus is AirMouseStatus.Active) RadioTvPalette.Red else RadioTvPalette.Black)
                                ) {
                                    Text(if (airMouseStatus is AirMouseStatus.Active) "ВЫКЛЮЧИТЬ АЭРОМЫШЬ" else "ВКЛЮЧИТЬ АЭРОМЫШЬ")
                                }
                                Text(
                                    if (airMouseStatus is AirMouseStatus.Active) "Аэромышь работает даже после закрытия этого окна." else if (hidConnected) "Свайпы перемещают курсор; аэромышь использует гироскоп." else "Для тачпада и аэромыши подключите Bluetooth HID.",
                                    color = RadioTvPalette.Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(onClick = { showTouchpad = false }, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
                            }
                        }
                    }
                }
                if (showDigitPad) {
                    Dialog(onDismissRequest = { showDigitPad = false }) {
                        Surface(shape = RoundedCornerShape(18.dp), color = RadioTvPalette.Raised, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Цифры", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                (1..9).toList().chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { digit ->
                                            Button(onClick = { dispatchRemoteKey(digitKey(digit)) }, modifier = Modifier.weight(1f)) { Text(digit.toString()) }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Spacer(Modifier.weight(1f))
                                    Button(onClick = { dispatchRemoteKey(RemoteKey.NUMBER_0) }, modifier = Modifier.weight(1f)) { Text("0") }
                                    Spacer(Modifier.weight(1f))
                                }
                                OutlinedButton(onClick = { showDigitPad = false }, modifier = Modifier.fillMaxWidth()) { Text("Назад / Закрыть") }
                            }
                        }
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                )
            }
        }
    }
}

private fun remoteKeyToAndroidKeyCode(key: RemoteKey): Int = when (key) {
    RemoteKey.UP -> 19
    RemoteKey.DOWN -> 20
    RemoteKey.LEFT -> 21
    RemoteKey.RIGHT -> 22
    RemoteKey.OK -> 23
    RemoteKey.BACK -> 4
    RemoteKey.EXIT -> 111
    RemoteKey.HOME -> 3
    RemoteKey.MENU -> 82
    RemoteKey.POWER -> 26
    RemoteKey.MUTE -> 164
    RemoteKey.VOLUME_UP -> 24
    RemoteKey.VOLUME_DOWN -> 25
    RemoteKey.CHANNEL_UP -> 166
    RemoteKey.CHANNEL_DOWN -> 167
    RemoteKey.SOURCE -> 178
    RemoteKey.PLAY_PAUSE -> 85
    RemoteKey.PLAY -> 126
    RemoteKey.PAUSE -> 127
    RemoteKey.STOP -> 86
    RemoteKey.REWIND -> 89
    RemoteKey.FAST_FORWARD -> 90
    RemoteKey.NUMBER_0 -> 7
    RemoteKey.NUMBER_1 -> 8
    RemoteKey.NUMBER_2 -> 9
    RemoteKey.NUMBER_3 -> 10
    RemoteKey.NUMBER_4 -> 11
    RemoteKey.NUMBER_5 -> 12
    RemoteKey.NUMBER_6 -> 13
    RemoteKey.NUMBER_7 -> 14
    RemoteKey.NUMBER_8 -> 15
    RemoteKey.NUMBER_9 -> 16
}

private fun friendlyDeviceName(device: DiscoveredRemoteDevice): String {
    if (device.type == RemoteDeviceType.KIVI_ADB) return "Kivi TV (ADB)"
    val name = device.name.trim()
    return when {
        name.isBlank() || name == device.ip -> "Телевизор"
        name.contains("miniupnpd", ignoreCase = true) || name.contains("upnp", ignoreCase = true) || name.contains("dlna", ignoreCase = true) -> "DLNA-устройство"
        name.contains("roku", ignoreCase = true) -> "Roku TV"
        name.contains("webos", ignoreCase = true) -> "LG webOS TV"
        name.contains("samsung", ignoreCase = true) || name.contains("tizen", ignoreCase = true) -> "Samsung TV"
        else -> name
    }
}

private fun remoteKeyFeedbackLabel(key: RemoteKey): String = when (key) {
    RemoteKey.POWER -> "Питание"
    RemoteKey.MENU -> "Меню"
    RemoteKey.OK -> "ОК"
    RemoteKey.UP -> "Вверх"
    RemoteKey.DOWN -> "Вниз"
    RemoteKey.LEFT -> "Влево"
    RemoteKey.RIGHT -> "Вправо"
    RemoteKey.BACK -> "Назад"
    RemoteKey.EXIT -> "Выход"
    RemoteKey.HOME -> "Домой"
    RemoteKey.MUTE -> "Без звука"
    RemoteKey.VOLUME_UP -> "Громкость плюс"
    RemoteKey.VOLUME_DOWN -> "Громкость минус"
    RemoteKey.CHANNEL_UP -> "Канал плюс"
    RemoteKey.CHANNEL_DOWN -> "Канал минус"
    RemoteKey.PLAY_PAUSE -> "Воспроизведение или пауза"
    RemoteKey.PLAY -> "Воспроизведение"
    RemoteKey.PAUSE -> "Пауза"
    RemoteKey.REWIND -> "Перемотка назад"
    RemoteKey.FAST_FORWARD -> "Перемотка вперёд"
    RemoteKey.SOURCE -> "Источник"
    RemoteKey.NUMBER_0 -> "Ноль"
    RemoteKey.NUMBER_1 -> "Один"
    RemoteKey.NUMBER_2 -> "Два"
    RemoteKey.NUMBER_3 -> "Три"
    RemoteKey.NUMBER_4 -> "Четыре"
    RemoteKey.NUMBER_5 -> "Пять"
    RemoteKey.NUMBER_6 -> "Шесть"
    RemoteKey.NUMBER_7 -> "Семь"
    RemoteKey.NUMBER_8 -> "Восемь"
    RemoteKey.NUMBER_9 -> "Девять"
    else -> "Кнопка пульта"
}
private enum class RadioTvTab {
    REMOTE, CONTROL, SETTINGS
}

@Composable
private fun DiscoverySkeletonCard() {
    val transition = rememberInfiniteTransition(label = "device-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "device-skeleton-alpha"
    )
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RadioTvPalette.Raised)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(36.dp).background(RadioTvPalette.Muted.copy(alpha = alpha), RoundedCornerShape(18.dp)))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.fillMaxWidth(0.64f).height(12.dp).background(RadioTvPalette.Muted.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(0.88f).height(9.dp).background(RadioTvPalette.Muted.copy(alpha = alpha * 0.8f), RoundedCornerShape(4.dp)))
            }
            Box(Modifier.size(32.dp).background(RadioTvPalette.Muted.copy(alpha = alpha * 0.7f), RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
private fun ControlActionButton(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = tween(140),
        label = "settings-feature-button-scale"
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp).graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            alpha = if (pressed) 0.82f else 1f
        },
        interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) RadioTvPalette.Red else RadioTvPalette.Raised,
            contentColor = Color.White,
            disabledContainerColor = RadioTvPalette.Raised,
            disabledContentColor = RadioTvPalette.Muted
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        val title = label.substringAfter('\n', label).trim()
        val actionIcon = when (title) {
            "Микрофон" -> Icons.Filled.Mic
            "Аэромышь" -> Icons.Filled.Mouse
            "Клавиатура" -> Icons.Filled.Dialpad
            "Тачпад" -> Icons.Filled.TouchApp
            "Трансляция" -> Icons.Filled.Cast
            else -> null
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (actionIcon != null) Icon(actionIcon, contentDescription = title, modifier = Modifier.size(21.dp), tint = if (active) Color.White else RadioTvPalette.Red)
            Text(title, fontSize = 10.sp, lineHeight = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        }
    }
}

private fun digitKey(number: Int): RemoteKey = RemoteKey.entries.first { it.name == "NUMBER_$number" }

private fun sendRemoteKey(
    key: RemoteKey,
    remote: AndroidTvRemoteV2Transport,
    otherRemote: OtherTvRemoteController,
    bluetoothHid: BluetoothHidController,
    wifiConnected: Boolean,
    otherConnected: Boolean,
    hidConnected: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        runCatching {
            when {
                wifiConnected -> remote.sendKey(key)
                otherConnected -> otherRemote.sendKey(key)
                hidConnected -> bluetoothHid.sendRemoteKey(key)
                else -> error("Сначала подключитесь к телевизору.")
            }
        }
    }
}

private fun deviceIcon(type: RemoteDeviceType): String = when (type) {
    RemoteDeviceType.ANDROID_TV -> "TV"
    RemoteDeviceType.CHROMECAST -> "CAST"
    RemoteDeviceType.SAMSUNG -> "SAM"
    RemoteDeviceType.LG -> "LG"
    RemoteDeviceType.ROKU -> "ROKU"
    RemoteDeviceType.DLNA -> "DLNA"
    RemoteDeviceType.AIRPLAY -> "AIR"
    RemoteDeviceType.KIVI_ADB -> "ADB"
    RemoteDeviceType.UNKNOWN -> "?"
}

private fun AirMouseStatus.asUserLabel(): String = when (this) {
    AirMouseStatus.Off -> "Аэромышь выключена."
    AirMouseStatus.Active -> "Аэромышь активна: поворачивайте телефон для движения курсора."
    is AirMouseStatus.Unavailable -> reason
    is AirMouseStatus.Error -> reason
}

private fun BluetoothHidStatus.asUserLabel(): String = when (this) {
    BluetoothHidStatus.Idle -> "HID выключен. Нажмите «Подключить» и выберите Radio.TV.Control на ТВ."
    BluetoothHidStatus.Starting -> "Запускаем HID-профиль или ждём соединения с телевизором…"
    BluetoothHidStatus.Ready -> "Профиль зарегистрирован. Откройте Bluetooth на ТВ и выберите Radio.TV.Control."
    is BluetoothHidStatus.Connected -> "Подключено: " + deviceName
    is BluetoothHidStatus.Error -> "Bluetooth HID: " + message
}

private fun VoiceInputStatus.asUserLabel(): String = when (this) {
    VoiceInputStatus.Off -> "Голосовой ввод выключен."
    VoiceInputStatus.Starting -> "Запускается голосовая сессия Android TV Remote v2…"
    VoiceInputStatus.Recording -> "Идёт запись: PCM 16-bit · mono · 8 kHz."
    is VoiceInputStatus.Error -> "Голосовой ввод: " + message
}

private fun OtherTvRemoteStatus.asUserLabel(): String = when (this) {
    OtherTvRemoteStatus.Disconnected -> "Внешний протокол отключён."
    is OtherTvRemoteStatus.Connecting -> "Подключение: " + type.label + " · " + ip
    is OtherTvRemoteStatus.Pairing -> "Ожидается сопряжение: " + message
    is OtherTvRemoteStatus.Connected -> "Подключено: " + type.label + " · " + ip
    is OtherTvRemoteStatus.Error -> "Ошибка подключения: " + message
}

private fun AdbRemoteStatus.asUserLabel(): String = when (this) {
    AdbRemoteStatus.Disconnected -> "ADB: отключён."
    is AdbRemoteStatus.Connecting -> "ADB: подключение к ${host}…"
    is AdbRemoteStatus.Connected -> "Kivi TV (ADB): подключено · ${host}:${port}"
    is AdbRemoteStatus.Error -> message
}
