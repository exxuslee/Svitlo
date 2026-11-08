package org.exxlexxlee.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectionDao {
    @Query("SELECT * FROM selection WHERE id = 1")
    fun observe(): Flow<SelectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SelectionEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_cache WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<ScheduleCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleCacheEntity)
}
