package org.exxlexxlee.svitlo.features.schedule.models

sealed interface Action {
    data object OpenAddress : Action
}
