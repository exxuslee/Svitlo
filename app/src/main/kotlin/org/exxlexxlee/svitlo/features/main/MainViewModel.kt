package org.exxlexxlee.svitlo.features.main

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.svitlo.features.main.models.Action
import org.exxlexxlee.svitlo.features.main.models.Event
import org.exxlexxlee.svitlo.features.main.models.ViewState
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class MainViewModel(
    observeSelection: ObserveSelection,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState()
) {
    init {
        viewModelScope.launch {
            observeSelection().collectLatest { selection ->
                viewState = viewState.copy(
                    ready = true,
                    initialRoute = if (selection == null) {
                        Routes.Address.route
                    } else {
                        Routes.Schedule.route
                    },
                )
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) = Unit
}
