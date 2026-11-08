package org.exxlexxlee.svitlo.widget

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleWidgetMapperTest {

    @Test
    fun mapsEmptySelection() {
        val ui = ScheduleWidgetMapper.map(null, null, fixedClock())
        assertEquals("Світло", ui.title)
        assertEquals("", ui.groupLabel)
        assertNull(ui.today)
    }

    @Test
    fun prefixesGroupNameAndFormatsToday() {
        val halves = MutableList(48) { PowerState.ON }
        for (index in 16 until 22) halves[index] = PowerState.OFF
        val selection = Selection.ByGroup(
            region = Region("kyiv", "Київ", "yasno"),
            group = OutageGroup("1.1", "1.1"),
        )
        val schedule = OutageSchedule(
            group = selection.group,
            today = DaySchedule(LocalDate(2026, 11, 8), halves),
            tomorrow = null,
            sourceUpdatedAt = Instant.parse("2026-11-08T10:00:00Z"),
            fetchedAt = Instant.parse("2026-11-08T10:00:00Z"),
        )

        val ui = ScheduleWidgetMapper.map(selection, schedule, fixedClock())

        assertEquals("Київ", ui.title)
        assertEquals("Група 1.1", ui.groupLabel)
        assertEquals(48, ui.today?.states?.size)
        assertEquals(PowerState.OFF, ui.today?.states?.get(16))
        assertEquals(PowerState.ON, ui.today?.states?.get(15))
        assertEquals(21, ui.nowHalfHourIndex)
        assertNull(ui.tomorrow)
    }

    private fun fixedClock(): Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-08T08:40:00Z")
    }
}
