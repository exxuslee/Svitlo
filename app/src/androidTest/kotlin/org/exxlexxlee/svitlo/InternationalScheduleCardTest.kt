package org.exxlexxlee.svitlo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.svitlo.features.home.ReadOnlyScheduleCard
import org.exxlexxlee.svitlo.features.international.InternationalCardMapper
import org.exxlexxlee.svitlo.theme.SvitloTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import android.graphics.Bitmap

class InternationalScheduleCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun lightCardSwitchesDays() = verify(false)
    @Test fun darkCardSwitchesDays() = verify(true)

    private fun verify(dark: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = OutageSource("ukpn", Country.UNITED_KINGDOM, "UK Power Networks", "https://example.com", "Europe/London", SourceAccess.LIVE)
        val events = listOf(
            InternationalOutage("1", "London · SW1A 1AA", "", Instant.parse("2026-11-15T10:00:00Z"), Instant.parse("2026-11-15T11:00:00Z")),
            InternationalOutage("2", "London · SW1A 1AA", "", Instant.parse("2026-11-16T17:00:00Z"), Instant.parse("2026-11-16T18:00:00Z")),
        )
        val card = InternationalCardMapper.map(events, source, Instant.parse("2026-11-15T10:15:00Z"))
        composeRule.setContent {
            SvitloTheme(darkTheme = dark) {
                Box(Modifier.padding(16.dp)) { ReadOnlyScheduleCard(card) }
            }
        }
        composeRule.onNodeWithText("15.11 10:00", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("UK Power Networks", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertDoesNotExist()
        val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "international-card-${if (dark) "dark" else "light"}.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        composeRule.onNodeWithText(context.getString(R.string.tomorrow)).performClick()
        composeRule.onNodeWithText("16.11 17:00", substring = true).assertIsDisplayed()
    }
}
