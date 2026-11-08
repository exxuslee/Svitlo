package org.exxlexxlee.svitlo.features.main

import org.exxlexxlee.svitlo.features.main.models.Action
import org.exxlexxlee.svitlo.features.main.models.Event
import org.exxlexxlee.svitlo.features.main.models.ViewState
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class MainViewModel : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState(ready = true, initialRoute = Routes.Home.route),
) {
    override fun obtainEvent(viewEvent: Event) = Unit
}
