package org.exxlexxlee.data.repository

import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScheduleRepository {
    private val regionLocks = catalog.regions().associate { it.id to Mutex() }
    private val requests = Semaphore(3)

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
        }.distinctUntilChanged().flowOn(dispatcher)

    override suspend fun refresh(selection: Selection): Result<Unit> =
        refreshAll(listOf(selection)).getValue(selection)

    override suspend fun refreshAll(selections: List<Selection>): Map<Selection, Result<Unit>> =
        withContext(dispatcher) {
            coroutineScope {
                selections.groupBy { it.region.id }.map { (regionId, regionalSelections) ->
                    async {
                        val lock = regionLocks[regionId]
                        if (lock == null) {
                            regionalSelections.associateWith { Result.failure<Unit>(IllegalArgumentException("Unknown region: $regionId")) }
                        } else {
                            // Keep fetch and writes in order across UI and Worker calls.
                            lock.withLock {
                                requests.withPermit { refreshRegion(regionalSelections) }
                            }
                        }
                    }
                }.awaitAll().flatMap { it.entries }.associate { it.toPair() }
            }
        }

    private suspend fun refreshRegion(selections: List<Selection>): Map<Selection, Result<Unit>> {
        val results = mutableMapOf<Selection, Result<Unit>>()
        val payload = try {
            remoteDataSource.fetch(catalog.source(selections.first().region.id))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return selections.associateWith { Result.failure(error) }
        }
        for (groupSelections in selections.groupBy { it.group.id }.values) {
            val selection = groupSelections.first()
            val result = try {
                val schedule = payload.toSchedule(selection.group, clock)
                scheduleDao.upsert(schedule.toEntity(selection))
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure<Unit>(error)
            }
            groupSelections.forEach { results[it] = result }
        }
        return results
    }
}
