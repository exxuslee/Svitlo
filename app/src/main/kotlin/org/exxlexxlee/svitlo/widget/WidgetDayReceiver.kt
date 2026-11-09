package org.exxlexxlee.svitlo.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import org.exxlexxlee.svitlo.R

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

internal object WidgetDaySelection {
    private const val PREFS = "widget_selected_day"
    private fun key(id: Int) = "tomorrow_$id"

    fun read(context: Context, id: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(id), false)

    fun save(context: Context, id: Int, tomorrow: Boolean) {
        // apply updates memory immediately; disk persistence does not block the click response.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(key(id), tomorrow) }
    }

    fun remove(context: Context, id: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(key(id)) }
    }

    suspend fun migrate(context: Context, id: GlanceId, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(key(appWidgetId))) return
        val legacy = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id)
        if (!prefs.contains(key(appWidgetId))) {
            save(
                context,
                appWidgetId,
                legacy[booleanPreferencesKey("widget_show_tomorrow")] == true
            )
        }
    }
}
