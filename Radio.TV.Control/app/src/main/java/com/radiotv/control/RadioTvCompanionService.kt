package com.radiotv.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.radiotv.control.companion.RadioTvCompanionServer
import com.radiotv.control.core.AdbRemoteDevice
import com.radiotv.control.core.AdbRemoteStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * TV-side Companion foreground service. ADB is used only against the TV's own loopback daemon;
 * incoming WebSocket messages cannot request an arbitrary shell command.
 */
class RadioTvCompanionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var adb: AdbRemoteDevice
    private var companionServer: RadioTvCompanionServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        adb = AdbRemoteDevice(applicationContext)
        companionServer = runCatching {
            RadioTvCompanionServer(applicationContext) { json ->
                serviceScope.launch { executeCommand(json) }
            }.also { it.start() }
        }.onFailure {
            Log.e(TAG, "Unable to start Companion WebSocket server", it)
            requestAdbSetup("Не удалось запустить сервер связи Radio.TV.")
        }.getOrNull()
        serviceScope.launch { ensureLoopbackAdb() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private suspend fun executeCommand(command: JSONObject) {
        if (!ensureLoopbackAdb()) return
        runCatching {
            when (command.optString("type")) {
                "key" -> {
                    val keyCode = command.optInt("keyCode", -1)
                    require(keyCode in 0..288) { "Недопустимый keycode." }
                    adb.sendKey(keyCode)
                }
                "text" -> {
                    val text = command.optString("text")
                    require(text.isNotEmpty() && text.length <= MAX_TEXT_CHARS) { "Текст пустой или слишком длинный." }
                    adb.sendText(text)
                }
                "launch" -> {
                    val packageName = command.optString("packageName")
                    require(PACKAGE_NAME.matches(packageName)) { "Некорректное имя приложения." }
                    adb.launchApp(packageName)
                }
                else -> error("Неизвестная команда Companion.")
            }
        }.onFailure {
            Log.w(TAG, "Companion command failed", it)
            if (adb.status.value !is AdbRemoteStatus.Connected) requestAdbSetup(it.message.orEmpty())
        }
    }

    private suspend fun ensureLoopbackAdb(): Boolean = withContext(Dispatchers.IO) {
        if (adb.status.value is AdbRemoteStatus.Connected) return@withContext true
        runCatching { adb.connect("127.0.0.1", ADB_PORT) }
            .onFailure { error ->
                Log.i(TAG, "TV loopback ADB is unavailable", error)
                requestAdbSetup(error.message.orEmpty())
            }
            .isSuccess
    }

    private fun requestAdbSetup(reason: String) {
        sendBroadcast(
            Intent(ACTION_ADB_REQUIRED)
                .setPackage(packageName)
                .putExtra(EXTRA_REASON, reason.take(240))
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio.TV Companion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Связь телефона с телевизором в локальной сети"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Radio.TV Companion активен")
                .setContentText("Ожидает команды от телефона в локальной сети")
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Radio.TV Companion активен")
                .setContentText("Ожидает команды от телефона в локальной сети")
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {
        companionServer?.close()
        companionServer = null
        if (::adb.isInitialized) adb.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_ADB_REQUIRED = "com.radiotv.control.action.ADB_REQUIRED"
        const val EXTRA_REASON = "reason"
        private const val TAG = "RadioTvCompanionService"
        private const val CHANNEL_ID = "radiotv_companion"
        private const val NOTIFICATION_ID = 8089
        private const val ADB_PORT = 5555
        private const val MAX_TEXT_CHARS = 2048
        private val PACKAGE_NAME = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")
    }
}
