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
    val soundType: String = SOUND_TYPE_CLICK
)

internal const val SOUND_TYPE_CLICK = "click"
internal const val SOUND_TYPE_VOICE = "voice"

private object FeedbackKeys {
    val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
    val clickSoundEnabled = booleanPreferencesKey("click_sound_enabled")
    val soundType = stringPreferencesKey("click_sound_type")
}

internal val Context.radioTvFeedbackSettings: Flow<RadioTvFeedbackSettings>
    get() = radioTvControlSettingsStore.data.map { values ->
        RadioTvFeedbackSettings(
            vibrationEnabled = values[FeedbackKeys.vibrationEnabled] ?: true,
            clickSoundEnabled = values[FeedbackKeys.clickSoundEnabled] ?: false,
            soundType = values[FeedbackKeys.soundType] ?: SOUND_TYPE_CLICK
        )
    }

internal suspend fun Context.setVibrationEnabled(enabled: Boolean) {
    radioTvControlSettingsStore.edit { it[FeedbackKeys.vibrationEnabled] = enabled }
}

internal suspend fun Context.setClickSoundEnabled(enabled: Boolean) {
    radioTvControlSettingsStore.edit { it[FeedbackKeys.clickSoundEnabled] = enabled }
}

internal suspend fun Context.setClickSoundType(type: String) {
    radioTvControlSettingsStore.edit {
        it[FeedbackKeys.soundType] = if (type == SOUND_TYPE_VOICE) SOUND_TYPE_VOICE else SOUND_TYPE_CLICK
    }
}
