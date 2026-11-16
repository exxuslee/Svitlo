package org.exxlexxlee.domain.model

import org.exxlexxlee.domain.mapper.OutageSnapshotMapper
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OutageSnapshotTest {
    @Test fun locationDistancesRemainGeographicAndDoNotClaimExactAddress() {
        val point = GeoPoint(0.0, 0.0)
        assertEquals(0.0, point.distanceTo(point), 0.001)
        assertEquals(111_195.0, point.distanceTo(GeoPoint(0.0, 1.0)), 1.0)
        val source = OutageSource("ukpn", Country.UNITED_KINGDOM, "UKPN", "", "Europe/London", SourceAccess.LIVE)
        val event = InternationalOutage("1", "Area", "", time(10), null, point = GeoPoint(0.0, 1.0))
        val snapshot = OutageSnapshotMapper.international(listOf(event), source, time(11), "Area", point) as OutageSnapshot.Timeline
        assertEquals(111_195.0, snapshot.intervals.single().distanceMeters!!, 1.0)
        assertEquals(LocationMatch.AREA, snapshot.locationMatch)
    }
    private val zone = ZoneId.of("Europe/Kyiv")
    private fun time(hour: Int) = Instant.parse("2026-11-16T${hour.toString().padStart(2, '0')}:00:00Z")
    private fun interval(start: Int, end: Int?, kind: OutageKind = OutageKind.PLANNED) =
        OutageInterval(time(start), end?.let(::time), kind)
    private fun timeline(intervals: List<OutageInterval>, coverage: Boolean = true) = OutageSnapshot.Timeline(
        intervals, "Operator", time(0), zone, LocationMatch.EXACT_ADDRESS,
        if (coverage) listOf(CoverageWindow(time(0), time(23))) else emptyList(),
    )

    @Test fun normalizationMergesAdjacentAndOverlappingButKeepsKindsAndMetadata() {
        assertEquals(listOf(interval(1, 5)), normalizeIntervals(listOf(interval(3, 5), interval(1, 3), interval(2, 4))))
        val distinct = listOf(interval(1, 3), interval(2, 4, OutageKind.POSSIBLE), interval(2, 5).copy(endIsEstimate = true))
        assertEquals(3, normalizeIntervals(distinct).size)
        assertTrue(normalizeIntervals(emptyList()).isEmpty())
    }

    @Test fun midnightClippingRetainsUnknownEnd() {
        val start = Instant.parse("2026-11-15T21:00:00Z")
        val end = Instant.parse("2026-11-17T01:00:00Z")
        val date = LocalDate.parse("2026-11-16")
        val known = intervalsForDay(listOf(OutageInterval(start, end, OutageKind.ACTUAL)), date, zone).single()
        assertEquals(date.atStartOfDay(zone).toInstant(), known.start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), known.end)
        assertNull(intervalsForDay(listOf(OutageInterval(start, null, OutageKind.ACTUAL)), date, zone).single().end)
    }

    @Test fun daylightSavingDaysHave23And25Hours() {
        listOf("2026-03-29" to 23L, "2026-10-25" to 25L).forEach { (date, hours) ->
            val day = LocalDate.parse(date)
            val fullDay = DaySchedule(kotlinx.datetime.LocalDate.parse(date), List(48) { PowerState.OFF })
            val schedule = OutageSchedule(OutageGroup("1", "1"), fullDay, null, null, kotlinx.datetime.Instant.parse("2026-01-01T00:00:00Z"))
            val snapshot = OutageSnapshotMapper.schedule(schedule, "Source", zone)
            val interval = snapshot.intervals.single()
            assertEquals(hours, Duration.between(interval.start, interval.end).toHours())
            assertEquals(day.atStartOfDay(zone).toInstant(), interval.start)
            assertEquals(day.plusDays(1).atStartOfDay(zone).toInstant(), interval.end)
        }
    }

    @Test fun statusUsesHalfOpenBoundsAndExtendsOverlappingOutages() {
        val snapshot = timeline(listOf(interval(10, 12), interval(11, 14, OutageKind.ACTUAL), interval(14, 16)))
        assertEquals(NowStatus.PowerOn(time(10)), snapshot.nowStatus(time(9)))
        assertEquals(NowStatus.PowerOff(time(16), false), snapshot.nowStatus(time(10)))
        assertEquals(NowStatus.PowerOn(null), snapshot.nowStatus(time(16)))
    }

    @Test fun possibleIncidentsAndEmptyIncidentFeedDoNotConfirmAvailability() {
        assertEquals(NowStatus.Unknown, timeline(listOf(interval(10, 12, OutageKind.POSSIBLE))).nowStatus(time(11)))
        assertEquals(NowStatus.Unknown, timeline(emptyList(), false).nowStatus(time(11)))
        assertEquals(NowStatus.PowerOn(null), timeline(emptyList()).nowStatus(time(11)))
        assertEquals(NowStatus.Unknown, timeline(emptyList()).copy(schedulesSuspended = true).nowStatus(time(11)))
    }

    @Test fun unknownAndEstimatedRestorationAreNotConfirmedPowerOn() {
        assertEquals(NowStatus.PowerOff(null, false), timeline(listOf(interval(10, null)), false).nowStatus(time(20)))
        val estimated = timeline(listOf(interval(10, 12).copy(endIsEstimate = true)), false)
        assertEquals(NowStatus.PowerOff(time(12), true), estimated.nowStatus(time(11)))
        assertEquals(NowStatus.Unknown, estimated.nowStatus(time(12)))
    }

    @Test fun summaryKeepsZeroUnknownAndMissingPercentageDistinct() {
        val source = OutageSource("odin", Country.UNITED_STATES, "ODIN", "https://example.com", "America/New_York", SourceAccess.LIVE)
        val event = InternationalOutage("county", "County", "", null, null, affectedCustomers = 0, isAreaSummary = true)
        val zero = OutageSnapshotMapper.international(listOf(event), source, time(0), "County") as OutageSnapshot.AreaSummary
        assertEquals(0, zero.customersWithoutPower)
        assertNull(zero.percentWithoutPower)
        val missing = OutageSnapshotMapper.international(emptyList(), source, time(0), "County") as OutageSnapshot.AreaSummary
        assertNull(missing.customersWithoutPower)
    }

    @Test fun scheduleMappingPreservesEveryHalfHourIncludingPossible() {
        val states = List(48) { when (it % 4) { 0 -> PowerState.OFF; 1 -> PowerState.MAYBE; 2 -> PowerState.ON; else -> PowerState.UNKNOWN } }
        val date = kotlinx.datetime.LocalDate.parse("2026-11-16")
        val snapshot = OutageSnapshotMapper.schedule(OutageSchedule(OutageGroup("1", "1"), DaySchedule(date, states), null, null,
            kotlinx.datetime.Instant.parse("2026-11-16T00:00:00Z")), "Source")
        states.forEachIndexed { index, state ->
            val instant = LocalDate.parse(date.toString()).atStartOfDay(zone).toInstant().plusSeconds(index * 1800L + 60)
            val active = snapshot.intervals.firstOrNull { it.start <= instant && instant < it.end!! }
            assertEquals(when(state) { PowerState.OFF -> OutageKind.PLANNED; PowerState.MAYBE -> OutageKind.POSSIBLE; else -> null }, active?.kind)
            if (state == PowerState.UNKNOWN) assertEquals(NowStatus.Unknown, snapshot.nowStatus(instant))
        }
    }
}
