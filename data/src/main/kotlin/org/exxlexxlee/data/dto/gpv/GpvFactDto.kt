package org.exxlexxlee.data.dto.gpv

import kotlinx.serialization.Serializable

@Serializable
data class GpvFactDto(
    val data: Map<String, Map<String, Map<String, String>>> = emptyMap(),
    val update: String? = null,
    val today: Long? = null,
)
