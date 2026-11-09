package org.exxlexxlee.domain.repository

import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
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
