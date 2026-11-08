package org.exxlexxlee.svitlo.features.home

import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.PowerState

enum class CountdownTarget {
    PowerOn,
    PowerOff,
}

data class PlaceCardUi(
    val id: Long,
    val title: String,
    val groupLabel: String,
    val iconId: String,
    val todayRanges: String?,
    val tomorrowRanges: String?,
    val todayStates: List<PowerState>,
    val tomorrowStates: List<PowerState>,
    val showNowMarker: Boolean,
    val nowIndex: Int,
    val currentState: PowerState?,
    val minutesUntilChange: Int?,
    val countdownTarget: CountdownTarget?,
    val schedulesSuspended: Boolean,
    val error: String?,
)

object PlaceCardMapper {
    fun map(
        place: Place,
        schedule: OutageSchedule?,
        minuteOfDay: Int,
        error: String?,
    ): PlaceCardUi {
        val selection = place.selection
        val today = schedule?.today?.takeIf { it.published }
        val tomorrow = schedule?.tomorrow?.takeIf { it.published }
        val snapshot = today?.let { OutageWindows.statusAt(it.halfHours, minuteOfDay) }
        val countdownTarget = snapshot?.minutesUntilChange?.let {
            if (snapshot.state == PowerState.OFF) CountdownTarget.PowerOn else CountdownTarget.PowerOff
        }
        return PlaceCardUi(
            id = place.id,
            title = selection.details?.name ?: selection.region.name,
            groupLabel = groupLabel(selection.group.name),
            iconId = selection.details?.iconId ?: "home",
            todayRanges = today?.let { ranges(it.halfHours) },
            tomorrowRanges = tomorrow?.let { ranges(it.halfHours) },
            todayStates = today?.halfHours ?: unknownDay,
            tomorrowStates = tomorrow?.halfHours ?: unknownDay,
            showNowMarker = today != null,
            nowIndex = (minuteOfDay.coerceIn(0, MINUTES_IN_DAY - 1) / 30),
            currentState = snapshot?.state,
            minutesUntilChange = snapshot?.minutesUntilChange,
            countdownTarget = countdownTarget,
            schedulesSuspended = schedule?.schedulesSuspended == true,
            error = error,
        )
    }

    private fun ranges(halfHours: List<PowerState>): String =
        OutageWindows.windows(halfHours).joinToString(", ") { window ->
            "${OutageWindows.formatClock(window.startMinutes)}–${OutageWindows.formatClock(window.endMinutes)}"
        }

    private fun groupLabel(name: String): String {
        val normalized = name.trim()
        return if (
            normalized.startsWith("Група", ignoreCase = true) ||
            normalized.startsWith("Черга", ignoreCase = true)
        ) {
            normalized
        } else {
            "Група $normalized"
        }
    }

    private val unknownDay = List(48) { PowerState.UNKNOWN }
    private const val MINUTES_IN_DAY = 24 * 60
}
