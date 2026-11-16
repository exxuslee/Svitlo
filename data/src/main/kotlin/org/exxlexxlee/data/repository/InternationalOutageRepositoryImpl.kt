package org.exxlexxlee.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.InternationalOutageMapper
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class InternationalOutageRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
    private val clock: Clock = Clock.systemUTC(),
) : InternationalOutageRepository {
    private val mapper = InternationalOutageMapper(json)
    override fun sources(country: Country) = InternationalSources.all.filter { it.country == country }

    override suspend fun outages(source: OutageSource, address: OutageAddress?): List<InternationalOutage> {
        require(source in sources(source.country))
        val records = when (source.id) {
            "ukpn" -> buildList {
                var offset = 0
                do {
                    val raw = client.get("https://ukpowernetworks.opendatasoft.com/api/explore/v2.1/catalog/datasets/ukpn-live-faults/records") {
                        parameter("limit", 100)
                        parameter("offset", offset)
                        parameter("order_by", "incidentreference")
                    }.bodyAsText()
                    val root = json.parseToJsonElement(raw).jsonObject
                    val page = mapper.ukpn(raw)
                    addAll(page)
                    val count = root.getValue("results").jsonArray.size
                    offset += count
                    val total = root.getValue("total_count").jsonPrimitive.int
                    require(count > 0 || offset >= total) { "Incomplete UKPN response" }
                    require(offset < 10000 || offset >= total) { "UKPN pagination limit reached" }
                } while (offset < total)
            }
            "manitoba" -> mapper.manitoba(client.get("https://services2.arcgis.com/QoeQkfdOG126FqSi/ArcGIS/rest/services/Manitoba_Hydro_Planned_Power_Outages/FeatureServer/0/query") {
                parameter("where", "1=1")
                parameter("outFields", "*")
                parameter("returnGeometry", false)
                parameter("f", "json")
            }.bodyAsText())
            "tauron" -> {
                requireNotNull(address)
                val today = LocalDate.now(clock.withZone(ZoneId.of(source.zoneId)))
                mapper.tauron(client.get("$TAURON/outages/address") {
                    parameter("cityGAID", address.cityId)
                    parameter("streetGAID", address.streetId)
                    parameter("houseNo", address.house)
                    parameter("fromDate", today)
                    parameter("toDate", today.plusDays(30))
                    parameter("getLightingSupport", true)
                    parameter("getServicedSwitchingoff", true)
                }.bodyAsText())
            }
            else -> error("Source does not provide a verified live feed")
        }
        return records.filter { source.id == "ukpn" || it.end?.let { end -> end > clock.instant() } != false }
            .distinctBy { it.id }.sortedBy { it.start }
    }

    override suspend fun cities(query: String): List<AddressOption> {
        require(query.trim().length >= 3)
        return options(client.get("$TAURON/enum/geo/cities") { parameter("partName", query.trim()) }.bodyAsText(), true)
    }

    override suspend fun streets(cityId: Long, query: String): List<AddressOption> {
        require(query.trim().length >= 3)
        return options(client.get("$TAURON/enum/geo/streets") {
            parameter("partName", query.trim())
            parameter("ownerGAID", cityId)
        }.bodyAsText(), false)
    }

    private fun options(raw: String, city: Boolean) = json.parseToJsonElement(raw).jsonArray.map { element ->
        val row = element.jsonObject
        AddressOption(row.getValue("GAID").jsonPrimitive.long,
            if (city) listOf("Name", "CommuneName", "ProvinceName").mapNotNull { row[it]?.jsonPrimitive?.contentOrNull }.distinct().joinToString(" · ")
            else row.getValue("FullName").jsonPrimitive.content)
    }

    private companion object { const val TAURON = "https://www.tauron-dystrybucja.pl/waapi" }
}
