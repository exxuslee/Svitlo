package org.exxlexxlee.svitlo.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.exxlexxlee.svitlo.messaging.PushNotifications
import org.exxlexxlee.svitlo.telemetry.CrashContext
import org.exxlexxlee.svitlo.telemetry.FirebaseCrashContext
import org.exxlexxlee.svitlo.telemetry.FirebaseScreenAnalytics
import org.exxlexxlee.svitlo.telemetry.ScreenAnalytics
import org.exxlexxlee.svitlo.telemetry.ScreenTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val firebaseModule = module {
    single<ScreenAnalytics> { FirebaseScreenAnalytics(FirebaseAnalytics.getInstance(androidContext())) }
    single<CrashContext> { FirebaseCrashContext(FirebaseCrashlytics.getInstance()) }
    single { ScreenTracker(get(), get()) }
    single { PushNotifications(androidContext()) }
}
