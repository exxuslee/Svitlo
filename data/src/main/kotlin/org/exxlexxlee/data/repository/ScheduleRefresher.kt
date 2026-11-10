package org.exxlexxlee.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.local.ScheduleDao
import org.exxlexxlee.data.local.toEntity
import org.exxlexxlee.data.remote.SchedulePayload
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.domain.model.Selection

/** Owns request limits and cache writes for all refreshes of one repository instance. */
internal class ScheduleRefresher(
    private val catalog: RegionCatalog,
    private val remoteDataSource: ScheduleRemoteDataSource,
    private val scheduleDao: ScheduleDao,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) {
    private val regionLocks = catalog.regions().associate { it.id to Mutex() }
    private val requests = Semaphore(MAX_CONCURRENT_REGIONS)

    suspend fun refreshAll(selections: List<Selection>): Map<Selection, Result<Unit>> =
        withContext(dispatcher) {
            val regionalResults = selections.groupBy { it.region.id }.map { (regionId, regionalSelections) ->
                async { refreshRegion(regionId, regionalSelections) }
            }.awaitAll()
            buildMap { regionalResults.forEach { putAll(it) } }
        }

    private suspend fun refreshRegion(
        regionId: String,
        selections: List<Selection>,
    ): Map<Selection, Result<Unit>> {
        val lock = regionLocks[regionId]
            ?: return selections.associateWith {
                Result.failure(IllegalArgumentException("Unknown region: $regionId"))
            }
        // Keep fetch and writes in order across UI and Worker calls.
        return lock.withLock {
            requests.withPermit { fetchAndCache(regionId, selections) }
        }
    }

    private suspend fun fetchAndCache(
        regionId: String,
        selections: List<Selection>,
    ): Map<Selection, Result<Unit>> {
        val payload = try {
            remoteDataSource.fetch(catalog.source(regionId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return selections.associateWith { Result.failure(error) }
        }
        return buildMap {
            for (groupSelections in selections.groupBy { it.group.id }.values) {
                val result = cacheGroup(payload, groupSelections.first())
                groupSelections.forEach { put(it, result) }
            }
        }
    }

    private suspend fun cacheGroup(payload: SchedulePayload, selection: Selection): Result<Unit> =
        try {
            val schedule = payload.toSchedule(selection.group, clock)
            scheduleDao.upsert(schedule.toEntity(selection))
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }

    private companion object {
        const val MAX_CONCURRENT_REGIONS = 3
    }
}
