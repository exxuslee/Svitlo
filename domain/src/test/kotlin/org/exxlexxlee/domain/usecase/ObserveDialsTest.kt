package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveDialsTest {

    @Test
    fun splitsTodayIntoAmAndPmHours() {
        val today = day(
            date = LocalDate(2026, 11, 8),
            fill = PowerState.ON,
            overrides = mapOf(
                0 to PowerState.OFF,
                1 to PowerState.OFF,
                24 to PowerState.MAYBE,
                47 to PowerState.OFF,
            ),
        )
        val schedule = OutageSchedule(
            group = OutageGroup("3.1", "Черга 3.1"),
            today = today,
            tomorrow = null,
            sourceUpdatedAt = Instant.parse("2026-11-08T09:00:00Z"),
            fetchedAt = Instant.parse("2026-11-08T09:05:00Z"),
        )

        val dials = ObserveDials.toDials(schedule)

        assertEquals(PowerState.OFF, dials.todayAm.hours[0].first)
        assertEquals(PowerState.OFF, dials.todayAm.hours[0].second)
        assertEquals(PowerState.ON, dials.todayAm.hours[1].first)
        assertEquals(PowerState.MAYBE, dials.todayPm.hours[0].first)
        assertEquals(PowerState.OFF, dials.todayPm.hours[11].second)
        assertNull(dials.tomorrowAm)
        assertNull(dials.tomorrowPm)
    }

    @Test
    fun mapsYasnoStyleHalfHoursOntoClock() {
        val halves = MutableList(48) { PowerState.ON }
        // 00:00-02:00 definite off, 03:00-03:30 possible
        repeat(4) { halves[it] = PowerState.OFF }
        halves[6] = PowerState.MAYBE
        val schedule = OutageSchedule(
            group = OutageGroup("3.1", "3.1"),
            today = DaySchedule(LocalDate(2026, 11, 8), halves),
            tomorrow = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.ON }),
            sourceUpdatedAt = Instant.parse("2026-11-08T08:55:54Z"),
            fetchedAt = Instant.parse("2026-11-08T09:00:00Z"),
        )

        val dials = ObserveDials.toDials(schedule)
        assertEquals(PowerState.OFF, dials.todayAm.hours[0].first)
        assertEquals(PowerState.OFF, dials.todayAm.hours[1].second)
        assertEquals(PowerState.ON, dials.todayAm.hours[2].first)
        assertEquals(PowerState.MAYBE, dials.todayAm.hours[3].first)
        assertEquals(PowerState.ON, dials.todayAm.hours[3].second)
        assertEquals(PowerState.ON, dials.tomorrowAm!!.hours[0].first)
    }

    @Test
    fun mapsDtekStyleFirstAndSecondHalfHours() {
        val halves = MutableList(48) { PowerState.ON }
        // hour 1 (01:00): first 30 min off
        halves[2] = PowerState.OFF
        halves[3] = PowerState.ON
        // hour 13 (13:00): second 30 min off
        halves[26] = PowerState.ON
        halves[27] = PowerState.OFF
        val schedule = OutageSchedule(
            group = OutageGroup("GPV3.1", "Черга 3.1"),
            today = DaySchedule(LocalDate(2026, 11, 8), halves),
            tomorrow = null,
            sourceUpdatedAt = Instant.parse("2026-11-08T09:22:10Z"),
            fetchedAt = Instant.parse("2026-11-08T09:30:00Z"),
        )

        val dials = ObserveDials.toDials(schedule)
        assertEquals(PowerState.OFF, dials.todayAm.hours[1].first)
        assertEquals(PowerState.ON, dials.todayAm.hours[1].second)
        assertEquals(PowerState.ON, dials.todayPm.hours[1].first)
        assertEquals(PowerState.OFF, dials.todayPm.hours[1].second)
    }

    @Test
    fun observesDialsFromRepository() = runTest {
        val selection = Selection.ByGroup(
            region = Region("kyiv", "Київ", "yasno"),
            group = OutageGroup("3.1", "3.1"),
        )
        val schedule = OutageSchedule(
            group = selection.group,
            today = DaySchedule(LocalDate(2026, 11, 8), List(48) { PowerState.OFF }),
            tomorrow = null,
            sourceUpdatedAt = Instant.parse("2026-11-08T09:00:00Z"),
            fetchedAt = Instant.parse("2026-11-08T09:05:00Z"),
        )
        val repo = FakeScheduleRepository(schedule)
        val observeDials = ObserveDials(ObserveSchedule(repo))

        val dials = observeDials(selection).first()
        assertEquals(PowerState.OFF, dials!!.todayAm.hours[0].first)
        assertEquals(PowerState.OFF, dials.todayPm.hours[11].second)
    }

    private fun day(
        date: LocalDate,
        fill: PowerState,
        overrides: Map<Int, PowerState>,
    ): DaySchedule {
        val halves = MutableList(48) { fill }
        overrides.forEach { (index, state) -> halves[index] = state }
        return DaySchedule(date, halves)
    }

    private class FakeScheduleRepository(
        schedule: OutageSchedule?,
    ) : ScheduleRepository {
        private val flow = MutableStateFlow(schedule)

        override fun observe(selection: Selection): Flow<OutageSchedule?> = flow

        override suspend fun refresh(selection: Selection): Result<Unit> = Result.success(Unit)
    }
}
