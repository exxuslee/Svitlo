package org.exxlexxlee.domain.repository

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Street

interface LocationRepository {
    suspend fun areas(): List<ServiceArea>
    suspend fun regions(): List<Region>
    suspend fun cities(regionId: String): List<City>
    suspend fun streets(cityId: String): List<Street>
    suspend fun houses(streetId: String): List<House>
    suspend fun groups(regionId: String): List<OutageGroup>
}

interface ScheduleRepository {
    fun observe(selection: Selection): Flow<OutageSchedule?>
    suspend fun refresh(selection: Selection): Result<Unit>
    suspend fun refreshAll(selections: List<Selection>): Map<Selection, Result<Unit>> {
        val results = mutableMapOf<Pair<String, String>, Result<Unit>>()
        return selections.associateWith { selection ->
            results.getOrPut(selection.region.id to selection.group.id) { refresh(selection) }
        }
    }
}

interface SelectionRepository {
    fun observePlaces(): Flow<List<Place>>
    fun observePlace(id: Long): Flow<Place?>
    fun observe(): Flow<Selection?>
    suspend fun save(selection: Selection, placeId: Long? = null): Long
    suspend fun delete(id: Long)
}
