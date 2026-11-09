package org.exxlexxlee.svitlo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.exxlexxlee.domain.usecase.GetDarkTheme
import org.exxlexxlee.domain.usecase.ObserveDarkTheme

class AppViewModel(
    observeDarkTheme: ObserveDarkTheme,
    getDarkTheme: GetDarkTheme,
) : ViewModel() {
    val isDarkTheme = observeDarkTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = getDarkTheme(),
    )
}
