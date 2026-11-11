package org.exxlexxlee.svitlo.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.koin.core.context.GlobalContext

class NextPlaceAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val koin = GlobalContext.getOrNull() ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val places = koin.get<ObservePlaces>()().first()
        val nextId = nextWidgetPlaceId(places, WidgetPlaceSelection.read(context, appWidgetId)) ?: return
        WidgetPlaceSelection.save(context, appWidgetId, nextId)
        ScheduleWidget().update(context, glanceId)
    }
}

internal fun nextWidgetPlaceId(places: List<Place>, selectedId: Long?): Long? {
    if (places.isEmpty()) return null
    val currentIndex = places.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    return places[(currentIndex + 1) % places.size].id
}

internal object WidgetPlaceSelection {
    private const val PREFS = "widget_selected_place"
    private fun key(id: Int) = "place_$id"
    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(context: Context, id: Int): Long? {
        val prefs = preferences(context)
        return if (prefs.contains(key(id))) prefs.getLong(key(id), 0L) else null
    }

    fun save(context: Context, id: Int, placeId: Long) {
        preferences(context).edit { putLong(key(id), placeId) }
    }

    fun remove(context: Context, id: Int) {
        preferences(context).edit { remove(key(id)) }
    }

    fun observe(context: Context, id: Int) = callbackFlow {
        val prefs = preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key(id) || changedKey == null) trySend(read(context, id))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read(context, id))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
