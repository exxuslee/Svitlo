package org.exxlexxlee.data.mapper.ukrainemonitor

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.dto.ukrainemonitor.UkraineMonitorRegionDto
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.PowerState

/** The aggregated last-message.json is distinct from the repository's older GPV artifacts. */
class UkraineMonitorMapper(private val json: Json) {
    fun decode(raw: String, region: String): RegionSchedule {
        val root = json.parseToJsonElement(raw).jsonObject
        val today = LocalDate.parse(root.getValue("date_today").jsonPrimitive.content)
        val matches = root.getValue("regions").jsonArray.map { it.jsonObject }
            .filter { it["cpu"]?.jsonPrimitive?.contentOrNull == region }
        require(matches.size == 1) { "Missing or duplicate monitor region: $region" }
        val row = matches.single()
        val dto = UkraineMonitorRegionDto(
            cpu = region,
            schedule = row.getValue("schedule").jsonObject.mapValues { (_, dates) ->
                dates.jsonObject.mapValues { (_, slots) ->
                    slots.jsonObject.mapValues { (_, state) -> state.jsonPrimitive.int }
                }
            },
            emergency = row.getValue("emergency").jsonPrimitive.boolean,
        )
        val days = dto.schedule.mapValues { (_, dates) ->
            dates.entries.associate { (date, slots) ->
                LocalDate.parse(date) to slots.onEach { (slot, state) ->
                    require(SLOT.matches(slot)) { "Invalid monitor half-hour slot" }
                    require(state in 1..2) { "Unknown monitor state" }
                }
            }
        }
        require(days.isNotEmpty()) { "Monitor region has no groups" }
        return RegionSchedule(today, days, dto.emergency)
    }

    fun groups(payload: RegionSchedule): List<OutageGroup> =
        payload.days.keys.sorted().map { OutageGroup(it, it) }

    fun toSchedule(payload: RegionSchedule, group: OutageGroup, clock: Clock): OutageSchedule {
        // Existing GPV selections remain usable when their supplier switches to this feed.
        val days =
            payload.days[group.id.removePrefix("GPV")] ?: error("Monitor has no group ${group.id}")
        val today = KyivTime.today(clock)
        fun day(date: LocalDate): DaySchedule = DaySchedule(
            date,
            List(48) { index ->
                val slot = "%02d:%02d".format(java.util.Locale.ROOT, index / 2, (index % 2) * 30)
                if (payload.emergency) PowerState.UNKNOWN else when (days[date]?.get(slot)) {
                    1 -> PowerState.ON
                    2 -> PowerState.OFF
                    else -> PowerState.UNKNOWN
                }
            },
            published = !payload.emergency && days.containsKey(date),
        )

        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return OutageSchedule(
            group, day(today), days[tomorrow]?.let { day(tomorrow) },
            sourceUpdatedAt = null, // The file publishes schedule dates, not a trustworthy update timestamp.
            fetchedAt = clock.now(), schedulesSuspended = payload.emergency,
        )
    }

    private companion object {
        val SLOT = Regex("(?:[01][0-9]|2[0-3]):(?:00|30)")
    }
}
