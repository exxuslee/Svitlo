package org.exxlexxlee.svitlo.navigation

import androidx.navigation.NavDestination

fun NavDestination?.asRoute(): Routes? {
    return when (this?.route) {
        Routes.Home.route -> Routes.Home
        Routes.Address.route -> Routes.Address
        else -> null
    }
}
