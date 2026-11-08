package org.exxlexxlee.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class YasnoGroupDto(
    val today: YasnoDayDto? = null,
    val tomorrow: YasnoDayDto? = null,
    val updatedOn: String? = null,
)

@Serializable
data class YasnoDayDto(
    val slots: List<YasnoSlotDto> = emptyList(),
    val date: String? = null,
    val status: String? = null,
)

@Serializable
data class YasnoSlotDto(
    val start: Int,
    val end: Int,
    val type: String? = null,
)
