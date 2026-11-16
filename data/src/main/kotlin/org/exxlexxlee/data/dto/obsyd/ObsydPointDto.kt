package org.exxlexxlee.data.dto.obsyd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ObsydPointDto(
    @SerialName("datetime_utc")
    val datetimeUtc: String = "",
    val t: String = "",
    val value: Double? = null,
)
