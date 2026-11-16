package org.exxlexxlee.data.mapper.blackoutmap

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.blackoutmap.BlackoutMapOutageDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class BlackoutMapMapper(private val json: Json) {
    fun parse(raw: String, countryCode: String? = null): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val power = root["power"]?.jsonArray ?: JsonArray(emptyList())
        return power.mapNotNull { element ->
            val rawRow = element.jsonObject
            val cc = rawRow.text("cc")
            if (countryCode != null && !cc.equals(
                    countryCode,
                    ignoreCase = true
                )
            ) return@mapNotNull null
            val row = decodeRecord(rawRow)
            map(row)
        }
    }

    private fun map(row: BlackoutMapOutageDto): InternationalOutage {
        val cc = row.cc
        val regions = row.regions.mapNotNull { it.firstOrNull()?.contentOrNull }
        val location = regions.joinToString(" · ").ifBlank { row.rs }
        val src = row.src
        val off = row.off
        val groups = row.groups
        val descParts = listOfNotNull(
            src.takeIf { it.isNotBlank() },
            off?.let { "off groups: $it" },
            groups?.let { "total groups: $it" },
            row.desc.takeIf { it.isNotBlank() },
        )
        return InternationalOutage(
            id = row.id.ifBlank { "${cc}_${row.start}" },
            location = location,
            description = descParts.joinToString(" · "),
            start = date(row.start, "UTC"),
            end = date(row.until, "UTC"),
            areaOnly = true,
        )
    }

    private fun decodeRecord(row: JsonObject): BlackoutMapOutageDto = BlackoutMapOutageDto(
        id = row.text("id"),
        cc = row.text("cc"),
        rs = row.text("rs"),
        src = row.text("src"),
        desc = row.text("desc"),
        start = row.text("start"),
        until = row.text("until"),
        off = row["off"]?.jsonPrimitive?.intOrNull,
        groups = row["groups"]?.jsonPrimitive?.intOrNull,
        regions = row["regions"]?.jsonArray?.map { r -> r.jsonArray.map { it.jsonPrimitive } }.orEmpty(),
    )
}
