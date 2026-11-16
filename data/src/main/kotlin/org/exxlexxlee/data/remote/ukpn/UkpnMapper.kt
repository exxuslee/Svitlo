package org.exxlexxlee.data.remote.ukpn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.exxlexxlee.data.remote.json.date
import org.exxlexxlee.data.remote.json.objectOf
import org.exxlexxlee.data.remote.json.text
import org.exxlexxlee.domain.model.InternationalOutage

internal class UkpnMapper(private val json: Json) {
    fun parse(raw: String): List<InternationalOutage> = json.objectOf(raw).getValue("results").jsonArray.filter {
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
}
