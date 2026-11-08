package org.exxlexxlee.svitlo.features.address.models

import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region

data class ViewState(
    val regions: List<Region> = emptyList(),
    val groups: List<OutageGroup> = emptyList(),
    val selectedRegion: Region? = null,
    val selectedGroup: OutageGroup? = null,
    val regionQuery: String = "",
    val groupQuery: String = "",
    val isLoadingRegions: Boolean = true,
    val isLoadingGroups: Boolean = false,
    val error: String? = null,
) {
    val filteredRegions: List<Region>
        get() = regions.filter { it.name.contains(regionQuery, ignoreCase = true) }

    val filteredGroups: List<OutageGroup>
        get() = groups.filter {
            it.name.contains(groupQuery, ignoreCase = true) ||
                it.id.contains(groupQuery, ignoreCase = true)
        }
}
