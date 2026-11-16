package org.exxlexxlee.data.remote

import kotlinx.serialization.json.*
import org.exxlexxlee.domain.model.InternationalOutage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class InternationalOutageMapper(private val json: Json) {
    fun ukpn(raw: String): List<InternationalOutage> = objectOf(raw).getValue("results").jsonArray.filter {
        it.jsonObject.text("restoreddatetime").isBlank()
    }.map { element ->
        val row = element.jsonObject
        InternationalOutage(
            id = row.text("incidentreference"),
            location = listOf(row.text("operatingzone"), row.text("postcodesaffected"), row.text("fullpostcodedata")).filter { it.isNotBlank() }.joinToString(" · "),
            description = listOf(row.text("powercuttype"), row.text("incidentcategorycustomerfriendlydescription"), row.text("mainmessage")).filter { it.isNotBlank() }.joinToString("\n"),
            start = date(row.text("planneddate").ifBlank { row.text("receiveddate") }, "Europe/London"),
            end = date(row.text("estimatedrestorationdate"), "Europe/London"),
        )
    }

    fun manitoba(raw: String): List<InternationalOutage> {
        val root = objectOf(raw)
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

    fun tauron(raw: String): List<InternationalOutage> {
        val root = objectOf(raw)
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

    private fun objectOf(raw: String) = json.parseToJsonElement(raw).jsonObject
    private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun date(value: String, zone: String): Instant? = if (value.isBlank()) null else
        runCatching { Instant.parse(value) }.getOrElse {
            LocalDateTime.parse(value).atZone(ZoneId.of(zone)).toInstant()
        }
}
