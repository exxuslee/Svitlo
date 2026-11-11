package org.exxlexxlee.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleDatesTest {
    private val schedule = OutageSchedule(
        group = OutageGroup("3.1", "3.1"),
        today = DaySchedule(LocalDate(2026, 11, 8), List(48) { PowerState.ON }),
        tomorrow = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.OFF }),
        sourceUpdatedAt = null,
        fetchedAt = Instant.parse("2026-11-08T10:00:00Z"),
    )

    @Test
    fun tomorrowBecomesTodayByItsDateAndOldGraphIsNotRepeated() {
        val rolled = schedule.forDate(LocalDate(2026, 11, 9))
        assertEquals(schedule.tomorrow, rolled.today)
        assertNull(rolled.tomorrow)
        assertEquals(schedule.fetchedAt, rolled.fetchedAt)
        assertEquals(rolled, rolled.forDate(LocalDate(2026, 11, 9)))
    }

    @Test
    fun expiredOrNonConsecutiveDaysAreNotShownUnderWrongLabels() {
        val expired = schedule.forDate(LocalDate(2026, 11, 10))
        assertFalse(expired.today.published)
        assertEquals(List(48) { PowerState.UNKNOWN }, expired.today.halfHours)
        assertNull(expired.tomorrow)
        assertNull(schedule.copy(tomorrow = schedule.tomorrow!!.copy(date = LocalDate(2026, 11, 11)))
            .forDate(LocalDate(2026, 11, 8)).tomorrow)
    }

    @Test
    fun unpublishedTomorrowRemainsUnpublishedWhenItBecomesToday() {
        assertFalse(schedule.copy(tomorrow = schedule.tomorrow!!.copy(published = false))
            .forDate(LocalDate(2026, 11, 9)).today.published)
    }

    @Test
    fun nextMidnightUsesKyivCalendarAcrossDaylightSavingTransitions() {
        fun midnightAt(value: String) = nextKyivMidnight(object : Clock {
            override fun now() = Instant.parse(value)
        })
        assertEquals(Instant.parse("2026-11-08T22:00:00Z"), midnightAt("2026-11-08T21:59:59Z"))
        assertEquals(Instant.parse("2026-11-09T22:00:00Z"), midnightAt("2026-11-08T22:00:00Z"))
        assertEquals(Instant.parse("2026-03-29T21:00:00Z"), midnightAt("2026-03-28T22:00:00Z"))
        assertEquals(Instant.parse("2026-10-25T22:00:00Z"), midnightAt("2026-10-24T21:00:00Z"))
    }
}
