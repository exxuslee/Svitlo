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
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWidgetUiTest {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-09T08:00:00Z")
    }
    private val selection = MutableStateFlow<Selection?>(
        Selection.ByGroup(
            Region("kyiv", "Київ", "yasno"), OutageGroup("3.1", "3.1"), AddressDetails("Дім"),
        )
    )
    private val schedule = MutableStateFlow<OutageSchedule?>(
        OutageSchedule(
            group = selection.value!!.group,
            today = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.ON }),
            tomorrow = null, sourceUpdatedAt = null, fetchedAt = clock.now(),
        )
    )
    private val selections = object : SelectionRepository {
        override fun observe() = selection
        override fun observePlaces(): Flow<List<Place>> =
            error("Widget should only observe first selection")

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
            observeWidgetUi(ObserveSelection(selections), ObserveSchedule(schedules), clock)
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
        selection.value =
            (selection.value as Selection.ByGroup).copy(details = AddressDetails("Офіс"))
        assertEquals("Офіс", received.last().title)
        selection.value = null
        assertNull(received.last().today)
    }
}
