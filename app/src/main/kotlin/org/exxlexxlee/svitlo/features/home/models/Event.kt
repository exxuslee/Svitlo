package org.exxlexxlee.svitlo.features.home.models

sealed interface Event {
    data object Add : Event
    data object Refresh : Event
    data class DeleteRequested(val id: Long) : Event
    data class Edit(val id: Long) : Event
    data object ConfirmDelete : Event
    data object DismissDelete : Event
}
