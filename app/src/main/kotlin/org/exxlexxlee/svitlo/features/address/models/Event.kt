package org.exxlexxlee.svitlo.features.address.models

import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region

sealed interface Event {
    data class RegionQuery(val query: String) : Event
    data class GroupQuery(val query: String) : Event
    data class RegionSelected(val region: Region) : Event
    data class GroupSelected(val group: OutageGroup) : Event
    data object Save : Event
}
