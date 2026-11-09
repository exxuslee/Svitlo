package org.exxlexxlee.svitlo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <Action> ObserveActions(actions: Flow<Action>, onAction: (Action) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(actions, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            actions.collect { currentOnAction(it) }
        }
    }
}
