package org.exxlexxlee.data.remote.schedule

import org.exxlexxlee.data.catalog.RegionSourceConfig

internal interface ScheduleSourceFetcher {
    val sourceType: String
    suspend fun fetch(source: RegionSourceConfig): SchedulePayload
}
