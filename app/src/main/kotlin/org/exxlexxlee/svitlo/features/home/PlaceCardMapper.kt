package org.exxlexxlee.svitlo.features.home

import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.svitlo.ui.schedule.CountdownTarget
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardMapper
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardUi

typealias CountdownTarget = CountdownTarget
typealias PlaceCardUi = ScheduleCardUi

object PlaceCardMapper {
    fun map(
        place: Place,
        schedule: OutageSchedule?,
        minuteOfDay: Int,
        error: String?,
        now: java.time.Instant = java.time.Instant.now(),
    ): PlaceCardUi =
        ScheduleCardMapper.map(place.selection, schedule, minuteOfDay, error, place.id, now)
}
