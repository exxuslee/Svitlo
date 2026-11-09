package org.exxlexxlee.data.catalog

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier

class RegionCatalog internal constructor(
    private val file: RegionsFile,
) {
    constructor(context: Context, json: Json) : this(
        json.decodeFromString(
            RegionsFile.serializer(),
            context.assets.open("regions.json").bufferedReader().use { it.readText() },
        ),
    )

    private val cachedAreas: List<ServiceArea> = file.areas.map { area ->
        ServiceArea(
            id = area.id,
            name = area.name,
            suppliers = area.suppliers.map { it.toSupplier() },
        )
    }

    private val cachedRegions = cachedAreas.flatMap { area -> area.suppliers.map { it.region } }
    private val regionsById = cachedRegions.associateBy { it.id }
    private val sourcesById = file.areas.flatMap { it.suppliers }.associate { it.id to it.source }

    fun areas(): List<ServiceArea> = cachedAreas

    fun regions(): List<Region> = cachedRegions

    fun source(regionId: String): RegionSourceConfig =
        sourcesById[regionId]
            ?: error("Unknown region: $regionId")

    fun region(regionId: String): Region =
        regionsById[regionId] ?: error("Unknown region: $regionId")
}

@Serializable
data class RegionsFile(
    val areas: List<AreaConfig>,
)

@Serializable
data class AreaConfig(
    val id: String,
    val name: String,
    val suppliers: List<SupplierConfig>,
)

@Serializable
data class SupplierConfig(
    val id: String,
    val name: String,
    val providerId: String,
    val primary: Boolean = false,
    val source: RegionSourceConfig,
)

@Serializable
data class RegionSourceConfig(
    val type: String,
    val url: String? = null,
    val regionId: Int? = null,
    val dsoId: Int? = null,
)

private fun SupplierConfig.toSupplier() = Supplier(
    region = Region(id = id, name = name, providerId = providerId),
    primary = primary,
)
