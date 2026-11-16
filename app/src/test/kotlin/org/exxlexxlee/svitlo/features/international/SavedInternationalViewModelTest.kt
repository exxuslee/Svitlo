package org.exxlexxlee.svitlo.features.international

import androidx.lifecycle.ViewModelStore
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SavedInternationalViewModelTest {
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
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }
    private var fail = false
    private var calls = 0
    private val repository = object : InternationalOutageRepository {
        override fun sources(country: Country) = listOf(source)
        override suspend fun outages(source: OutageSource, address: OutageAddress?): List<InternationalOutage> {
            calls++
            if (fail) error("offline")
            return listOf(InternationalOutage("1", "London", "Fault", now.minusSeconds(600), null, point = GeoPoint(51.5, -0.1)))
        }
        override suspend fun cities(query: String) = emptyList<AddressOption>()
        override suspend fun streets(cityId: Long, query: String) = emptyList<AddressOption>()
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun viewModel() = SavedInternationalViewModel(selections, repository, clock).also { store.put("cards", it) }
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    @Test fun choosingAFeedLocationRetainsItsOptionalGeographicAnchor() = runViewModelTest {
        val wizard = InternationalViewModel(selections, repository).also { store.put("wizard", it) }
        wizard.selectCountry(Country.UNITED_KINGDOM)
        wizard.select(source)
        runCurrent()
        wizard.query("London")
        wizard.next()
        assertEquals(GeoPoint(51.5, -0.1), wizard.state.value.details.locationPoint)
        assertEquals(InternationalStep.Details, wizard.state.value.step)
    }

    @Test fun networkFailureKeepsOriginalSnapshotAndTimestampThenRecovers() = runViewModelTest {
        val vm = viewModel()
        runCurrent()
        val original = (vm.cards.value.getValue(1).outageState as OutageCardState.Content).snapshot
        now = now.plusSeconds(120)
        fail = true
        vm.refresh()
        runCurrent()
        val stale = vm.cards.value.getValue(1).outageState as OutageCardState.Stale
        assertSame(original, stale.snapshot)
        assertEquals(120L, stale.age.seconds)
        fail = false
        vm.refresh()
        runCurrent()
        assertEquals(now, (vm.cards.value.getValue(1).outageState as OutageCardState.Content).snapshot.fetchedAt)
    }

    @Test fun initialFailureIsErrorAndPollingRetriesEveryFiveMinutes() = runViewModelTest {
        fail = true
        val vm = viewModel()
        runCurrent()
        assertEquals(OutageCardState.Error, vm.cards.value.getValue(1).outageState)
        fail = false
        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(2, calls)
        assertTrue(vm.cards.value.getValue(1).outageState is OutageCardState.Content)
        places.value = emptyList()
        runCurrent()
        assertTrue(vm.cards.value.isEmpty())
    }

    @Test fun editingLocationDiscardsCachedDataBeforeNetworkFailure() = runViewModelTest {
        val vm = viewModel()
        runCurrent()
        val selection = place.selection as Selection.ByGroup
        fail = true
        places.value = listOf(place.copy(selection = selection.copy(details = selection.details!!.copy(filter = "Cambridge"))))
        runCurrent()
        assertEquals(OutageCardState.Error, vm.cards.value.getValue(1).outageState)
    }
}
