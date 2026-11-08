package org.exxlexxlee.svitlo.features.home

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceCardMapperTest {
    @Test
    fun mapsPublishedWindowsAndCountdown() {
        val halves = MutableList(48) { PowerState.ON }
        for (index in 36 until 42) halves[index] = PowerState.OFF
        val card = PlaceCardMapper.map(
            place = place(),
            schedule = schedule(halves, tomorrowPublished = true, suspended = false),
            minuteOfDay = 18 * 60 + 26,
            error = null,
        )

        assertEquals("Київська область", card.title)
        assertEquals("Група 2.2", card.groupLabel)
        assertEquals("18:00–21:00", card.todayRanges)
        assertEquals("", card.tomorrowRanges)
        assertEquals(PowerState.OFF, card.currentState)
        assertEquals(2 * 60 + 34, card.minutesUntilChange)
        assertEquals(CountdownTarget.PowerOn, card.countdownTarget)
        assertTrue(card.showNowMarker)
        assertEquals(36, card.nowIndex)
        assertFalse(card.schedulesSuspended)
    }

    @Test
    fun hidesUnpublishedTomorrow() {
        val halves = List(48) { PowerState.ON }
        val card = PlaceCardMapper.map(
            place = place(),
            schedule = schedule(halves, tomorrowPublished = false, suspended = true),
            minuteOfDay = 12 * 60,
            error = "немає мережі",
        )

        assertNull(card.tomorrowRanges)
        assertTrue(card.tomorrowStates.all { it == PowerState.UNKNOWN })
        assertTrue(card.schedulesSuspended)
        assertEquals("немає мережі", card.error)
        assertNull(card.minutesUntilChange)
    }

    private fun place() = Place(
        id = 7,
        selection = Selection.ByGroup(
            region = Region(id = "kyiv-region", name = "Київська область", providerId = "dtek"),
            group = OutageGroup(id = "2.2", name = "2.2"),
            details = AddressDetails(name = "Київська область"),
        ),
    )

    private fun schedule(
        today: List<PowerState>,
        tomorrowPublished: Boolean,
        suspended: Boolean,
    ) = OutageSchedule(
        group = OutageGroup("2.2", "2.2"),
        today = DaySchedule(LocalDate(2026, 11, 8), today),
        tomorrow = DaySchedule(
            date = LocalDate(2026, 11, 9),
            halfHours = List(48) { PowerState.ON },
            published = tomorrowPublished,
        ),
        sourceUpdatedAt = Instant.parse("2026-11-08T08:00:00Z"),
        fetchedAt = Instant.parse("2026-11-08T08:05:00Z"),
        schedulesSuspended = suspended,
    )
}
