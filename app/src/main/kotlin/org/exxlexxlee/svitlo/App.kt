package org.exxlexxlee.svitlo

import android.app.Application
import androidx.work.Configuration
import org.exxlexxlee.svitlo.di.appModule
import org.exxlexxlee.svitlo.messaging.PushNotifications
import org.exxlexxlee.svitlo.notification.NotificationCoordinator
import org.exxlexxlee.svitlo.sync.ScheduleRefreshScheduler
import org.exxlexxlee.svitlo.sync.ScheduleWorkerFactory
import org.exxlexxlee.svitlo.widget.WidgetCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application(), Configuration.Provider {
    private lateinit var koin: Koin

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(koin.get<ScheduleWorkerFactory>())
            .build()

    override fun onCreate() {
        super.onCreate()
        koin = startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule)
        }.koin
        koin.get<PushNotifications>().createChannel()
        koin.get<NotificationCoordinator>().start()
        koin.get<WidgetCoordinator>().start()
        koin.get<ScheduleRefreshScheduler>().start()
    }
}
