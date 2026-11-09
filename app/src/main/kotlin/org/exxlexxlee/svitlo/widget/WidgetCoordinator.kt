package org.exxlexxlee.svitlo.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.PlaceSchedule
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

object WidgetCoordinator {
    private const val WORK_NAME = "svitlo-widget-refresh"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context) {
        val appContext = context.applicationContext
        enqueueRefresh(appContext)
        observeAndUpdate(appContext)
    }

    suspend fun update(context: Context) {
        ScheduleWidget().updateAll(context.applicationContext)
    }

    fun hasInstalledWidgets(context: Context): Boolean =
        WidgetManagerImpl(context).hasInstalledWidgets()

    fun requestPinWidget(context: Context): Boolean =
        WidgetManagerImpl(context).requestPinWidget()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAndUpdate(context: Context) {
        scope.launch {
            val koin = GlobalContext.getOrNull() ?: return@launch
            val observeSelection = koin.get<ObserveSelection>()
            val observeSchedule = koin.get<ObserveSchedule>()
            observeSelection()
                .flatMapLatest { selection ->
                    if (selection == null) flowOf(null) else observeSchedule(selection)
                }
                .distinctUntilChanged()
                .collect { update(context) }
        }
    }

    private fun enqueueRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val koin = GlobalContext.getOrNull() ?: return Result.retry()
        val places = koin.get<ObservePlaces>()().first()
        val refresh = koin.get<RefreshSchedule>()
        val observeSchedule = koin.get<ObserveSchedule>()
        val placeSchedules = places.map { place ->
            refresh(place.selection)
            val schedule = observeSchedule(place.selection).first()
            PlaceSchedule(place, schedule)
        }
        WidgetCoordinator.update(applicationContext)
        org.exxlexxlee.svitlo.notification.NotificationScheduler.scheduleNotifications(applicationContext, placeSchedules)
        return Result.success()
    }
}
