package org.exxlexxlee.domain.model

import org.exxlexxlee.domain.mapper.InternationalScheduleMapper
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InternationalScheduleMapperTest {
    private fun event(start: String?, end: String?) = InternationalOutage(
        "event", "Address", "", start?.let(Instant::parse), end?.let(Instant::parse),
    )
    private val date = LocalDate.parse("2026-11-15")

    @Test fun usesOperatorLocalDateAndSplitsAcrossMidnight() {
        val events = listOf(event("2026-11-16T05:30:00Z", "2026-11-16T07:00:00Z"))
        val zone = ZoneId.of("America/Winnipeg")
        val today = InternationalScheduleMapper.day(events, date, zone)
        val tomorrow = InternationalScheduleMapper.day(events, date.plusDays(1), zone)
        assertEquals("23:30–24:00", today.ranges)
        assertEquals("00:00–01:00", tomorrow.ranges)
        assertEquals(PowerState.OFF, today.states[47])
        assertEquals(listOf(PowerState.OFF, PowerState.OFF), tomorrow.states.take(2))
        assertEquals(PowerState.UNKNOWN, tomorrow.states[2])
    }

    @Test fun mergesOverlappingAndAdjacentEventsWithoutAssumingPowerBetweenThem() {
        val day = InternationalScheduleMapper.day(listOf(
            event("2026-11-15T08:00:00Z", "2026-11-15T10:00:00Z"),
            event("2026-11-15T09:30:00Z", "2026-11-15T11:00:00Z"),
            event("2026-11-15T11:00:00Z", "2026-11-15T12:00:00Z"),
        ), date, ZoneId.of("Europe/London"))
        assertEquals("08:00–12:00", day.ranges)
        assertEquals(48, day.states.size)
        assertTrue(day.states.slice(16..23).all { it == PowerState.OFF })
        assertEquals(PowerState.UNKNOWN, day.states[24])
        assertFalse(PowerState.ON in day.states)
    }

    @Test fun preservesExactMinutesAndMarksPartialCells() {
        val day = InternationalScheduleMapper.day(listOf(event("2026-11-15T08:10:00Z", "2026-11-15T09:20:00Z")), date, ZoneId.of("UTC"))
        assertEquals("08:10–09:20", day.ranges)
        assertEquals(listOf(PowerState.MAYBE, PowerState.OFF, PowerState.MAYBE), day.states.slice(16..18))
    }

    @Test fun missingInvalidAndOutOfDayIntervalsStayUnknown() {
        val day = InternationalScheduleMapper.day(listOf(
            event(null, "2026-11-15T10:00:00Z"),
            event("2026-11-15T08:00:00Z", null),
            event("2026-11-15T10:00:00Z", "2026-11-15T09:00:00Z"),
            event("2026-11-17T08:00:00Z", "2026-11-17T09:00:00Z"),
        ), date, ZoneId.of("UTC"))
        assertNull(day.ranges)
        assertTrue(day.states.all { it == PowerState.UNKNOWN })
    }

    @Test fun repeatedHourRequiresBothOccurrencesToConfirmFullSlotOutage() {
        val zone = ZoneId.of("Europe/London")
        val day = InternationalScheduleMapper.day(listOf(event("2026-10-25T00:00:00Z", "2026-10-25T00:30:00Z")), LocalDate.parse("2026-10-25"), zone)
        assertEquals(48, day.states.size)
        assertEquals(PowerState.MAYBE, day.states[2])
        assertEquals(PowerState.UNKNOWN, day.states[3])
    }

    @Test fun missingSpringHourDoesNotAcquireAnOutage() {
        val day = InternationalScheduleMapper.day(listOf(event("2026-03-29T00:00:00Z", "2026-03-29T03:00:00Z")), LocalDate.parse("2026-03-29"), ZoneId.of("Europe/London"))
        assertEquals(PowerState.UNKNOWN, day.states[2])
        assertEquals(PowerState.UNKNOWN, day.states[3])
        assertEquals(PowerState.OFF, day.states[4])
    }
}
