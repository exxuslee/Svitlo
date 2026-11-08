package org.exxlexxlee.svitlo.features.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.features.schedule.models.Action
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)

    ScheduleContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    when (viewAction) {
        Action.Close -> {
            viewModel.clearAction()
            navController.popBackStack()
        }

        null -> {}
    }
}
