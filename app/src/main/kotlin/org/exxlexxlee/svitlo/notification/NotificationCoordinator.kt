package org.exxlexxlee.svitlo.notification

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules

class NotificationCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observePlaceSchedules: ObservePlaceSchedules,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        NotificationHelper.createNotificationChannel(context)
        job = scope.launch {
            observePlaceSchedules().distinctUntilChanged().collect { schedules ->
                NotificationScheduler.scheduleNotifications(context, schedules)
            }
        }
    }
}
