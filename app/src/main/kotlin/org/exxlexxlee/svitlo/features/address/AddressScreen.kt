package org.exxlexxlee.svitlo.features.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.features.address.models.Action
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.common.ObserveActions
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressScreen(
    viewModel: AddressViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()

    AddressContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    ObserveActions(viewModel.viewActions()) { action ->
        when (action) {
            Action.Saved -> {
                val popped = navController.popBackStack()
                if (!popped) {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Address.route) { inclusive = true }
                    }
                }
            }

            Action.Close -> {
                navController.popBackStack()
            }
        }
    }
}
