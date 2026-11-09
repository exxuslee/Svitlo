package org.exxlexxlee.domain.model

data class PlaceSchedule(
    val place: Place,
    val schedule: OutageSchedule?,
)
