package org.exxlexxlee.svitlo.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection

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
        return ScheduleWidgetUi(
            title = selection.details?.name ?: selection.region.name,
            groupLabel = groupLabel(selection.group.name),
            today = schedule?.today?.toUi(),
            tomorrow = schedule?.tomorrow?.toUi(),
            nowHalfHourIndex = KyivTime.halfHourIndex(clock),
        )
    }

    private fun DaySchedule.toUi(): WidgetDayUi = WidgetDayUi(states = halfHours)

    private fun groupLabel(name: String): String {
        val normalized = name.trim()
        return if (
            normalized.startsWith("Група", ignoreCase = true) ||
            normalized.startsWith("Черга", ignoreCase = true)
        ) {
            normalized
        } else {
            "Група $normalized"
        }
    }
}
