package org.exxlexxlee.svitlo.features.home.models

sealed interface Action {
    data class OpenAddress(val placeId: Long? = null) : Action
    data class OpenSchedule(val placeId: Long) : Action
}
