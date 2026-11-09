package org.exxlexxlee.data.repository

import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Street
import org.exxlexxlee.domain.repository.LocationRepository

class LocationRepositoryImpl(
    private val catalog: RegionCatalog,
    private val remoteDataSource: ScheduleRemoteDataSource,
) : LocationRepository {

    override suspend fun areas(): List<ServiceArea> = catalog.areas()

    override suspend fun regions(): List<Region> = catalog.regions()

    override suspend fun cities(regionId: String): List<City> = emptyList()

    override suspend fun streets(cityId: String): List<Street> = emptyList()

    override suspend fun houses(streetId: String): List<House> = emptyList()

    override suspend fun groups(regionId: String): List<OutageGroup> =
        remoteDataSource.fetch(catalog.source(regionId)).groups
}
