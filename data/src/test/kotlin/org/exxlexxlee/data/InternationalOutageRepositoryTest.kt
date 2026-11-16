package org.exxlexxlee.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.InternationalOutageMapper
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
        val mapper = InternationalOutageMapper(json)
        assertThrows(IllegalArgumentException::class.java) { mapper.manitoba("""{"error":{"code":400}}""") }
        assertThrows(IllegalArgumentException::class.java) { mapper.manitoba("""{"features":[],"exceededTransferLimit":true}""") }
        assertThrows(Exception::class.java) { mapper.ukpn("<html>Unavailable</html>") }
    }

    @Test fun tauronPreservesUtcAndAreaScopeAndSkipsInactiveFaults() {
        val events = InternationalOutageMapper(json).tauron("""{"OutageListType":2,"OutageItems":[{"OutageId":"a","TypeId":1,"StartDate":"2026-11-19T07:00:00Z","EndDate":"2026-11-19T13:00:00Z","Message":"Kraków"},{"OutageId":"inactive","TypeId":2,"IsActive":false}]}""")
        assertTrue(events.single().areaOnly)
        assertEquals(Instant.parse("2026-11-19T07:00:00Z"), events.single().start)
    }

    @Test fun countryCatalogDoesNotMixProviders() {
        assertTrue(InternationalSources.all.filter { it.country == Country.POLAND }.all { it.id in listOf("tauron", "enea", "pge") })
        assertTrue(InternationalSources.all.none { it.country == Country.UKRAINE })
    }
}
