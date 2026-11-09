package org.exxlexxlee.domain.model

data class HourState(
    val first: PowerState,
    val second: PowerState,
)

data class Dial(
    val hours: List<HourState>,
) {
    init {
        require(hours.size == HOURS_PER_DIAL) { "Dial must contain 12 hours" }
    }
}

data class Dials(
    val todayAm: Dial,
    val todayPm: Dial,
    val tomorrowAm: Dial?,
    val tomorrowPm: Dial?,
)
