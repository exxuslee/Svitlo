package org.exxlexxlee.data.remote.obsyd

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.exxlexxlee.data.mapper.obsyd.ObsydMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class ObsydOutageFeed(
    private val client: HttpClient,
    private val obsyd: ObsydMapper,
    private val clock: Clock,
) : InternationalOutageFeed {
    override val sourceId = "obsyd"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = run {
        val zone = obsydZone(source.country)
        val today = LocalDate.now(clock.withZone(ZoneId.of(source.zoneId)))
        obsyd.parse(client.get("https://obsyd.dev/api/v1/series") {
            parameter("series", "outage.forced")
            parameter("zone", zone)
            parameter("resolution", "hourly")
            parameter("start", today)
            parameter("end", today.plusDays(1))
        }.bodyAsText(), zone)
    }

    private fun obsydZone(country: Country) =
        zones[country] ?: error("No Obsyd bidding zone for $country")

    private companion object {
        val zones =
            mapOf(Country.POLAND to "PL", Country.UNITED_KINGDOM to "GB", Country.IRELAND to "IE")
    }
}
