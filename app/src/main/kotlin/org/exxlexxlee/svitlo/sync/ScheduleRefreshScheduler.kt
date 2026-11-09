package org.exxlexxlee.svitlo.sync

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ScheduleRefreshScheduler(private val workManager: WorkManager) {
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
    }
}
