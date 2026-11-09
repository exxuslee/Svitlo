package org.exxlexxlee.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY position ASC, id ASC LIMIT 1")
    fun observeFirst(): Flow<PlaceEntity?>

    @Query("SELECT * FROM places WHERE id = :id")
    fun observe(id: Long): Flow<PlaceEntity?>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun get(id: Long): PlaceEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM places")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaceEntity): Long

    @Update
    suspend fun update(entity: PlaceEntity)

    @Transaction
    suspend fun save(json: String, placeId: Long?): Long {
        val existing = placeId?.takeIf { it > 0 }?.let { get(it) }
        if (existing != null) {
            update(existing.copy(json = json))
            return existing.id
        }
        return insert(PlaceEntity(position = maxPosition() + 1, json = json))
    }

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_cache WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<ScheduleCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleCacheEntity)
}
