package org.exxlexxlee.svitlo.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainContent(
    mainViewModel: MainViewModel = koinViewModel(),
) {
    val viewState by mainViewModel.viewStates().collectAsState()
    if (!viewState.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHostContent(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            viewState = viewState,
        )
    }
}
