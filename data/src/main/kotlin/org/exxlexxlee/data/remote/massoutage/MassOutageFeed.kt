package org.exxlexxlee.data.remote.massoutage

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.exxlexxlee.data.mapper.massoutage.MassOutageMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class MassOutageFeed(
    private val client: HttpClient,
    private val massOutage: MassOutageMapper,
) : InternationalOutageFeed {
    override val sourceId = "massoutage"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = run {
        // National state-level summary (always available). When address carries a US county FIPS
        // in house/city fields we also pull incidents for that county.
        val summaryRaw = client.get("https://massoutage.com/api/v1/live/summary").bodyAsText()
        val summary = massOutage.summary(summaryRaw)
        val fips = address?.house?.takeIf { it.length == 5 && it.all(Char::isDigit) }
            ?: address?.cityId?.toString()?.padStart(5, '0')?.takeIf { it.length == 5 }
        if (fips != null) {
            val incidentsRaw =
                client.get("https://massoutage.com/api/v1/live/counties/$fips/incidents") {
                    parameter("min_customers", 1)
                }.bodyAsText()
            summary + massOutage.incidents(incidentsRaw, "FIPS $fips")
        } else {
            summary
        }
    }
}
