package org.exxlexxlee.data.dto.gpv

import kotlinx.serialization.Serializable

@Serializable
data class GpvPresetDto(
    val sch_names: Map<String, String> = emptyMap(),
)
