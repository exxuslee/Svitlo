package org.exxlexxlee.data.mapper.ssen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.ssen.SsenFaultDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class SsenMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> =
        json.parseToJsonElement(raw).jsonObject.getValue("faults").jsonArray
            .map { map(decodeRecord(it.jsonObject)) }

    fun parseFault(raw: String): InternationalOutage =
        map(decodeRecord(json.parseToJsonElement(raw).jsonObject))

    fun map(row: SsenFaultDto): InternationalOutage {
        val reference =
            row.reference.also { require(it.isNotBlank()) }
        val areas = row.affectedAreas
        val planned = row.type == "PSI"
        return InternationalOutage(
            id = reference,
            location = (listOf(row.title) + areas).filter { it.isNotBlank() }.distinct()
                .joinToString(" · "),
            description = listOf(
                row.type,
                row.message,
                row.localSevereMessage
            ).filter { it.isNotBlank() }.joinToString("\n"),
            // A planned job's loggedAtUtc is its creation time, often weeks before the interruption.
            start = if (planned) null else date(row.loggedAtUtc, "Europe/London"),
            end = date(row.estimatedRestorationTimeUtc, "Europe/London"),
            endIsEstimate = true,
            kind = if (planned) org.exxlexxlee.domain.model.OutageKind.PLANNED else org.exxlexxlee.domain.model.OutageKind.EMERGENCY,
        )
    }

    private fun decodeRecord(row: JsonObject): SsenFaultDto = SsenFaultDto(
        reference = row.getValue("reference").jsonPrimitive.content,
        affectedAreas = row.getValue("affectedAreas").jsonArray.map { it.jsonPrimitive.content },
        type = row.getValue("type").jsonPrimitive.content,
        title = row.text("title"),
        message = row.text("message"),
        localSevereMessage = row.text("localSevereMessage"),
        loggedAtUtc = row.text("loggedAtUtc"),
        estimatedRestorationTimeUtc = row.text("estimatedRestorationTimeUtc"),
    )
}
