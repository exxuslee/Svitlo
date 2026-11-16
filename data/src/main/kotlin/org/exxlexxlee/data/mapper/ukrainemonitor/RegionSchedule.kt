package org.exxlexxlee.data.mapper.ukrainemonitor

import kotlinx.datetime.LocalDate

/** Validated dates and slots for the selected region, ready for schedule mapping. */
data class RegionSchedule(
    val today: LocalDate,
    val days: Map<String, Map<LocalDate, Map<String, Int>>>,
    val emergency: Boolean,
)
