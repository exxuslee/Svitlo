package org.exxlexxlee.svitlo.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWidgetUiTest {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-09T08:00:00Z")
    }
    private val selection =
        Selection.ByGroup(
            Region("kyiv", "Київ", "yasno"), OutageGroup("3.1", "3.1"), AddressDetails("Дім"),
        )
    private val places = MutableStateFlow(listOf(Place(1, selection)))
    private val selectedPlaceId = MutableStateFlow<Long?>(null)
    private val schedule = MutableStateFlow<OutageSchedule?>(
        OutageSchedule(
            group = selection.group,
            today = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.ON }),
            tomorrow = null, sourceUpdatedAt = null, fetchedAt = clock.now(),
        )
    )
    private val selections = object : SelectionRepository {
        override fun observe(): Flow<Selection?> = error("Widget observes saved cards")
        override fun observePlaces(): Flow<List<Place>> = places

        override fun observePlace(id: Long): Flow<Place?> = error("Not used")
        override suspend fun save(selection: Selection, placeId: Long?) = error("Not used")
        override suspend fun delete(id: Long) = error("Not used")
    }
    private val schedules = object : ScheduleRepository {
        override fun observe(selection: Selection) = schedule
        override suspend fun refresh(selection: Selection): Result<Unit> =
            error("Widget must not fetch")
    }

    @Test
    fun runningSessionReceivesNewGraphAndSelectionButIgnoresFetchTimestamps() = runTest {
        val received = mutableListOf<ScheduleWidgetUi>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUi(
                ObservePlaceSchedules(ObservePlaces(selections), ObserveSchedule(schedules)),
                selectedPlaceId,
                clock,
            )
                .collect { received += it }
        }
        assertEquals(1, received.size)
        schedule.value = schedule.value!!.copy(fetchedAt = Instant.parse("2026-11-09T08:30:00Z"))
        assertEquals(1, received.size)
        schedule.value = schedule.value!!.copy(
            tomorrow = DaySchedule(
                LocalDate(2026, 11, 10), List(48) { PowerState.OFF },
            )
        )
        assertEquals(PowerState.OFF, received.last().tomorrow!!.states.first())
        places.value = listOf(Place(1, selection.copy(details = AddressDetails("Офіс"))))
        assertEquals("Офіс", received.last().title)
        places.value = emptyList()
        assertNull(received.last().today)
    }

    @Test
    fun selectedCardUpdatesLiveAndFallsBackWhenDeleted() = runTest {
        val office = selection.copy(group = OutageGroup("4.1", "4.1"), details = AddressDetails("Офіс"))
        places.value = places.value + Place(2, office)
        val officeSchedule = MutableStateFlow(schedule.value!!.copy(group = office.group))
        val perPlaceSchedules = object : ScheduleRepository {
            override fun observe(selection: Selection) =
                if (selection.group == office.group) officeSchedule else schedule

            override suspend fun refresh(selection: Selection): Result<Unit> = error("Widget must not fetch")
        }
        val received = mutableListOf<ScheduleWidgetUi>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetUi(
                ObservePlaceSchedules(ObservePlaces(selections), ObserveSchedule(perPlaceSchedules)),
                selectedPlaceId,
                clock,
            ).collect { received += it }
        }
        assertEquals("Дім", received.last().title)
        selectedPlaceId.value = 2
        assertEquals("Офіс", received.last().title)
        officeSchedule.value = officeSchedule.value.copy(
            today = officeSchedule.value.today.copy(halfHours = List(48) { PowerState.OFF }),
        )
        assertEquals(PowerState.OFF, received.last().today!!.states.first())
        places.value = places.value.filter { it.id != 2L }
        assertEquals("Дім", received.last().title)
        selectedPlaceId.value = 999
        assertEquals("Дім", received.last().title)
    }

    @Test
    fun cyclesThroughCardsAndHandlesEmptySingleAndDeletedSelections() {
        val cards = listOf(Place(10, selection), Place(20, selection), Place(30, selection))
        assertEquals(20L, nextWidgetPlaceId(cards, null))
        assertEquals(30L, nextWidgetPlaceId(cards, 20))
        assertEquals(10L, nextWidgetPlaceId(cards, 30))
        assertEquals(20L, nextWidgetPlaceId(cards, 999))
        assertEquals(10L, nextWidgetPlaceId(cards.take(1), 10))
        assertNull(nextWidgetPlaceId(emptyList(), 10))
    }
}
