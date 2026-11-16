package org.exxlexxlee.svitlo.ui.schedule

import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.svitlo.ui.common.groupLabel

enum class CountdownTarget {
    PowerOn,
    PowerOff,
}

data class ScheduleCardUi(
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
    val localizeGroupLabel: Boolean = true,
    val outageState: org.exxlexxlee.domain.model.OutageCardState? = null,
    val now: java.time.Instant? = null,
)

object ScheduleCardMapper {
    fun map(
        selection: Selection,
        schedule: OutageSchedule?,
        minuteOfDay: Int,
        error: String? = null,
        id: Long = 0,
        now: java.time.Instant = java.time.Instant.now(),
    ): ScheduleCardUi {
        val today = schedule?.today?.takeIf { it.published }
        val tomorrow = schedule?.tomorrow?.takeIf { it.published }
        val snapshot = today?.let { OutageWindows.statusAt(it.halfHours, minuteOfDay) }
        val countdownTarget = snapshot?.minutesUntilChange?.let {
            if (snapshot.state == PowerState.OFF) CountdownTarget.PowerOn else CountdownTarget.PowerOff
        }
        return ScheduleCardUi(
            id = id,
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
            outageState = schedule?.let {
                val model = org.exxlexxlee.domain.mapper.OutageSnapshotMapper.schedule(it, selection.region.name)
                if (error != null) org.exxlexxlee.domain.model.OutageCardState.Stale(
                    model, java.time.Duration.between(model.fetchedAt, now).coerceAtLeast(java.time.Duration.ZERO),
                ) else org.exxlexxlee.domain.model.OutageCardState.Content(model)
            } ?: if (error != null) org.exxlexxlee.domain.model.OutageCardState.Error else org.exxlexxlee.domain.model.OutageCardState.Loading,
            now = now,
        )
    }

    private fun ranges(halfHours: List<PowerState>): String =
        OutageWindows.windows(halfHours).joinToString(", ") { window ->
            "${OutageWindows.formatClock(window.startMinutes)}–${OutageWindows.formatClock(window.endMinutes)}"
        }


    private val unknownDay = List(48) { PowerState.UNKNOWN }
    private const val MINUTES_IN_DAY = 24 * 60
}
