package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules

class WidgetCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observePlaceSchedules: ObservePlaceSchedules,
    private val clock: Clock,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            observePlaceSchedules().map { places ->
                places.map { it.place.id to ScheduleWidgetMapper.map(it.place.selection, it.schedule, clock) }
            }.distinctUntilChanged().collect { update() }
        }
    }

    suspend fun update() {
        ScheduleWidget().updateAll(context)
    }
}
