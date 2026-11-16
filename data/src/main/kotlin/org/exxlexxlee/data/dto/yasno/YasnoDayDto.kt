package org.exxlexxlee.data.dto.yasno

import kotlinx.serialization.Serializable

@Serializable
data class YasnoDayDto(
    val slots: List<YasnoSlotDto>,
    val date: String? = null,
    val status: String? = null,
)
