package org.exxlexxlee.svitlo.features.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.features.address.models.Action
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressScreen(
    viewModel: AddressViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)

    AddressContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    when (viewAction) {
        Action.Saved -> {
            viewModel.clearAction()
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.Address.route) { inclusive = true }
                }
            }
        }

        Action.Close -> {
            viewModel.clearAction()
            navController.popBackStack()
        }

        null -> {}
    }
}
