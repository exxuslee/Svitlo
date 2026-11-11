package org.exxlexxlee.svitlo.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.nextKyivMidnight
import java.util.concurrent.TimeUnit

class ScheduleRefreshScheduler(private val context: Context, private val clock: Clock) {
    private val workManager get() = WorkManager.getInstance(context)

    fun start() {
        val request = PeriodicWorkRequestBuilder<ScheduleRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            // Preserve existing installations' unique work; the factory handles the legacy class.
            "svitlo-widget-refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        scheduleNextDay()
    }

    fun scheduleNextDay() {
        val midnight = nextKyivMidnight(clock)
        val request = OneTimeWorkRequestBuilder<ScheduleRefreshWorker>()
            // No network constraint: cached tomorrow must become today even while offline.
            .setInitialDelay((midnight - clock.now()).inWholeMilliseconds.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "svitlo-day-refresh-${midnight.toEpochMilliseconds()}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
