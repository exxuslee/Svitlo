package org.exxlexxlee.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyivTimeTest {

    @Test
    fun derivesDateAndSlotAcrossLocalMidnight() {
        val clock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-08T22:00:00Z")
        }

        assertEquals(kotlinx.datetime.LocalDate(2026, 11, 9), KyivTime.today(clock))
        assertEquals(0, KyivTime.minuteOfDay(clock))
        assertEquals(0, KyivTime.halfHourIndex(clock))
    }

    @Test
    fun advancesSlotAtHalfHourBoundaryInSummerTime() {
        fun clockAt(instant: String) = object : Clock {
            override fun now(): Instant = Instant.parse(instant)
        }

        val before = clockAt("2026-07-08T09:29:59Z")
        val after = clockAt("2026-07-08T09:30:00Z")
        assertEquals(12 * 60 + 29, KyivTime.minuteOfDay(before))
        assertEquals(24, KyivTime.halfHourIndex(before))
        assertEquals(12 * 60 + 30, KyivTime.minuteOfDay(after))
        assertEquals(25, KyivTime.halfHourIndex(after))
    }

    @Test
    fun zoneInitializesWithoutException() {
        assertNotNull(KyivTime.ZONE)
        assertTrue(
            KyivTime.ZONE.id == "Europe/Kyiv" ||
                    KyivTime.ZONE.id == "Europe/Kiev" ||
                    KyivTime.ZONE.id.startsWith("UTC") ||
                    KyivTime.ZONE.id.startsWith("+02"),
        )
    }

    @Test
    fun hourOfDayReturnsValidHour() {
        val fixedClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-08T12:00:00Z")
        }
        val hour = KyivTime.hourOfDay(fixedClock)
        // UTC 12:00 in November (winter time UTC+2) is 14:00 Kyiv time
        assertEquals(14, hour)
    }
}
