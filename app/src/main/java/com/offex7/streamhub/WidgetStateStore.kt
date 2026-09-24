package com.offex7.streamhub

import android.content.Context

internal object WidgetStateStore {
    private const val PREFS = "radio_tv_widget_state"
    private const val LAST_RADIO = "last_radio_name"
    private const val RADIO_PLAYING = "radio_playing"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setLastRadio(context: Context, name: String) {
        prefs(context).edit().putString(LAST_RADIO, name).apply()
    }

    fun lastRadio(context: Context): String =
        prefs(context).getString(LAST_RADIO, "").orEmpty()

    fun setRadioPlaying(context: Context, playing: Boolean) {
        prefs(context).edit().putBoolean(RADIO_PLAYING, playing).apply()
    }

    fun isRadioPlaying(context: Context): Boolean =
        prefs(context).getBoolean(RADIO_PLAYING, false)
}
