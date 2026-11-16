package org.exxlexxlee.data.dto.gpv

import kotlinx.serialization.Serializable

@Serializable
data class GpvFileDto(
    val lastUpdated: String? = null,
    val fact: GpvFactDto? = null,
    val preset: GpvPresetDto? = null,
)
