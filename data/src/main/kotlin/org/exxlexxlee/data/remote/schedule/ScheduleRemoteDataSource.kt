package org.exxlexxlee.data.remote.schedule

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.gpv.GpvMapper
import org.exxlexxlee.data.mapper.ukrainemonitor.UkraineMonitorMapper
import org.exxlexxlee.data.mapper.yasno.YasnoMapper
import org.exxlexxlee.data.remote.github.GithubJsonApi
import org.exxlexxlee.data.remote.github.GithubJsonScheduleFetcher
import org.exxlexxlee.data.remote.ukrainemonitor.UkraineMonitorScheduleFetcher
import org.exxlexxlee.data.remote.yasno.YasnoApi
import org.exxlexxlee.data.remote.yasno.YasnoScheduleFetcher

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
                UkraineMonitorScheduleFetcher(
                    githubJsonApi,
                    UkraineMonitorMapper(Json { ignoreUnknownKeys = true })
                ),
            ),
            dispatcher = dispatcher,
        )
    }
}
