package org.exxlexxlee.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyivTimeTest {

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
