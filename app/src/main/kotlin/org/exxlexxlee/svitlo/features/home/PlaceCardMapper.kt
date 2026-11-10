package org.exxlexxlee.svitlo.features.home

import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardMapper

typealias CountdownTarget = org.exxlexxlee.svitlo.ui.schedule.CountdownTarget
typealias PlaceCardUi = org.exxlexxlee.svitlo.ui.schedule.ScheduleCardUi

object PlaceCardMapper {
    fun map(place: Place, schedule: OutageSchedule?, minuteOfDay: Int, error: String?): PlaceCardUi =
        ScheduleCardMapper.map(place.selection, schedule, minuteOfDay, error, place.id)
}
