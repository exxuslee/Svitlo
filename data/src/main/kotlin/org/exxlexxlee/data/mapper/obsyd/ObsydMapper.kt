package org.exxlexxlee.data.mapper.obsyd

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.obsyd.ObsydPointDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class ObsydMapper(private val json: Json) {
    fun parse(raw: String, zone: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["available"]?.jsonPrimitive?.booleanOrNull != false) {
            root["reason"]?.jsonPrimitive?.contentOrNull ?: "Obsyd returned no outage series"
        }
        val rows = root["data"]?.jsonArray ?: root["records"]?.jsonArray ?: JsonArray(emptyList())
        val latest = rows.mapNotNull { element ->
            val rawRow = element.jsonObject
            if (rawRow["value"]?.jsonPrimitive?.doubleOrNull == null) return@mapNotNull null
            val row = decodeRecord(rawRow)
            val mw = checkNotNull(row.value)
            val instant = date(row.datetimeUtc.ifBlank { row.t }, "UTC")
                ?: return@mapNotNull null
            instant to mw
        }.maxByOrNull { it.first } ?: return emptyList()
        if (latest.second <= 0.0) return emptyList()
        return listOf(
            InternationalOutage(
                id = "obsyd-$zone-${latest.first}",
                location = "ENTSO-E zone $zone",
                description = "Forced generation offline ${latest.second.toLong()} MW · ENTSO-E A77 via Obsyd",
                start = latest.first,
                end = null,
                areaOnly = true,
                // Generation telemetry does not establish a consumer outage interval.
                isAreaSummary = true,
            ),
        )
    }

    private fun decodeRecord(row: JsonObject): ObsydPointDto = ObsydPointDto(
        datetimeUtc = row.text("datetime_utc"),
        t = row.text("t"),
        value = row["value"]?.jsonPrimitive?.doubleOrNull,
    )
}
