package com.offex7.streamhub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

private fun remoteWidgetPendingIntent(
    context: Context,
    appWidgetId: Int
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_WIDGET_SECTION, MainActivity.WIDGET_REMOTE_SECTION)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    return PendingIntent.getActivity(context, appWidgetId, intent, flags)
}

class RemoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_remote)
            views.setImageViewResource(R.id.widget_icon, R.drawable.widget_remote_icon)
            views.setOnClickPendingIntent(R.id.widget_root, remoteWidgetPendingIntent(context, id))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
