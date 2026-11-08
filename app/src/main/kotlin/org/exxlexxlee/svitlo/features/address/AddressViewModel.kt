package org.exxlexxlee.svitlo.features.address

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.domain.usecase.GetAreas
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.SaveSelection
import org.exxlexxlee.svitlo.features.address.models.Action
import org.exxlexxlee.svitlo.features.address.models.AddressStep
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.features.address.models.detailLabel
import org.exxlexxlee.svitlo.ui.common.BaseViewModel

class AddressViewModel(
    private val getAreas: GetAreas,
    private val getGroups: GetGroups,
    private val saveSelection: SaveSelection,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState()
) {
    init {
        viewModelScope.launch {
            runCatching { getAreas() }
                .onSuccess { areas ->
                    viewState = viewState.copy(areas = areas, isLoadingAreas = false)
                }
                .onFailure { error ->
                    viewState = viewState.copy(isLoadingAreas = false, error = error.message)
                }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            Event.Back -> onBack()
            is Event.AreaSelected -> onAreaSelected(viewEvent.area)
            is Event.SupplierSelected -> onSupplierSelected(viewEvent.supplier)
            is Event.GroupSelected -> onGroupSelected(viewEvent.group)
            Event.FindByAddress -> viewState = viewState.copy(showAddressSearchHint = true)
            Event.DismissAddressSearch -> viewState = viewState.copy(showAddressSearchHint = false)
            is Event.AddressNameChanged -> viewState = viewState.copy(
                addressName = viewEvent.name,
                nameEdited = true,
            )
            Event.OpenReminder -> viewState = viewState.copy(showReminderPicker = true)
            is Event.ReminderSelected -> viewState = viewState.copy(
                reminderMinutes = viewEvent.minutes,
                showReminderPicker = false,
            )
            Event.DismissReminder -> viewState = viewState.copy(showReminderPicker = false)
            is Event.QuietNightChanged -> viewState = viewState.copy(quietNight = viewEvent.enabled)
            is Event.IconSelected -> viewState = viewState.copy(icon = viewEvent.icon)
            Event.Save -> save()
        }
    }

    private fun onBack() {
        viewState = when (viewState.step) {
            AddressStep.Region -> {
                viewAction = Action.Close
                viewState
            }
            AddressStep.Supplier -> viewState.copy(step = AddressStep.Region, error = null)
            AddressStep.Group -> viewState.copy(step = AddressStep.Supplier, error = null)
            AddressStep.Details -> viewState.copy(step = AddressStep.Group, error = null)
        }
    }

    private fun onAreaSelected(area: ServiceArea) {
        viewState = viewState.copy(
            selectedArea = area,
            selectedSupplier = null,
            selectedGroup = null,
            groups = emptyList(),
            step = AddressStep.Supplier,
            nameEdited = false,
            error = null,
        )
    }

    private fun onSupplierSelected(supplier: Supplier) {
        viewState = viewState.copy(
            selectedSupplier = supplier,
            selectedGroup = null,
            groups = emptyList(),
            isLoadingGroups = true,
            step = AddressStep.Group,
            nameEdited = false,
            error = null,
        )
        val supplierId = supplier.region.id
        viewModelScope.launch {
            runCatching { getGroups(supplierId) }
                .onSuccess { groups ->
                    if (viewState.selectedSupplier?.region?.id != supplierId) return@onSuccess
                    viewState = viewState.copy(groups = groups, isLoadingGroups = false)
                }
                .onFailure { error ->
                    if (viewState.selectedSupplier?.region?.id != supplierId) return@onFailure
                    viewState = viewState.copy(isLoadingGroups = false, error = error.message)
                }
        }
    }

    private fun onGroupSelected(group: OutageGroup) {
        val generated = viewState.selectedArea?.let { area ->
            "${area.name} — ${group.detailLabel()}"
        }.orEmpty()
        viewState = viewState.copy(
            selectedGroup = group,
            step = AddressStep.Details,
            addressName = if (viewState.nameEdited) viewState.addressName else generated,
            error = null,
        )
    }

    private fun save() {
        val supplier = viewState.selectedSupplier ?: return
        val group = viewState.selectedGroup ?: return
        val name = viewState.addressName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val selection = Selection.ByGroup(
                region = supplier.region,
                group = group,
                details = AddressDetails(
                    name = name,
                    reminderMinutes = viewState.reminderMinutes,
                    quietNight = viewState.quietNight,
                    iconId = viewState.icon.id,
                ),
            )
            runCatching { saveSelection(selection) }
                .onSuccess { viewAction = Action.Saved }
                .onFailure { error -> viewState = viewState.copy(error = error.message) }
        }
    }
}
