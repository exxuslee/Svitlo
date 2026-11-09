package org.exxlexxlee.svitlo

import android.app.Application
import org.exxlexxlee.svitlo.di.appModule
import org.exxlexxlee.svitlo.widget.WidgetCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule)
        }
        WidgetCoordinator.start(this)
        org.exxlexxlee.svitlo.notification.NotificationCoordinator.start(this)
    }
}
