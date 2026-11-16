package org.exxlexxlee.data.dto.blackoutmap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class BlackoutMapOutageDto(
    val id: String = "",
    val cc: String = "",
    val rs: String = "",
    val src: String = "",
    val desc: String = "",
    val start: String = "",
    val until: String = "",
    val off: Int? = null,
    val groups: Int? = null,
    val regions: List<List<JsonPrimitive>> = emptyList(),
)
