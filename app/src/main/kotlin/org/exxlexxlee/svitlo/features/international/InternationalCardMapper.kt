package org.exxlexxlee.svitlo.features.international

import org.exxlexxlee.domain.mapper.InternationalScheduleMapper
import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardUi
import java.time.Instant
import java.time.ZoneId

object InternationalCardMapper {
    fun map(events: List<InternationalOutage>, source: OutageSource, now: Instant): ScheduleCardUi {
        require(events.isNotEmpty())
        val zone = ZoneId.of(source.zoneId)
        val localNow = now.atZone(zone)
        val today = InternationalScheduleMapper.day(events, localNow.toLocalDate(), zone)
        val tomorrow =
            InternationalScheduleMapper.day(events, localNow.toLocalDate().plusDays(1), zone)
        val index = (localNow.hour * 60 + localNow.minute) / 30
        // An incident's estimated end does not confirm that power will be restored.
        // Consequently there is no availability countdown for incident-only feeds.
        return ScheduleCardUi(
            id = (source.id + ":" + events.first().location).hashCode().toLong(),
            title = events.first().location,
            groupLabel = source.name.substringBefore(" · "),
            iconId = "home",
            todayRanges = today.ranges,
            tomorrowRanges = tomorrow.ranges,
            todayStates = today.states,
            tomorrowStates = tomorrow.states,
            showNowMarker = true,
            nowIndex = index,
            currentState = if (events.any { event ->
                    val start = event.start
                    val end = event.end
                    start != null && end != null && start <= now && now < end
                }) PowerState.OFF else PowerState.UNKNOWN,
            minutesUntilChange = null,
            countdownTarget = null,
            schedulesSuspended = false,
            error = null,
            localizeGroupLabel = false,
        )
    }
}
