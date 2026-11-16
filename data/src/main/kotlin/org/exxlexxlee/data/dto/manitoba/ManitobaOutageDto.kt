package org.exxlexxlee.data.dto.manitoba

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ManitobaOutageDto(
    @SerialName("ORDER_ID")
    val orderId: String = "",
    @SerialName("CSC")
    val csc: String = "",
    @SerialName("DEVICE_ADDRESS")
    val deviceAddress: String = "",
    @SerialName("ORDER_CAUSE")
    val orderCause: String = "",
    @SerialName("ORDER_DESCRIPTION")
    val orderDescription: String = "",
    @SerialName("SCHEDULED_START_TIME")
    val scheduledStartTime: Long? = null,
    @SerialName("SCHEDULED_END_TIME")
    val scheduledEndTime: Long? = null,
)
