package org.exxlexxlee.svitlo.features.international

import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class InternationalCardMapperTest {
    @Test fun everyCountryUsesSameCardWithOperatorTimezone() {
        Country.entries.filter { it != Country.UKRAINE }.forEach { country ->
            val source = OutageSource("test", country, "Operator", "https://example.com", "Africa/Johannesburg", SourceAccess.LIVE)
            val event = InternationalOutage("1", "Address", "", Instant.parse("2026-11-15T08:00:00Z"), Instant.parse("2026-11-15T09:00:00Z"))
            val card = InternationalCardMapper.map(listOf(event), source, Instant.parse("2026-11-15T08:15:00Z"))
            assertEquals("10:00–11:00", card.todayRanges)
            assertEquals(20, card.nowIndex)
            assertEquals(48, card.todayStates.size)
            assertEquals(PowerState.OFF, card.currentState)
            assertEquals(PowerState.UNKNOWN, card.todayStates[22])
            assertNull(card.tomorrowRanges)
            assertNull(card.countdownTarget)
            assertNull(card.minutesUntilChange)
        }
    }
}
