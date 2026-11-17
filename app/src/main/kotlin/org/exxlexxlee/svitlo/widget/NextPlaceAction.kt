package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.flow.first
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.svitlo.widget.state.WidgetPlaceSelection
import org.exxlexxlee.svitlo.widget.state.nextWidgetPlaceId
import org.koin.core.context.GlobalContext

class NextPlaceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val koin = GlobalContext.getOrNull() ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val places = koin.get<ObservePlaces>()().first()
        val nextId =
            nextWidgetPlaceId(places, WidgetPlaceSelection.read(context, appWidgetId)) ?: return
        WidgetPlaceSelection.save(context, appWidgetId, nextId)
        ScheduleWidget().update(context, glanceId)
    }
}
