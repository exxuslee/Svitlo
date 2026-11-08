package org.exxlexxlee.svitlo.features.address.models

sealed interface Action {
    data object Saved : Action
}
