package org.exxlexxlee.svitlo.telemetry

import org.exxlexxlee.svitlo.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTrackerTest {
    private val analyticsScreens = mutableListOf<AppScreen>()
    private val crashScreens = mutableListOf<AppScreen>()
    private val tracker = ScreenTracker(
        ScreenAnalytics { analyticsScreens += it },
        CrashContext { crashScreens += it },
    )

    @Test
    fun allDestinationsUseSameFixedNamesForAnalyticsAndCrashContext() {
        listOf(Routes.Home, Routes.Address, Routes.Settings, Routes.About).forEach {
            tracker.onDestinationChanged(it.route)
        }

        assertEquals(listOf("home", "address", "settings", "about"), analyticsScreens.map { it.analyticsName })
        assertEquals(analyticsScreens, crashScreens)
    }

    @Test
    fun unknownAndConcreteRoutesCannotLeakArguments() {
        listOf(null, "", "address?placeId=42", "home/private-address", "unknown").forEach {
            tracker.onDestinationChanged(it)
        }

        assertTrue(analyticsScreens.isEmpty())
        assertTrue(crashScreens.isEmpty())
    }

    @Test
    fun returningToScreenIsANewView() {
        listOf(Routes.Home, Routes.Settings, Routes.Home).forEach {
            tracker.onDestinationChanged(it.route)
        }

        assertEquals(listOf(AppScreen.HOME, AppScreen.SETTINGS, AppScreen.HOME), analyticsScreens)
        assertEquals(analyticsScreens, crashScreens)
    }
}
