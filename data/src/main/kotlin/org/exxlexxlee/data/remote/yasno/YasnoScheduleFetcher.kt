package org.exxlexxlee.data.remote.yasno

import org.exxlexxlee.data.catalog.RegionSourceConfig
import org.exxlexxlee.data.mapper.yasno.YasnoMapper
import org.exxlexxlee.data.remote.schedule.SOURCE_TYPE_YASNO
import org.exxlexxlee.data.remote.schedule.SchedulePayload
import org.exxlexxlee.data.remote.schedule.ScheduleSourceFetcher

internal class YasnoScheduleFetcher(
    private val api: YasnoApi,
    private val mapper: YasnoMapper,
) : ScheduleSourceFetcher {
    override val sourceType: String = SOURCE_TYPE_YASNO

    override suspend fun fetch(source: RegionSourceConfig): SchedulePayload {
        val regionId = checkNotNull(source.regionId) { "Yasno regionId missing" }
        val dsoId = checkNotNull(source.dsoId) { "Yasno dsoId missing" }
        val payload =
            api.fetchPlannedOutages(regionId = regionId, dsoId = dsoId, decode = mapper::decode)
        return SchedulePayload(groups = mapper.groups(payload)) { group, clock ->
            mapper.toSchedule(payload, group, clock)
        }
    }
}
