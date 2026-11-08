package org.exxlexxlee.domain.model

data class OutageWindow(
    val startMinutes: Int,
    val endMinutes: Int,
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
            var end = index + 1
            while (end < halfHours.size && halfHours[end] == PowerState.OFF) {
                end += 1
            }
            result += OutageWindow(startMinutes = index * 30, endMinutes = end * 30)
            index = end
        }
        return result
    }

    fun totalOffMinutes(halfHours: List<PowerState>): Int =
        halfHours.count { it == PowerState.OFF } * 30

    fun formatClock(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
    }

    fun formatRanges(halfHours: List<PowerState>): String =
        windows(halfHours).joinToString(" · ") { window ->
            "${formatClock(window.startMinutes)}–${formatClock(window.endMinutes)}"
        }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "$hours год $rest хв"
            hours > 0 -> "$hours год"
            else -> "$rest хв"
        }
    }
}
