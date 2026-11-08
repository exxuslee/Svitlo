package org.exxlexxlee.svitlo.features.schedule.models

sealed interface Event {
    data object Refresh : Event
    data object ChangeSelection : Event
}
