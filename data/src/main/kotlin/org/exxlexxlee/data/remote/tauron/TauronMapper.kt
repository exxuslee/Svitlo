package org.exxlexxlee.data.remote.tauron

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.json.date
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class TauronMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["IdsWWW"]?.jsonArray?.map { it.jsonPrimitive.intOrNull } != listOf(0)) { "Unsupported Tauron area" }
        val areaOnly = root["OutageListType"]?.jsonPrimitive?.intOrNull != 1
        return root.getValue("OutageItems").let { if (it is JsonNull) JsonArray(emptyList()) else it.jsonArray }.filter {
            it.jsonObject["TypeId"]?.jsonPrimitive?.intOrNull == 1 || it.jsonObject["IsActive"]?.jsonPrimitive?.booleanOrNull == true
        }.map { element ->
            val row = element.jsonObject
            InternationalOutage(
                id = row.text("OutageId") + row.text("StartDate"),
                location = row.text("Message"),
                description = "",
                start = date(row.text("StartDate"), "Europe/Warsaw"),
                end = date(row.text("EndDate"), "Europe/Warsaw"),
                areaOnly = areaOnly,
            )
        }
    }
}
