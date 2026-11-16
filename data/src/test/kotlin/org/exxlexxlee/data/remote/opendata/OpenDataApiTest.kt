package org.exxlexxlee.data.remote.opendata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.junit.Assert.*
import org.junit.Test

class OpenDataApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun odsEncodesOdsqlAndPaginatesByRawRows() = runTest {
        val offsets = mutableListOf<String?>()
        val filter = "within_distance(geo_point, geom'POINT(30 50)', 5km)"
        val client = HttpClient(MockEngine { request ->
            assertEquals(filter, request.url.parameters["where"])
            assertEquals("reference", request.url.parameters["select"])
            assertEquals("Apikey test-key", request.headers["Authorization"])
            assertNull(request.url.parameters["apikey"])
            offsets += request.url.parameters["offset"]
            respond(if (offsets.size == 1) """{"total_count":2,"results":[{"reference":"a"}]}"""
                else """{"total_count":2,"results":[{"reference":"b"}]}""")
        })
        try {
            val rows = OpendatasoftApi(ConditionalHttpClient(client), json).records(
                "https://example.com", "faults", select = "reference", where = filter, orderBy = "reference", apiKey = "test-key",
            )
            assertEquals(listOf("0", "1"), offsets)
            assertEquals(listOf("a", "b"), rows.map { it.getValue("reference").jsonPrimitive.content })
        } finally { client.close() }
    }

    @Test fun ckanEncodesFiltersAndResolvesResources() = runTest {
        val offsets = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("package_show")) {
                assertEquals("live-power-cuts", request.url.parameters["id"])
                respond("""{"success":true,"result":{"resources":[{"id":"current-resource","format":"CSV","url":"https://example.com/current.csv"}]}}""")
            } else {
                assertEquals("current-resource", request.url.parameters["resource_id"])
                assertEquals("ENEL SP", json.parseToJsonElement(checkNotNull(request.url.parameters["filters"])).jsonObject.getValue("SigAgente").jsonPrimitive.content)
                assertEquals("_id desc", request.url.parameters["sort"])
                offsets += request.url.parameters["offset"]
                respond("""{"success":true,"result":{"total":2,"records":[{"_id":${offsets.size}}]}}""")
            }
        })
        try {
            val api = CkanApi(ConditionalHttpClient(client), json)
            val resource = api.resources("https://example.com", "live-power-cuts").single().getValue("id").jsonPrimitive.content
            val rows = api.records("https://example.com", resource, mapOf("SigAgente" to "ENEL SP"), sort = "_id desc")
            assertEquals(2, rows.size)
            assertEquals(listOf("0", "1"), offsets)
        } finally { client.close() }
    }

    @Test fun incompletePagesAndCkanFailureAreErrors() = runTest {
        val client = HttpClient(MockEngine { respond("""{"total_count":1,"results":[],"success":false,"error":{"message":"Resource missing"}}""") })
        try {
            try { OpendatasoftApi(ConditionalHttpClient(client), json).records("https://example.com", "faults", orderBy = "id"); fail() }
            catch (_: IllegalArgumentException) { }
            try { CkanApi(ConditionalHttpClient(client), json).records("https://example.com", "missing"); fail() }
            catch (_: IllegalArgumentException) { }
        } finally { client.close() }
    }
}
