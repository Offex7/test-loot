package com.offex7.streamhub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_TOGGLE_PLAYBACK, true)
        }
        context.startActivity(activityIntent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.offex7.streamhub.PIP_TOGGLE"
        const val EXTRA_TOGGLE_PLAYBACK = "toggle_playback"
    }
}
