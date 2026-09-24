package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

internal const val EXTRA_WIDGET_SECTION = "widget_section"
internal const val EXTRA_WIDGET_RADIO_TOGGLE = "widget_radio_toggle"

class QuickAccessWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_access)
        views.setOnClickPendingIntent(
            R.id.widget_tv_button,
            openIntent(context, Section.TV, id * 2)
        )
        views.setOnClickPendingIntent(
            R.id.widget_radio_button,
            openIntent(context, Section.RADIO, id * 2 + 1)
        )
        manager.updateAppWidget(id, views)
    }

    private fun openIntent(context: Context, section: Section, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_WIDGET_SECTION, section.name)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, QuickAccessWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            QuickAccessWidgetProvider().onUpdate(appContext, manager, ids)
        }
    }
}
