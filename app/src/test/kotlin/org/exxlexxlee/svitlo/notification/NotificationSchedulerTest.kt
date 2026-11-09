package org.exxlexxlee.svitlo.notification

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
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationSchedulerTest {
    @Test
    fun testScheduleCreation() {
        val halves = MutableList(48) { PowerState.ON }
        // Outage from 14:00 to 16:00 (slots 28 to 31)
        for (index in 28 until 32) halves[index] = PowerState.OFF

        val place = Place(
            id = 1,
            selection = Selection.ByGroup(
                region = Region(id = "kyiv-region", name = "Київська область", providerId = "dtek"),
                group = OutageGroup(id = "1", name = "1"),
                details = AddressDetails(name = "Дім", reminderMinutes = 15, quietNight = true),
            ),
        )

        val schedule = OutageSchedule(
            group = OutageGroup("1", "1"),
            today = DaySchedule(LocalDate(2026, 11, 8), halves),
            tomorrow = null,
            sourceUpdatedAt = Instant.parse("2026-11-08T08:00:00Z"),
            fetchedAt = Instant.parse("2026-11-08T08:05:00Z"),
        )

        val pair = place to schedule
        assertNotNull(pair)
    }
}
