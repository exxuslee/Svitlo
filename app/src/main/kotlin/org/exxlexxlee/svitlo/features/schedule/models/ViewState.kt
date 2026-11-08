package org.exxlexxlee.svitlo.features.schedule.models

import org.exxlexxlee.domain.model.Dials
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection

data class ViewState(
    val selection: Selection? = null,
    val dials: Dials? = null,
    val schedule: OutageSchedule? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val error: String? = null,
    val currentHour: Int = KyivTime.hourOfDay(),
    val loaded: Boolean = false,
)
