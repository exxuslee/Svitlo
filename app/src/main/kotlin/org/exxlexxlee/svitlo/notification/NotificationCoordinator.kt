package org.exxlexxlee.svitlo.notification

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.koin.core.context.GlobalContext

object NotificationCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context) {
        val appContext = context.applicationContext
        NotificationHelper.createNotificationChannel(appContext)
        observeAndUpdate(appContext)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAndUpdate(context: Context) {
        scope.launch {
            val koin = GlobalContext.getOrNull() ?: return@launch
            val observePlaces = koin.get<ObservePlaces>()
            val observeSchedule = koin.get<ObserveSchedule>()

            observePlaces()
                .flatMapLatest { places ->
                    if (places.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val flows = places.map { place ->
                            observeSchedule(place.selection).map { schedule -> place to schedule }
                        }
                        combine(flows) { array -> array.toList() }
                    }
                }
                .distinctUntilChanged()
                .collect { placeSchedulePairs ->
                    NotificationScheduler.scheduleNotifications(context, placeSchedulePairs)
                }
        }
    }
}
