package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class QuickAccessWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
        WidgetRefreshWorker.schedule(context)
    }
    private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_access)
        val tv = PendingIntent.getActivity(context, id * 10 + 3,
            Intent(context, MainActivity::class.java).setAction(WidgetActions.OPEN_TV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val radio = PendingIntent.getActivity(context, id * 10 + 4,
            Intent(context, MainActivity::class.java).setAction(WidgetActions.OPEN_RADIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_tv, tv)
        views.setOnClickPendingIntent(R.id.widget_radio, radio)
        manager.updateAppWidget(id, views)
    }
    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = QuickAccessWidgetProvider()
            val component = ComponentName(context, QuickAccessWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { provider.updateOne(context, manager, it) }
        }
    }
}
