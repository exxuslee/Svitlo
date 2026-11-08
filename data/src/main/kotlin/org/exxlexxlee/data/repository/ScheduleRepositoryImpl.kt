package org.exxlexxlee.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.local.ScheduleDao
import org.exxlexxlee.data.local.cacheKey
import org.exxlexxlee.data.local.toDomain
import org.exxlexxlee.data.local.toEntity
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.GithubJsonApi
import org.exxlexxlee.data.remote.YasnoApi
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository

class ScheduleRepositoryImpl(
    private val catalog: RegionCatalog,
    private val yasnoApi: YasnoApi,
    private val githubJsonApi: GithubJsonApi,
    private val yasnoMapper: YasnoMapper,
    private val gpvMapper: GpvMapper,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock = Clock.System,
) : ScheduleRepository {

    override fun observe(selection: Selection): Flow<OutageSchedule?> =
        scheduleDao.observe(cacheKey(selection)).map { entity -> entity?.toDomain() }

    override suspend fun refresh(selection: Selection): Result<Unit> = runCatching {
        val source = catalog.source(selection.region.id)
        val schedule = when (source.type) {
            "yasno" -> {
                val region = source.regionId ?: error("Yasno regionId missing")
                val dso = source.dsoId ?: error("Yasno dsoId missing")
                val payload = yasnoMapper.decode(yasnoApi.fetchPlannedOutages(region, dso))
                yasnoMapper.toSchedule(payload, selection.group, clock)
            }
            "github_json" -> {
                val url = source.url ?: error("GitHub JSON url missing")
                val dto = gpvMapper.decode(githubJsonApi.fetch(url))
                gpvMapper.toSchedule(dto, selection.group, clock)
            }
            else -> error("Unknown source type: ${source.type}")
        }
        scheduleDao.upsert(schedule.toEntity(selection))
    }
}
