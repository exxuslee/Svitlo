package org.exxlexxlee.svitlo.features.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.localize.LocalNavController
import org.exxlexxlee.svitlo.common.ObserveActions
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
    var showWidgetHelp by rememberSaveable { mutableStateOf(false) }

    if (showWidgetHelp) {
        AlertDialog(
            onDismissRequest = { showWidgetHelp = false },
            title = { Text(stringResource(R.string.widget_banner_button)) },
            text = { Text(stringResource(R.string.widget_pin_unsupported)) },
            confirmButton = {
                TextButton(onClick = { showWidgetHelp = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

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
                showWidgetHelp = !widgetManager.requestPinWidget()
            }
        }
    }
}
