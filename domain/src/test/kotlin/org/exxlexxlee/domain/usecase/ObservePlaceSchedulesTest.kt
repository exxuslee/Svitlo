package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.PlaceSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservePlaceSchedulesTest {
    private val places = MutableStateFlow<List<Place>>(emptyList())
    private val schedules = mutableMapOf<Selection, MutableStateFlow<OutageSchedule?>>()
    private val cancelled = mutableListOf<Selection>()
    private val selectionRepository = object : SelectionRepository {
        override fun observePlaces(): Flow<List<Place>> = places
        override fun observePlace(id: Long): Flow<Place?> = error("Unused")
        override fun observe(): Flow<Selection?> = error("Unused")
        override suspend fun save(selection: Selection, placeId: Long?): Long = error("Unused")
        override suspend fun delete(id: Long) = error("Unused")
    }
    private val scheduleRepository = object : ScheduleRepository {
        override fun observe(selection: Selection): Flow<OutageSchedule?> =
            schedules.getOrPut(selection) { MutableStateFlow(null) }
                .onCompletion { cancelled += selection }

        override suspend fun refresh(selection: Selection): Result<Unit> = error("Unused")
    }
    private val observe = ObservePlaceSchedules(
        ObservePlaces(selectionRepository),
        ObserveSchedule(scheduleRepository),
    )

    @Test
    fun emitsEmptyListWithoutPlaces() = runTest {
        assertEquals(emptyList<PlaceSchedule>(), observe().first())
    }

    @Test
    fun preservesPlaceOrderAndUpdatesIndividualSchedules() = runTest {
        val first = place(1)
        val second = place(2)
        places.value = listOf(second, first)
        var latest = emptyList<PlaceSchedule>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observe().collect { latest = it }
        }
        runCurrent()
        assertEquals(listOf(PlaceSchedule(second, null), PlaceSchedule(first, null)), latest)

        val schedule = schedule(first)
        schedules.getValue(first.selection).value = schedule
        runCurrent()
        assertEquals(listOf(PlaceSchedule(second, null), PlaceSchedule(first, schedule)), latest)
    }

    @Test
    fun cancelsOldSubscriptionsWhenPlaceChangesOrIsDeleted() = runTest {
        val original = place(1)
        places.value = listOf(original)
        val emissions = mutableListOf<List<PlaceSchedule>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observe().collect { emissions += it }
        }
        runCurrent()

        val edited = original.copy(selection = place(2).selection)
        places.value = listOf(edited)
        runCurrent()
        assertTrue(original.selection in cancelled)
        assertEquals(listOf(PlaceSchedule(edited, null)), emissions.last())

        val count = emissions.size
        schedules.getValue(original.selection).value = schedule(original)
        runCurrent()
        assertEquals(count, emissions.size)

        places.value = emptyList()
        runCurrent()
        assertTrue(edited.selection in cancelled)
        assertEquals(emptyList<PlaceSchedule>(), emissions.last())
    }

    private fun place(id: Long) = Place(
        id,
        Selection.ByGroup(Region("kyiv", "Київ", "yasno"), OutageGroup("$id", "$id")),
    )

    private fun schedule(place: Place) = OutageSchedule(
        group = place.selection.group,
        today = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.ON }),
        tomorrow = null,
        sourceUpdatedAt = null,
        fetchedAt = Instant.parse("2026-11-09T08:00:00Z"),
    )
}
