package org.exxlexxlee.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class OutageKind { PLANNED, POSSIBLE, ACTUAL, EMERGENCY }
enum class LocationMatch { EXACT_ADDRESS, AREA, UNKNOWN }

data class OutageInterval(
    val start: Instant,
    val end: Instant?,
    val kind: OutageKind,
    val endIsEstimate: Boolean = false,
    val cause: String? = null,
    val affectedCustomers: Int? = null,
    val distanceMeters: Double? = null,
) {
    init {
        require(end == null || end > start)
        require(affectedCustomers == null || affectedCustomers >= 0)
        require(distanceMeters == null || distanceMeters.isFinite() && distanceMeters >= 0)
    }
}

data class CoverageWindow(val start: Instant, val end: Instant) {
    init { require(end > start) }
    operator fun contains(now: Instant) = now >= start && now < end
}

sealed interface OutageSnapshot {
    val sourceName: String
    val fetchedAt: Instant
    val zone: ZoneId
    val locationMatch: LocationMatch

    data class Timeline(
        val intervals: List<OutageInterval>,
        override val sourceName: String,
        override val fetchedAt: Instant,
        override val zone: ZoneId,
        override val locationMatch: LocationMatch,
        // Only an authoritative schedule can confirm power outside its outages.
        val knownCoverage: List<CoverageWindow> = emptyList(),
        val schedulesSuspended: Boolean = false,
    ) : OutageSnapshot

    data class AreaSummary(
        val areaName: String,
        val customersWithoutPower: Int?,
        val percentWithoutPower: Double?,
        override val sourceName: String,
        override val fetchedAt: Instant,
        override val zone: ZoneId,
        override val locationMatch: LocationMatch = LocationMatch.AREA,
    ) : OutageSnapshot {
        init {
            require(customersWithoutPower == null || customersWithoutPower >= 0)
            require(percentWithoutPower == null || percentWithoutPower.isFinite() && percentWithoutPower in 0.0..100.0)
        }
    }
}

sealed interface NowStatus {
    data class PowerOn(val nextOutageAt: Instant?) : NowStatus
    data class PowerOff(val until: Instant?, val estimated: Boolean) : NowStatus
    data object Unknown : NowStatus
}

sealed interface OutageCardState {
    data object Loading : OutageCardState
    data class Content(val snapshot: OutageSnapshot) : OutageCardState
    data class Stale(val snapshot: OutageSnapshot, val age: Duration) : OutageCardState
    data object Error : OutageCardState
}

/** Merge only semantically identical intervals; never erase possible/emergency metadata. */
fun normalizeIntervals(intervals: List<OutageInterval>): List<OutageInterval> =
    intervals.groupBy { listOf(it.kind, it.endIsEstimate, it.cause, it.affectedCustomers, it.distanceMeters) }
        .values.flatMap { group ->
            val merged = mutableListOf<OutageInterval>()
            group.sortedBy { it.start }.forEach { next ->
                val previous = merged.lastOrNull()
                if (previous != null && (previous.end == null || next.start <= previous.end)) {
                    merged[merged.lastIndex] = previous.copy(
                        end = if (previous.end == null || next.end == null) null else maxOf(previous.end, next.end),
                    )
                } else merged += next
            }
            merged
        }.sortedBy { it.start }

/** Keep an unknown end unknown; rendering clamps it to the day's right edge. */
fun intervalsForDay(intervals: List<OutageInterval>, date: LocalDate, zone: ZoneId): List<OutageInterval> {
    val start = date.atStartOfDay(zone).toInstant()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant()
    return normalizeIntervals(intervals).filter { it.start < end && (it.end == null || it.end > start) }
        .map { it.copy(start = maxOf(it.start, start), end = it.end?.let { value -> minOf(value, end) }) }
}

fun OutageSnapshot.Timeline.nowStatus(now: Instant): NowStatus {
    if (schedulesSuspended) return NowStatus.Unknown
    val definite = intervals.filter { it.kind != OutageKind.POSSIBLE }.sortedBy { it.start }
    val active = definite.filter { it.start <= now && (it.end == null || now < it.end) }
    if (active.isNotEmpty()) {
        var until = active.maxOf { it.end ?: Instant.MAX }
        var estimated = active.any { it.endIsEstimate }
        // A neighbouring/overlapping outage extends the current interruption.
        definite.filter { it.start > now }.forEach {
            if (it.start <= until) {
                val end = it.end ?: Instant.MAX
                if (end >= until) { until = end; estimated = it.endIsEstimate || estimated }
            }
        }
        return NowStatus.PowerOff(until.takeUnless { it == Instant.MAX }, estimated)
    }
    if (intervals.any { it.kind == OutageKind.POSSIBLE && it.start <= now && (it.end == null || now < it.end) })
        return NowStatus.Unknown
    return if (knownCoverage.any { now in it })
        NowStatus.PowerOn(definite.firstOrNull { it.start > now }?.start)
    else NowStatus.Unknown
}
