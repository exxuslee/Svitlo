package org.exxlexxlee.svitlo.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Address : Routes("address?placeId={placeId}") {
        fun create(placeId: Long? = null): String =
            if (placeId != null) "address?placeId=$placeId" else "address"
    }
}
