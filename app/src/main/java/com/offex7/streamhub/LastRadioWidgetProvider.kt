package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LastRadioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_last_radio)
        val name = WidgetStateStore.lastRadio(appContext).ifBlank { "Последнее радио" }
        val playing = WidgetStateStore.isRadioPlaying(appContext)

        views.setTextViewText(R.id.widget_last_radio_name, name)
        views.setImageViewResource(
            R.id.widget_last_radio_art,
            R.drawable.start_radio_cd
        )
        views.setImageViewResource(
            R.id.widget_last_radio_toggle,
            if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
        views.setOnClickPendingIntent(
            R.id.widget_last_radio_name,
            openRadioIntent(appContext, id * 2)
        )
        views.setOnClickPendingIntent(
            R.id.widget_last_radio_toggle,
            toggleIntent(appContext, id * 2 + 1)
        )
        manager.updateAppWidget(id, views)
    }

    private fun openRadioIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_WIDGET_SECTION, Section.RADIO.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun toggleIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_WIDGET_SECTION, Section.RADIO.name)
                .putExtra(EXTRA_WIDGET_RADIO_TOGGLE, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, LastRadioWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            LastRadioWidgetProvider().onUpdate(appContext, manager, ids)
        }
    }
}
