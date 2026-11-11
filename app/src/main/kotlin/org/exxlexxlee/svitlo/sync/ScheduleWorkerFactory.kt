package org.exxlexxlee.svitlo.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.RefreshSchedules
import org.exxlexxlee.svitlo.notification.NotificationScheduler
import org.exxlexxlee.svitlo.widget.WidgetCoordinator

class ScheduleWorkerFactory(
    private val observePlaces: ObservePlaces,
    private val refreshSchedules: RefreshSchedules,
    private val observePlaceSchedules: ObservePlaceSchedules,
    private val widgetCoordinator: WidgetCoordinator,
    private val notificationScheduler: NotificationScheduler,
    private val refreshScheduler: ScheduleRefreshScheduler,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ScheduleRefreshWorker::class.java.name,
        "org.exxlexxlee.svitlo.widget.WidgetRefreshWorker" -> ScheduleRefreshWorker(
            appContext, workerParameters, observePlaces, refreshSchedules,
            observePlaceSchedules, widgetCoordinator, notificationScheduler, refreshScheduler,
        )
        else -> null // Let WorkManager construct other workers, including Glance workers.
    }
}
