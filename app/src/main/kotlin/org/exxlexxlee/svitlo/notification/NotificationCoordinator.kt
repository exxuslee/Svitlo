package org.exxlexxlee.svitlo.notification

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.koin.core.context.GlobalContext

object NotificationCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context) {
        val appContext = context.applicationContext
        NotificationHelper.createNotificationChannel(appContext)
        observeAndUpdate(appContext)
    }

    private fun observeAndUpdate(context: Context) {
        scope.launch {
            val koin = GlobalContext.getOrNull() ?: return@launch
            val observePlaceSchedules = koin.get<ObservePlaceSchedules>()
            observePlaceSchedules()
                .distinctUntilChanged()
                .collect { placeSchedules ->
                    NotificationScheduler.scheduleNotifications(context, placeSchedules)
                }
        }
    }
}
