package org.exxlexxlee.svitlo.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.widget.state.WidgetDaySelection

/** Switches already rendered pages without starting a Glance worker or loading the schedule. */
class WidgetDayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SELECT_DAY || !intent.hasExtra(EXTRA_TOMORROW)) return
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetInfo(id)?.provider?.className != ScheduleWidgetReceiver::class.java.name) return
        val tomorrow = intent.getBooleanExtra(EXTRA_TOMORROW, false)
        // Keep the selected panel in the host's cached RemoteViews, including after launcher restart.
        manager.partiallyUpdateAppWidget(id, dayVisibility(context, tomorrow))
        WidgetDaySelection.save(context, id, tomorrow)
    }

    companion object {
        private const val ACTION_SELECT_DAY = "org.exxlexxlee.svitlo.widget.SELECT_DAY"
        private const val EXTRA_TOMORROW = "tomorrow"

        fun intent(context: Context, appWidgetId: Int, tomorrow: Boolean): Intent =
            Intent(context, WidgetDayReceiver::class.java).apply {
                action = ACTION_SELECT_DAY
                data = "svitlo://widget/$appWidgetId/day/${if (tomorrow) 1 else 0}".toUri()
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_TOMORROW, tomorrow)
            }

        internal fun dayVisibility(context: Context, tomorrow: Boolean): RemoteViews =
            RemoteViews(context.packageName, R.layout.schedule_widget_days).apply {
                setViewVisibility(
                    R.id.widget_today_panel,
                    if (tomorrow) View.GONE else View.VISIBLE
                )
                setViewVisibility(
                    R.id.widget_tomorrow_panel,
                    if (tomorrow) View.VISIBLE else View.GONE
                )
            }
    }
}
