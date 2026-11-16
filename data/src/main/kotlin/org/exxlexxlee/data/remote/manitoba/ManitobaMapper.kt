package org.exxlexxlee.data.remote.manitoba

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage
import java.time.Instant

internal class ManitobaMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["error"] == null && root["exceededTransferLimit"]?.jsonPrimitive?.booleanOrNull != true) { "Incomplete ArcGIS response" }
        return root.getValue("features").jsonArray.map { element ->
            val row = element.jsonObject.getValue("attributes").jsonObject
            InternationalOutage(
                id = row.text("ORDER_ID"),
                location = listOf(row.text("CSC"), row.text("DEVICE_ADDRESS").trim()).joinToString(" · "),
                description = row.text("ORDER_CAUSE") + "\n" + row.text("ORDER_DESCRIPTION"),
                start = row["SCHEDULED_START_TIME"]?.jsonPrimitive?.longOrNull?.let(Instant::ofEpochMilli),
                end = row["SCHEDULED_END_TIME"]?.jsonPrimitive?.longOrNull?.let(Instant::ofEpochMilli),
            )
        }
    }
}
