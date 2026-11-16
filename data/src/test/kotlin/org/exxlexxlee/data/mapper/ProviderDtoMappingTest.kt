package org.exxlexxlee.data.mapper

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.mapper.blackoutmap.BlackoutMapMapper
import org.exxlexxlee.data.mapper.eskom.EskomMapper
import org.exxlexxlee.data.mapper.manitoba.ManitobaMapper
import org.exxlexxlee.data.mapper.massoutage.MassOutageMapper
import org.exxlexxlee.data.mapper.odin.OdinMapper
import org.exxlexxlee.data.mapper.tauron.TauronMapper
import org.exxlexxlee.data.mapper.ukpn.UkpnMapper
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.data.remote.ssen.SsenApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDtoMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun excludedRecordsDoNotRequireActiveRecordFields() {
        val blackout = BlackoutMapMapper(json).parse(
            """{"power":[{"cc":"PL","regions":{}},{"cc":"UA","id":"active"}]}""", "UA",
        )
        assertEquals(listOf("active"), blackout.map { it.id })
        val ukpn = UkpnMapper(json).parse(
            """{"results":[{"restoreddatetime":"2026-11-16","incidentreference":{}},{"incidentreference":"active"}]}""",
        )
        assertEquals(listOf("active"), ukpn.map { it.id })
        val tauron = TauronMapper(json).parse(
            """{"OutageItems":[{"TypeId":2,"IsActive":false,"Message":{}},{"TypeId":1,"OutageId":"active"}]}""",
        )
        assertEquals(listOf("active"), tauron.map { it.id })
        assertTrue(MassOutageMapper(json).summary(
            """{"data":{"states":[{"customers_out":0,"name":{}}]}}""",
        ).isEmpty())
    }

    @Test
    fun optionalInvalidScalarsKeepTheirPreviousFallbacks() {
        val event = ManitobaMapper(json).parse(
            """{"features":[{"attributes":{"ORDER_ID":123,"CSC":null,"SCHEDULED_START_TIME":"bad","SCHEDULED_END_TIME":null}}]}""",
        ).single()
        assertEquals("123", event.id)
        assertNull(event.start)
        assertNull(event.end)
        val incident = MassOutageMapper(json).incidents(
            """{"data":[{"utility_key":"test","customers_out":"unknown","cause":null,"started_at":null,"etr":null}]}""",
        ).single()
        assertEquals("test", incident.location)
        assertEquals("", incident.description)
        assertNull(incident.start)
        assertNull(incident.end)
    }

    @Test
    fun tauronAddressesRetainCityDeduplicationAndRequiredStreetLabels() {
        val mapper = TauronMapper(json)
        val city = mapper.options(
            """[{"GAID":42,"Name":"City","CommuneName":"City","ProvinceName":null}]""", true,
        ).single()
        assertEquals(42L, city.id)
        assertEquals("City", city.name)
        assertThrows(NoSuchElementException::class.java) {
            mapper.options("""[{"GAID":42}]""", false)
        }
    }

    @Test
    fun odinRetainsCountyValidationAndZeroMeterRecordsWithoutUtilities() {
        val mapper = OdinMapper(json)
        val summary = mapper.parse("""[{"id":"06037","outageType":"COUNTY","metersOut":0}]""").single()
        assertEquals(0, summary.affectedCustomers)
        assertTrue(summary.isAreaSummary)
        for (raw in listOf(
            """[{"id":"06037","outageType":"STATE","metersOut":0}]""",
            """[{"id":"wrong","outageType":"COUNTY","metersOut":0}]""",
            """[{"id":"06037","outageType":"COUNTY","metersOut":-1,"outages":[]}]""",
        )) {
            assertThrows(IllegalArgumentException::class.java) { mapper.parse(raw) }
        }
    }

    @Test
    fun eskomRejectsInvalidStagesAndReversedIntervals() {
        for (record in listOf(
            "area,2026-11-16T08:00:00Z,2026-11-16T10:00:00Z,9,source",
            "area,2026-11-16T10:00:00Z,2026-11-16T08:00:00Z,3,source",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                EskomMapper.parse("area_name,start,finsh,stage,source\n$record\n")
            }
        }
    }

    @Test
    fun ssenDtoSchemaFailurePreservesTheLastValidatedCachedResponse() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { request ->
            when (++requests) {
                1 -> respond(
                    """{"reference":"job","type":"PSI","affectedAreas":["AB1"],"loggedAtUtc":"2026-09-28T07:00:00Z"}""",
                    headers = headersOf(HttpHeaders.ETag, "v1"),
                )
                2 -> respond("""{"error":"schema changed"}""", headers = headersOf(HttpHeaders.ETag, "v2"))
                else -> {
                    assertEquals("v1", request.headers[HttpHeaders.IfNoneMatch])
                    respond("", HttpStatusCode.NotModified)
                }
            }
        })
        try {
            val api = SsenApi(ConditionalHttpClient(client), json)
            val first = api.outage("job")
            try {
                api.outage("job")
                throw AssertionError("Schema failure expected")
            } catch (_: NoSuchElementException) { }
            assertEquals(first, api.outage("job"))
            assertNull(first.start)
        } finally {
            client.close()
        }
    }
}
