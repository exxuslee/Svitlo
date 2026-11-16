package org.exxlexxlee.data.dto.ukpn

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UkpnOutageDto(
    @SerialName("incidentreference")
    val incidentReference: String = "",
    @SerialName("restoreddatetime")
    val restoredDateTime: String = "",
    @SerialName("operatingzone")
    val operatingZone: String = "",
    @SerialName("postcodesaffected")
    val postcodesAffected: String = "",
    @SerialName("fullpostcodedata")
    val fullPostcodeData: String = "",
    @SerialName("powercuttype")
    val powerCutType: String = "",
    @SerialName("incidentcategorycustomerfriendlydescription")
    val incidentDescription: String = "",
    @SerialName("mainmessage")
    val mainMessage: String = "",
    @SerialName("planneddate")
    val plannedDate: String = "",
    @SerialName("receiveddate")
    val receivedDate: String = "",
    @SerialName("estimatedrestorationdate")
    val estimatedRestorationDate: String = "",
    val affectedCustomers: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
