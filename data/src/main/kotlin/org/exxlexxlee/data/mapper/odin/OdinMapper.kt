package org.exxlexxlee.data.mapper.odin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.odin.OdinCountyDto
import org.exxlexxlee.data.dto.odin.OdinUtilityDto
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class OdinMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> =
        json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
            map(decodeRecord(element.jsonObject))
        }

    private fun map(row: OdinCountyDto): InternationalOutage? {
        require(row.outageType == "COUNTY") { "Unexpected ODIN resolution" }
        val id =
            row.id.also { require(it.matches(Regex("[0-9]{5}"))) }
        val meters = row.metersOut.also { require(it >= 0) }
        val utilities =
            row.utilities.map { it.utilityName }
                .filter { it.isNotBlank() }.distinct()
        return InternationalOutage(
            id = "odin-$id",
            location = listOf(
                row.countyName,
                row.stateName,
                "FIPS $id"
            ).filter { it.isNotBlank() }.joinToString(" · "),
            description = (listOf(
                "$meters meters without power",
                row.precisionLabel
            ) + utilities).filter { it.isNotBlank() }.joinToString("\n"),
            // County counts carry neither a confirmed interruption interval nor a restoration estimate.
            start = null, end = null, areaOnly = true,
            affectedCustomers = meters,
            isAreaSummary = true,
        )
    }

    private fun decodeRecord(row: JsonObject): OdinCountyDto = OdinCountyDto(
        outageType = row.getValue("outageType").jsonPrimitive.content,
        id = row.getValue("id").jsonPrimitive.content,
        metersOut = row.getValue("metersOut").jsonPrimitive.int,
        utilities = if (row.getValue("metersOut").jsonPrimitive.int <= 0) emptyList() else {
            row.getValue("outages").jsonArray.map {
                OdinUtilityDto(it.jsonObject.text("utilityName"))
            }
        },
        countyName = row.text("countyName"),
        stateName = row.text("stateName"),
        precisionLabel = row.text("precisionLabel"),
    )
}
