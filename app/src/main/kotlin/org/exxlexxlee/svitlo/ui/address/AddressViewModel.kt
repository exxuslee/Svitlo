package org.exxlexxlee.svitlo.ui.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.GetRegions
import org.exxlexxlee.domain.usecase.SaveSelection

data class AddressUiState(
    val regions: List<Region> = emptyList(),
    val groups: List<OutageGroup> = emptyList(),
    val selectedRegion: Region? = null,
    val selectedGroup: OutageGroup? = null,
    val regionQuery: String = "",
    val groupQuery: String = "",
    val isLoadingRegions: Boolean = true,
    val isLoadingGroups: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val filteredRegions: List<Region>
        get() = regions.filter { it.name.contains(regionQuery, ignoreCase = true) }

    val filteredGroups: List<OutageGroup>
        get() = groups.filter {
            it.name.contains(groupQuery, ignoreCase = true) ||
                it.id.contains(groupQuery, ignoreCase = true)
        }
}

class AddressViewModel(
    private val getRegions: GetRegions,
    private val getGroups: GetGroups,
    private val saveSelection: SaveSelection,
) : ViewModel() {

    private val _state = MutableStateFlow(AddressUiState())
    val state: StateFlow<AddressUiState> = _state

    init {
        viewModelScope.launch {
            runCatching { getRegions() }
                .onSuccess { regions ->
                    _state.update { it.copy(regions = regions, isLoadingRegions = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoadingRegions = false, error = error.message)
                    }
                }
        }
    }

    fun onRegionQuery(query: String) {
        _state.update { it.copy(regionQuery = query) }
    }

    fun onGroupQuery(query: String) {
        _state.update { it.copy(groupQuery = query) }
    }

    fun onRegionSelected(region: Region) {
        _state.update {
            it.copy(
                selectedRegion = region,
                selectedGroup = null,
                groups = emptyList(),
                groupQuery = "",
                isLoadingGroups = true,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { getGroups(region.id) }
                .onSuccess { groups ->
                    _state.update { it.copy(groups = groups, isLoadingGroups = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoadingGroups = false, error = error.message) }
                }
        }
    }

    fun onGroupSelected(group: OutageGroup) {
        _state.update { it.copy(selectedGroup = group) }
    }

    fun save() {
        val region = _state.value.selectedRegion ?: return
        val group = _state.value.selectedGroup ?: return
        viewModelScope.launch {
            runCatching { saveSelection(Selection.ByGroup(region, group)) }
                .onSuccess { _state.update { it.copy(saved = true) } }
                .onFailure { error -> _state.update { it.copy(error = error.message) } }
        }
    }
}
