package org.exxlexxlee.svitlo.features.main

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.GetDarkTheme
import org.exxlexxlee.domain.usecase.ObserveDarkTheme
import org.exxlexxlee.svitlo.features.main.models.Action
import org.exxlexxlee.svitlo.features.main.models.Event
import org.exxlexxlee.svitlo.features.main.models.ViewState
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class MainViewModel(
    private val observeDarkTheme: ObserveDarkTheme,
    getDarkTheme: GetDarkTheme,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState(
        ready = true,
        initialRoute = Routes.Home.route,
        isDarkTheme = getDarkTheme(),
    ),
) {
    init {
        viewModelScope.launch {
            observeDarkTheme().collect { isDark ->
                viewState = viewState.copy(isDarkTheme = isDark)
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) = Unit
}
