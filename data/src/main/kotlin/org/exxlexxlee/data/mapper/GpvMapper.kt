package org.exxlexxlee.data.mapper

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.dto.GpvFileDto
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState

class GpvMapper(
    private val json: Json,
) {
    fun decode(text: String): GpvFileDto = json.decodeFromString(GpvFileDto.serializer(), text)

    fun groups(dto: GpvFileDto): List<OutageGroup> {
        val names = dto.preset?.sch_names.orEmpty()
        if (names.isNotEmpty()) {
            return names.map { (id, name) -> OutageGroup(id = id, name = name) }
        }
        val firstDay = dto.fact?.data?.values?.firstOrNull().orEmpty()
        return firstDay.keys.sorted().map { id -> OutageGroup(id = id, name = id) }
    }

    fun toSchedule(
        dto: GpvFileDto,
        group: OutageGroup,
        clock: Clock = Clock.System,
    ): OutageSchedule {
        val fact = dto.fact ?: error("GPV file has no fact block")
        val todayKey = fact.today?.toString()
            ?: fact.data.keys.minOrNull()
            ?: error("GPV file has no day keys")
        val todayDate = dayDate(todayKey)
        val todayHours = fact.data[todayKey]?.get(group.id)
            ?: error("Group ${group.id} is missing for today")

        val tomorrowEntry = fact.data.entries
            .mapNotNull { (key, groups) ->
                val date = runCatching { dayDate(key) }.getOrNull() ?: return@mapNotNull null
                if (key == todayKey || date <= todayDate) null
                else Triple(key, date, groups[group.id])
            }
            .minByOrNull { it.second }

        return OutageSchedule(
            group = group.copy(name = dto.preset?.sch_names?.get(group.id) ?: group.name),
            today = DaySchedule(todayDate, hourMapToHalves(todayHours)),
            tomorrow = tomorrowEntry?.third?.let { hours ->
                DaySchedule(tomorrowEntry.second, hourMapToHalves(hours))
            },
            sourceUpdatedAt = parseUpdatedAt(dto),
            fetchedAt = clock.now(),
        )
    }

    private fun dayDate(epochSeconds: String): LocalDate {
        val instant = Instant.fromEpochSeconds(epochSeconds.toLong())
        return KyivTime.date(instant)
    }

    private fun hourMapToHalves(hours: Map<String, String>): List<PowerState> {
        return (1..24).flatMap { hour ->
            val status = hours[hour.toString()] ?: hours[hour.toString().padStart(2, '0')]
            gpvStatusToHalves(status)
        }
    }

    private fun gpvStatusToHalves(status: String?): List<PowerState> {
        return when (status?.lowercase()) {
            "yes" -> listOf(PowerState.ON, PowerState.ON)
            "no" -> listOf(PowerState.OFF, PowerState.OFF)
            "first" -> listOf(PowerState.OFF, PowerState.ON)
            "second" -> listOf(PowerState.ON, PowerState.OFF)
            "maybe" -> listOf(PowerState.MAYBE, PowerState.MAYBE)
            "mfirst" -> listOf(PowerState.MAYBE, PowerState.ON)
            "msecond" -> listOf(PowerState.ON, PowerState.MAYBE)
            else -> listOf(PowerState.UNKNOWN, PowerState.UNKNOWN)
        }
    }

    private fun parseUpdatedAt(dto: GpvFileDto): Instant? {
        dto.lastUpdated?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
        }
        val update = dto.fact?.update ?: return null
        val match =
            Regex("""(\d{2})\.(\d{2})\.(\d{4}) (\d{2}):(\d{2})""").matchEntire(update.trim())
                ?: return null
        val (day, month, year, hour, minute) = match.destructured
        val local = LocalDateTime(
            year.toInt(),
            month.toInt(),
            day.toInt(),
            hour.toInt(),
            minute.toInt(),
        )
        return local.toInstant(KyivTime.ZONE)
    }
}
