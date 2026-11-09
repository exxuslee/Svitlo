package org.exxlexxlee.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class OutageGroup(
    val id: String,
    val name: String,
)

enum class PowerState {
    ON,
    OFF,
    MAYBE,
    UNKNOWN,
}

data class DaySchedule(
    val date: LocalDate,
    val halfHours: List<PowerState>,
    val published: Boolean = true,
) {
    init {
        require(halfHours.size == HALF_HOURS_PER_DAY) { "Day schedule must contain 48 half-hour slots" }
    }
}

data class OutageSchedule(
    val group: OutageGroup,
    val today: DaySchedule,
    val tomorrow: DaySchedule?,
    val sourceUpdatedAt: Instant?,
    val fetchedAt: Instant,
    val schedulesSuspended: Boolean = false,
)
