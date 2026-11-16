package org.exxlexxlee.data.mapper.ukrainemonitor

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.PowerState
import org.junit.Assert.*
import org.junit.Test

class UkraineMonitorMapperTest {
    private val mapper = UkraineMonitorMapper(Json { ignoreUnknownKeys = true })
    private val clock = object : Clock { override fun now() = Instant.parse("2026-11-16T10:00:00Z") }
    private fun sample(emergency: Boolean = false) = """
        {"date_today":"2026-11-16","regions":[
          {"cpu":"cherkaska-oblast","emergency":$emergency,"schedule":{"1.1":{
            "2026-11-16":{"00:00":1,"00:30":2},"2026-11-17":{"00:00":2}}}},
          {"cpu":"other","emergency":false,"schedule":{"1.1":{"2026-11-16":{"00:00":2}}}}
        ]}
    """.trimIndent()

    @Test fun selectsRegionAndPreservesSavedGpvGroupIdsAndMissingSlots() {
        val payload = mapper.decode(sample(), "cherkaska-oblast")
        assertEquals(listOf("1.1"), mapper.groups(payload).map { it.id })
        val group = OutageGroup("GPV1.1", "Черга 1.1")
        val schedule = mapper.toSchedule(payload, group, clock)
        assertEquals(group, schedule.group)
        assertEquals(listOf(PowerState.ON, PowerState.OFF, PowerState.UNKNOWN), schedule.today.halfHours.take(3))
        assertEquals(LocalDate(2026, 11, 17), schedule.tomorrow!!.date)
        assertEquals(PowerState.OFF, schedule.tomorrow!!.halfHours.first())
        assertNull(schedule.sourceUpdatedAt)
    }

    @Test fun emergencyAndStaleDaysNeverProvePowerAvailability() {
        val emergency = mapper.toSchedule(mapper.decode(sample(true), "cherkaska-oblast"), OutageGroup("1.1", "1.1"), clock)
        assertTrue(emergency.schedulesSuspended)
        assertFalse(emergency.today.published)
        assertTrue(emergency.today.halfHours.all { it == PowerState.UNKNOWN })
        val later = object : Clock { override fun now() = Instant.parse("2026-11-18T10:00:00Z") }
        val stale = mapper.toSchedule(mapper.decode(sample(), "cherkaska-oblast"), OutageGroup("1.1", "1.1"), later)
        assertEquals(LocalDate(2026, 11, 18), stale.today.date)
        assertFalse(stale.today.published)
        assertTrue(stale.today.halfHours.all { it == PowerState.UNKNOWN })
        assertNull(stale.tomorrow)
    }

    @Test fun schemaChangesAndMissingRegionsFailExplicitly() {
        assertThrows(Exception::class.java) { mapper.decode("<html>Error</html>", "cherkaska-oblast") }
        assertThrows(IllegalArgumentException::class.java) { mapper.decode(sample(), "missing") }
        assertThrows(IllegalArgumentException::class.java) { mapper.decode(sample().replace("\"00:30\":2", "\"00:31\":2"), "cherkaska-oblast") }
        assertThrows(IllegalArgumentException::class.java) { mapper.decode(sample().replace("\"00:30\":2", "\"00:30\":9"), "cherkaska-oblast") }
    }
}
