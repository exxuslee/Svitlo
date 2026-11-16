package org.exxlexxlee.data.remote.massoutage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.json.date
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage
import java.time.Instant

internal class MassOutageMapper(private val json: Json) {
    fun summary(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val data = root["data"]?.jsonObject ?: return emptyList()
        val states = data["states"]?.jsonArray ?: JsonArray(emptyList())
        val national = data["national"]?.jsonObject
        val asOf = national?.text("as_of")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return states.mapNotNull { element ->
            val row = element.jsonObject
            val out = row["customers_out"]?.jsonPrimitive?.intOrNull ?: 0
            if (out <= 0) return@mapNotNull null
            val name = row.text("name")
            val abbr = row.text("abbr")
            InternationalOutage(
                id = "us-state-${row.text("state_fips")}",
                location = "$name ($abbr)",
                description = "$out customers without power · ${row["county_count"]?.jsonPrimitive?.intOrNull ?: 0} counties",
                start = asOf,
                end = null,
                areaOnly = true,
            )
        }.sortedByDescending {
            it.description.substringBefore(" ").toIntOrNull() ?: 0
        }
    }

    fun incidents(raw: String, countyLabel: String = ""): List<InternationalOutage> {
        val root = json.objectOf(raw)
        val items = root["data"]?.jsonArray ?: return emptyList()
        return items.mapIndexed { index, element ->
            val row = element.jsonObject
            val utility = row.text("utility_name").ifBlank { row.text("utility_key") }
            val customers = row["customers_out"]?.jsonPrimitive?.intOrNull
            val cause = row.text("cause")
            val etr = row.text("etr")
            val started = row.text("started_at")
            InternationalOutage(
                id = "mo-${row.text("utility_key")}-$index-${started.ifBlank { etr }}",
                location = listOf(countyLabel, utility).filter { it.isNotBlank() }.joinToString(" · "),
                description = listOfNotNull(
                    customers?.let { "$it customers" },
                    cause.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                start = date(started, "UTC") ?: date(row.text("as_of"), "UTC"),
                end = date(etr, "UTC"),
            )
        }
    }
}
