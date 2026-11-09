package org.exxlexxlee.svitlo.features.settings.models

sealed interface Action {
    data object NavigateToAbout : Action
    data object NavigateBack : Action
}
