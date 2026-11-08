package org.exxlexxlee.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class GpvFileDto(
    val lastUpdated: String? = null,
    val fact: GpvFactDto? = null,
    val preset: GpvPresetDto? = null,
)

@Serializable
data class GpvFactDto(
    val data: Map<String, Map<String, Map<String, String>>> = emptyMap(),
    val update: String? = null,
    val today: Long? = null,
)

@Serializable
data class GpvPresetDto(
    val sch_names: Map<String, String> = emptyMap(),
)
