package org.exxlexxlee.data.dto.yasno

import kotlinx.serialization.Serializable

@Serializable
data class YasnoGroupDto(
    val today: YasnoDayDto? = null,
    val tomorrow: YasnoDayDto? = null,
    val updatedOn: String? = null,
)
