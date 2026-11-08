package org.exxlexxlee.svitlo.ui.dial

import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.PowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialDescriptionTest {
    @Test
    fun allOn() {
        val hours = List(12) { HourState(PowerState.ON, PowerState.ON) }
        assertEquals("світло є протягом усієї половини доби", buildDialDescription(hours, 0))
    }

    @Test
    fun allOff() {
        val hours = List(12) { HourState(PowerState.OFF, PowerState.OFF) }
        assertEquals("світла немає протягом усієї половини доби", buildDialDescription(hours, 12))
    }

    @Test
    fun mixedHalfHours() {
        val hours = List(12) { HourState(PowerState.ON, PowerState.ON) }.toMutableList()
        hours[6] = HourState(PowerState.OFF, PowerState.ON)
        hours[7] = HourState(PowerState.OFF, PowerState.OFF)
        val description = buildDialDescription(hours, 12)
        assertTrue(description.contains("з 18:00 до 18:30 світла немає"))
        assertTrue(description.contains("з 19:00 до 20:00 світла немає"))
    }
}
