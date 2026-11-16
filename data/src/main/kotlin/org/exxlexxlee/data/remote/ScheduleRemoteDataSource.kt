package org.exxlexxlee.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.github.GithubJsonApi
import org.exxlexxlee.data.remote.yasno.YasnoApi
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule

const val SOURCE_TYPE_YASNO = "yasno"
const val SOURCE_TYPE_GITHUB_JSON = "github_json"

/**
 * Remote data source responsible for fetching schedule payloads from configured regional sources.
 */
fun interface ScheduleRemoteDataSource {

    suspend fun fetch(source: RegionSourceConfig): SchedulePayload

    companion object {
        operator fun invoke(
            yasnoApi: YasnoApi,
            githubJsonApi: GithubJsonApi,
            yasnoMapper: YasnoMapper,
            gpvMapper: GpvMapper,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): ScheduleRemoteDataSource = ScheduleRemoteDataSourceImpl(
            fetchers = listOf(
                YasnoScheduleFetcher(yasnoApi, yasnoMapper),
                GithubJsonScheduleFetcher(githubJsonApi, gpvMapper),
            ),
            dispatcher = dispatcher,
        )
    }
}

internal class ScheduleRemoteDataSourceImpl(
    fetchers: List<ScheduleSourceFetcher>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScheduleRemoteDataSource {

    private val fetchersByType: Map<String, ScheduleSourceFetcher> =
        fetchers.associateBy { it.sourceType }

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload =
        withContext(dispatcher) {
            val fetcher = fetchersByType[source.type]
                ?: error("Unknown source type: ${source.type}")
            fetcher.fetch(source)
        }
}

internal interface ScheduleSourceFetcher {
    val sourceType: String
    suspend fun fetch(source: RegionSourceConfig): SchedulePayload
}

internal class YasnoScheduleFetcher(
    private val api: YasnoApi,
    private val mapper: YasnoMapper,
) : ScheduleSourceFetcher {
    override val sourceType: String = SOURCE_TYPE_YASNO

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
        val regionId = checkNotNull(source.regionId) { "Yasno regionId missing" }
        val dsoId = checkNotNull(source.dsoId) { "Yasno dsoId missing" }
        val raw = api.fetchPlannedOutages(regionId = regionId, dsoId = dsoId)
        val payload = mapper.decode(raw)
        return SchedulePayload(groups = mapper.groups(payload)) { group, clock ->
            mapper.toSchedule(payload, group, clock)
        }
    }
}

internal class GithubJsonScheduleFetcher(
    private val api: GithubJsonApi,
    private val mapper: GpvMapper,
) : ScheduleSourceFetcher {
    override val sourceType: String = SOURCE_TYPE_GITHUB_JSON

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
        val url = checkNotNull(source.url) { "GitHub JSON url missing" }
        val raw = api.fetch(url)
        val payload = mapper.decode(raw)
        return SchedulePayload(groups = mapper.groups(payload)) { group, clock ->
            mapper.toSchedule(payload, group, clock)
        }
    }
}

class SchedulePayload(
    val groups: List<OutageGroup>,
    private val mapSchedule: (OutageGroup, Clock) -> OutageSchedule,
) {
    fun toSchedule(group: OutageGroup, clock: Clock): OutageSchedule = mapSchedule(group, clock)
}

