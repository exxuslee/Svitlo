package org.exxlexxlee.svitlo.navigation

sealed class Routes(val route: String) {
    data object Address : Routes("address")
    data object Schedule : Routes("schedule")
}
