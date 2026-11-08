package org.exxlexxlee.data.catalog

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.exxlexxlee.domain.model.Region

class RegionCatalog(
    context: Context,
    private val json: Json,
) {
    private val file: RegionsFile = json.decodeFromString(
        RegionsFile.serializer(),
        context.assets.open("regions.json").bufferedReader().use { it.readText() },
    )

    fun regions(): List<Region> = file.regions.map { it.toRegion() }

    fun source(regionId: String): RegionSourceConfig =
        file.regions.firstOrNull { it.id == regionId }?.source
            ?: error("Unknown region: $regionId")

    fun region(regionId: String): Region =
        file.regions.first { it.id == regionId }.toRegion()
}

@Serializable
data class RegionsFile(
    val regions: List<RegionConfig>,
)

@Serializable
data class RegionConfig(
    val id: String,
    val name: String,
    val providerId: String,
    val source: RegionSourceConfig,
)

@Serializable
data class RegionSourceConfig(
    val type: String,
    val url: String? = null,
    val regionId: Int? = null,
    val dsoId: Int? = null,
)

private fun RegionConfig.toRegion() = Region(
    id = id,
    name = name,
    providerId = providerId,
)
