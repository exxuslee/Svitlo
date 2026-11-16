package org.exxlexxlee.data.remote.tauron

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.exxlexxlee.data.mapper.tauron.TauronMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.international.OutageAddressLookup
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class TauronApi(
    private val client: HttpClient,
    private val tauron: TauronMapper,
    private val clock: Clock,
) : InternationalOutageFeed, OutageAddressLookup {
    override val sourceId = "tauron"
    override val retainsEndedOutages = false

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = run {
        requireNotNull(address)
        val today = LocalDate.now(clock.withZone(ZoneId.of(source.zoneId)))
        tauron.parse(client.get("$TAURON/outages/address") {
            parameter("cityGAID", address.cityId)
            parameter("streetGAID", address.streetId)
            parameter("houseNo", address.house)
            parameter("fromDate", today)
            parameter("toDate", today.plusDays(30))
            parameter("getLightingSupport", true)
            parameter("getServicedSwitchingoff", true)
        }.bodyAsText())
    }

    override suspend fun cities(query: String): List<AddressOption> {
        require(query.trim().length >= 3)
        return tauron.options(client.get("$TAURON/enum/geo/cities") { parameter("partName", query.trim()) }
            .bodyAsText(), true)
    }

    override suspend fun streets(cityId: Long, query: String): List<AddressOption> {
        require(query.trim().length >= 3)
        return tauron.options(client.get("$TAURON/enum/geo/streets") {
            parameter("partName", query.trim())
            parameter("ownerGAID", cityId)
        }.bodyAsText(), false)
    }

    private companion object {
        const val TAURON = "https://www.tauron-dystrybucja.pl/waapi"
    }
}
