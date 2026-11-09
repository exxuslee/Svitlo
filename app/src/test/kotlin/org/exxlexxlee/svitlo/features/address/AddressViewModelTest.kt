package org.exxlexxlee.svitlo.features.address

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.domain.repository.LocationRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.usecase.*
import org.exxlexxlee.svitlo.features.address.models.Event
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val supplierA = Supplier(Region("a", "A", "provider"), true)
    private val supplierB = Supplier(Region("b", "B", "provider"), true)
    private val area = ServiceArea("area", "Area", listOf(supplierA, supplierB))
    private var loadAreas: suspend () -> List<ServiceArea> = { listOf(area) }
    private var loadGroups: suspend (String) -> List<OutageGroup> = { emptyList() }
    private var save: suspend () -> Long = { 1L }
    private var place: Place? = null
    private val locations = object : LocationRepository {
        override suspend fun areas() = loadAreas()
        override suspend fun regions() = listOf(supplierA.region, supplierB.region)
        override suspend fun cities(regionId: String) = emptyList<City>()
        override suspend fun streets(cityId: String) = emptyList<Street>()
        override suspend fun houses(streetId: String) = emptyList<House>()
        override suspend fun groups(regionId: String) = loadGroups(regionId)
    }
    private val selections = object : SelectionRepository {
        override fun observePlaces() = flowOf(emptyList<Place>())
        override fun observePlace(id: Long) = flowOf(place)
        override fun observe() = flowOf<Selection?>(null)
        override suspend fun save(selection: Selection, placeId: Long?) = save()
        override suspend fun delete(id: Long) {}
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun create(editing: Boolean = false): AddressViewModel = AddressViewModel(
        GetAreas(locations), GetGroups(locations), SaveSelection(selections),
        ObservePlace(selections), SavedStateHandle(if (editing) mapOf("placeId" to 1L) else emptyMap()),
    ).also { store.put("address", it) }

    @Test fun staleResponseDoesNotOverwriteLatestChoiceWhenReturningToSameSupplier() = runTest(dispatcher) {
        val first = CompletableDeferred<List<OutageGroup>>()
        val latest = CompletableDeferred<List<OutageGroup>>()
        var aCalls = 0
        loadGroups = { id ->
            if (id == "a") {
                val response = if (aCalls++ == 0) first else latest
                // Simulate a source that finishes even after its caller was cancelled.
                withContext(NonCancellable) { response.await() }
            } else awaitCancellation()
        }
        val vm = create()
        runCurrent()
        vm.obtainEvent(Event.SupplierSelected(supplierA)); runCurrent()
        vm.obtainEvent(Event.SupplierSelected(supplierB)); runCurrent()
        vm.obtainEvent(Event.SupplierSelected(supplierA)); runCurrent()
        val expected = listOf(OutageGroup("new", "New"))
        latest.complete(expected); runCurrent()
        first.complete(listOf(OutageGroup("old", "Old"))); runCurrent()
        assertEquals(expected, vm.viewStates().value.groups)
        assertNull(vm.viewStates().value.error)
        assertFalse(vm.viewStates().value.isLoadingGroups)
    }

    @Test fun changingAreaCancelsGroupRequest() = runTest(dispatcher) {
        var cancelled = false
        loadGroups = { try { awaitCancellation() } finally { cancelled = true } }
        val vm = create(); runCurrent()
        vm.obtainEvent(Event.AreaSelected(area))
        vm.obtainEvent(Event.SupplierSelected(supplierA)); runCurrent()
        vm.obtainEvent(Event.AreaSelected(area.copy(id = "other"))); runCurrent()
        assertTrue(cancelled)
        assertTrue(vm.viewStates().value.groups.isEmpty())
        assertNull(vm.viewStates().value.selectedSupplier)
        assertFalse(vm.viewStates().value.isLoadingGroups)
        assertNull(vm.viewStates().value.error)
    }

    @Test fun cancellationIsNotDisplayedAsAnAreaLoadError() = runTest(dispatcher) {
        loadAreas = { throw CancellationException("cancelled") }
        val vm = create(); runCurrent()
        assertNull(vm.viewStates().value.error)
        assertTrue(vm.viewStates().value.isLoadingAreas)
    }

    @Test fun cancellationDuringRestoreDoesNotProduceFallbackEditingState() = runTest(dispatcher) {
        place = Place(1L, Selection.ByGroup(supplierA.region, OutageGroup("1", "1")))
        loadGroups = { throw CancellationException("cancelled") }
        val vm = create(editing = true); runCurrent()
        assertNull(vm.viewStates().value.error)
        assertTrue(vm.viewStates().value.isLoadingAreas)
        assertNull(vm.viewStates().value.selectedGroup)
    }

    @Test fun failedGroupLoadCanBeRetriedAndCancellationDuringSaveIsNotAnError() = runTest(dispatcher) {
        loadGroups = { error("offline") }
        val vm = create(); runCurrent()
        vm.obtainEvent(Event.AreaSelected(area))
        vm.obtainEvent(Event.SupplierSelected(supplierA)); runCurrent()
        assertEquals("offline", vm.viewStates().value.error)
        assertFalse(vm.viewStates().value.isLoadingGroups)
        val group = OutageGroup("1", "1")
        loadGroups = { listOf(group) }
        vm.obtainEvent(Event.SupplierSelected(supplierA)); runCurrent()
        assertEquals(listOf(group), vm.viewStates().value.groups)
        assertNull(vm.viewStates().value.error)
        vm.obtainEvent(Event.GroupSelected(group))
        save = { throw CancellationException("cancelled") }
        vm.obtainEvent(Event.Save); runCurrent()
        assertNull(vm.viewStates().value.error)
    }

    @Test fun editingKeepsSavedGroupWhenSourceIsUnavailable() = runTest(dispatcher) {
        val selection = Selection.ByGroup(supplierA.region, OutageGroup("1", "1"), AddressDetails("Home"))
        place = Place(1L, selection)
        loadGroups = { error("offline") }
        val vm = create(editing = true); runCurrent()
        val state = vm.viewStates().value
        assertTrue(state.isEditing)
        assertFalse(state.isLoadingAreas)
        assertEquals(selection.group, state.selectedGroup)
        assertEquals("Home", state.addressName)
    }
}
