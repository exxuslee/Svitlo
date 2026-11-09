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
    private val notificationScheduler: NotificationScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val places = observePlaces().first()
        val results = refreshSchedules(places.map { it.selection })
        val schedules = observePlaceSchedules().first()
        widgetCoordinator.update()
        notificationScheduler.scheduleNotifications(schedules)
        // Apply cached and successfully refreshed data even when some requests failed.
        return when (refreshOutcome(results.values, runAttemptCount)) {
            RefreshOutcome.Success -> Result.success()
            RefreshOutcome.Retry -> Result.retry()
            RefreshOutcome.Failure -> Result.failure()
        }
    }
}
