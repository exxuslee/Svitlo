package org.exxlexxlee.svitlo.features.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.usecase.GetDarkTheme
import org.exxlexxlee.domain.usecase.ObserveDarkTheme
import org.exxlexxlee.domain.usecase.SetDarkTheme
import org.exxlexxlee.svitlo.features.settings.models.Action
import org.exxlexxlee.svitlo.features.settings.models.Event
import org.exxlexxlee.svitlo.features.settings.models.ViewState
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class SettingsViewModel(
    private val observeDarkTheme: ObserveDarkTheme,
    getDarkTheme: GetDarkTheme,
    private val setDarkTheme: SetDarkTheme,
    appVersion: String,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState(
        isDarkTheme = getDarkTheme(),
        appVersion = appVersion,
    ),
) {
    init {
        viewModelScope.launch {
            observeDarkTheme().collect { isDark ->
                viewState = viewState.copy(isDarkTheme = isDark)
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            is Event.DarkThemeChanged -> {
                viewModelScope.launch {
                    setDarkTheme(viewEvent.isDark)
                }
            }

            Event.AboutClicked -> {
                sendAction(Action.NavigateToAbout)
            }

            Event.BackClicked -> {
                sendAction(Action.NavigateBack)
            }
        }
    }
}
