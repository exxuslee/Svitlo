package org.exxlexxlee.svitlo.features.settings.models

sealed interface Event {
    data class DarkThemeChanged(val isDark: Boolean) : Event
    data object AboutClicked : Event
    data object BackClicked : Event
}
