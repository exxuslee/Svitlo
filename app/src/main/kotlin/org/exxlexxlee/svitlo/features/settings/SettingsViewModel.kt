package org.exxlexxlee.svitlo.features.settings

import android.content.Context
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
    context: Context,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState(
        isDarkTheme = getDarkTheme(),
        appVersion = resolveAppVersion(context),
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
                viewAction = Action.NavigateToAbout
            }

            Event.BackClicked -> {
                viewAction = Action.NavigateBack
            }
        }
    }

    companion object {
        private fun resolveAppVersion(context: Context?): String {
            return try {
                val pInfo = context?.packageManager?.getPackageInfo(context.packageName, 0)
                pInfo?.versionName ?: "1.0"
            } catch (_: Throwable) {
                "1.0"
            }
        }
    }
}
