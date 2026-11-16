package org.exxlexxlee.svitlo.ui.schedule

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val fontScale = context.resources.configuration.fontScale
    val width = (LocalSize.current.width.value - 24f).coerceAtLeast(1f)
    val timeline = remember(states, nowHalfHourIndex, width, density, fontScale) {
        scheduleTimelineBitmap(states, nowHalfHourIndex, width, density, fontScale)
    }
    // One image per day keeps the Binder payload small instead of sending 24 nested cell views.
    Image(
        provider = ImageProvider(timeline),
        contentDescription = states.chunked(2).mapIndexed { hour, halves ->
            "$hour: ${halves.joinToString(" / ") { context.getString(it.labelRes()) }}"
        }.joinToString(", "),
        modifier = modifier.fillMaxWidth()
            .height((ScheduleTimeline.CellHeight * 2 + ScheduleTimeline.RowSpacing).dp),
    )
}

private fun PowerState.labelRes(): Int = when (this) {
    PowerState.ON -> R.string.legend_on
    PowerState.OFF -> R.string.legend_off
    PowerState.MAYBE -> R.string.legend_maybe
    PowerState.UNKNOWN -> R.string.legend_unknown
}

private fun scheduleTimelineBitmap(
    states: List<PowerState>,
    nowHalfHourIndex: Int?,
    widthDp: Float,
    density: Float,
    fontScale: Float,
): Bitmap {
    val heightDp = ScheduleTimeline.CellHeight * 2 + ScheduleTimeline.RowSpacing
    val bitmap = Bitmap.createBitmap(
        (widthDp * density).toInt().coerceAtLeast(1),
        (heightDp * density).toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bitmap)
    val cellWidth = bitmap.width / 12f
    val spacing = ScheduleTimeline.CellSpacing * density
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = minOf(
            ScheduleTimeline.LabelSize * density * fontScale,
            cellWidth * 0.75f,
            ScheduleTimeline.CellHeight * density * 0.8f,
        )
    }
    for (row in 0..1) {
        for ((column, cell) in ScheduleTimeline.row(states, row * 12, nowHalfHourIndex)
            .withIndex()) {
            val left = column * cellWidth + spacing / 2f
            val top = row * (ScheduleTimeline.CellHeight + ScheduleTimeline.RowSpacing) * density
            val save = canvas.save()
            canvas.translate(left, top)
            drawHourCell(canvas, cell, (cellWidth - spacing).coerceAtLeast(1f), density)
            canvas.restoreToCount(save)
            label.color = ScheduleTimeline.labelColor(cell.first, cell.second).toArgb()
            val centerY = top + ScheduleTimeline.CellHeight * density / 2f
            canvas.drawText(
                cell.hour.toString(), (column + 0.5f) * cellWidth,
                centerY - (label.ascent() + label.descent()) / 2f, label,
            )
        }
    }
    return bitmap
}

/** Draw each corner explicitly: RemoteViews cannot clip individual corners. */
private fun drawHourCell(
    canvas: Canvas,
    cell: ScheduleTimeline.Cell,
    width: Float,
    density: Float,
) {
    val (hour, first, second, marked) = cell
    val height = ScheduleTimeline.CellHeight * density
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

    val bounds = RectF(0f, 0f, width, height)
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
    canvas.clipPath(
        shape(
            bounds,
            if (marked) ScheduleTimeline.MarkerHorizontalInset.toFloat() else 0f
        )
    )
    paint.color = DarkSchedulePalette.stateColor(first).toArgb()
    canvas.drawRect(bounds.left, bounds.top, bounds.centerX(), bounds.bottom, paint)
    paint.color = DarkSchedulePalette.stateColor(second).toArgb()
    canvas.drawRect(bounds.centerX(), bounds.top, bounds.right, bounds.bottom, paint)
}

private object WidgetColors {
    val selectedDayBackground = glanceColor(Color(0xFF3A3A3C))
    val unselectedDayText = glanceColor(DarkSchedulePalette.Title)
}
