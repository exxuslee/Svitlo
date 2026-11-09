package org.exxlexxlee.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.exxlexxlee.data.catalog.AreaConfig
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.catalog.RegionsFile
import org.exxlexxlee.data.catalog.SupplierConfig
import org.exxlexxlee.data.local.ScheduleCacheEntity
import org.exxlexxlee.data.local.ScheduleDao
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.GithubJsonApi
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.data.remote.YasnoApi
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScheduleRepositoriesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<String>()
    private val clients = mutableListOf<HttpClient>()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-09T08:00:00Z")
    }

    @After
    fun closeClients() {
        clients.forEach { it.close() }
    }

    @Test
    fun yasnoGroupsAndScheduleUseSameSourceAndScheduleIsCached() = runTest {
        val fixture = fixture(
            RegionSourceConfig("yasno", regionId = 25, dsoId = 902),
            "yasno-kyiv-sample.json",
            "3.1",
        )
        assertTrue(fixture.locations.groups("region").any { it.id == "3.1" })
        assertTrue(fixture.schedules.refresh(fixture.selection).isSuccess)
        val schedule = fixture.schedules.observe(fixture.selection).first()
        assertNotNull(schedule)
        assertEquals(48, schedule!!.today.halfHours.size)
        assertEquals(clock.now(), schedule.fetchedAt)
        assertEquals(listOf("region|3.1"), fixture.dao.observedKeys)
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.endsWith("/regions/25/dsos/902/planned-outages") })
    }

    @Test
    fun githubGroupsAndScheduleUseConfiguredUrl() = runTest {
        val fixture = fixture(
            RegionSourceConfig("github_json", url = "https://example.test/outages.json"),
            "gpv-kyiv-region-sample.json",
            "GPV3.1",
        )
        assertTrue(fixture.locations.groups("region").any { it.id == "GPV3.1" })
        assertTrue(fixture.schedules.refresh(fixture.selection).isSuccess)
        val schedule = fixture.schedules.observe(fixture.selection).first()!!
        assertEquals("Черга 3.1", schedule.group.name)
        assertEquals(clock.now(), schedule.fetchedAt)
        assertEquals(List(2) { "https://example.test/outages.json" }, requests)
    }

    @Test
    fun failedRefreshPreservesPreviouslyCachedSchedule() = runTest {
        val fixture = fixture(
            RegionSourceConfig("yasno", regionId = 25, dsoId = 902),
            "yasno-kyiv-sample.json",
            "3.1",
        )
        assertTrue(fixture.schedules.refresh(fixture.selection).isSuccess)
        val cached = fixture.dao.entity.value
        fixture.response.status = HttpStatusCode.ServiceUnavailable
        assertTrue(fixture.schedules.refresh(fixture.selection).isFailure)
        assertSame(cached, fixture.dao.entity.value)

        fixture.response.status = HttpStatusCode.OK
        fixture.response.body = "invalid json"
        assertTrue(fixture.schedules.refresh(fixture.selection).isFailure)
        assertSame(cached, fixture.dao.entity.value)
    }

    @Test
    fun cancellationIsPropagatedInsteadOfReturnedAsFailure() = runTest {
        val fixture = fixture(
            RegionSourceConfig("yasno", regionId = 25, dsoId = 902),
            "yasno-kyiv-sample.json",
            "3.1",
        )
        val cancellation = CancellationException("Cancelled write")
        fixture.dao.writeFailure = cancellation
        try {
            fixture.schedules.refresh(fixture.selection)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertEquals(null, fixture.dao.entity.value)
    }

    @Test
    fun invalidSourceIsRejectedBeforeAnyRequest() = runTest {
        val sources = listOf(
            RegionSourceConfig("yasno"),
            RegionSourceConfig("yasno", regionId = 25),
            RegionSourceConfig("github_json"),
            RegionSourceConfig("unknown"),
        )
        for (source in sources) {
            val fixture = fixture(source, "yasno-kyiv-sample.json", "3.1")
            assertTrue(fixture.schedules.refresh(fixture.selection).isFailure)
            assertEquals(null, fixture.dao.entity.value)
        }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun batchDownloadsRegionOnceAndWritesEachGroupOnceForMultipleAddresses() = runTest {
        val fixture = fixture(
            RegionSourceConfig("yasno", regionId = 25, dsoId = 902),
            "yasno-kyiv-sample.json", "3.1",
        )
        val groups = json.parseToJsonElement(fixture.response.body).jsonObject
        fixture.response.body = JsonObject(groups + ("3.2" to groups.getValue("3.1"))).toString()
        val home = fixture.selection.copy(details = AddressDetails("Дім"))
        val office = fixture.selection.copy(details = AddressDetails("Офіс"))
        val otherGroup = fixture.selection.copy(group = OutageGroup("3.2", "3.2"))

        val results = fixture.schedules.refreshAll(listOf(home, office, otherGroup, home))

        assertEquals(1, requests.size)
        assertEquals(listOf("region|3.1", "region|3.2"), fixture.dao.writtenKeys)
        assertEquals(setOf(home, office, otherGroup), results.keys)
        assertTrue(results.values.all { it.isSuccess })
        // A separate manual refresh must still go to the network.
        fixture.schedules.refresh(home)
        assertEquals(2, requests.size)
    }

    @Test
    fun batchPreservesSuccessfulGroupsWhenAnotherGroupOrRegionFails() = runTest {
        val fixture = fixture(
            RegionSourceConfig("yasno", regionId = 25, dsoId = 902),
            "yasno-kyiv-sample.json", "3.1",
        )
        val missingGroup = fixture.selection.copy(group = OutageGroup("missing", "missing"))
        val missingRegion = fixture.selection.copy(region = Region("missing", "missing", "missing"))

        val results =
            fixture.schedules.refreshAll(listOf(missingRegion, missingGroup, fixture.selection))

        assertTrue(results.getValue(missingRegion).isFailure)
        assertTrue(results.getValue(missingGroup).isFailure)
        assertTrue(results.getValue(fixture.selection).isSuccess)
        assertEquals(1, requests.size)
        assertEquals(listOf("region|3.1"), fixture.dao.writtenKeys)
    }

    private fun fixture(source: RegionSourceConfig, resource: String, groupId: String): Fixture {
        val body = checkNotNull(javaClass.classLoader?.getResourceAsStream(resource))
            .bufferedReader().use { it.readText() }
        val response = RemoteResponse(body)
        val client = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            respond(response.body, response.status)
        }) {
            expectSuccess = true
        }.also { clients += it }
        val remote = ScheduleRemoteDataSource(
            YasnoApi(client), GithubJsonApi(client), YasnoMapper(json), GpvMapper(json),
        )
        val catalog = RegionCatalog(
            RegionsFile(
                listOf(
                    AreaConfig(
                        id = "area",
                        name = "Область",
                        suppliers = listOf(
                            SupplierConfig(
                                "region",
                                "Київ",
                                "provider",
                                source = source
                            )
                        ),
                    )
                )
            )
        )
        val dao = FakeScheduleDao()
        return Fixture(
            locations = LocationRepositoryImpl(catalog, remote),
            schedules = ScheduleRepositoryImpl(catalog, remote, dao, clock),
            selection = Selection.ByGroup(
                Region("region", "Київ", "provider"),
                OutageGroup(groupId, groupId)
            ),
            dao = dao,
            response = response,
        )
    }

    private data class Fixture(
        val locations: LocationRepositoryImpl,
        val schedules: ScheduleRepositoryImpl,
        val selection: Selection.ByGroup,
        val dao: FakeScheduleDao,
        val response: RemoteResponse,
    )

    private data class RemoteResponse(
        var body: String,
        var status: HttpStatusCode = HttpStatusCode.OK,
    )

    private class FakeScheduleDao : ScheduleDao {
        val entity = MutableStateFlow<ScheduleCacheEntity?>(null)
        val observedKeys = mutableListOf<String>()
        val writtenKeys = mutableListOf<String>()
        var writeFailure: Exception? = null

        override fun observe(cacheKey: String): Flow<ScheduleCacheEntity?> {
            observedKeys += cacheKey
            return entity
        }

        override suspend fun upsert(entity: ScheduleCacheEntity) {
            writeFailure?.let { throw it }
            writtenKeys += entity.cacheKey
            this.entity.value = entity
        }
    }
}
