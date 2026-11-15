package org.exxlexxlee.svitlo.ui.schedule

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
private fun DayChip(
    onDaySelected: (Boolean) -> Action,
    text: String,
    selected: Boolean,
    showTomorrow: Boolean
) {
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
                color = if (selected) glanceColor(Color.White) else WidgetColors.unselectedDayText,
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
                Canvas(bitmap).drawCircle(
                    size / 2f,
                    size / 2f,
                    size / 2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
            style = TextStyle(
                color = glanceColor(LightSchedulePalette.GroupBadgeText),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
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
    val density = LocalContext.current.resources.displayMetrics.density
    // The card has 12dp padding on each side; each row contains twelve hours.
    val width = ((LocalSize.current.width.value - 24f) / 12f - ScheduleTimeline.CellSpacing)
        .coerceAtLeast(1f)
    val background = remember(hour, first, second, marked, width, density) {
        hourCellBackground(hour, first, second, marked, width, density)
    }
    Box(
        modifier = modifier
            .height(ScheduleTimeline.CellHeight.dp)
            .padding(horizontal = (ScheduleTimeline.CellSpacing / 2f).dp),
    ) {
        // Glance applies padding inside a view's background, regardless of modifier order.
        // Keep the background on a child so the parent's padding stays transparent.
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(ScheduleTimeline.CellHeight.dp)
                .background(ImageProvider(background)),
            contentAlignment = Alignment.Center,
        ) {
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

/** Draw each corner explicitly: RemoteViews cannot clip individual corners. */
private fun hourCellBackground(
    hour: Int,
    first: PowerState,
    second: PowerState,
    marked: Boolean,
    widthDp: Float,
    density: Float,
): Bitmap {
    val width = (widthDp * density).toInt().coerceAtLeast(1)
    val height = (ScheduleTimeline.CellHeight * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val small = ScheduleTimeline.CornerRadius.toFloat()
    val big = ScheduleTimeline.OuterCornerRadius.toFloat()
    // Path radii are ordered top-left, top-right, bottom-right, bottom-left.
    val corners = floatArrayOf(
        if (hour == 0) big else small,
        if (hour == 11) big else small,
        if (hour == 23) big else small,
        if (hour == 12) big else small,
    )
    fun shape(bounds: RectF, inset: Float): Path {
        val radii = corners.flatMap { corner ->
            val radius = (if (marked && inset > 0f) (corner - inset).coerceAtLeast(2.5f)
                else corner) * density
            listOf(radius, radius)
        }.toFloatArray()
        return Path().apply { addRoundRect(bounds, radii, Path.Direction.CW) }
    }
    val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
    val outer = shape(bounds, 0f)
    if (marked) {
        bounds.inset(
            ScheduleTimeline.MarkerHorizontalInset * density,
            ScheduleTimeline.MarkerVerticalInset * density,
        )
        val inner = shape(bounds, ScheduleTimeline.MarkerHorizontalInset.toFloat())
        val frame = Path(outer).apply { op(inner, Path.Op.DIFFERENCE) }
        paint.color = DarkSchedulePalette.Marker.toArgb()
        canvas.drawPath(frame, paint)
    }
    canvas.clipPath(outer)
    canvas.clipPath(shape(bounds, if (marked) ScheduleTimeline.MarkerHorizontalInset.toFloat() else 0f))
    paint.color = DarkSchedulePalette.stateColor(first).toArgb()
    canvas.drawRect(bounds.left, bounds.top, bounds.centerX(), bounds.bottom, paint)
    paint.color = DarkSchedulePalette.stateColor(second).toArgb()
    canvas.drawRect(bounds.centerX(), bounds.top, bounds.right, bounds.bottom, paint)
    return bitmap
}

private fun labelColor(first: PowerState, second: PowerState): ColorProvider =
    glanceColor(ScheduleTimeline.labelColor(first, second))

private object WidgetColors {
    val selectedDayBackground = glanceColor(Color(0xFF3A3A3C))
    val unselectedDayText = glanceColor(DarkSchedulePalette.Title)
}
