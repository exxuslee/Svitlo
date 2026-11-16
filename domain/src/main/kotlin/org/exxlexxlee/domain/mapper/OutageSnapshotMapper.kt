package org.exxlexxlee.domain.mapper

import org.exxlexxlee.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object OutageSnapshotMapper {
    fun schedule(schedule: OutageSchedule, sourceName: String, zone: ZoneId = locationZone("Europe/Kyiv")): OutageSnapshot.Timeline {
        val intervals = mutableListOf<OutageInterval>()
        val coverage = mutableListOf<CoverageWindow>()
        listOfNotNull(schedule.today, schedule.tomorrow).filter { it.published }.forEach { day ->
            val date = LocalDate.parse(day.date.toString())
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            // Iterate real instants: spring days have 46 slots, autumn days 50.
            // Both occurrences of a repeated wall-clock slot use the operator's slot value.
            generateSequence(start) { it.plusSeconds(1800).takeIf { next -> next < end } }.forEach { instant ->
                val local = instant.atZone(zone)
                val state = day.halfHours[(local.hour * 60 + local.minute) / 30]
                val slotEnd = minOf(instant.plusSeconds(1800), end)
                if (state != PowerState.UNKNOWN) coverage += CoverageWindow(instant, slotEnd)
                val kind = when (state) {
                    PowerState.OFF -> OutageKind.PLANNED
                    PowerState.MAYBE -> OutageKind.POSSIBLE
                    else -> null
                }
                if (kind != null) intervals += OutageInterval(instant, slotEnd, kind)
            }
        }
        return OutageSnapshot.Timeline(
            normalizeIntervals(intervals), sourceName, Instant.parse(schedule.fetchedAt.toString()), zone,
            LocationMatch.EXACT_ADDRESS, coverage, schedule.schedulesSuspended,
        )
    }

    fun international(events: List<InternationalOutage>, source: OutageSource, fetchedAt: Instant, location: String, locationPoint: GeoPoint? = null): OutageSnapshot {
        val zone = locationZone(source.zoneId)
        val summaries = events.filter { it.isAreaSummary }
        val incidents = events.filterNot { it.isAreaSummary }
        val intervals = incidents.mapNotNull { event ->
            val start = event.start ?: return@mapNotNull null
            if (event.end != null && event.end <= start) return@mapNotNull null
            OutageInterval(start, event.end, event.kind, event.endIsEstimate,
                event.description.takeIf { it.isNotBlank() }, event.affectedCustomers,
                locationPoint?.let { point -> event.point?.let(point::distanceTo) })
        }
        // A timeline takes precedence when the feed supplies both types.
        if (intervals.isEmpty() && (summaries.isNotEmpty() || source.id == "odin")) {
            val counts = summaries.mapNotNull { it.affectedCustomers }
            return OutageSnapshot.AreaSummary(
                location, counts.takeIf { it.isNotEmpty() && it.size == summaries.size }?.sum(),
                summaries.singleOrNull()?.percentWithoutPower, source.name, fetchedAt, zone,
            )
        }
        return OutageSnapshot.Timeline(
            normalizeIntervals(intervals), source.name, fetchedAt, zone,
            when {
                incidents.isEmpty() -> LocationMatch.UNKNOWN
                incidents.all { !it.areaOnly } -> LocationMatch.EXACT_ADDRESS
                else -> LocationMatch.AREA
            },
        )
    }
}
