package org.exxlexxlee.domain.usecase

import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Street
import org.exxlexxlee.domain.repository.LocationRepository

class GetRegions(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(): List<Region> = locationRepository.regions()
}

class GetAreas(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(): List<ServiceArea> = locationRepository.areas()
}

class GetCities(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(regionId: String): List<City> = locationRepository.cities(regionId)
}

class GetStreets(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(cityId: String): List<Street> = locationRepository.streets(cityId)
}

class GetHouses(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(streetId: String): List<House> = locationRepository.houses(streetId)
}

class GetGroups(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(regionId: String): List<OutageGroup> =
        locationRepository.groups(regionId)
}
