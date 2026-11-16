package org.exxlexxlee.data.remote.schedule

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.exxlexxlee.data.catalog.RegionSourceConfig

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
