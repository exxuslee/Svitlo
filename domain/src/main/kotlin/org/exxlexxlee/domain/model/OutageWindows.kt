package org.exxlexxlee.domain.model

data class OutageWindow(
    val startMinutes: Int,
    val endMinutes: Int,
)

data class PowerSnapshot(
    val state: PowerState,
    val minutesUntilChange: Int?,
)

object OutageWindows {
    fun windows(halfHours: List<PowerState>): List<OutageWindow> {
        val result = mutableListOf<OutageWindow>()
        var index = 0
        while (index < halfHours.size) {
            if (halfHours[index] != PowerState.OFF) {
                index += 1
                continue
            }
            val end = halfHours.endOfRun(index)
            result += OutageWindow(
                startMinutes = index * MINUTES_PER_HALF_HOUR,
                endMinutes = end * MINUTES_PER_HALF_HOUR,
            )
            index = end
        }
        return result
    }

    fun totalOffMinutes(halfHours: List<PowerState>): Int =
        halfHours.count { it == PowerState.OFF } * MINUTES_PER_HALF_HOUR

    fun formatClock(minutes: Int): String {
        val hour = minutes / MINUTES_PER_HOUR
        val minute = minutes % MINUTES_PER_HOUR
        return hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
    }

    fun formatRanges(halfHours: List<PowerState>): String =
        windows(halfHours).joinToString(" · ") { window ->
            "${formatClock(window.startMinutes)}–${formatClock(window.endMinutes)}"
        }

    fun statusAt(halfHours: List<PowerState>, minuteOfDay: Int): PowerSnapshot {
        val minute = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        val index = minute / MINUTES_PER_HALF_HOUR
        val state = halfHours[index]
        val end = halfHours.endOfRun(index)
        val until = if (end >= halfHours.size) null else end * MINUTES_PER_HALF_HOUR - minute
        return PowerSnapshot(state = state, minutesUntilChange = until)
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / MINUTES_PER_HOUR
        val rest = minutes % MINUTES_PER_HOUR
        return when {
            hours > 0 && rest > 0 -> "$hours год $rest хв"
            hours > 0 -> "$hours год"
            else -> "$rest хв"
        }
    }

    private fun List<PowerState>.endOfRun(start: Int): Int {
        var end = start + 1
        while (end < size && this[end] == this[start]) {
            end += 1
        }
        return end
    }
}
