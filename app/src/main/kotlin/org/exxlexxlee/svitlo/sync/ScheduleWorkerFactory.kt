package org.exxlexxlee.svitlo.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.RefreshSchedules
import org.exxlexxlee.svitlo.widget.WidgetCoordinator

class ScheduleWorkerFactory(
    private val observePlaces: ObservePlaces,
    private val refreshSchedules: RefreshSchedules,
    private val observePlaceSchedules: ObservePlaceSchedules,
    private val widgetCoordinator: WidgetCoordinator,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ScheduleRefreshWorker::class.java.name,
        "org.exxlexxlee.svitlo.widget.WidgetRefreshWorker" -> ScheduleRefreshWorker(
            appContext, workerParameters, observePlaces, refreshSchedules,
            observePlaceSchedules, widgetCoordinator,
        )
        else -> null // Let WorkManager construct other workers, including Glance workers.
    }
}
