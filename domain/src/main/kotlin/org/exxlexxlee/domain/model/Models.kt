package org.exxlexxlee.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Region(
    val id: String,
    val name: String,
    val providerId: String,
)

data class City(
    val id: String,
    val name: String,
)

data class Street(
    val id: String,
    val name: String,
)

data class House(
    val id: String,
    val name: String,
    val number: String,
    val groupId: String,
)

data class Address(
    val region: Region,
    val city: City,
    val street: Street,
    val house: House,
)

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
) {
    init {
        require(halfHours.size == 48) { "Day schedule must contain 48 half-hour slots" }
    }
}

data class OutageSchedule(
    val group: OutageGroup,
    val today: DaySchedule,
    val tomorrow: DaySchedule?,
    val sourceUpdatedAt: Instant?,
    val fetchedAt: Instant,
)

data class HourState(
    val first: PowerState,
    val second: PowerState,
)

data class Dial(
    val hours: List<HourState>,
) {
    init {
        require(hours.size == 12) { "Dial must contain 12 hours" }
    }
}

data class Dials(
    val todayAm: Dial,
    val todayPm: Dial,
    val tomorrowAm: Dial?,
    val tomorrowPm: Dial?,
)

sealed interface Selection {
    val region: Region
    val group: OutageGroup

    data class ByGroup(
        override val region: Region,
        override val group: OutageGroup,
    ) : Selection

    data class ByAddress(
        val address: Address,
        override val group: OutageGroup,
    ) : Selection {
        override val region: Region get() = address.region
    }
}
