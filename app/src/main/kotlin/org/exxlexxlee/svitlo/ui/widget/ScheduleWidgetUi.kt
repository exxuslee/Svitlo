package org.exxlexxlee.svitlo.ui.widget

import org.exxlexxlee.domain.model.PowerState

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
