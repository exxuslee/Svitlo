package org.exxlexxlee.data.mapper.ukpn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import org.exxlexxlee.data.dto.ukpn.UkpnOutageDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class UkpnMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> =
        records(json.objectOf(raw).getValue("results").jsonArray.map { it.jsonObject })

    fun records(records: List<JsonObject>): List<InternationalOutage> = records.filter {
        it.text("restoreddatetime").isBlank()
    }.map { map(decodeRecord(it)) }

    private fun map(row: UkpnOutageDto): InternationalOutage {
        return InternationalOutage(
            id = row.incidentReference
                .also { require(it.isNotBlank()) { "Missing UKPN incident reference" } },
            location = listOf(
                row.operatingZone,
                row.postcodesAffected,
                row.fullPostcodeData
            ).filter { it.isNotBlank() }.joinToString(" · "),
            description = listOf(
                row.powerCutType,
                row.incidentDescription,
                row.mainMessage
            ).filter { it.isNotBlank() }.joinToString("\n"),
            start = date(
                row.plannedDate.ifBlank { row.receivedDate },
                "Europe/London"
            ),
            end = date(row.estimatedRestorationDate, "Europe/London"),
            endIsEstimate = true,
            affectedCustomers = row.affectedCustomers,
            point = row.latitude?.let { lat -> row.longitude?.let { lon ->
                runCatching { org.exxlexxlee.domain.model.GeoPoint(lat, lon) }.getOrNull()
            } },
            kind = if (row.powerCutType.equals("Planned", true) || row.powerCutType.isBlank() && row.plannedDate.isNotBlank()) org.exxlexxlee.domain.model.OutageKind.PLANNED
                else org.exxlexxlee.domain.model.OutageKind.EMERGENCY,
        )
    }

    private fun decodeRecord(row: JsonObject): UkpnOutageDto = UkpnOutageDto(
        incidentReference = row.text("incidentreference"),
        restoredDateTime = row.text("restoreddatetime"),
        operatingZone = row.text("operatingzone"),
        postcodesAffected = row.text("postcodesaffected"),
        fullPostcodeData = row.text("fullpostcodedata"),
        powerCutType = row.text("powercuttype"),
        incidentDescription = row.text("incidentcategorycustomerfriendlydescription"),
        mainMessage = row.text("mainmessage"),
        plannedDate = row.text("planneddate"),
        receivedDate = row.text("receiveddate"),
        estimatedRestorationDate = row.text("estimatedrestorationdate"),
        affectedCustomers = row["nocustomeraffected"]?.jsonPrimitive?.intOrNull?.takeIf { it >= 0 },
        latitude = (row["geopoint"] as? JsonObject)?.get("lat")?.jsonPrimitive?.doubleOrNull,
        longitude = (row["geopoint"] as? JsonObject)?.get("lon")?.jsonPrimitive?.doubleOrNull,
    )
}
