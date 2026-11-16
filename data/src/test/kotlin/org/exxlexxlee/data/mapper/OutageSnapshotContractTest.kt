package org.exxlexxlee.data.mapper

import kotlinx.serialization.json.Json
import org.exxlexxlee.data.mapper.odin.OdinMapper
import org.exxlexxlee.data.mapper.ukpn.UkpnMapper
import org.exxlexxlee.domain.mapper.OutageSnapshotMapper
import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class OutageSnapshotContractTest {
    @Test fun ukpnKeepsOptionalGeometryAndToleratesMissingPoints() {
        val events = UkpnMapper(Json).parse("""{"results":[
            {"incidentreference":"a","geopoint":{"lat":51.5,"lon":-0.1}},
            {"incidentreference":"b","geopoint":null}
        ]}""")
        assertEquals(GeoPoint(51.5, -0.1), events.first().point)
        assertNull(events.last().point)
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val now = Instant.parse("2026-11-16T17:00:00Z")
    private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()

    @Test fun capturedUkpnResponseKeepsPlannedKindEstimateAndCustomerCount() {
        val events = UkpnMapper(json).parse(fixture("ukpn-live-faults-sample.json"))
        assertEquals(1, events.size) // The other record is already restored.
        val source = OutageSource("ukpn", Country.UNITED_KINGDOM, "UKPN", "", "Europe/London", SourceAccess.LIVE)
        val snapshot = OutageSnapshotMapper.international(events, source, now, "Mid Kent") as OutageSnapshot.Timeline
        val interval = snapshot.intervals.single()
        assertEquals(OutageKind.PLANNED, interval.kind)
        assertEquals(Instant.parse("2026-11-16T09:00:00Z"), interval.start)
        assertEquals(Instant.parse("2026-11-16T16:00:00Z"), interval.end)
        assertTrue(interval.endIsEstimate)
        assertEquals(5, interval.affectedCustomers)
        assertEquals(LocationMatch.AREA, snapshot.locationMatch)
        assertEquals(NowStatus.Unknown, snapshot.nowStatus(now))
    }

    @Test fun capturedOdinResponseIsCountOnlyAndNeverBecomesATimeline() {
        val events = OdinMapper(json).parse(fixture("odin-county-sample.json"))
        assertTrue(events.isNotEmpty())
        assertTrue(events.all { it.isAreaSummary && it.start == null && it.end == null })
        val source = OutageSource("odin", Country.UNITED_STATES, "ODIN", "", "America/Los_Angeles", SourceAccess.LIVE)
        val snapshot = OutageSnapshotMapper.international(events, source, now, "Los Angeles") as OutageSnapshot.AreaSummary
        assertEquals(events.sumOf { it.affectedCustomers!! }, snapshot.customersWithoutPower)
        assertNull(snapshot.percentWithoutPower)
    }
}
