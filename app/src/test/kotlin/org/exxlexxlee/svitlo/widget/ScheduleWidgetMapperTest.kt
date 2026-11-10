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
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.svitlo.features.home.PlaceCardMapper
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

    @Test
    fun widgetAndCardAgreeOnPublishedDaysAndCurrentState() {
        val selection = Selection.ByGroup(
            region = Region("kyiv", "Київ", "yasno"),
            group = OutageGroup("1.1", "1.1"),
        )
        val halves = List(48) { if (it in 20..23) PowerState.OFF else PowerState.ON }
        val today = DaySchedule(LocalDate(2026, 11, 8), halves)
        val schedule = OutageSchedule(
            group = selection.group,
            today = today,
            tomorrow = today.copy(date = LocalDate(2026, 11, 9), published = false),
            sourceUpdatedAt = fixedClock().now(),
            fetchedAt = fixedClock().now(),
        )
        val card = PlaceCardMapper.map(Place(7, selection), schedule, 10 * 60 + 40, null)
        val widget = ScheduleWidgetMapper.map(selection, schedule, fixedClock())
        assertEquals(card.title, widget.title)
        assertEquals(card.groupLabel, widget.groupLabel)
        assertEquals(card.todayStates, widget.today?.states)
        assertEquals(card.currentState, widget.currentState)
        assertEquals(card.nowIndex, widget.nowHalfHourIndex)
        assertNull(widget.tomorrow)

        val unpublished = ScheduleWidgetMapper.map(
            selection, schedule.copy(today = today.copy(published = false)), fixedClock(),
        )
        assertEquals(List(48) { PowerState.UNKNOWN }, unpublished.today?.states)
        assertNull(unpublished.nowHalfHourIndex)
        assertNull(unpublished.currentState)

        val published = ScheduleWidgetMapper.map(
            selection, schedule.copy(tomorrow = today.copy(published = true)), fixedClock(),
        )
        assertEquals(halves, published.tomorrow?.states)
    }

    private fun fixedClock(): Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-08T08:40:00Z")
    }
}
