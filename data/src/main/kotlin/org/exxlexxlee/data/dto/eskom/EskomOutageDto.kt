package org.exxlexxlee.data.dto.eskom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EskomOutageDto(
    @SerialName("area_name")
    val areaName: String,
    val start: String,
    @SerialName("finsh")
    val finish: String,
    val stage: String,
    val source: String,
)
