package org.exxlexxlee.data.remote.blackoutmap

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.json.date
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class BlackoutMapMapper(private val json: Json) {
    fun parse(raw: String, countryCode: String? = null): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val power = root["power"]?.jsonArray ?: JsonArray(emptyList())
        return power.mapNotNull { element ->
            val row = element.jsonObject
            val cc = row.text("cc")
            if (countryCode != null && !cc.equals(countryCode, ignoreCase = true)) return@mapNotNull null
            val regions = row["regions"]?.jsonArray?.mapNotNull { r ->
                r.jsonArray.getOrNull(0)?.jsonPrimitive?.contentOrNull
            }.orEmpty()
            val location = regions.joinToString(" · ").ifBlank { row.text("rs") }
            val src = row.text("src")
            val off = row["off"]?.jsonPrimitive?.intOrNull
            val groups = row["groups"]?.jsonPrimitive?.intOrNull
            val descParts = listOfNotNull(
                src.takeIf { it.isNotBlank() },
                off?.let { "off groups: $it" },
                groups?.let { "total groups: $it" },
                row.text("desc").takeIf { it.isNotBlank() },
            )
            InternationalOutage(
                id = row.text("id").ifBlank { "${cc}_${row.text("start")}" },
                location = location,
                description = descParts.joinToString(" · "),
                start = date(row.text("start"), "UTC"),
                end = date(row.text("until"), "UTC"),
                areaOnly = true,
            )
        }
    }
}
