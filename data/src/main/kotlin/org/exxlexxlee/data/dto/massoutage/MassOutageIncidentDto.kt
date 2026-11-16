package org.exxlexxlee.data.dto.massoutage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MassOutageIncidentDto(
    @SerialName("utility_name")
    val utilityName: String = "",
    @SerialName("utility_key")
    val utilityKey: String = "",
    @SerialName("customers_out")
    val customersOut: Int? = null,
    val cause: String = "",
    val etr: String = "",
    @SerialName("started_at")
    val startedAt: String = "",
    @SerialName("as_of")
    val asOf: String = "",
)
