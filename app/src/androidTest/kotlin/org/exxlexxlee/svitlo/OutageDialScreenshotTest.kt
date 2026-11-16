package org.exxlexxlee.svitlo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.ui.schedule.ScheduleTimelineView
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import org.junit.Rule
import org.junit.Test

/** Regression coverage for the hourly timeline that replaced the old dial. */
class OutageDialScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun allOnShowsFullDay() = verify(List(48) { PowerState.ON })
    @Test fun allOffShowsFullDay() = verify(List(48) { PowerState.OFF })
    @Test fun mixedAndUnknownStatesShowFullDay() = verify(List(48) { index ->
        when (index) {
            in 12..19 -> PowerState.OFF
            in 20..23 -> PowerState.MAYBE
            in 24..47 -> PowerState.UNKNOWN
            else -> PowerState.ON
        }
    })

    private fun verify(states: List<PowerState>) {
        composeRule.setContent {
            SvitloTheme {
                ScheduleTimelineView(states = states, nowHalfHourIndex = 20)
            }
        }
        (0..23).forEach { composeRule.onNodeWithText(it.toString()).assertIsDisplayed() }
    }
}
