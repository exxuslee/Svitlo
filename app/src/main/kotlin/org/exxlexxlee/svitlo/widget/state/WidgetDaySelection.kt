package org.exxlexxlee.svitlo.widget.state

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

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
