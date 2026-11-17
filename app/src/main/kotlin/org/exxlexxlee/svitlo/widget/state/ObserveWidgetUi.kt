package org.exxlexxlee.svitlo.widget.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.svitlo.ui.widget.ScheduleWidgetMapper
import org.exxlexxlee.svitlo.ui.widget.ScheduleWidgetUi
import org.exxlexxlee.svitlo.widget.update.observeWidgetTime

internal fun observeWidgetUi(
    observePlaceSchedules: ObservePlaceSchedules,
    selectedPlaceId: Flow<Long?>,
    clock: Clock,
): Flow<ScheduleWidgetUi> =
    combine(observePlaceSchedules(), selectedPlaceId, observeWidgetTime(clock)) { places, selectedId, _ ->
        val selected = places.firstOrNull { it.place.id == selectedId } ?: places.firstOrNull()
        ScheduleWidgetMapper.map(selected?.place?.selection, selected?.schedule, clock)
    }.distinctUntilChanged()
