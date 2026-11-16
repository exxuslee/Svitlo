package org.exxlexxlee.data.repository

import java.util.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.local.PlaceDao
import org.exxlexxlee.data.local.PlaceEntity
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.SelectionRepository

class SelectionRepositoryImpl(
    private val placeDao: PlaceDao,
    private val parser: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SelectionRepository {

    override fun observePlaces(): Flow<List<Place>> = placeDao.observeAll().map { entities ->
        entities.mapNotNull { entity -> entity.toPlace() }
    }.distinctUntilChanged().flowOn(dispatcher)

    override fun observePlace(id: Long): Flow<Place?> =
        placeDao.observe(id).map { entity -> entity?.toPlace() }
            .distinctUntilChanged().flowOn(dispatcher)

    override fun observe(): Flow<Selection?> = placeDao.observeFirst()
        .map { it?.toPlace()?.selection }
        .distinctUntilChanged().flowOn(dispatcher)

    override suspend fun save(selection: Selection, placeId: Long?): Long {
        val json = withContext(dispatcher) {
            parser.encodeToString(StoredSelection.serializer(), selection.toStored())
        }
        return placeDao.save(json, placeId)
    }

    override suspend fun delete(id: Long) {
        placeDao.delete(id)
    }

    private fun PlaceEntity.toPlace(): Place? {
        val selection = runCatching {
            parser.decodeFromString(StoredSelection.serializer(), json).toDomain()
        }.getOrElse { error ->
            Logger.getLogger(SelectionRepositoryImpl::class.java.name)
                .warning("Cannot decode saved place $id: ${error.javaClass.simpleName}")
            return null
        }
        return Place(id = id, selection = selection)
    }

    private fun Selection.toStored(): StoredSelection = StoredSelection(
        regionId = region.id,
        regionName = region.name,
        providerId = region.providerId,
        groupId = group.id,
        groupName = group.name,
        addressName = details?.name,
        reminderMinutes = details?.reminderMinutes ?: 15,
        quietNight = details?.quietNight ?: false,
        iconId = details?.iconId ?: "home",
        countryCode = details?.country?.code ?: "UA",
        sourceId = details?.sourceId,
        filter = details?.filter.orEmpty(),
        cityId = details?.cityId,
        streetId = details?.streetId,
        house = details?.house.orEmpty(),
        latitude = details?.locationPoint?.latitude,
        longitude = details?.locationPoint?.longitude,
    )
}

@Serializable
private data class StoredSelection(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val countryCode: String = "UA",
    val sourceId: String? = null,
    val filter: String = "",
    val cityId: Long? = null,
    val streetId: Long? = null,
    val house: String = "",
    val regionId: String,
    val regionName: String,
    val providerId: String,
    val groupId: String,
    val groupName: String,
    val addressName: String? = null,
    val reminderMinutes: Int = 15,
    val quietNight: Boolean = false,
    val iconId: String = "home",
) {
    fun toDomain(): Selection = Selection.ByGroup(
        region = Region(id = regionId, name = regionName, providerId = providerId),
        group = OutageGroup(id = groupId, name = groupName),
        details = addressName?.let { name ->
            AddressDetails(
                name = name,
                country = org.exxlexxlee.domain.model.Country.fromCode(countryCode)
                    ?: org.exxlexxlee.domain.model.Country.UKRAINE,
                sourceId = sourceId,
                filter = filter,
                cityId = cityId,
                streetId = streetId,
                house = house,
                locationPoint = latitude?.let { lat -> longitude?.let { lon ->
                    runCatching { org.exxlexxlee.domain.model.GeoPoint(lat, lon) }.getOrNull()
                } },
                reminderMinutes = reminderMinutes,
                quietNight = quietNight,
                iconId = iconId,
            )
        },
    )
}
