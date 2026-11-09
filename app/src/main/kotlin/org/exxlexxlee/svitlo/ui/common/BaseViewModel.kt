package org.exxlexxlee.svitlo.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : Any, Action : Any, Event>(initialState: State) : ViewModel() {
    private val _viewStates = MutableStateFlow(initialState)
    private val actions = Channel<Action>(Channel.BUFFERED)
    private val actionFlow = actions.receiveAsFlow()

    fun viewStates(): StateFlow<State> = _viewStates.asStateFlow()

    // One screen consumes each action once. Pending actions wait while the screen is paused.
    fun viewActions(): Flow<Action> = actionFlow

    protected var viewState: State
        get() = _viewStates.value
        set(value) {
            _viewStates.update { value }
        }

    protected fun sendAction(action: Action) {
        viewModelScope.launch { actions.send(action) }
    }

    abstract fun obtainEvent(viewEvent: Event)
}
