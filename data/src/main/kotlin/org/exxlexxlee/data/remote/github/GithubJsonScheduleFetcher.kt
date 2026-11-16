package org.exxlexxlee.data.remote.github

import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.gpv.GpvMapper
import org.exxlexxlee.data.remote.schedule.SOURCE_TYPE_GITHUB_JSON
import org.exxlexxlee.data.remote.schedule.SchedulePayload
import org.exxlexxlee.data.remote.schedule.ScheduleSourceFetcher

internal class GithubJsonScheduleFetcher(
    private val api: GithubJsonApi,
    private val mapper: GpvMapper,
) : ScheduleSourceFetcher {
    override val sourceType: String = SOURCE_TYPE_GITHUB_JSON

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
        val url = checkNotNull(source.url) { "GitHub JSON url missing" }
        val payload = api.fetch(url, mapper::decode)
        return SchedulePayload(groups = mapper.groups(payload)) { group, clock ->
            mapper.toSchedule(payload, group, clock)
        }
    }
}
