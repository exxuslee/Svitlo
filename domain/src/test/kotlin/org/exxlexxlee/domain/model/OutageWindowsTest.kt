package org.exxlexxlee.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OutageWindowsTest {

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
