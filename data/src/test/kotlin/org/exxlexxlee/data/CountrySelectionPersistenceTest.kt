package org.exxlexxlee.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.local.*
import org.exxlexxlee.data.repository.SelectionRepositoryImpl
import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CountrySelectionPersistenceTest {
    private class MemoryDao : PlaceDao {
        val entries = MutableStateFlow<List<PlaceEntity>>(emptyList())
        override fun observeAll() = entries
        override fun observeFirst() = entries.map { it.firstOrNull() }
        override fun observe(id: Long) = entries.map { list -> list.find { it.id == id } }
        override suspend fun get(id: Long) = entries.value.find { it.id == id }
        override suspend fun maxPosition() = entries.value.maxOfOrNull { it.position } ?: -1
        override suspend fun insert(entity: PlaceEntity): Long {
            val id = entries.value.size + 1L
            entries.value = entries.value + entity.copy(id = id)
            return id
        }
        override suspend fun update(entity: PlaceEntity) { entries.value = entries.value.map { if (it.id == entity.id) entity else it } }
        override suspend fun delete(id: Long) { entries.value = entries.value.filter { it.id != id } }
    }
    @Test fun countriesAndOperatorAddressesSurviveRepositoryRecreation() = runTest {
        val dao = MemoryDao()
        val repository = SelectionRepositoryImpl(dao, Json)
        val ua = Selection.ByGroup(Region("kyiv", "Kyiv", "yasno"), OutageGroup("1", "1"), AddressDetails("Home"))
        val pl = Selection.ByGroup(Region("international:tauron", "Tauron", "tauron"), OutageGroup("a", "Tauron"), AddressDetails("Kraków", country = Country.POLAND, sourceId = "tauron", cityId = 106173, streetId = 903765, house = "1", locationPoint = GeoPoint(50.0614, 19.9366)))
        repository.save(ua, null)
        repository.save(pl, null)
        val restored = SelectionRepositoryImpl(dao, Json).observePlaces().first()
        assertEquals(listOf(ua, pl), restored.map { it.selection })
        assertEquals(listOf(Country.UKRAINE, Country.POLAND), restored.map { it.selection.details!!.country })
    }
}
