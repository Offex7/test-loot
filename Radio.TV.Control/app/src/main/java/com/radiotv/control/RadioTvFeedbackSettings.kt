package com.radiotv.control

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.radioTvControlSettingsStore by preferencesDataStore(name = "radio_tv_control_settings")

internal data class RadioTvFeedbackSettings(
    val vibrationEnabled: Boolean = true,
    val clickSoundEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val connectionMethod: String = CONNECTION_AUTO,
)

internal const val CONNECTION_AUTO = "auto"
internal const val CONNECTION_MANUAL = "manual"
internal const val CONNECTION_BLUETOOTH = "bluetooth"

internal data class RadioTvLastDevice(
    val host: String = "",
    val name: String = "",
    val type: String = ""
)

private object FeedbackKeys {
    val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
    val clickSoundEnabled = booleanPreferencesKey("click_sound_enabled")
    val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
    val connectionMethod = stringPreferencesKey("connection_method")
    val lastDeviceHost = stringPreferencesKey("last_device_host")
    val lastDeviceName = stringPreferencesKey("last_device_name")
    val lastDeviceType = stringPreferencesKey("last_device_type")
}

internal val Context.radioTvFeedbackSettings: Flow<RadioTvFeedbackSettings>
    get() = radioTvControlSettingsStore.data.map { values ->
        RadioTvFeedbackSettings(
            vibrationEnabled = values[FeedbackKeys.vibrationEnabled] ?: true,
            clickSoundEnabled = values[FeedbackKeys.clickSoundEnabled] ?: false,
            notificationsEnabled = values[FeedbackKeys.notificationsEnabled] ?: true,
            connectionMethod = values[FeedbackKeys.connectionMethod] ?: CONNECTION_AUTO,
        )
    }

internal val Context.radioTvLastDevice: Flow<RadioTvLastDevice>
    get() = radioTvControlSettingsStore.data.map { values ->
        RadioTvLastDevice(
            host = values[FeedbackKeys.lastDeviceHost].orEmpty(),
            name = values[FeedbackKeys.lastDeviceName].orEmpty(),
            type = values[FeedbackKeys.lastDeviceType].orEmpty()
        )
    }

internal suspend fun Context.setLastConnectedDevice(host: String, name: String, type: String) {
    val normalizedHost = host.trim()
    if (normalizedHost.isBlank()) return
    radioTvControlSettingsStore.edit { values ->
        values[FeedbackKeys.lastDeviceHost] = normalizedHost
        values[FeedbackKeys.lastDeviceName] = name.trim().ifBlank { "Телевизор" }
        values[FeedbackKeys.lastDeviceType] = type
    }
}

internal suspend fun Context.setVibrationEnabled(enabled: Boolean) {
    radioTvControlSettingsStore.edit { it[FeedbackKeys.vibrationEnabled] = enabled }
}

internal suspend fun Context.setClickSoundEnabled(enabled: Boolean) {
    radioTvControlSettingsStore.edit { it[FeedbackKeys.clickSoundEnabled] = enabled }
}

internal suspend fun Context.setNotificationsEnabled(enabled: Boolean) {
    radioTvControlSettingsStore.edit { it[FeedbackKeys.notificationsEnabled] = enabled }
}

internal suspend fun Context.setConnectionMethod(method: String) {
    val normalized = when (method) {
        CONNECTION_MANUAL -> CONNECTION_MANUAL
        CONNECTION_BLUETOOTH -> CONNECTION_BLUETOOTH
        else -> CONNECTION_AUTO
    }
    radioTvControlSettingsStore.edit { it[FeedbackKeys.connectionMethod] = normalized }
}
