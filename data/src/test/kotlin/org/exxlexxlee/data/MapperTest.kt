package org.exxlexxlee.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.PowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-08T10:00:00Z")
    }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val gpvMapper = GpvMapper(json)
    private val yasnoMapper = YasnoMapper(json)

    @Test
    fun gpvKyivRegionMapsFortyEightHalfHours() {
        val dto = gpvMapper.decode(readResource("gpv-kyiv-region-sample.json"))
        val schedule = gpvMapper.toSchedule(dto, OutageGroup("GPV3.1", "Черга 3.1"), clock)

        assertEquals(48, schedule.today.halfHours.size)
        assertEquals(PowerState.ON, schedule.today.halfHours[0])
        assertEquals(PowerState.ON, schedule.today.halfHours[1])
        assertEquals(PowerState.OFF, schedule.today.halfHours[2])
        assertEquals(PowerState.ON, schedule.today.halfHours[3])
        assertEquals(PowerState.OFF, schedule.today.halfHours[4])
        assertEquals(PowerState.OFF, schedule.today.halfHours[5])
        assertEquals(PowerState.ON, schedule.today.halfHours[6])
        assertEquals(PowerState.OFF, schedule.today.halfHours[7])
        assertEquals(PowerState.MAYBE, schedule.today.halfHours[8])
        assertEquals(PowerState.MAYBE, schedule.today.halfHours[10])
        assertEquals(PowerState.ON, schedule.today.halfHours[11])
        assertEquals(PowerState.ON, schedule.today.halfHours[12])
        assertEquals(PowerState.MAYBE, schedule.today.halfHours[13])
        assertNotNull(schedule.tomorrow)
        assertEquals(PowerState.OFF, schedule.tomorrow!!.halfHours[0])
        assertEquals("Черга 3.1", schedule.group.name)
        assertEquals(Instant.parse("2026-11-08T09:22:10.774Z"), schedule.sourceUpdatedAt)
    }

    @Test
    fun yasnoKyivMapsSlotsOntoHalfHours() {
        val payload = yasnoMapper.decode(readResource("yasno-kyiv-sample.json"))
        val schedule = yasnoMapper.toSchedule(payload, OutageGroup("3.1", "3.1"))

        assertEquals(48, schedule.today.halfHours.size)
        assertEquals(PowerState.OFF, schedule.today.halfHours[0])
        assertEquals(PowerState.OFF, schedule.today.halfHours[3])
        assertEquals(PowerState.ON, schedule.today.halfHours[4])
        assertEquals(PowerState.MAYBE, schedule.today.halfHours[6])
        assertEquals(PowerState.ON, schedule.today.halfHours[7])
        assertEquals(48, schedule.tomorrow!!.halfHours.size)
        assertEquals(PowerState.ON, schedule.tomorrow!!.halfHours[0])
        assertEquals(2026, schedule.today.date.year)
        assertEquals(11, schedule.today.date.monthNumber)
        assertEquals(8, schedule.today.date.dayOfMonth)
        assertEquals(9, schedule.tomorrow!!.date.dayOfMonth)
        assertTrue(schedule.today.published)
        assertTrue(schedule.tomorrow!!.published)
        assertEquals(false, schedule.schedulesSuspended)
    }

    @Test
    fun yasnoEmergencyWithEmptySlotsIsAllOn() {
        val payload = yasnoMapper.decode(
            """
            {"3.1":{"today":{"slots":[],"date":"2026-11-08T00:00:00+02:00","status":"EmergencyShutdowns"},
            "tomorrow":{"slots":[],"date":"2026-11-09T00:00:00+02:00","status":"EmergencyShutdowns"},
            "updatedOn":"2026-11-08T08:55:54+00:00"}}
            """.trimIndent(),
        )
        val schedule = yasnoMapper.toSchedule(payload, OutageGroup("3.1", "3.1"))
        assertTrue(schedule.today.halfHours.all { it == PowerState.ON })
        assertTrue(schedule.tomorrow!!.halfHours.all { it == PowerState.ON })
        assertTrue(schedule.schedulesSuspended)
        assertEquals(false, schedule.today.published)
        assertEquals(false, schedule.tomorrow!!.published)
    }

    @Test
    fun gpvUsesActualDatesWhenSourceTodayPointerStillPointsToYesterday() {
        val dto = gpvMapper.decode(readResource("gpv-kyiv-region-sample.json"))
        val fact = dto.fact!!
        val dayKeys = fact.data.keys.sortedBy { it.toLong() }
        val currentDay = dayKeys.last()
        val nextDay = (currentDay.toLong() + 86_400).toString()
        val withThreeDays = dto.copy(fact = fact.copy(
            data = fact.data + (nextDay to fact.data.getValue(dayKeys.first())),
        ))
        val afterMidnight = object : Clock {
            override fun now() = Instant.parse("2026-11-08T22:00:00Z")
        }
        val schedule = gpvMapper.toSchedule(withThreeDays, OutageGroup("GPV3.1", "3.1"), afterMidnight)
        assertEquals(LocalDate(2026, 11, 9), schedule.today.date)
        assertEquals(PowerState.OFF, schedule.today.halfHours.first())
        assertEquals(LocalDate(2026, 11, 10), schedule.tomorrow!!.date)
        assertEquals(PowerState.ON, schedule.tomorrow!!.halfHours.first())
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
