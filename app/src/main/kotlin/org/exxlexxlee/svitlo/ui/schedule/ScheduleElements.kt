package org.exxlexxlee.svitlo.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R

@Composable
internal fun ScheduleDaySwitch(
    showTomorrow: Boolean,
    onTabSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(30))
            .background(CardPalette.Banner)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayChip(
            text = stringResource(R.string.today),
            selected = !showTomorrow,
            onClick = { onTabSelected(false) },
        )
        DayChip(
            text = stringResource(R.string.tomorrow),
            selected = showTomorrow,
            onClick = { onTabSelected(true) },
        )
    }
}

@Composable
private fun DayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30))
            .background(if (selected) CardPalette.Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else CardPalette.Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ScheduleTimelineView(
    states: List<PowerState>,
    nowHalfHourIndex: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ScheduleTimeline.RowSpacing.dp),
    ) {
        HourRow(states = states, startHour = 0, nowHalfHourIndex = nowHalfHourIndex)
        HourRow(states = states, startHour = 12, nowHalfHourIndex = nowHalfHourIndex)
    }
}

@Composable
private fun HourRow(
    states: List<PowerState>,
    startHour: Int,
    nowHalfHourIndex: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ScheduleTimeline.CellSpacing.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (cell in ScheduleTimeline.row(states, startHour, nowHalfHourIndex)) {
            HourCell(
                hour = cell.hour,
                first = cell.first,
                second = cell.second,
                marked = cell.marked,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HourCell(
    hour: Int,
    first: PowerState,
    second: PowerState,
    marked: Boolean,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(ScheduleTimeline.CornerRadius.dp)
    Box(
        modifier = modifier
            .height(ScheduleTimeline.CellHeight.dp)
            .clip(outerShape)
            .then(
                if (marked) {
                    Modifier
                        .background(CardPalette.Marker)
                        .padding(
                            horizontal = ScheduleTimeline.MarkerHorizontalInset.dp,
                            vertical = ScheduleTimeline.MarkerVerticalInset.dp,
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (marked) RoundedCornerShape(2.5.dp) else outerShape),
            contentAlignment = Alignment.Center,
        ) {
            if (first == second) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(stateColor(first)),
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(stateColor(first)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(stateColor(second)),
                    )
                }
            }
            Text(
                text = hour.toString(),
                color = ScheduleTimeline.labelColor(first, second),
                fontSize = ScheduleTimeline.LabelSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}


@Composable
private fun stateColor(state: PowerState?): Color = CardPalette.stateColor(state)
