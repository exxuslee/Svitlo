package org.exxlexxlee.svitlo.features.home.models

sealed interface Action {
    data object OpenAddress : Action
    data class OpenSchedule(val placeId: Long) : Action
}
