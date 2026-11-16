package org.exxlexxlee.data.mapper.manitoba

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.exxlexxlee.data.dto.manitoba.ManitobaOutageDto
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class ManitobaMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["error"] == null && root["exceededTransferLimit"]?.jsonPrimitive?.booleanOrNull != true) { "Incomplete ArcGIS response" }
        return root.getValue("features").jsonArray.map { element ->
            val row = decodeRecord(element.jsonObject.getValue("attributes").jsonObject)
            map(row)
        }
    }

    private fun map(row: ManitobaOutageDto): InternationalOutage {
        return InternationalOutage(
            id = row.orderId,
            location = listOf(
                row.csc,
                row.deviceAddress.trim()
            ).joinToString(" · "),
            description = row.orderCause + "\n" + row.orderDescription,
            start = row.scheduledStartTime?.let(Instant::ofEpochMilli),
            end = row.scheduledEndTime?.let(Instant::ofEpochMilli),
            kind = org.exxlexxlee.domain.model.OutageKind.PLANNED,
        )
    }

    private fun decodeRecord(row: JsonObject): ManitobaOutageDto = ManitobaOutageDto(
        orderId = row.text("ORDER_ID"),
        csc = row.text("CSC"),
        deviceAddress = row.text("DEVICE_ADDRESS"),
        orderCause = row.text("ORDER_CAUSE"),
        orderDescription = row.text("ORDER_DESCRIPTION"),
        scheduledStartTime = row["SCHEDULED_START_TIME"]?.jsonPrimitive?.longOrNull,
        scheduledEndTime = row["SCHEDULED_END_TIME"]?.jsonPrimitive?.longOrNull,
    )
}
