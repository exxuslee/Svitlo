package org.exxlexxlee.data.remote

import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule

class ScheduleRemoteDataSource(
    private val yasnoApi: YasnoApi,
    private val githubJsonApi: GithubJsonApi,
    private val yasnoMapper: YasnoMapper,
    private val gpvMapper: GpvMapper,
) {
    suspend fun fetch(source: RegionSourceConfig): SchedulePayload = when (source.type) {
        "yasno" -> {
            val region = source.regionId ?: error("Yasno regionId missing")
            val dso = source.dsoId ?: error("Yasno dsoId missing")
            val payload = yasnoMapper.decode(yasnoApi.fetchPlannedOutages(region, dso))
            SchedulePayload(yasnoMapper.groups(payload)) { group, clock ->
                yasnoMapper.toSchedule(payload, group, clock)
            }
        }

        "github_json" -> {
            val url = source.url ?: error("GitHub JSON url missing")
            val payload = gpvMapper.decode(githubJsonApi.fetch(url))
            SchedulePayload(gpvMapper.groups(payload)) { group, clock ->
                gpvMapper.toSchedule(payload, group, clock)
            }
        }

        else -> error("Unknown source type: ${source.type}")
    }
}

class SchedulePayload internal constructor(
    val groups: List<OutageGroup>,
    private val mapSchedule: (OutageGroup, Clock) -> OutageSchedule,
) {
    fun toSchedule(group: OutageGroup, clock: Clock): OutageSchedule = mapSchedule(group, clock)
}
