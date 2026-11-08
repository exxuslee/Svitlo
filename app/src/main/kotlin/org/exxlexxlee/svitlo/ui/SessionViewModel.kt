package org.exxlexxlee.svitlo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObserveSelection

data class SessionUiState(
    val ready: Boolean = false,
    val selection: Selection? = null,
)

class SessionViewModel(
    observeSelection: ObserveSelection,
) : ViewModel() {
    val state: StateFlow<SessionUiState> = observeSelection()
        .map { selection -> SessionUiState(ready = true, selection = selection) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionUiState(),
        )
}
