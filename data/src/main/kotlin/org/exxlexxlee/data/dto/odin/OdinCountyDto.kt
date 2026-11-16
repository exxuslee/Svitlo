package org.exxlexxlee.data.dto.odin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OdinCountyDto(
    val outageType: String,
    val id: String,
    val metersOut: Int,
    @SerialName("outages")
    val utilities: List<OdinUtilityDto> = emptyList(),
    val countyName: String = "",
    val stateName: String = "",
    val precisionLabel: String = "",
)
