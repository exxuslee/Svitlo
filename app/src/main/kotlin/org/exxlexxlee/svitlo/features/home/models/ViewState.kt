package org.exxlexxlee.svitlo.features.home.models

import org.exxlexxlee.svitlo.features.home.PlaceCardUi

data class ViewState(
    val loaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val cards: List<PlaceCardUi> = emptyList(),
    val pendingDeleteId: Long? = null,
    val showWidgetBanner: Boolean = false,
)
