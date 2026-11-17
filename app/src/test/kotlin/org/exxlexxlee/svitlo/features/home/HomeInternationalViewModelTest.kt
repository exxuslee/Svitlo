package org.exxlexxlee.svitlo.features.home

import org.exxlexxlee.svitlo.features.international.models.Event
import org.exxlexxlee.svitlo.features.international.models.InternationalStep
import org.exxlexxlee.svitlo.features.home.models.Event as HomeEvent
import org.exxlexxlee.svitlo.features.international.InternationalViewModel
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.usecase.*
import org.exxlexxlee.svitlo.widget.WidgetManager
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant as KotlinInstant
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeInternationalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val source = OutageSource("ukpn", Country.UNITED_KINGDOM, "UKPN", "https://example.com", "Europe/London", SourceAccess.LIVE)
    private val place = Place(1, Selection.ByGroup(Region("ukpn", "London", "ukpn"), OutageGroup("London", "London"),
        AddressDetails("Home", country = Country.UNITED_KINGDOM, sourceId = "ukpn", filter = "London")))
    private val places = MutableStateFlow(listOf(place))
    private val selections = object : SelectionRepository {
        override fun observePlaces() = places
        override fun observePlace(id: Long) = MutableStateFlow(places.value.find { it.id == id })
        override fun observe() = MutableStateFlow(places.value.firstOrNull()?.selection)
        override suspend fun save(selection: Selection, placeId: Long?) = 1L
        override suspend fun delete(id: Long) { places.value = places.value.filterNot { it.id == id } }
    }
    private var now = Instant.parse("2026-11-16T10:00:00Z")
    private val clock = object : Clock {
        override fun now() = KotlinInstant.parse(now.toString())
    }
    private val refreshedSelections = mutableListOf<Selection>()
    private val schedules = object : ScheduleRepository {
        override fun observe(selection: Selection) = MutableStateFlow<OutageSchedule?>(null)
        override suspend fun refresh(selection: Selection): Result<Unit> {
            refreshedSelections += selection
            return Result.success(Unit)
        }
    }
    private val widgets = object : WidgetManager {
        override fun hasInstalledWidgets() = false
        override fun isBannerDismissed() = true
        override fun setBannerDismissed(dismissed: Boolean) {}
        override fun requestPinWidget() = false
    }
    private var fail = false
    private var calls = 0
    private var response: CompletableDeferred<List<InternationalOutage>>? = null
    private val repository = object : InternationalOutageRepository {
        override fun sources(country: Country) = listOf(source)
        override suspend fun outages(source: OutageSource, address: OutageAddress?): List<InternationalOutage> {
            calls++
            if (fail) error("offline")
            response?.let { return it.await() }
            return listOf(InternationalOutage("1", "London", "Fault", now.minusSeconds(600), null, point = GeoPoint(51.5, -0.1)))
        }
        override suspend fun cities(query: String) = emptyList<AddressOption>()
        override suspend fun streets(cityId: Long, query: String) = emptyList<AddressOption>()
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun viewModel() = HomeViewModel(
        ObservePlaceSchedules(ObservePlaces(selections), ObserveSchedule(schedules)),
        RefreshSchedules(schedules), DeletePlace(selections), clock, widgets, repository,
    ).also { store.put("home", it) }
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    @Test fun choosingAFeedLocationRetainsItsOptionalGeographicAnchor() = runViewModelTest {
        val wizard = InternationalViewModel(selections, repository).also { store.put("wizard", it) }
        wizard.obtainEvent(Event.CountrySelected(Country.UNITED_KINGDOM))
        wizard.obtainEvent(Event.SourceSelected(source))
        runCurrent()
        wizard.obtainEvent(Event.QueryChanged("London"))
        wizard.obtainEvent(Event.Next)
        assertEquals(GeoPoint(51.5, -0.1), wizard.viewStates().value.details.locationPoint)
        assertEquals(InternationalStep.Details, wizard.viewStates().value.step)
    }

    @Test fun networkFailureKeepsOriginalSnapshotAndTimestampThenRecovers() = runViewModelTest {
        val vm = viewModel()
        runCurrent()
        val original = (vm.viewStates().value.cards.single { it.id == 1L }.outageState as OutageCardState.Content).snapshot
        now = now.plusSeconds(120)
        fail = true
        vm.obtainEvent(HomeEvent.Refresh)
        runCurrent()
        val stale = vm.viewStates().value.cards.single { it.id == 1L }.outageState as OutageCardState.Stale
        assertSame(original, stale.snapshot)
        assertEquals(120L, stale.age.seconds)
        assertEquals(now, vm.viewStates().value.cards.single().now)
        fail = false
        vm.obtainEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(now, (vm.viewStates().value.cards.single { it.id == 1L }.outageState as OutageCardState.Content).snapshot.fetchedAt)
    }

    @Test fun initialFailureIsErrorAndPollingRetriesEveryFiveMinutes() = runViewModelTest {
        fail = true
        val vm = viewModel()
        runCurrent()
        assertEquals(OutageCardState.Error, vm.viewStates().value.cards.single { it.id == 1L }.outageState)
        fail = false
        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(2, calls)
        assertTrue(vm.viewStates().value.cards.single { it.id == 1L }.outageState is OutageCardState.Content)
        places.value = emptyList()
        runCurrent()
        assertTrue(vm.viewStates().value.cards.isEmpty())
    }

    @Test fun editingLocationDiscardsCachedDataBeforeNetworkFailure() = runViewModelTest {
        val vm = viewModel()
        runCurrent()
        val selection = place.selection as Selection.ByGroup
        fail = true
        places.value = listOf(place.copy(selection = selection.copy(details = selection.details!!.copy(filter = "Cambridge"))))
        runCurrent()
        assertEquals(OutageCardState.Error, vm.viewStates().value.cards.single { it.id == 1L }.outageState)
    }

    @Test fun mixedPlacesKeepOrderAndRefreshThroughTheirOwnSources() = runViewModelTest {
        val ukrainian = Place(2, Selection.ByGroup(
            Region("kyiv", "Kyiv", "dtek"), OutageGroup("1", "1"), AddressDetails("Kyiv"),
        ))
        places.value = listOf(ukrainian, place)
        val vm = viewModel()
        runCurrent()
        assertEquals(listOf(2L, 1L), vm.viewStates().value.cards.map { it.id })
        assertEquals(listOf("Kyiv", "Home"), vm.viewStates().value.cards.map { it.title })
        assertEquals(listOf(ukrainian.selection), refreshedSelections)
        assertEquals(1, calls)
        vm.obtainEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(listOf(ukrainian.selection, ukrainian.selection), refreshedSelections)
        assertEquals(2, calls)
        assertFalse(vm.viewStates().value.isRefreshing)
    }

    @Test fun timeTickUpdatesCardWithoutFetchingAgain() = runViewModelTest {
        val vm = viewModel()
        runCurrent()
        val original = vm.viewStates().value.cards.single().outageState
        now = now.plusSeconds(30)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(now, vm.viewStates().value.cards.single().now)
        assertSame(original, vm.viewStates().value.cards.single().outageState)
        assertEquals(1, calls)
    }

    @Test fun refreshIndicatorWaitsForInternationalRequestAndPreservesDeleteDialog() = runViewModelTest {
        val pending = CompletableDeferred<List<InternationalOutage>>()
        response = pending
        val vm = viewModel()
        runCurrent()
        assertTrue(vm.viewStates().value.isRefreshing)
        assertEquals(OutageCardState.Loading, vm.viewStates().value.cards.single().outageState)
        vm.obtainEvent(HomeEvent.DeleteRequested(1L))
        pending.complete(emptyList())
        runCurrent()
        assertFalse(vm.viewStates().value.isRefreshing)
        assertEquals(1L, vm.viewStates().value.pendingDeleteId)
        assertTrue(vm.viewStates().value.cards.single().outageState is OutageCardState.Content)
    }
}
