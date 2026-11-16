package org.exxlexxlee.data.dto.tauron

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TauronAddressDto(
    @SerialName("GAID") val gaid: Long,
    @SerialName("Name") val name: String? = null,
    @SerialName("CommuneName") val communeName: String? = null,
    @SerialName("ProvinceName") val provinceName: String? = null,
    @SerialName("FullName") val fullName: String? = null,
)
