package org.exxlexxlee.svitlo.features.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.widget.WidgetCoordinator
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.obtainEvent(Event.CheckWidget)
        onPauseOrDispose { }
    }

    HomeContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    when (val action = viewAction) {
        is Action.OpenAddress -> {
            val placeId = action.placeId
            viewModel.clearAction()
            navController.navigate(Routes.Address.create(placeId))
        }

        Action.OpenSettings -> {
            viewModel.clearAction()
            navController.navigate(Routes.Settings.route)
        }

        Action.RequestPinWidget -> {
            viewModel.clearAction()
            val success = WidgetCoordinator.requestPinWidget(context)
            if (!success) {
                Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
            }
        }

        null -> {}
    }
}
