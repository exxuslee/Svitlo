package org.exxlexxlee.svitlo.ui.schedule

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R

@Composable
internal fun GlanceScheduleDaySwitch(showTomorrow: Boolean, onDaySelected: (Boolean) -> Action) {
    Row(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_control_background))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayChip(
            onDaySelected = onDaySelected,
            text = LocalContext.current.getString(R.string.today),
            selected = !showTomorrow,
            showTomorrow = false,
        )
        DayChip(
            onDaySelected = onDaySelected,
            text = LocalContext.current.getString(R.string.tomorrow),
            selected = showTomorrow,
            showTomorrow = true,
        )
    }
}

@Composable
private fun DayChip(onDaySelected: (Boolean) -> Action, text: String, selected: Boolean, showTomorrow: Boolean) {
    val click = if (selected) GlanceModifier else GlanceModifier.clickable(
        onDaySelected(showTomorrow),
    )
    val background = if (selected) {
        // Explicit tint also replaces a drawable retained by the launcher after an app update.
        GlanceModifier.background(
            ImageProvider(R.drawable.widget_selected_day_background),
            colorFilter = ColorFilter.tint(WidgetColors.selectedDayBackground),
        )
    } else {
        GlanceModifier
    }
    Box(
        modifier = GlanceModifier
            .then(background)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (selected) glanceColor(Color.White) else WidgetColors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
internal fun GlanceScheduleGroupBadge(
    text: String,
    currentState: PowerState?,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier
            .background(ImageProvider(R.drawable.widget_control_background))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // RemoteViews cornerRadius is ignored before Android 12; draw the circle itself.
        val density = LocalContext.current.resources.displayMetrics.density
        val indicator = remember(currentState, density) {
            val size = (18 * density).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
                val color = if (currentState == PowerState.ON) LightSchedulePalette.Yes
                else LightSchedulePalette.stateColor(currentState)
                Canvas(bitmap).drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color.toArgb()
                })
            }
        }
        Image(
            provider = ImageProvider(indicator),
            contentDescription = null,
            modifier = GlanceModifier.size(18.dp),
        )
        Spacer(GlanceModifier.size(8.dp))
        Text(
            text = text,
            style = TextStyle(color = glanceColor(LightSchedulePalette.GroupBadgeText), fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
internal fun GlanceScheduleTimeline(
    states: List<PowerState>,
    nowHalfHourIndex: Int?,
    modifier: GlanceModifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HourRow(states = states, startHour = 0, nowHalfHourIndex = nowHalfHourIndex)
        Spacer(GlanceModifier.height(ScheduleTimeline.RowSpacing.dp))
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
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // RemoteViews supports at most ten children per row.
        val groups = ScheduleTimeline.row(states, startHour, nowHalfHourIndex).chunked(6)
        groups.forEach { cells -> HourGroup(cells, GlanceModifier.defaultWeight()) }
    }
}

@Composable
private fun HourGroup(
    cells: List<ScheduleTimeline.Cell>,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (cell in cells) {
            HourCell(
                hour = cell.hour,
                first = cell.first,
                second = cell.second,
                marked = cell.marked,
                modifier = GlanceModifier.defaultWeight(),
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
    modifier: GlanceModifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = (ScheduleTimeline.CellSpacing / 2f).dp)
            .height(ScheduleTimeline.CellHeight.dp)
            .background(if (marked) WidgetColors.nowRing else WidgetColors.cellBackground)
            .cornerRadius(ScheduleTimeline.CornerRadius.dp)
            .padding(
                horizontal = if (marked) ScheduleTimeline.MarkerHorizontalInset.dp else 0.dp,
                vertical = if (marked) ScheduleTimeline.MarkerVerticalInset.dp else 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight()
                .cornerRadius(if (marked) 2.5.dp else ScheduleTimeline.CornerRadius.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (first == second) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(colorFor(first)),
                ) {}
            } else {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(colorFor(first)),
                    ) {}
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(colorFor(second)),
                    ) {}
                }
            }
            Text(
                text = hour.toString(),
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = labelColor(first, second),
                    fontSize = ScheduleTimeline.LabelSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun colorFor(state: PowerState): ColorProvider =
    glanceColor(DarkSchedulePalette.stateColor(state))

private fun labelColor(first: PowerState, second: PowerState): ColorProvider =
    glanceColor(ScheduleTimeline.labelColor(first, second))

private object WidgetColors {
    val selectedDayBackground = glanceColor(Color(0xFF3A3A3C))
    val transparent = glanceColor(Color.Transparent)
    val cellBackground = transparent
    val muted = glanceColor(LightSchedulePalette.BannerText)
    val nowRing = glanceColor(DarkSchedulePalette.Marker)
}
