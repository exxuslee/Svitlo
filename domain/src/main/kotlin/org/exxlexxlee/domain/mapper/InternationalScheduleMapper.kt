package org.exxlexxlee.domain.mapper

import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.PowerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A feed of incidents proves outages, but never proves power availability between them. */
object InternationalScheduleMapper {
    data class Day(val states: List<PowerState>, val ranges: String?)

    fun day(events: List<InternationalOutage>, date: LocalDate, zone: ZoneId): Day {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val intervals = events.mapNotNull { event ->
            val from = event.start ?: return@mapNotNull null
            val until = event.end ?: return@mapNotNull null
            if (until <= from || until <= start || from >= end) null
            else maxOf(from, start) to minOf(until, end)
        }.sortedBy { it.first }
        val merged = mutableListOf<Pair<Instant, Instant>>()
        for (interval in intervals) {
            val last = merged.lastOrNull()
            if (last != null && interval.first <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, interval.second)
            } else merged.add(interval)
        }
        val states = List(48) { index ->
            val local = date.atStartOfDay().plusMinutes(index * 30L)
            val occurrences = zone.rules.getValidOffsets(local).map { offset ->
                local.toInstant(offset) to local.toInstant(offset).plusSeconds(1800)
            }
            // On DST overlap a wall-clock slot can represent two distinct instants.
            // Show confirmed OFF only if every occurrence is fully covered.
            when {
                occurrences.isEmpty() -> PowerState.UNKNOWN
                occurrences.all { (from, until) -> merged.any { it.first <= from && it.second >= until } } -> PowerState.OFF
                occurrences.any { (from, until) -> merged.any { it.first < until && it.second > from } } -> PowerState.MAYBE
                else -> PowerState.UNKNOWN
            }
        }
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val ranges = merged.takeIf { it.isNotEmpty() }?.joinToString(", ") { (from, until) ->
            val fromText = from.atZone(zone).format(formatter)
            val untilText = if (until == end) "24:00" else until.atZone(zone).format(formatter)
            "$fromText–$untilText"
        }
        return Day(states, ranges)
    }
}
