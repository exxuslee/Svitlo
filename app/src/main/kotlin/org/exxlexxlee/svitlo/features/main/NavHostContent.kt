package org.exxlexxlee.svitlo.features.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.exxlexxlee.svitlo.features.address.AddressScreen
import org.exxlexxlee.svitlo.features.home.HomeScreen
import org.exxlexxlee.svitlo.features.main.models.ViewState
import org.exxlexxlee.svitlo.features.settings.AboutScreen
import org.exxlexxlee.svitlo.features.settings.SettingsScreen
import org.exxlexxlee.svitlo.navigation.Routes

@Composable
fun NavHostContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewState: ViewState,
) {
    NavHost(
        navController = navController,
        startDestination = viewState.initialRoute,
        modifier = modifier,
    ) {
        composable(Routes.Home.route) { HomeScreen() }
        composable(
            route = Routes.Address.route,
            arguments = listOf(
                navArgument("placeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { AddressScreen() }
        composable(Routes.Settings.route) { SettingsScreen() }
        composable(Routes.About.route) { AboutScreen() }
    }
}
