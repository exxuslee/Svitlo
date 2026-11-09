package org.exxlexxlee.svitlo.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.RefreshSchedules
import org.exxlexxlee.svitlo.notification.NotificationScheduler
import org.exxlexxlee.svitlo.widget.WidgetCoordinator

class ScheduleRefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val observePlaces: ObservePlaces,
    private val refreshSchedules: RefreshSchedules,
    private val observePlaceSchedules: ObservePlaceSchedules,
    private val widgetCoordinator: WidgetCoordinator,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val places = observePlaces().first()
        refreshSchedules(places.map { it.selection })
        val schedules = observePlaceSchedules().first()
        widgetCoordinator.update()
        NotificationScheduler.scheduleNotifications(applicationContext, schedules)
        return Result.success()
    }
}
