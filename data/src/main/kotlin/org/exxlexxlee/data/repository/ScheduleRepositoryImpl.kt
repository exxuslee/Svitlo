package org.exxlexxlee.data.repository

import java.util.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.local.ScheduleDao
import org.exxlexxlee.data.local.cacheKey
import org.exxlexxlee.data.local.toDomain
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.forDate
import org.exxlexxlee.domain.model.observeKyivDate
import org.exxlexxlee.domain.repository.ScheduleRepository

class ScheduleRepositoryImpl(
    catalog: RegionCatalog,
    remoteDataSource: ScheduleRemoteDataSource,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock = Clock.System,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScheduleRepository {
    private val refresher = ScheduleRefresher(catalog, remoteDataSource, scheduleDao, clock, dispatcher)

    override fun observe(selection: Selection): Flow<OutageSchedule?> =
        scheduleDao.observe(cacheKey(selection)).distinctUntilChanged().transform { entity ->
            if (entity == null) {
                emit(null)
            } else {
                val decoded = runCatching { entity.toDomain() }
                emit(decoded.getOrNull())
                if (decoded.isFailure) {
                    Logger.getLogger(ScheduleRepositoryImpl::class.java.name)
                        .warning("Invalid schedule cache for ${entity.cacheKey}")
                    // Rebuild only the damaged entry; a failed request leaves observation alive.
                    refresh(selection)
                }
            }
        }.combine(observeKyivDate(clock)) { schedule, date ->
            schedule?.forDate(date)
        }.distinctUntilChanged().flowOn(dispatcher)

    override suspend fun refresh(selection: Selection): Result<Unit> =
        refreshAll(listOf(selection)).getValue(selection)

    override suspend fun refreshAll(selections: List<Selection>): Map<Selection, Result<Unit>> =
        refresher.refreshAll(selections)
}
