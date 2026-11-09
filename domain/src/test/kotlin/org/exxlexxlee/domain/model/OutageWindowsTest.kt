package org.exxlexxlee.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OutageWindowsTest {

    @Test
    fun includesOutagesAtBothEndsOfTheDay() {
        val halves = MutableList(48) { PowerState.ON }
        halves[0] = PowerState.OFF
        halves[47] = PowerState.OFF

        assertEquals(
            listOf(OutageWindow(0, 30), OutageWindow(1410, 1440)),
            OutageWindows.windows(halves),
        )
        assertEquals("00:00–00:30 · 23:30–24:00", OutageWindows.formatRanges(halves))
    }

    @Test
    fun switchesStateExactlyAtHalfHourBoundary() {
        val halves = MutableList(48) { PowerState.ON }
        halves[1] = PowerState.MAYBE
        halves[2] = PowerState.UNKNOWN

        assertEquals(PowerSnapshot(PowerState.ON, 1), OutageWindows.statusAt(halves, 29))
        assertEquals(PowerSnapshot(PowerState.MAYBE, 30), OutageWindows.statusAt(halves, 30))
        assertEquals(PowerSnapshot(PowerState.UNKNOWN, 30), OutageWindows.statusAt(halves, 60))
    }

    @Test
    fun clampsStatusTimeToTheCurrentDay() {
        val halves = MutableList(48) { PowerState.ON }
        halves[0] = PowerState.OFF

        assertEquals(PowerSnapshot(PowerState.OFF, 30), OutageWindows.statusAt(halves, -1))
        assertEquals(PowerSnapshot(PowerState.ON, null), OutageWindows.statusAt(halves, 1440))
    }

    @Test
    fun formatsScreenshotTodayWindowsAndTotal() {
        val halves = MutableList(48) { PowerState.ON }
        for (index in 16 until 22) halves[index] = PowerState.OFF
        for (index in 28 until 35) halves[index] = PowerState.OFF
        for (index in 40 until 44) halves[index] = PowerState.OFF

        assertEquals(
            "08:00–11:00 · 14:00–17:30 · 20:00–22:00",
            OutageWindows.formatRanges(halves),
        )
        assertEquals(
            "8 год 30 хв",
            OutageWindows.formatDuration(OutageWindows.totalOffMinutes(halves))
        )
    }

    @Test
    fun formatsWholeHoursOnly() {
        val halves = MutableList(48) { PowerState.ON }
        for (index in 12 until 28) halves[index] = PowerState.OFF

        assertEquals("06:00–14:00", OutageWindows.formatRanges(halves))
        assertEquals("8 год", OutageWindows.formatDuration(OutageWindows.totalOffMinutes(halves)))
    }

    @Test
    fun countsMinutesUntilTheStateChanges() {
        val halves = MutableList(48) { PowerState.ON }
        for (index in 36 until 42) halves[index] = PowerState.OFF

        val status = OutageWindows.statusAt(halves, minuteOfDay = 18 * 60 + 26)

        assertEquals(PowerState.OFF, status.state)
        assertEquals(2 * 60 + 34, status.minutesUntilChange)
    }

    @Test
    fun hasNoCountdownWhenTheStateLastsUntilMidnight() {
        val halves = MutableList(48) { PowerState.ON }

        val status = OutageWindows.statusAt(halves, minuteOfDay = 10 * 60)

        assertEquals(PowerState.ON, status.state)
        assertEquals(null, status.minutesUntilChange)
    }

    @Test
    fun ignoresMaybeWhenBuildingOffWindows() {
        val halves = MutableList(48) { PowerState.ON }
        halves[0] = PowerState.OFF
        halves[1] = PowerState.MAYBE
        halves[2] = PowerState.OFF

        assertEquals("00:00–00:30 · 01:00–01:30", OutageWindows.formatRanges(halves))
        assertEquals("1 год", OutageWindows.formatDuration(OutageWindows.totalOffMinutes(halves)))
    }
}
