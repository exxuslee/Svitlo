package org.exxlexxlee.data.dto.yasno

import kotlinx.serialization.Serializable

@Serializable
data class YasnoSlotDto(
    val start: Int,
    val end: Int,
    val type: String? = null,
)
