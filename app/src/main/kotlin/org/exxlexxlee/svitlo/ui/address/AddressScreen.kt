package org.exxlexxlee.svitlo.ui.address

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.exxlexxlee.svitlo.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    onSaved: () -> Unit,
    viewModel: AddressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.address_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.address_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.regionQuery,
                onValueChange = viewModel::onRegionQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.region_label)) },
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
            )
            if (state.isLoadingRegions) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(state.filteredRegions, key = { it.id }) { region ->
                        ListItem(
                            headlineContent = { Text(region.name) },
                            modifier = Modifier.clickable { viewModel.onRegionSelected(region) },
                            supportingContent = if (state.selectedRegion?.id == region.id) {
                                { Text("✓") }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            if (state.selectedRegion != null) {
                OutlinedTextField(
                    value = state.groupQuery,
                    onValueChange = viewModel::onGroupQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_label)) },
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    enabled = !state.isLoadingGroups,
                )
                if (state.isLoadingGroups) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(state.filteredGroups, key = { it.id }) { group ->
                            ListItem(
                                headlineContent = { Text(group.name) },
                                modifier = Modifier.clickable { viewModel.onGroupSelected(group) },
                                supportingContent = if (state.selectedGroup?.id == group.id) {
                                    { Text("✓") }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }

            state.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = state.selectedRegion != null && state.selectedGroup != null,
            ) {
                Text(stringResource(R.string.save_and_show))
            }
        }
    }
}
