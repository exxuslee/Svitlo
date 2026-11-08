package org.exxlexxlee.svitlo.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)

    HomeContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    when (val action = viewAction) {
        Action.OpenAddress -> {
            viewModel.clearAction()
            navController.navigate(Routes.Address.route)
        }

        is Action.OpenSchedule -> {
            viewModel.clearAction()
            navController.navigate(Routes.Schedule.create(action.placeId))
        }

        null -> {}
    }
}
