package org.exxlexxlee.svitlo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.exxlexxlee.svitlo.ui.address.AddressScreen
import org.exxlexxlee.svitlo.ui.schedule.ScheduleScreen
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val ADDRESS = "address"
    const val SCHEDULE = "schedule"
}

@Composable
fun SvitloNav(
    sessionViewModel: SessionViewModel = koinViewModel(),
) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    if (!session.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val start = if (session.selection == null) Routes.ADDRESS else Routes.SCHEDULE
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ADDRESS) {
            AddressScreen(
                onSaved = {
                    navController.navigate(Routes.SCHEDULE) {
                        popUpTo(Routes.ADDRESS) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SCHEDULE) {
            ScheduleScreen(
                onChangeSelection = {
                    navController.navigate(Routes.ADDRESS)
                },
            )
        }
    }
}
