package org.exxlexxlee.data.repository

import kotlinx.coroutines.flow.Flow
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
) : SelectionRepository {

    override fun observePlaces(): Flow<List<Place>> = placeDao.observeAll().map { entities ->
        entities.mapNotNull { entity -> entity.toPlace() }
    }

    override fun observePlace(id: Long): Flow<Place?> =
        placeDao.observe(id).map { entity -> entity?.toPlace() }

    override fun observe(): Flow<Selection?> = observePlaces().map { places ->
        places.firstOrNull()?.selection
    }

    override suspend fun save(selection: Selection, placeId: Long?): Long {
        val stored = selection.toStored()
        val json = parser.encodeToString(StoredSelection.serializer(), stored)
        if (placeId != null && placeId > 0) {
            val existing = placeDao.get(placeId)
            if (existing != null) {
                placeDao.update(existing.copy(json = json))
                return placeId
            }
        }
        return placeDao.insert(
            PlaceEntity(
                position = placeDao.maxPosition() + 1,
                json = json,
            ),
        )
    }

    override suspend fun delete(id: Long) {
        placeDao.delete(id)
    }

    private fun PlaceEntity.toPlace(): Place? {
        val selection = runCatching {
            parser.decodeFromString(StoredSelection.serializer(), json).toDomain()
        }.getOrNull() ?: return null
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
    )
}

@Serializable
private data class StoredSelection(
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
                reminderMinutes = reminderMinutes,
                quietNight = quietNight,
                iconId = iconId,
            )
        },
    )
}
