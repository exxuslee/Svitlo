package org.exxlexxlee.svitlo.features.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.svitlo.features.address.models.Action
import org.exxlexxlee.svitlo.features.address.models.AddressStep
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.international.InternationalScreen
import org.exxlexxlee.svitlo.features.international.InternationalViewModel
import org.exxlexxlee.svitlo.features.international.models.Event as InternationalEvent
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.localize.LocalNavController
import org.exxlexxlee.svitlo.common.ObserveActions
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressScreen(
    viewModel: AddressViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()
    val international: InternationalViewModel = koinViewModel()

    if (viewState.step != AddressStep.Country && viewState.country != Country.UKRAINE) {
        InternationalScreen(
            viewModel = international,
            onCountry = { viewModel.obtainEvent(Event.Back) },
            onClose = { viewModel.obtainEvent(Event.Close) },
            onSaved = { navController.popBackStack() },
        )
    } else {
        AddressContent(
            viewState = viewState,
            eventHandler = { event ->
                if (event is Event.CountrySelected) {
                    international.obtainEvent(InternationalEvent.CountrySelected(event.country))
                }
                viewModel.obtainEvent(event)
            },
        )
    }

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
