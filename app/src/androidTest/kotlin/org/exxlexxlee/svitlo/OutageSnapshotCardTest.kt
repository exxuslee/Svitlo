package org.exxlexxlee.svitlo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import org.exxlexxlee.domain.model.*
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.exxlexxlee.svitlo.schedule.OutageSnapshotContent
import org.exxlexxlee.svitlo.schedule.OutageStatePreviewProvider
import org.exxlexxlee.svitlo.schedule.previewNow
import org.exxlexxlee.svitlo.theme.SvitloTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class OutageSnapshotCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun lightStatesRender() = render(false)
    @Test fun darkStatesRender() = render(true)

    @Test fun nearestActiveIncidentIsPrimaryAndOtherIncidentsExpand() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val snapshot = OutageSnapshot.Timeline(listOf(
            OutageInterval(previewNow.minusSeconds(7200), previewNow.plusSeconds(3600), OutageKind.ACTUAL, cause = "Far incident", distanceMeters = 1000.0),
            OutageInterval(previewNow.minusSeconds(3600), previewNow.plusSeconds(7200), OutageKind.ACTUAL, cause = "Near incident", distanceMeters = 100.0),
        ), "UKPN", previewNow, ZoneId.of("Europe/London"), LocationMatch.AREA)
        composeRule.setContent {
            SvitloTheme { Column { OutageSnapshotContent(OutageCardState.Content(snapshot), previewNow, 1) } }
        }
        composeRule.onNodeWithText("Near incident").assertIsDisplayed()
        composeRule.onNodeWithText("Far incident").assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.outage_more, 1)).performClick()
        composeRule.onNodeWithText("Far incident").assertIsDisplayed()
    }

    private fun render(dark: Boolean) {
        val states = OutageStatePreviewProvider().values.toList()
        val current = mutableStateOf(states.first())
        composeRule.setContent {
            SvitloTheme(darkTheme = dark) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutageSnapshotContent(current.value, previewNow, 1)
                    }
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        states.forEachIndexed { index, state ->
            composeRule.runOnIdle { current.value = state }
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            assertTrue(bitmap.width > 0 && bitmap.height > 0)
            File(context.getExternalFilesDir(null), "outage-${if (dark) "dark" else "light"}-$index.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
