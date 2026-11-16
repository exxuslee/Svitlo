package org.exxlexxlee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.internationalOutageRepository
import org.exxlexxlee.data.remote.eskom.EskomCalendarApi
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.data.remote.http.FeedFailureReporter
import org.exxlexxlee.data.remote.ssen.SsenApi
import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class PlannedOutageFeedsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-11-16T16:00:00Z"), ZoneOffset.UTC)

    @Test fun ssenNeverUsesJobCreationAsPlannedStartAndKeepsOverdueFaults() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"timestampUtc":"2026-11-16T16:00:00Z","faults":[
              {"reference":"planned","title":"RG27 Area","type":"PSI","affectedAreas":["RG27 8SW"],"loggedAtUtc":"2026-09-28T07:00:00Z","estimatedRestorationTimeUtc":"2026-11-23T10:00:00Z"},
              {"reference":"active","title":"RG21 Area","type":"LV","affectedAreas":["RG21 7PG"],"loggedAtUtc":"2026-11-16T10:00:00Z","estimatedRestorationTimeUtc":"2026-11-16T12:00:00Z"}]}""")
        })
        try {
            val events = internationalOutageRepository(client, json, clock).outages(source("ssen"))
            assertEquals(2, events.size)
            assertNull(events.first { it.id == "planned" }.start)
            assertEquals(Instant.parse("2026-11-16T10:00:00Z"), events.first { it.id == "active" }.start)
            assertTrue(events.all { it.areaOnly })
        } finally { client.close() }
    }

    @Test fun ssenFetchesOneReferenceAndRejectsChangedSchema() = runTest {
        var broken = false
        val client = HttpClient(MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/getbyid/job-1"))
            respond(if (broken) """{"error":"unavailable"}""" else """{"reference":"job-1","type":"HV","affectedAreas":["AB1"],"title":"Aberdeen"}""")
        })
        try {
            val api = SsenApi(ConditionalHttpClient(client), json)
            assertEquals("job-1", api.outage("job-1").id)
            broken = true
            try { api.outage("job-1"); fail() } catch (_: NoSuchElementException) { }
        } finally { client.close() }
    }

    @Test fun northernRetainsAllPostcodesForSameReferenceAndSkipsRestored() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("isaffected=1", request.url.parameters["where"])
            respond("""{"total_count":3,"results":[
              {"reference":"fault","id":1,"isaffected":1,"postcode":["YO15 1AN"],"area":"Yorkshire","loggedtime":"2026-11-16T10:00:00+00:00","estimatedtimetillresolution":"2026-11-16T12:00:00+00:00"},
              {"reference":"fault","id":2,"isaffected":1,"postcode":["YO15 1AP"],"area":"Yorkshire","natureofoutage":"Planned Work on System","loggedtime":"2026-11-16T10:00:00+00:00"},
              {"reference":"restored","id":3,"isaffected":0,"postcode":["WF12 8LW"]}]}""")
        })
        try {
            val events = internationalOutageRepository(client, json, clock).outages(source("northern_powergrid"))
            assertEquals(2, events.size)
            assertEquals(setOf("fault:1", "fault:2"), events.map { it.id }.toSet())
            assertTrue(events.any { "YO15 1AP" in it.location })
            assertNull(events.first { it.id == "fault:2" }.start)
        } finally { client.close() }
    }

    @Test fun odinRequestsCountyScaleAndDoesNotInventIntervals() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/odi/map", request.url.encodedPath)
            assertEquals("COUNTY", request.url.parameters["scale"])
            assertEquals("06037", request.url.parameters["countyCd"])
            respond("""[
              {"id":"06037","outageType":"COUNTY","countyName":"Los Angeles","stateName":"California","metersOut":280,"outages":[{"utilityName":"SOUTHERN CALIFORNIA EDISON"}]},
              {"id":"06001","outageType":"COUNTY","metersOut":0,"outages":[]} ]""")
        })
        try {
            val event = internationalOutageRepository(client, json, clock).outages(source("odin"), OutageAddress(0, 0, "06037")).single()
            assertEquals("odin-06037", event.id)
            assertNull(event.start)
            assertNull(event.end)
            assertTrue(event.areaOnly)
            assertTrue(event.description.contains("280"))
        } finally { client.close() }
    }

    @Test fun schemaFailureIsReportedAndUnavailableSourcesDoNotMakeRequests() = runTest {
        var requests = 0
        val failures = mutableListOf<String>()
        val client = HttpClient(MockEngine { requests++; respond("<html>Unavailable</html>") })
        try {
            val repository = internationalOutageRepository(client, json, clock, reporter = FeedFailureReporter { source, _ -> failures += source })
            try { repository.outages(source("odin")); fail() } catch (_: Exception) { }
            try { repository.outages(source("hydro_quebec")); fail() } catch (_: IllegalArgumentException) { }
            assertEquals(1, requests)
            assertEquals(listOf("odin", "hydro_quebec"), failures)
        } finally { client.close() }
    }

    @Test fun eskomReadsQuotedCsvWithOriginalFinshHeaderButStaysArchived() {
        val client = HttpClient(MockEngine { error("No network expected") })
        try {
            val api = EskomCalendarApi(ConditionalHttpClient(client))
            val events = api.parse("area_name,start,finsh,stage,source\r\n\"cape,town\",2026-11-16T08:00:00+02:00,2026-11-16T10:30:00+02:00,3,\"https://example.com/a,b\"\r\n")
            assertEquals("cape,town", events.single().location)
            assertEquals(Instant.parse("2026-11-16T06:00:00Z"), events.single().start)
            assertTrue(events.single().description.contains("Stage 3"))
            assertEquals(SourceAccess.ARCHIVED, source("eskom_calendar").access)
            assertThrows(IllegalArgumentException::class.java) { api.parse("<html>Unavailable</html>") }
            assertThrows(IllegalArgumentException::class.java) { api.parse("area_name,start,finsh,stage,source\n\"unclosed") }
        } finally { client.close() }
    }

    @Test fun catalogHasSourcesForEveryCountryAndStableUniqueIdentifiers() {
        assertEquals(Country.entries.toSet(), InternationalSources.all.map { it.country }.toSet())
        assertEquals(InternationalSources.all.size, InternationalSources.all.distinctBy { it.country to it.id }.size)
        InternationalSources.all.forEach { ZoneId.of(it.zoneId) }
    }

    private fun source(id: String) = InternationalSources.all.first { it.id == id }
}
