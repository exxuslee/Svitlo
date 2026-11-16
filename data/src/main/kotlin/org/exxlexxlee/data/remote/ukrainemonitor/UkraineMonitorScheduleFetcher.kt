package org.exxlexxlee.data.remote.ukrainemonitor

import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.ukrainemonitor.UkraineMonitorMapper
import org.exxlexxlee.data.remote.github.GithubJsonApi
import org.exxlexxlee.data.remote.schedule.SOURCE_TYPE_UKRAINE_MONITOR
import org.exxlexxlee.data.remote.schedule.SchedulePayload
import org.exxlexxlee.data.remote.schedule.ScheduleSourceFetcher

internal class UkraineMonitorScheduleFetcher(
    private val api: GithubJsonApi,
    private val mapper: UkraineMonitorMapper,
) : ScheduleSourceFetcher {
    override val sourceType = SOURCE_TYPE_UKRAINE_MONITOR

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
        val region = checkNotNull(source.aggregateRegion) { "Monitor region missing" }
        val url = source.url
            ?: "https://raw.githubusercontent.com/chaichuk/UA-power-outages-monitor/main/artifacts/last-message.json"
        val payload = api.fetch(url) { mapper.decode(it, region) }
        return SchedulePayload(mapper.groups(payload)) { group, clock ->
            mapper.toSchedule(
                payload,
                group,
                clock
            )
        }
    }
}
