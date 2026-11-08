package org.exxlexxlee.data.local

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Selection

fun cacheKey(selection: Selection): String = "${selection.region.id}|${selection.group.id}"

fun OutageSchedule.toEntity(selection: Selection): ScheduleCacheEntity = ScheduleCacheEntity(
    cacheKey = cacheKey(selection),
    groupId = group.id,
    groupName = group.name,
    todayDate = today.date.toString(),
    todayHalfHours = today.halfHours.encode(),
    tomorrowDate = tomorrow?.date?.toString(),
    tomorrowHalfHours = tomorrow?.halfHours?.encode(),
    sourceUpdatedAtEpochMs = sourceUpdatedAt?.toEpochMilliseconds(),
    fetchedAtEpochMs = fetchedAt.toEpochMilliseconds(),
    todayPublished = today.published,
    tomorrowPublished = tomorrow?.published,
    schedulesSuspended = schedulesSuspended,
)

fun ScheduleCacheEntity.toDomain(): OutageSchedule = OutageSchedule(
    group = OutageGroup(id = groupId, name = groupName),
    today = DaySchedule(
        date = LocalDate.parse(todayDate),
        halfHours = todayHalfHours.decodeStates(),
        published = todayPublished,
    ),
    tomorrow = if (tomorrowDate != null && tomorrowHalfHours != null) {
        DaySchedule(
            date = LocalDate.parse(tomorrowDate),
            halfHours = tomorrowHalfHours.decodeStates(),
            published = tomorrowPublished ?: true,
        )
    } else {
        null
    },
    sourceUpdatedAt = sourceUpdatedAtEpochMs?.let(Instant::fromEpochMilliseconds),
    fetchedAt = Instant.fromEpochMilliseconds(fetchedAtEpochMs),
    schedulesSuspended = schedulesSuspended,
)

private fun List<PowerState>.encode(): String = joinToString(",") { it.name }

private fun String.decodeStates(): List<PowerState> =
    split(',').map { name ->
        runCatching { PowerState.valueOf(name) }.getOrDefault(PowerState.UNKNOWN)
    }
