package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection

class WidgetCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observeSelection: ObserveSelection,
    private val observeSchedule: ObserveSchedule,
    private val clock: Clock,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            observeWidgetUi(observeSelection, observeSchedule, clock).collect { update() }
        }
    }

    suspend fun update() {
        ScheduleWidget().updateAll(context)
    }
}
