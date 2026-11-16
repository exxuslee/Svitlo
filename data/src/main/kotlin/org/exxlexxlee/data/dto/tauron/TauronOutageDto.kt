package org.exxlexxlee.data.dto.tauron

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TauronOutageDto(
    @SerialName("OutageId")
    val outageId: String = "",
    @SerialName("StartDate")
    val startDate: String = "",
    @SerialName("EndDate")
    val endDate: String = "",
    @SerialName("Message")
    val message: String = "",
    @SerialName("TypeId")
    val typeId: Int? = null,
    @SerialName("IsActive")
    val isActive: Boolean? = null,
)
