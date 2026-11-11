package org.exxlexxlee.svitlo.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardMapper

internal fun observeWidgetUi(
    observePlaceSchedules: ObservePlaceSchedules,
    selectedPlaceId: Flow<Long?>,
    clock: Clock,
): Flow<ScheduleWidgetUi> = combine(observePlaceSchedules(), selectedPlaceId) { places, selectedId ->
    val selected = places.firstOrNull { it.place.id == selectedId } ?: places.firstOrNull()
    ScheduleWidgetMapper.map(selected?.place?.selection, selected?.schedule, clock)
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
