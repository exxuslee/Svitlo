package org.exxlexxlee.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.blackoutmap.BlackoutMapMapper
import org.exxlexxlee.data.remote.manitoba.ManitobaMapper
import org.exxlexxlee.data.remote.massoutage.MassOutageMapper
import org.exxlexxlee.data.remote.obsyd.ObsydMapper
import org.exxlexxlee.data.remote.tauron.TauronMapper
import org.exxlexxlee.data.remote.ukpn.UkpnMapper
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
    private val ukpn = UkpnMapper(json)
    private val manitoba = ManitobaMapper(json)
    private val tauron = TauronMapper(json)
    private val blackoutMap = BlackoutMapMapper(json)
    private val massOutage = MassOutageMapper(json)
    private val obsyd = ObsydMapper(json)
    override fun sources(country: Country) = InternationalSources.all.filter { it.country == country }

    override suspend fun outages(source: OutageSource, address: OutageAddress?): List<InternationalOutage> {
        require(source in sources(source.country))
        val records = when (source.id) {
            "blackoutmap" -> {
                val raw = client.get("https://blackoutmap.net/data/status.json").bodyAsText()
                // Prefer ISO2 country code when available; Ukraine is the primary scheduled feed today
                val code = when (source.country) {
                    Country.UKRAINE -> "UA"
                    else -> null
                }
                blackoutMap.parse(raw, code)
            }
            "massoutage" -> {
                // National state-level summary (always available). When address carries a US county FIPS
                // in house/city fields we also pull incidents for that county.
                val summaryRaw = client.get("https://massoutage.com/api/v1/live/summary").bodyAsText()
                val summary = massOutage.summary(summaryRaw)
                val fips = address?.house?.takeIf { it.length == 5 && it.all(Char::isDigit) }
                    ?: address?.cityId?.toString()?.padStart(5, '0')?.takeIf { it.length == 5 }
                if (fips != null) {
                    val incidentsRaw = client.get("https://massoutage.com/api/v1/live/counties/$fips/incidents") {
                        parameter("min_customers", 1)
                    }.bodyAsText()
                    summary + massOutage.incidents(incidentsRaw, "FIPS $fips")
                } else {
                    summary
                }
            }
            "obsyd" -> {
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
            "ukpn" -> buildList {
                var offset = 0
                do {
                    val raw = client.get("https://ukpowernetworks.opendatasoft.com/api/explore/v2.1/catalog/datasets/ukpn-live-faults/records") {
                        parameter("limit", 100)
                        parameter("offset", offset)
                        parameter("order_by", "incidentreference")
                    }.bodyAsText()
                    val root = json.parseToJsonElement(raw).jsonObject
                    val page = ukpn.parse(raw)
                    addAll(page)
                    val count = root.getValue("results").jsonArray.size
                    offset += count
                    val total = root.getValue("total_count").jsonPrimitive.int
                    require(count > 0 || offset >= total) { "Incomplete UKPN response" }
                    require(offset < 10000 || offset >= total) { "UKPN pagination limit reached" }
                } while (offset < total)
            }
            "manitoba" -> manitoba.parse(client.get("https://services2.arcgis.com/QoeQkfdOG126FqSi/ArcGIS/rest/services/Manitoba_Hydro_Planned_Power_Outages/FeatureServer/0/query") {
                parameter("where", "1=1")
                parameter("outFields", "*")
                parameter("returnGeometry", false)
                parameter("f", "json")
            }.bodyAsText())
            "tauron" -> {
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
            else -> error("Source does not provide a verified live feed")
        }
        return records.filter { source.id == "ukpn" || source.id == "blackoutmap" || source.id == "massoutage" || source.id == "obsyd" || it.end?.let { end -> end > clock.instant() } != false }
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

    private fun obsydZone(country: Country) = when (country) {
        Country.POLAND -> "PL"
        Country.UNITED_KINGDOM -> "GB"
        Country.IRELAND -> "IE"
        else -> error("No Obsyd bidding zone for $country")
    }

    private companion object { const val TAURON = "https://www.tauron-dystrybucja.pl/waapi" }
}
