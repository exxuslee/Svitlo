package org.exxlexxlee.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: Int,
    val json: String,
)

@Entity(tableName = "schedule_cache")
data class ScheduleCacheEntity(
    @PrimaryKey val cacheKey: String,
    val groupId: String,
    val groupName: String,
    val todayDate: String,
    val todayHalfHours: String,
    val tomorrowDate: String?,
    val tomorrowHalfHours: String?,
    val sourceUpdatedAtEpochMs: Long?,
    val fetchedAtEpochMs: Long,
    val todayPublished: Boolean,
    val tomorrowPublished: Boolean?,
    val schedulesSuspended: Boolean,
)
