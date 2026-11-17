package org.exxlexxlee.svitlo.ui.widget

import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardMapper

object ScheduleWidgetMapper {
    fun map(
        selection: Selection?,
        schedule: OutageSchedule?,
        clock: Clock,
    ): ScheduleWidgetUi {
        if (selection == null) {
            return ScheduleWidgetUi(
                title = "Світло",
                groupLabel = "",
                today = null,
                tomorrow = null,
                nowHalfHourIndex = null,
            )
        }
        val card = ScheduleCardMapper.map(selection, schedule, KyivTime.minuteOfDay(clock))
        return ScheduleWidgetUi(
            title = card.title,
            groupLabel = card.groupLabel,
            today = WidgetDayUi(card.todayStates),
            tomorrow = card.tomorrowRanges?.let { WidgetDayUi(card.tomorrowStates) },
            nowHalfHourIndex = card.nowIndex.takeIf { card.showNowMarker },
            currentState = card.currentState,
        )
    }
}
