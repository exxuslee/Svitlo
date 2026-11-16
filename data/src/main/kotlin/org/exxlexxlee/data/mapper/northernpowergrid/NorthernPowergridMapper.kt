package org.exxlexxlee.data.mapper.northernpowergrid

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.northernpowergrid.NorthernPowergridOutageDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal object NorthernPowergridMapper {
    fun map(records: List<JsonObject>): List<InternationalOutage> = records.mapNotNull { raw ->
        if (raw.getValue("isaffected").jsonPrimitive.int == 0) return@mapNotNull null
        val row = decodeRecord(raw)
        val id = row.id
        val reference = row.reference
        val postcodes = row.postcode
        val planned = row.natureOfOutage.contains("planned", ignoreCase = true)
        InternationalOutage(
            // The same incident has separate rows/postcodes; retain them for saved location filters.
            id = "$reference:$id",
            location = (listOf(row.area) + postcodes).filter { it.isNotBlank() }
                .joinToString(" · "),
            description = listOf(
                row.natureOfOutage,
                row.reason,
                row.customerStageSequenceMessage
            ).filter { it.isNotBlank() }.joinToString("\n"),
            start = if (planned) null else date(row.loggedTime, "Europe/London"),
            end = date(row.estimatedTimeTillResolution, "Europe/London"),
            endIsEstimate = true,
            kind = if (planned) org.exxlexxlee.domain.model.OutageKind.PLANNED else org.exxlexxlee.domain.model.OutageKind.EMERGENCY,
        )
    }

    private fun decodeRecord(row: JsonObject): NorthernPowergridOutageDto = NorthernPowergridOutageDto(
        id = row.getValue("id").jsonPrimitive.content,
        reference = row.getValue("reference").jsonPrimitive.content,
        postcode = row.getValue("postcode").jsonArray.map { it.jsonPrimitive.content },
        area = row.text("area"),
        natureOfOutage = row.text("natureofoutage"),
        reason = row.text("reason"),
        customerStageSequenceMessage = row.text("customerstagesequencemessage"),
        loggedTime = row.text("loggedtime"),
        estimatedTimeTillResolution = row.text("estimatedtimetillresolution"),
    )
}
