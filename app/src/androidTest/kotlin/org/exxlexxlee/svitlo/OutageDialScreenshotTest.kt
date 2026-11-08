package org.exxlexxlee.svitlo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.ui.dial.OutageDial
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import org.junit.Rule
import org.junit.Test

class OutageDialScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allOnHasDescription() {
        composeRule.setContent {
            SvitloTheme {
                OutageDial(
                    hours = List(12) { HourState(PowerState.ON, PowerState.ON) },
                    hourOffset = 0,
                )
            }
        }
        composeRule.onNodeWithContentDescription("світло є протягом усієї половини доби")
            .assertIsDisplayed()
    }

    @Test
    fun allOffHasDescription() {
        composeRule.setContent {
            SvitloTheme {
                OutageDial(
                    hours = List(12) { HourState(PowerState.OFF, PowerState.OFF) },
                    hourOffset = 12,
                )
            }
        }
        composeRule.onNodeWithContentDescription("світла немає протягом усієї половини доби")
            .assertIsDisplayed()
    }

    @Test
    fun mixedHasOutageRange() {
        composeRule.setContent {
            SvitloTheme {
                OutageDial(
                    hours = List(12) { index ->
                        if (index in 6..9) HourState(PowerState.OFF, PowerState.OFF)
                        else HourState(PowerState.ON, PowerState.ON)
                    },
                    hourOffset = 12,
                )
            }
        }
        composeRule.onNodeWithContentDescription("з 18:00 до 22:00 світла немає")
            .assertIsDisplayed()
    }
}
