package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import org.exxlexxlee.svitlo.widget.state.WidgetDaySelection

/** Handles PendingIntents left in a launcher by an older installed version. */
class SelectDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val tomorrow = parameters[ActionParameters.Key<Boolean>("widget_show_tomorrow")] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetDaySelection.save(context, appWidgetId, tomorrow)
        ScheduleWidget().update(context, glanceId)
    }
}
