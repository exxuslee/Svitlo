package org.exxlexxlee.data.dto.ssen

import kotlinx.serialization.Serializable

@Serializable
internal data class SsenFaultDto(
    val reference: String,
    val affectedAreas: List<String>,
    val type: String,
    val title: String = "",
    val message: String = "",
    val localSevereMessage: String = "",
    val loggedAtUtc: String = "",
    val estimatedRestorationTimeUtc: String = "",
)
