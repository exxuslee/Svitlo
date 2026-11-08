package org.exxlexxlee.svitlo

import android.app.Application
import org.exxlexxlee.data.di.dataModule
import org.exxlexxlee.svitlo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SvitloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SvitloApp)
            modules(dataModule, appModule)
        }
    }
}
