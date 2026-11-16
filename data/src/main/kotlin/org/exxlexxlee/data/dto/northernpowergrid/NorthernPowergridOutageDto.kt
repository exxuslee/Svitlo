package org.exxlexxlee.data.dto.northernpowergrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NorthernPowergridOutageDto(
    val id: String,
    val reference: String,
    val postcode: List<String>,
    val area: String = "",
    @SerialName("natureofoutage")
    val natureOfOutage: String = "",
    val reason: String = "",
    @SerialName("customerstagesequencemessage")
    val customerStageSequenceMessage: String = "",
    @SerialName("loggedtime")
    val loggedTime: String = "",
    @SerialName("estimatedtimetillresolution")
    val estimatedTimeTillResolution: String = "",
)
