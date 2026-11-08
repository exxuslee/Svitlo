package org.exxlexxlee.svitlo.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Address : Routes("address")
    data object Schedule : Routes("schedule/{placeId}") {
        fun create(placeId: Long) = "schedule/$placeId"
    }
}
