package org.exxlexxlee.data.repository

import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.GithubJsonApi
import org.exxlexxlee.data.remote.YasnoApi
import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Street
import org.exxlexxlee.domain.repository.LocationRepository

class LocationRepositoryImpl(
    private val catalog: RegionCatalog,
    private val yasnoApi: YasnoApi,
    private val githubJsonApi: GithubJsonApi,
    private val yasnoMapper: YasnoMapper,
    private val gpvMapper: GpvMapper,
) : LocationRepository {

    override suspend fun areas(): List<ServiceArea> = catalog.areas()

    override suspend fun regions(): List<Region> = catalog.regions()

    override suspend fun cities(regionId: String): List<City> = emptyList()

    override suspend fun streets(cityId: String): List<Street> = emptyList()

    override suspend fun houses(streetId: String): List<House> = emptyList()

    override suspend fun groups(regionId: String): List<OutageGroup> {
        val source = catalog.source(regionId)
        return when (source.type) {
            "yasno" -> {
                val region = source.regionId ?: error("Yasno regionId missing")
                val dso = source.dsoId ?: error("Yasno dsoId missing")
                yasnoMapper.groups(yasnoMapper.decode(yasnoApi.fetchPlannedOutages(region, dso)))
            }
            "github_json" -> {
                val url = source.url ?: error("GitHub JSON url missing")
                gpvMapper.groups(gpvMapper.decode(githubJsonApi.fetch(url)))
            }
            else -> error("Unknown source type: ${source.type}")
        }
    }
}
