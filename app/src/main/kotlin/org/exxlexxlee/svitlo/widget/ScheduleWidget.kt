package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.svitlo.widget.state.WidgetDaySelection
import org.exxlexxlee.svitlo.widget.state.WidgetPlaceSelection
import org.exxlexxlee.svitlo.widget.state.observeWidgetUi
import org.exxlexxlee.svitlo.widget.ui.ScheduleWidgetContent
import org.exxlexxlee.svitlo.widget.ui.ScheduleWidgetMapper
import org.exxlexxlee.svitlo.widget.update.WidgetTimeUpdates
import org.koin.core.context.GlobalContext

class ScheduleWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        WidgetTimeUpdates.schedule(context)
        WidgetDaySelection.migrate(context, id, appWidgetId)
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            provideContent {
                ScheduleWidgetContent(
                    ScheduleWidgetMapper.map(null, null, Clock.System),
                    appWidgetId
                )
            }
        } else {
            // Share the initial load and live observation instead of subscribing to Room twice.
            val ui = observeWidgetUi(
                koin.get<ObservePlaceSchedules>(),
                WidgetPlaceSelection.observe(context, appWidgetId),
                koin.getOrNull<Clock>() ?: Clock.System,
            ).shareIn(this, SharingStarted.Eagerly, replay = 1)
            val initial = ui.first()
            provideContent {
                val data by ui.collectAsState(initial)
                ScheduleWidgetContent(data, appWidgetId)
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetDaySelection.remove(context, appWidgetId)
        WidgetPlaceSelection.remove(context, appWidgetId)
    }
}
