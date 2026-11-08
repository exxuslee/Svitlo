package org.exxlexxlee.svitlo.features.main.models

import org.exxlexxlee.svitlo.navigation.Routes

data class ViewState(
    val ready: Boolean = false,
    val initialRoute: String = Routes.Address.route,
)
