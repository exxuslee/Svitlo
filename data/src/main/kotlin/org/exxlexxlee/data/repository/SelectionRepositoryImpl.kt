package org.exxlexxlee.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.local.SelectionDao
import org.exxlexxlee.data.local.SelectionEntity
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.SelectionRepository

class SelectionRepositoryImpl(
    private val selectionDao: SelectionDao,
    private val json: Json,
) : SelectionRepository {

    override fun observe(): Flow<Selection?> = selectionDao.observe().map { entity ->
        entity?.json?.let { payload ->
            runCatching { json.decodeFromString(StoredSelection.serializer(), payload).toDomain() }
                .getOrNull()
        }
    }

    override suspend fun save(selection: Selection) {
        val stored = StoredSelection(
            regionId = selection.region.id,
            regionName = selection.region.name,
            providerId = selection.region.providerId,
            groupId = selection.group.id,
            groupName = selection.group.name,
        )
        selectionDao.upsert(
            SelectionEntity(json = json.encodeToString(StoredSelection.serializer(), stored)),
        )
    }
}

@Serializable
private data class StoredSelection(
    val regionId: String,
    val regionName: String,
    val providerId: String,
    val groupId: String,
    val groupName: String,
) {
    fun toDomain(): Selection = Selection.ByGroup(
        region = Region(id = regionId, name = regionName, providerId = providerId),
        group = OutageGroup(id = groupId, name = groupName),
    )
}
