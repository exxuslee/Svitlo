package org.exxlexxlee.svitlo.telemetry

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseScreenAnalytics(private val analytics: FirebaseAnalytics) : ScreenAnalytics {
    override fun trackScreen(screen: AppScreen) {
        // https://firebase.google.com/docs/analytics/screenviews#kotlin
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.analyticsName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        })
    }
}

internal class FirebaseCrashContext(private val crashlytics: FirebaseCrashlytics) : CrashContext {
    override fun setScreen(screen: AppScreen) {
        // https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
        crashlytics.setCustomKey("screen", screen.analyticsName)
    }
}
