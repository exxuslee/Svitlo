package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.svitlo.ui.widget.ScheduleWidgetMapper
import org.exxlexxlee.svitlo.widget.update.WidgetTimeUpdates
import org.exxlexxlee.svitlo.widget.update.observeWidgetTime

class WidgetCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observePlaceSchedules: ObservePlaceSchedules,
    private val clock: Clock,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        WidgetTimeUpdates.schedule(context)
        job = scope.launch {
            combine(observePlaceSchedules(), observeWidgetTime(clock)) { places, _ ->
                places.map {
                    it.place.id to ScheduleWidgetMapper.map(
                        it.place.selection,
                        it.schedule,
                        clock
                    )
                }
            }.distinctUntilChanged().collect { update() }
        }
    }

    suspend fun update() {
        ScheduleWidget().updateAll(context)
    }
}
