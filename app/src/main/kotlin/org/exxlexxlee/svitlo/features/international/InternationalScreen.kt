package org.exxlexxlee.svitlo.features.international

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.exxlexxlee.svitlo.features.international.models.Action
import org.exxlexxlee.svitlo.common.ObserveActions
import org.koin.androidx.compose.koinViewModel

@Composable
fun InternationalScreen(
    onCountry: () -> Unit,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: InternationalViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()
    InternationalContent(viewState, viewModel::obtainEvent)
    ObserveActions(viewModel.viewActions()) { action ->
        when (action) {
            Action.OpenCountry -> onCountry()
            Action.Close -> onClose()
            Action.Saved -> onSaved()
        }
    }
}
