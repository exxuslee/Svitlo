package org.exxlexxlee.svitlo.features.address

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.domain.usecase.GetAreas
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.ObservePlace
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
    private val observePlace: ObservePlace,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState()
) {
    private val editingPlaceId: Long? = savedStateHandle.get<Long>("placeId")?.takeIf { it > 0 }

    private var groupsJob: Job? = null

    init {
        viewState = viewState.copy(isEditing = editingPlaceId != null)
        viewModelScope.launch {
            try {
                val areas = getAreas()
                val selection = editingPlaceId?.let { observePlace(it).firstOrNull()?.selection }
                if (selection == null) {
                    viewState = viewState.copy(areas = areas, isLoadingAreas = false, isEditing = false)
                } else {
                    restoreSelection(areas, selection)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                viewState = viewState.copy(isLoadingAreas = false, error = error.message)
            }
        }
    }

    private suspend fun restoreSelection(areas: List<ServiceArea>, selection: Selection) {
        val region = selection.region
        val area = areas.find { area -> area.suppliers.any { it.region.id == region.id } }
            ?: ServiceArea(region.id, region.name, listOf(Supplier(region, primary = true)))
        val supplier = area.suppliers.find { it.region.id == region.id }
            ?: Supplier(region, primary = true)
        val groups = try {
            getGroups(region.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        val details = selection.details
        viewState = viewState.copy(
            areas = areas,
            selectedArea = area,
            selectedSupplier = supplier,
            groups = groups,
            selectedGroup = groups.find { it.id == selection.group.id } ?: selection.group,
            addressName = details?.name.orEmpty(),
            nameEdited = true,
            reminderMinutes = details?.reminderMinutes ?: 15,
            quietNight = details?.quietNight ?: false,
            icon = AddressIcon.fromId(details?.iconId ?: "home"),
            step = AddressStep.Region,
            isLoadingAreas = false,
            isLoadingGroups = false,
            isEditing = true,
        )
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            Event.Close -> sendAction(Action.Close)
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
                sendAction(Action.Close)
                viewState
            }

            AddressStep.Supplier -> viewState.copy(step = AddressStep.Region, error = null)
            AddressStep.Group -> viewState.copy(step = AddressStep.Supplier, error = null)
            AddressStep.Details -> viewState.copy(step = AddressStep.Group, error = null)
        }
    }

    private fun onAreaSelected(area: ServiceArea) {
        val sameArea = area.id == viewState.selectedArea?.id
        if (!sameArea) groupsJob?.cancel()
        viewState = viewState.copy(
            isLoadingGroups = sameArea && viewState.isLoadingGroups,
            selectedArea = area,
            selectedSupplier = if (sameArea) viewState.selectedSupplier else null,
            selectedGroup = if (sameArea) viewState.selectedGroup else null,
            groups = if (sameArea) viewState.groups else emptyList(),
            step = AddressStep.Supplier,
            nameEdited = viewState.isEditing && viewState.nameEdited,
            error = null,
        )
    }

    private fun onSupplierSelected(supplier: Supplier) {
        val sameSupplier = supplier.region.id == viewState.selectedSupplier?.region?.id
        val hasGroups = sameSupplier && viewState.groups.isNotEmpty()
        groupsJob?.cancel()
        viewState = viewState.copy(
            selectedSupplier = supplier,
            selectedGroup = if (sameSupplier) viewState.selectedGroup else null,
            groups = if (sameSupplier) viewState.groups else emptyList(),
            isLoadingGroups = !hasGroups,
            step = AddressStep.Group,
            nameEdited = viewState.isEditing && viewState.nameEdited,
            error = null,
        )
        if (!hasGroups) loadGroups(supplier.region.id)
    }

    private fun loadGroups(supplierId: String) {
        groupsJob = viewModelScope.launch {
            try {
                val groups = getGroups(supplierId)
                currentCoroutineContext().ensureActive()
                viewState = viewState.copy(groups = groups, isLoadingGroups = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
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
            addressName = if (viewState.nameEdited && viewState.addressName.isNotBlank()) {
                viewState.addressName
            } else {
                generated
            },
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
            try {
                saveSelection(selection, editingPlaceId)
                sendAction(Action.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                viewState = viewState.copy(error = error.message)
            }
        }
    }
}
