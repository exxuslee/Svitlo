package org.exxlexxlee.svitlo.features.international.models

sealed interface Action {
    data object OpenCountry : Action
    data object Close : Action
    data object Saved : Action
}
