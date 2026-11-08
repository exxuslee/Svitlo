package org.exxlexxlee.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SelectionEntity::class, ScheduleCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun selectionDao(): SelectionDao
    abstract fun scheduleDao(): ScheduleDao
}
