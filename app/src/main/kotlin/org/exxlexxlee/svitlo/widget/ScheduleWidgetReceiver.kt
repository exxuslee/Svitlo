package org.exxlexxlee.svitlo.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import org.exxlexxlee.svitlo.widget.update.ACTION_WIDGET_TIME
import org.exxlexxlee.svitlo.widget.update.WidgetTimeUpdates

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onReceive(context: Context, intent: Intent) {
        WidgetTimeUpdates.schedule(context)
        when (intent.action) {
            ACTION_WIDGET_TIME,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                val ids = installedScheduleWidgetIds(context)
                if (ids.isNotEmpty()) {
                    // Let Glance enqueue its worker using the receiver's goAsync lifecycle.
                    super.onReceive(context, Intent(context, ScheduleWidgetReceiver::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    })
                }
            }
            else -> super.onReceive(context, intent)
        }
    }
}

internal fun installedScheduleWidgetIds(context: Context): IntArray =
    AppWidgetManager.getInstance(context).getAppWidgetIds(
        ComponentName(context, ScheduleWidgetReceiver::class.java),
    )
