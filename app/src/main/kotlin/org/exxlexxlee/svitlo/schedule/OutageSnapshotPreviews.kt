package org.exxlexxlee.svitlo.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.svitlo.theme.SvitloTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal val previewNow: Instant = Instant.parse("2026-11-16T12:00:00Z")

class OutageStatePreviewProvider : PreviewParameterProvider<OutageCardState> {
    private val timeline = OutageSnapshot.Timeline(
        emptyList(), "Operator", previewNow.minusSeconds(60), locationZone("Europe/Kyiv"), LocationMatch.EXACT_ADDRESS,
        listOf(CoverageWindow(previewNow.minusSeconds(12 * 3600), previewNow.plusSeconds(10 * 3600))),
    )
    override val values: Sequence<OutageCardState> = sequenceOf(
        OutageCardState.Loading,
        OutageCardState.Error,
        OutageCardState.Content(timeline),
        OutageCardState.Content(timeline.copy(knownCoverage = emptyList())),
        *OutageKind.entries.map { kind -> OutageCardState.Content(timeline.copy(intervals = listOf(
            OutageInterval(previewNow.minusSeconds(1800), previewNow.plusSeconds(5400), kind, cause = "Maintenance", affectedCustomers = 200),
        ))) }.toTypedArray(),
        OutageCardState.Content(timeline.copy(intervals = listOf(OutageInterval(previewNow.minusSeconds(3600), null, OutageKind.ACTUAL)))),
        OutageCardState.Content(timeline.copy(intervals = listOf(OutageInterval(previewNow.minusSeconds(3600), previewNow.plusSeconds(7200), OutageKind.ACTUAL, endIsEstimate = true)), locationMatch = LocationMatch.AREA)),
        OutageCardState.Stale(timeline.copy(fetchedAt = previewNow.minusSeconds(3600)), Duration.ofHours(1)),
        *listOf(null, 0, 300).map { count -> OutageCardState.Content(OutageSnapshot.AreaSummary(
            "County", count, if (count == 300) 12.5 else null, "ODIN", previewNow, ZoneId.of("America/New_York"),
        )) }.toTypedArray(),
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun OutageCardLight(@PreviewParameter(OutageStatePreviewProvider::class) state: OutageCardState) {
    OutageCardPreview(state, false)
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun OutageCardDark(@PreviewParameter(OutageStatePreviewProvider::class) state: OutageCardState) {
    OutageCardPreview(state, true)
}

@Composable
private fun OutageCardPreview(state: OutageCardState, dark: Boolean) {
    SvitloTheme(darkTheme = dark) {
        Surface(color = CardPalette.Card) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutageSnapshotContent(state, previewNow, 1)
            }
        }
    }
}
