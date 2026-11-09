package org.exxlexxlee.svitlo.features.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.common.ObserveActions
import org.exxlexxlee.svitlo.widget.WidgetManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()
    val widgetManager: WidgetManager = koinInject()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.obtainEvent(Event.CheckWidget)
        onPauseOrDispose { }
    }

    HomeContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )

    ObserveActions(viewModel.viewActions()) { action ->
        when (action) {
            is Action.OpenAddress -> {
                val placeId = action.placeId
                navController.navigate(Routes.Address.create(placeId))
            }

            Action.OpenSettings -> {
                navController.navigate(Routes.Settings.route)
            }

            Action.RequestPinWidget -> {
                val success = widgetManager.requestPinWidget()
                if (!success) {
                    Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
