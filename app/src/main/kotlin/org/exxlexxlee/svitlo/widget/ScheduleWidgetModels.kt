package org.exxlexxlee.svitlo.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardMapper

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeWidgetUi(
    observeSelection: ObserveSelection,
    observeSchedule: ObserveSchedule,
    clock: Clock,
): Flow<ScheduleWidgetUi> = observeSelection().distinctUntilChanged().flatMapLatest { selection ->
    if (selection == null) {
        flowOf(ScheduleWidgetMapper.map(null, null, clock))
    } else {
        observeSchedule(selection).map { ScheduleWidgetMapper.map(selection, it, clock) }
    }
}.distinctUntilChanged()

data class WidgetDayUi(
    val states: List<PowerState>,
)

data class ScheduleWidgetUi(
    val title: String,
    val groupLabel: String,
    val today: WidgetDayUi?,
    val tomorrow: WidgetDayUi?,
    val nowHalfHourIndex: Int?,
    val currentState: PowerState? = null,
)

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
