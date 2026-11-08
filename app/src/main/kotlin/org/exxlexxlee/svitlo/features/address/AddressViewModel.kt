package org.exxlexxlee.svitlo.features.address

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.GetRegions
import org.exxlexxlee.domain.usecase.SaveSelection
import org.exxlexxlee.svitlo.features.address.models.Action
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class AddressViewModel(
    private val getRegions: GetRegions,
    private val getGroups: GetGroups,
    private val saveSelection: SaveSelection,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState()
) {
    init {
        viewModelScope.launch {
            runCatching { getRegions() }
                .onSuccess { regions ->
                    viewState = viewState.copy(regions = regions, isLoadingRegions = false)
                }
                .onFailure { error ->
                    viewState = viewState.copy(isLoadingRegions = false, error = error.message)
                }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            is Event.RegionQuery -> viewState = viewState.copy(regionQuery = viewEvent.query)
            is Event.GroupQuery -> viewState = viewState.copy(groupQuery = viewEvent.query)
            is Event.RegionSelected -> onRegionSelected(viewEvent.region)
            is Event.GroupSelected -> onGroupSelected(viewEvent.group)
            Event.Save -> save()
        }
    }

    private fun onRegionSelected(region: Region) {
        viewState = viewState.copy(
            selectedRegion = region,
            selectedGroup = null,
            groups = emptyList(),
            groupQuery = "",
            isLoadingGroups = true,
            error = null,
        )
        viewModelScope.launch {
            runCatching { getGroups(region.id) }
                .onSuccess { groups ->
                    viewState = viewState.copy(groups = groups, isLoadingGroups = false)
                }
                .onFailure { error ->
                    viewState = viewState.copy(isLoadingGroups = false, error = error.message)
                }
        }
    }

    private fun onGroupSelected(group: OutageGroup) {
        viewState = viewState.copy(selectedGroup = group)
    }

    private fun save() {
        val region = viewState.selectedRegion ?: return
        val group = viewState.selectedGroup ?: return
        viewModelScope.launch {
            runCatching { saveSelection(Selection.ByGroup(region, group)) }
                .onSuccess { viewAction = Action.Saved }
                .onFailure { error -> viewState = viewState.copy(error = error.message) }
        }
    }
}
