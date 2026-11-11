package org.exxlexxlee.svitlo.di

import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
import org.exxlexxlee.svitlo.notification.NotificationScheduler
import org.exxlexxlee.svitlo.notification.NotificationCoordinator
import org.exxlexxlee.svitlo.sync.ScheduleRefreshScheduler
import org.exxlexxlee.svitlo.sync.ScheduleWorkerFactory
import org.exxlexxlee.svitlo.widget.WidgetCoordinator
import org.exxlexxlee.svitlo.widget.WidgetManager
import org.exxlexxlee.svitlo.widget.WidgetManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val applicationScope = named("applicationScope")
val appVersion = named("appVersion")

val platformModule = module {
    single<Clock> { Clock.System }
    single(applicationScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single(appVersion) {
        val context = androidContext()
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }
    single<WidgetManager> { WidgetManagerImpl(androidContext()) }
    single { WidgetCoordinator(androidContext(), get(applicationScope), get(), get()) }
    single { NotificationScheduler(androidContext(), get()) }
    single { NotificationCoordinator(androidContext(), get(applicationScope), get(), get()) }
    single { ScheduleWorkerFactory(get(), get(), get(), get(), get(), get()) }
    single { WorkManager.getInstance(androidContext()) }
    single { ScheduleRefreshScheduler(androidContext(), get()) }
}
