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
        WidgetRefreshWorker.schedule(context)
    }
    override fun onEnabled(context: Context) { WidgetRefreshWorker.schedule(context) }
    private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_last_radio)
        val store = SettingsStore(context.applicationContext)
        val last = kotlinx.coroutines.runBlocking { store.lastStream(Section.RADIO) }
        val playing = kotlinx.coroutines.runBlocking { store.radioPlaying() }
        views.setTextViewText(R.id.widget_station, last?.name ?: "Нет последней станции")
        views.setTextViewText(R.id.widget_play, if (playing) "❚❚" else "▶")
        val bitmap = last?.let { RadioLogoAssets.bitmap(it.name) }
        if (bitmap != null) views.setImageViewBitmap(R.id.widget_logo, bitmap)
        else views.setImageViewResource(R.id.widget_logo, R.drawable.ic_app_icon)
        val toggle = PendingIntent.getActivity(context, id * 10 + 1,
            Intent(context, MainActivity::class.java).setAction(WidgetActions.TOGGLE_LAST_RADIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(context, id * 10 + 2,
            Intent(context, MainActivity::class.java).setAction(WidgetActions.OPEN_RADIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_play, toggle)
        views.setOnClickPendingIntent(R.id.widget_open, open)
        manager.updateAppWidget(id, views)
    }
    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = LastRadioWidgetProvider()
            val component = ComponentName(context, LastRadioWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { provider.updateOne(context, manager, it) }
        }
    }
}
