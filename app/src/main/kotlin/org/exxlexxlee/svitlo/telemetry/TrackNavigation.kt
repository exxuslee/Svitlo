package org.exxlexxlee.svitlo.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController

@Composable
fun TrackNavigation(navController: NavController, tracker: ScreenTracker) {
    DisposableEffect(navController, tracker) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            tracker.onDestinationChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}
