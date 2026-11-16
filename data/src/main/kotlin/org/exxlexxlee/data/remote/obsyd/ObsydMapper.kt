package org.exxlexxlee.data.remote.obsyd

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.json.date
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class ObsydMapper(private val json: Json) {
    fun parse(raw: String, zone: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["available"]?.jsonPrimitive?.booleanOrNull != false) {
            root["reason"]?.jsonPrimitive?.contentOrNull ?: "Obsyd returned no outage series"
        }
        val rows = root["data"]?.jsonArray ?: root["records"]?.jsonArray ?: JsonArray(emptyList())
        val latest = rows.mapNotNull { element ->
            val row = element.jsonObject
            val mw = row["value"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val instant = date(row.text("datetime_utc").ifBlank { row.text("t") }, "UTC") ?: return@mapNotNull null
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
            ),
        )
    }
}
