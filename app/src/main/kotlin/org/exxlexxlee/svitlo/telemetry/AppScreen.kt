package org.exxlexxlee.svitlo.telemetry

import org.exxlexxlee.svitlo.navigation.Routes

/** Only these fixed names may be sent to telemetry, never route arguments. */
enum class AppScreen(val analyticsName: String) {
    HOME("home"),
    ADDRESS("address"),
    SETTINGS("settings"),
    ABOUT("about");

    companion object {
        fun fromRoute(route: String?): AppScreen? = when (route) {
            Routes.Home.route -> HOME
            Routes.Address.route -> ADDRESS
            Routes.Settings.route -> SETTINGS
            Routes.About.route -> ABOUT
            else -> null
        }
    }
}
