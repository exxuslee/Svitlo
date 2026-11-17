package org.exxlexxlee.svitlo.features.international

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.svitlo.features.address.models.Event as DetailsEvent
import org.exxlexxlee.svitlo.features.international.models.Action
import org.exxlexxlee.svitlo.features.international.models.Event
import org.exxlexxlee.svitlo.features.international.models.InternationalStep
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InternationalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val source = OutageSource(
        "ukpn", Country.UNITED_KINGDOM, "UKPN", "https://example.com",
        "Europe/London", SourceAccess.LIVE,
    )
    private var save: suspend () -> Long = { 1L }
    private var saved: Selection? = null
    private val selections = object : SelectionRepository {
        override fun observePlaces() = flowOf(emptyList<Place>())
        override fun observePlace(id: Long) = flowOf<Place?>(null)
        override fun observe() = flowOf<Selection?>(null)
        override suspend fun save(selection: Selection, placeId: Long?): Long {
            val id = save()
            saved = selection
            return id
        }
        override suspend fun delete(id: Long) {}
    }
    private val repository = object : InternationalOutageRepository {
        override fun sources(country: Country) = listOf(source)
        override suspend fun outages(source: OutageSource, address: OutageAddress?) =
            emptyList<InternationalOutage>()
        override suspend fun cities(query: String) = emptyList<AddressOption>()
        override suspend fun streets(cityId: Long, query: String) = emptyList<AddressOption>()
    }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }

    private fun create() = InternationalViewModel(selections, repository).also {
        store.put("wizard", it)
        it.obtainEvent(Event.CountrySelected(Country.UNITED_KINGDOM))
    }

    @Test fun backWithinWizardChangesStepAndBackFromSupplierEmitsCountryAction() = runTest(dispatcher) {
        val vm = create()
        val actions = mutableListOf<Action>()
        backgroundScope.launch { vm.viewActions().collect { actions += it } }
        vm.obtainEvent(Event.SourceSelected(source))
        runCurrent()
        vm.obtainEvent(Event.LocationSelected("London"))
        assertEquals(InternationalStep.Details, vm.viewStates().value.step)
        vm.obtainEvent(Event.Back)
        assertEquals(InternationalStep.Location, vm.viewStates().value.step)
        vm.obtainEvent(Event.Back)
        assertEquals(InternationalStep.Supplier, vm.viewStates().value.step)
        runCurrent()
        assertTrue(actions.isEmpty())
        vm.obtainEvent(Event.Back)
        runCurrent()
        assertEquals(listOf(Action.OpenCountry), actions)
    }

    @Test fun saveBlocksNavigationAndEmitsSavedOnlyAfterPersistence() = runTest(dispatcher) {
        val result = CompletableDeferred<Long>()
        save = { result.await() }
        val vm = create()
        val actions = mutableListOf<Action>()
        backgroundScope.launch { vm.viewActions().collect { actions += it } }
        vm.obtainEvent(Event.SourceSelected(source))
        runCurrent()
        vm.obtainEvent(Event.LocationSelected(" London "))
        vm.obtainEvent(Event.DetailsChanged(DetailsEvent.AddressNameChanged(" Home ")))
        vm.obtainEvent(Event.DetailsChanged(DetailsEvent.Save))
        runCurrent()
        assertTrue(vm.viewStates().value.saving)
        vm.obtainEvent(Event.Back)
        vm.obtainEvent(Event.Close)
        vm.obtainEvent(Event.Save)
        runCurrent()
        assertEquals(InternationalStep.Details, vm.viewStates().value.step)
        assertTrue(actions.isEmpty())
        assertNull(saved)
        result.complete(1L)
        runCurrent()
        assertFalse(vm.viewStates().value.saving)
        assertEquals("Home", saved?.details?.name)
        assertEquals("London", saved?.details?.filter)
        assertEquals(Country.UNITED_KINGDOM, saved?.details?.country)
        assertEquals(listOf(Action.Saved), actions)
    }

    @Test fun saveFailureKeepsDetailsAndAllowsRetryWithoutNavigating() = runTest(dispatcher) {
        save = { error("offline") }
        val vm = create()
        val actions = mutableListOf<Action>()
        backgroundScope.launch { vm.viewActions().collect { actions += it } }
        vm.obtainEvent(Event.SourceSelected(source))
        runCurrent()
        vm.obtainEvent(Event.LocationSelected("London"))
        vm.obtainEvent(Event.Save)
        runCurrent()
        assertTrue(vm.viewStates().value.failed)
        assertTrue(vm.viewStates().value.canSave)
        assertEquals("London", vm.viewStates().value.details.name)
        assertTrue(actions.isEmpty())
        save = { 1L }
        vm.obtainEvent(Event.Save)
        runCurrent()
        assertFalse(vm.viewStates().value.failed)
        assertEquals(listOf(Action.Saved), actions)
    }
}
