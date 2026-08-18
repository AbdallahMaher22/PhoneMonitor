package com.example.phonemonitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StatsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        private const val ACTION_REFRESH_WIDGET = "com.example.phonemonitor.ACTION_REFRESH_WIDGET"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_stats)
            val statsProvider = SystemStatsProvider(context)

            // قراءة الإحصائيات الحالية
            val ramUsage = statsProvider.ramUsagePercent
            val temp = statsProvider.batteryTemp.toInt()
            val batteryLevel = statsProvider.batteryLevel

            // تحديث النصوص
            views.setTextViewText(R.id.tv_widget_ram, "$ramUsage%")
            views.setTextViewText(R.id.tv_widget_temp, "$temp°C")
            views.setTextViewText(R.id.tv_widget_battery, "$batteryLevel%")

            // فتح التطبيق الرئيسي تلقائياً وبأمان
            val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // زر التحديث اليدوي
            val refreshIntent = Intent(context, StatsWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
