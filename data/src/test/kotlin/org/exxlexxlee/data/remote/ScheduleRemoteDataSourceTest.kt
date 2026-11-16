package org.exxlexxlee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.github.GithubJsonApi
import org.exxlexxlee.data.remote.yasno.YasnoApi
import org.exxlexxlee.domain.model.OutageGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRemoteDataSourceTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-08T10:00:00Z")
    }

    @Test
    fun fetchYasnoSuccess() = runTest {
        val sample = readResource("yasno-kyiv-sample.json")
        val client = HttpClient(MockEngine {
            respond(sample, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val remote = ScheduleRemoteDataSource(
            yasnoApi = YasnoApi(client),
            githubJsonApi = GithubJsonApi(client),
            yasnoMapper = YasnoMapper(json),
            gpvMapper = GpvMapper(json),
        )

        val payload = remote.fetch(RegionSourceConfig(SOURCE_TYPE_YASNO, regionId = 25, dsoId = 902))
        assertTrue(payload.groups.isNotEmpty())
        val schedule = payload.toSchedule(payload.groups.first(), clock)
        assertNotNull(schedule.today)
    }

    @Test
    fun fetchGithubJsonSuccess() = runTest {
        val sample = readResource("gpv-kyiv-region-sample.json")
        val client = HttpClient(MockEngine {
            respond(sample, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val remote = ScheduleRemoteDataSource(
            yasnoApi = YasnoApi(client),
            githubJsonApi = GithubJsonApi(client),
            yasnoMapper = YasnoMapper(json),
            gpvMapper = GpvMapper(json),
        )

        val payload = remote.fetch(RegionSourceConfig(SOURCE_TYPE_GITHUB_JSON, url = "https://example.com/data.json"))
        assertTrue(payload.groups.isNotEmpty())
        val schedule = payload.toSchedule(payload.groups.first(), clock)
        assertNotNull(schedule.today)
    }

    @Test
    fun fetchThrowsOnMissingParameters() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val remote = ScheduleRemoteDataSource(
            yasnoApi = YasnoApi(client),
            githubJsonApi = GithubJsonApi(client),
            yasnoMapper = YasnoMapper(json),
            gpvMapper = GpvMapper(json),
        )

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { remote.fetch(RegionSourceConfig(SOURCE_TYPE_YASNO)) }
        }
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { remote.fetch(RegionSourceConfig(SOURCE_TYPE_YASNO, regionId = 25)) }
        }
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { remote.fetch(RegionSourceConfig(SOURCE_TYPE_GITHUB_JSON)) }
        }
    }

    @Test
    fun fetchThrowsOnUnknownSourceType() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val remote = ScheduleRemoteDataSource(
            yasnoApi = YasnoApi(client),
            githubJsonApi = GithubJsonApi(client),
            yasnoMapper = YasnoMapper(json),
            gpvMapper = GpvMapper(json),
        )

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { remote.fetch(RegionSourceConfig("unknown_type")) }
        }
        assertTrue(error.message?.contains("Unknown source type: unknown_type") == true)
    }

    @Test
    fun extensibleWithCustomFetcher() = runTest {
        val customFetcher = object : ScheduleSourceFetcher {
            override val sourceType: String = "custom"
            override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
                val group = OutageGroup("custom-1", "Custom Group")
                return SchedulePayload(listOf(group)) { _, _ ->
                    error("Unused in test")
                }
            }
        }
        val remote = ScheduleRemoteDataSourceImpl(listOf(customFetcher))
        val payload = remote.fetch(RegionSourceConfig("custom"))
        assertEquals(1, payload.groups.size)
        assertEquals("custom-1", payload.groups.first().id)
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
