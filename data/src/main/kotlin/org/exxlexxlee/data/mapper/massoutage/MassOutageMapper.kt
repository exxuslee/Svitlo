package org.exxlexxlee.data.mapper.massoutage

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.massoutage.MassOutageIncidentDto
import org.exxlexxlee.data.dto.massoutage.MassOutageStateDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class MassOutageMapper(private val json: Json) {
    fun summary(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val data = root["data"]?.jsonObject ?: return emptyList()
        val states = data["states"]?.jsonArray ?: JsonArray(emptyList())
        val national = data["national"]?.jsonObject
        val asOf = national?.text("as_of")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return states.mapNotNull { element ->
            val rawRow = element.jsonObject
            val out = rawRow["customers_out"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            require(out >= 0)
            // A zero count is useful only when the response identifies its area.
            if (out == 0 && (rawRow["name"] as? JsonPrimitive)?.contentOrNull.isNullOrBlank()) return@mapNotNull null
            val row = decodeState(rawRow)
            val name = row.name
            val abbr = row.abbr
            InternationalOutage(
                id = "us-state-${row.stateFips}",
                location = "$name ($abbr)",
                description = "$out customers without power · ${row.countyCount ?: 0} counties",
                start = asOf,
                end = null,
                areaOnly = true,
                affectedCustomers = out,
                isAreaSummary = true,
            )
        }.sortedByDescending {
            it.description.substringBefore(" ").toIntOrNull() ?: 0
        }
    }

    fun incidents(raw: String, countyLabel: String = ""): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val items = root["data"]?.jsonArray ?: return emptyList()
        return items.mapIndexed { index, element ->
            val row = decodeIncident(element.jsonObject)
            val utility = row.utilityName.ifBlank { row.utilityKey }
            val customers = row.customersOut
            val cause = row.cause
            val etr = row.etr
            val started = row.startedAt
            InternationalOutage(
                id = "mo-${row.utilityKey}-$index-${started.ifBlank { etr }}",
                location = listOf(countyLabel, utility).filter { it.isNotBlank() }
                    .joinToString(" · "),
                description = listOfNotNull(
                    customers?.let { "$it customers" },
                    cause.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                start = date(started, "UTC") ?: date(row.asOf, "UTC"),
                end = date(etr, "UTC"),
                affectedCustomers = customers?.takeIf { it >= 0 },
                endIsEstimate = true,
            )
        }
    }

    private fun decodeState(row: JsonObject): MassOutageStateDto = MassOutageStateDto(
        stateFips = row.text("state_fips"),
        name = row.text("name"),
        abbr = row.text("abbr"),
        customersOut = row["customers_out"]?.jsonPrimitive?.intOrNull,
        countyCount = row["county_count"]?.jsonPrimitive?.intOrNull,
    )

    private fun decodeIncident(row: JsonObject): MassOutageIncidentDto = MassOutageIncidentDto(
        utilityName = row.text("utility_name"),
        utilityKey = row.text("utility_key"),
        customersOut = row["customers_out"]?.jsonPrimitive?.intOrNull,
        cause = row.text("cause"),
        etr = row.text("etr"),
        startedAt = row.text("started_at"),
        asOf = row.text("as_of"),
    )
}
