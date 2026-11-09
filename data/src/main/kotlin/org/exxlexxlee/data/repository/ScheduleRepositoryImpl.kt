package org.exxlexxlee.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.local.ScheduleDao
import org.exxlexxlee.data.local.cacheKey
import org.exxlexxlee.data.local.toDomain
import org.exxlexxlee.data.local.toEntity
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository

class ScheduleRepositoryImpl(
    private val catalog: RegionCatalog,
    private val remoteDataSource: ScheduleRemoteDataSource,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock = Clock.System,
) : ScheduleRepository {

    override fun observe(selection: Selection): Flow<OutageSchedule?> =
        scheduleDao.observe(cacheKey(selection)).map { entity -> entity?.toDomain() }

    override suspend fun refresh(selection: Selection): Result<Unit> = try {
        val schedule = remoteDataSource.fetch(catalog.source(selection.region.id))
            .toSchedule(selection.group, clock)
        scheduleDao.upsert(schedule.toEntity(selection))
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
