package org.exxlexxlee.svitlo.ui.schedule

import androidx.compose.ui.graphics.Color
import org.exxlexxlee.domain.model.PowerState

/** Shared presentation of the two timeline rows for Compose and Glance. */
internal object ScheduleTimeline {
    const val CellHeight = 26
    const val CellSpacing = 2
    const val RowSpacing = 4
    const val CornerRadius = 4
    const val MarkerHorizontalInset = 2
    const val MarkerVerticalInset = 3.5f
    const val LabelSize = 14

    data class Cell(val hour: Int, val first: PowerState, val second: PowerState, val marked: Boolean)

    fun row(states: List<PowerState>, startHour: Int, nowHalfHourIndex: Int?): List<Cell> =
        (startHour until startHour + 12).map { hour ->
            Cell(
                hour = hour,
                first = states.getOrElse(hour * 2) { PowerState.UNKNOWN },
                second = states.getOrElse(hour * 2 + 1) { PowerState.UNKNOWN },
                marked = nowHalfHourIndex in 0..47 && nowHalfHourIndex?.div(2) == hour,
            )
        }

    fun labelColor(first: PowerState, second: PowerState): Color =
        if (listOf(first, second).all { it == PowerState.OFF || it == PowerState.UNKNOWN }) {
            Color.White
        } else {
            Color(0xFF1B2437)
        }
}
