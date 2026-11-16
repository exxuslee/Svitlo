package org.exxlexxlee.data.remote.schedule

import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule

class SchedulePayload(
    val groups: List<OutageGroup>,
    private val mapSchedule: (OutageGroup, Clock) -> OutageSchedule,
) {
    fun toSchedule(group: OutageGroup, clock: Clock): OutageSchedule = mapSchedule(group, clock)
}
