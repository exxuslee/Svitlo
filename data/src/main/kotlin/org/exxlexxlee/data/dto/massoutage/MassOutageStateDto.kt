package org.exxlexxlee.data.dto.massoutage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MassOutageStateDto(
    @SerialName("state_fips")
    val stateFips: String = "",
    val name: String = "",
    val abbr: String = "",
    @SerialName("customers_out")
    val customersOut: Int? = null,
    @SerialName("county_count")
    val countyCount: Int? = null,
)
