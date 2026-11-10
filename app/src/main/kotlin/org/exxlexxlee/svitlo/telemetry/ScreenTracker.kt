package org.exxlexxlee.svitlo.telemetry

fun interface ScreenAnalytics {
    fun trackScreen(screen: AppScreen)
}

fun interface CrashContext {
    fun setScreen(screen: AppScreen)
}

class ScreenTracker(
    private val analytics: ScreenAnalytics,
    private val crashContext: CrashContext,
) {
    fun onDestinationChanged(route: String?) {
        val screen = AppScreen.fromRoute(route) ?: return
        crashContext.setScreen(screen)
        analytics.trackScreen(screen)
    }
}
