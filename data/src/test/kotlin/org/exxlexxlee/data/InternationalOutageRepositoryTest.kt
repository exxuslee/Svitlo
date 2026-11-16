package org.exxlexxlee.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.blackoutmap.BlackoutMapMapper
import org.exxlexxlee.data.remote.manitoba.ManitobaMapper
import org.exxlexxlee.data.remote.massoutage.MassOutageMapper
import org.exxlexxlee.data.remote.obsyd.ObsydMapper
import org.exxlexxlee.data.remote.tauron.TauronMapper
import org.exxlexxlee.data.remote.ukpn.UkpnMapper
import org.exxlexxlee.data.repository.InternationalOutageRepositoryImpl
import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InternationalOutageRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)

    @Test fun ukpnPaginatesUsingRawRecordCountAndOmitsRestoredEvents() = runTest {
        val offsets = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            val offset = request.url.parameters["offset"]
            offsets += offset
            respond(if (offset == "0") """{"total_count":2,"results":[{"incidentreference":"restored","restoreddatetime":"2026-11-15T11:00:00"}]}"""
            else """{"total_count":2,"results":[{"incidentreference":"active","operatingzone":"London","estimatedrestorationdate":"2026-11-15T15:00:00"}]}""")
        })
        try {
            val repository = InternationalOutageRepositoryImpl(client, json, clock)
            val events = repository.outages(InternationalSources.all.first { it.id == "ukpn" })
            assertEquals(listOf("0", "1"), offsets)
            assertEquals(listOf("active"), events.map { it.id })
            assertEquals(Instant.parse("2026-11-15T15:00:00Z"), events.single().end)
        } finally { client.close() }
    }

    @Test fun manitobaUsesEpochMillisecondsAndDoesNotKeepExpiredEvents() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"features":[{"attributes":{"ORDER_ID":"old","SCHEDULED_END_TIME":1}},{"attributes":{"ORDER_ID":"future","CSC":"WINNIPEG","DEVICE_ADDRESS":"Street","SCHEDULED_START_TIME":1794747600000,"SCHEDULED_END_TIME":1794920400000}}]}""")
        })
        try {
            val events = InternationalOutageRepositoryImpl(client, json, clock).outages(InternationalSources.all.first { it.id == "manitoba" })
            assertEquals("future", events.single().id)
            assertEquals(Instant.ofEpochMilli(1794920400000), events.single().end)
        } finally { client.close() }
    }

    @Test fun incompleteOrHtmlPayloadsAreFailuresInsteadOfEmptySchedules() {
        val mapper = ManitobaMapper(json)
        assertThrows(IllegalArgumentException::class.java) { mapper.parse("""{"error":{"code":400}}""") }
        assertThrows(IllegalArgumentException::class.java) { mapper.parse("""{"features":[],"exceededTransferLimit":true}""") }
        assertThrows(Exception::class.java) { UkpnMapper(json).parse("<html>Unavailable</html>") }
    }

    @Test fun tauronPreservesUtcAndAreaScopeAndSkipsInactiveFaults() {
        val events = TauronMapper(json).parse("""{"OutageListType":2,"OutageItems":[{"OutageId":"a","TypeId":1,"StartDate":"2026-11-19T07:00:00Z","EndDate":"2026-11-19T13:00:00Z","Message":"Kraków"},{"OutageId":"inactive","TypeId":2,"IsActive":false}]}""")
        assertTrue(events.single().areaOnly)
        assertEquals(Instant.parse("2026-11-19T07:00:00Z"), events.single().start)
    }

    @Test fun blackoutmapMapsScheduledPowerGroupsForUkraine() {
        val raw = """
            {"power":[
              {"id":"s:UA:12","cc":"UA","scope":"scheduled","regions":[["Kyiv City",50.45,30.53]],"start":"2026-11-16T07:00:00+00:00","until":"2026-11-16T16:30:00+00:00","off":40,"groups":60,"src":"YASNO (DTEK Kyiv)","rs":"kyiv-city"},
              {"id":"s:PL:1","cc":"PL","scope":"scheduled","regions":[["Warsaw",52.2,21.0]],"start":"2026-11-16T08:00:00+00:00","until":"2026-11-16T12:00:00+00:00","src":"Other"}
            ]}
        """.trimIndent()
        val events = BlackoutMapMapper(json).parse(raw, "UA")
        assertEquals(1, events.size)
        assertEquals("s:UA:12", events.single().id)
        assertEquals("Kyiv City", events.single().location)
        assertTrue(events.single().description.contains("YASNO"))
        assertEquals(Instant.parse("2026-11-16T07:00:00Z"), events.single().start)
        assertEquals(Instant.parse("2026-11-16T16:30:00Z"), events.single().end)
        assertTrue(events.single().areaOnly)
    }

    @Test fun massoutageSummaryKeepsOnlyStatesWithOutages() {
        val raw = """
            {"data":{"national":{"customers_out":100,"as_of":"2026-11-16T12:00:00.000Z"},
              "states":[
                {"state_fips":"06","name":"California","abbr":"CA","customers_out":90,"county_count":5},
                {"state_fips":"48","name":"Texas","abbr":"TX","customers_out":0,"county_count":0}
              ]}}
        """.trimIndent()
        val events = MassOutageMapper(json).summary(raw)
        assertEquals(1, events.size)
        assertEquals("us-state-06", events.single().id)
        assertTrue(events.single().location.contains("California"))
        assertTrue(events.single().description.startsWith("90"))
    }

    @Test fun obsydKeepsLatestForcedMegawattsAndRejectsEmptySeries() {
        val mapper = ObsydMapper(json)
        val events = mapper.parse(
            """{"available":true,"series":"outage.forced","zone":"PL","data":[
              {"datetime_utc":"2026-11-16T10:00:00Z","value":100.0},
              {"datetime_utc":"2026-11-16T11:00:00Z","value":2400.4}
            ]}""",
            "PL",
        )
        assertEquals("obsyd-PL-2026-11-16T11:00:00Z", events.single().id)
        assertEquals(Instant.parse("2026-11-16T11:00:00Z"), events.single().start)
        assertTrue(events.single().description.contains("2400"))
        assertTrue(events.single().areaOnly)
        assertTrue(mapper.parse("""{"available":true,"data":[{"datetime_utc":"2026-11-16T11:00:00Z","value":0}]}""", "IE").isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            mapper.parse("""{"available":false,"reason":"unknown zone"}""", "XX")
        }
    }

    @Test fun countryCatalogDoesNotMixProviders() {
        assertTrue(InternationalSources.all.filter { it.country == Country.POLAND }.all { it.id in listOf("tauron", "enea", "pge", "obsyd") })
        assertTrue(InternationalSources.all.filter { it.country == Country.UKRAINE }.all { it.id == "blackoutmap" })
        assertTrue(InternationalSources.all.any { it.id == "massoutage" && it.country == Country.UNITED_STATES })
        assertEquals(
            setOf(Country.POLAND, Country.UNITED_KINGDOM, Country.IRELAND),
            InternationalSources.all.filter { it.id == "obsyd" }.map { it.country }.toSet(),
        )
    }
}
