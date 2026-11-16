package org.exxlexxlee.data.mapper.tauron

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.exxlexxlee.data.dto.tauron.TauronAddressDto
import org.exxlexxlee.data.dto.tauron.TauronOutageDto
import org.exxlexxlee.data.mapper.json.date
import org.exxlexxlee.data.mapper.json.objectOf
import org.exxlexxlee.data.mapper.json.text
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.InternationalOutage

internal class TauronMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> {
        val root = json.objectOf(raw)
        require(root["IdsWWW"]?.jsonArray?.map { it.jsonPrimitive.intOrNull } != listOf(0)) { "Unsupported Tauron area" }
        val areaOnly = root["OutageListType"]?.jsonPrimitive?.intOrNull != 1
        return root.getValue("OutageItems")
            .let { if (it is JsonNull) JsonArray(emptyList()) else it.jsonArray }.filter {
                it.jsonObject["TypeId"]?.jsonPrimitive?.intOrNull == 1 ||
                    it.jsonObject["IsActive"]?.jsonPrimitive?.booleanOrNull == true
            }.map { map(decodeRecord(it.jsonObject), areaOnly) }
    }

    private fun map(row: TauronOutageDto, areaOnly: Boolean): InternationalOutage {
        return InternationalOutage(
            id = row.outageId + row.startDate,
            location = row.message,
            description = "",
            start = date(row.startDate, "Europe/Warsaw"),
            end = date(row.endDate, "Europe/Warsaw"),
            areaOnly = areaOnly,
            kind = if (row.typeId == 1) org.exxlexxlee.domain.model.OutageKind.PLANNED else org.exxlexxlee.domain.model.OutageKind.ACTUAL,
        )
    }

    private fun decodeRecord(row: JsonObject): TauronOutageDto = TauronOutageDto(
        outageId = row.text("OutageId"),
        startDate = row.text("StartDate"),
        endDate = row.text("EndDate"),
        message = row.text("Message"),
        typeId = row["TypeId"]?.jsonPrimitive?.intOrNull,
        isActive = row["IsActive"]?.jsonPrimitive?.booleanOrNull,
    )

    fun options(raw: String, city: Boolean): List<AddressOption> =
        json.parseToJsonElement(raw).jsonArray.map { element ->
            val row = element.jsonObject
            val dto = TauronAddressDto(
                gaid = row.getValue("GAID").jsonPrimitive.long,
                name = if (city) row["Name"]?.jsonPrimitive?.contentOrNull else null,
                communeName = if (city) row["CommuneName"]?.jsonPrimitive?.contentOrNull else null,
                provinceName = if (city) row["ProvinceName"]?.jsonPrimitive?.contentOrNull else null,
                fullName = if (city) null else row.getValue("FullName").jsonPrimitive.content,
            )
            AddressOption(
                dto.gaid,
                if (city) listOfNotNull(dto.name, dto.communeName, dto.provinceName)
                    .distinct().joinToString(" · ") else checkNotNull(dto.fullName),
            )
        }
}
