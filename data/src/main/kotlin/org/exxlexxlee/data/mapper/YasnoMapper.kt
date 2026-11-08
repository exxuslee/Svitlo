package org.exxlexxlee.data.mapper

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.dto.YasnoDayDto
import org.exxlexxlee.data.dto.YasnoGroupDto
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState

class YasnoMapper(
    private val json: Json,
) {
    fun decode(text: String): Map<String, YasnoGroupDto> =
        json.decodeFromString(
            MapSerializer(String.serializer(), YasnoGroupDto.serializer()),
            text,
        )

    fun groups(payload: Map<String, YasnoGroupDto>): List<OutageGroup> =
        payload.keys.sortedWith(groupComparator).map { id -> OutageGroup(id = id, name = id) }

    fun toSchedule(
        payload: Map<String, YasnoGroupDto>,
        group: OutageGroup,
        clock: Clock = Clock.System,
    ): OutageSchedule {
        val dto = payload[group.id] ?: error("Yasno response has no group ${group.id}")
        val todayDto = dto.today ?: error("Yasno group ${group.id} has no today schedule")
        val today = todayDto.toDaySchedule(fallbackDate = KyivTime.today(clock))
        val tomorrow = dto.tomorrow?.toDaySchedule(
            fallbackDate = today.date.plus(1, DateTimeUnit.DAY),
        )

        return OutageSchedule(
            group = group,
            today = today,
            tomorrow = tomorrow,
            sourceUpdatedAt = dto.updatedOn?.let { runCatching { Instant.parse(it) }.getOrNull() },
            fetchedAt = clock.now(),
        )
    }

    private fun YasnoDayDto.toDaySchedule(fallbackDate: LocalDate): DaySchedule {
        val date = date?.let(::parseDate) ?: fallbackDate
        val halves = MutableList(48) { PowerState.ON }
        slots.forEach { slot ->
            val state = when (slot.type?.lowercase()) {
                "definite" -> PowerState.OFF
                "possible" -> PowerState.MAYBE
                else -> PowerState.OFF
            }
            val start = slot.start.coerceIn(0, 1440)
            val end = slot.end.coerceIn(0, 1440)
            if (end <= start) return@forEach
            val firstIndex = start / 30
            val lastIndex = ((end - 1) / 30).coerceAtMost(47)
            for (index in firstIndex..lastIndex) {
                halves[index] = merge(halves[index], state)
            }
        }
        return DaySchedule(date, halves)
    }

    private fun parseDate(value: String): LocalDate {
        runCatching { Instant.parse(value) }.getOrNull()?.let { return KyivTime.date(it) }
        return LocalDate.parse(value.take(10))
    }

    private fun merge(current: PowerState, incoming: PowerState): PowerState {
        return when {
            current == PowerState.OFF || incoming == PowerState.OFF -> PowerState.OFF
            current == PowerState.MAYBE || incoming == PowerState.MAYBE -> PowerState.MAYBE
            else -> incoming
        }
    }

    private val groupComparator = Comparator<String> { a, b ->
        val aNum = a.substringBefore('.', a.substringBefore('-')).toIntOrNull()
        val bNum = b.substringBefore('.', b.substringBefore('-')).toIntOrNull()
        when {
            aNum != null && bNum != null -> {
                val primary = aNum.compareTo(bNum)
                if (primary != 0) primary else a.compareTo(b)
            }
            else -> a.compareTo(b)
        }
    }
}
