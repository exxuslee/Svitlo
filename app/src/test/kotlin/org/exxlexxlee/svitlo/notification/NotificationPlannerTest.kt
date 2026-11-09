package org.exxlexxlee.svitlo.notification

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.PlaceSchedule
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NotificationPlannerTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-09T08:00:00Z")
    }
    private val place = Place(
        id = 1,
        selection = Selection.ByGroup(
            region = Region("kyiv", "Київ", "yasno"),
            group = OutageGroup("1", "Черга 1"),
            details = AddressDetails("Дім", reminderMinutes = 15),
        ),
    )

    @Test
    fun plansOneReminderPerOutageAndPreservesAlarmId() {
        val schedule = schedule(day(9, 28, 29, 30, 31))
        val notification =
            NotificationPlanner.plan(listOf(PlaceSchedule(place, schedule)), clock).single()

        assertEquals("Дім", notification.placeName)
        assertEquals("14:00", notification.outageTime)
        assertEquals(15, notification.reminderMinutes)
        assertEquals(
            Instant.parse("2026-11-09T11:45:00Z").toEpochMilliseconds(),
            notification.triggerEpochMs
        )
        val outageMs = Instant.parse("2026-11-09T12:00:00Z").toEpochMilliseconds()
        assertEquals(abs(place.id.hashCode() * 31 + outageMs.hashCode()), notification.id)
    }

    @Test
    fun ignoresPastRemindersAndUnpublishedDays() {
        val schedule = schedule(day(9, 16, 20), day(10, 28).copy(published = false))
        assertTrue(
            NotificationPlanner.plan(listOf(PlaceSchedule(place, schedule)), clock).isEmpty()
        )
    }

    @Test
    fun ignoresDisabledRemindersMissingDetailsAndMissingSchedules() {
        val selection = place.selection as Selection.ByGroup
        val disabled = place.copy(selection = selection.copy(details = AddressDetails("Дім", 0)))
        val noDetails = place.copy(selection = selection.copy(details = null))
        val schedule = schedule(day(9, 28))
        assertTrue(
            NotificationPlanner.plan(
                listOf(
                    PlaceSchedule(disabled, schedule),
                    PlaceSchedule(noDetails, schedule),
                    PlaceSchedule(place, null)
                ),
                clock,
            ).isEmpty(),
        )
    }

    @Test
    fun quietNightUsesReminderTimeAndAllowsSevenButExcludesTwentyThree() {
        val selection = place.selection as Selection.ByGroup
        val quietPlace = place.copy(
            selection = selection.copy(
                details = AddressDetails("Дім", reminderMinutes = 30, quietNight = true),
            )
        )
        // Reminders at 06:30, 07:00, 22:30 and 23:00 Kyiv time.
        val times = listOf(14, 15, 46, 47).flatMap { slot ->
            NotificationPlanner.plan(
                listOf(PlaceSchedule(quietPlace, schedule(day(10, slot)))),
                clock
            )
        }.map { it.outageTime }
        assertEquals(listOf("07:30", "23:00"), times)
    }

    @Test
    fun midnightOutageCanHaveReminderOnPreviousDayAndUsesGroupAsFallbackName() {
        val selection = place.selection as Selection.ByGroup
        val unnamed = place.copy(selection = selection.copy(details = AddressDetails(" ", 15)))
        val schedule = schedule(day(9), day(10, 0))
        val notification =
            NotificationPlanner.plan(listOf(PlaceSchedule(unnamed, schedule)), clock).single()
        assertEquals("Черга 1", notification.placeName)
        assertEquals("00:00", notification.outageTime)
        assertEquals(
            Instant.parse("2026-11-09T21:45:00Z").toEpochMilliseconds(),
            notification.triggerEpochMs
        )
    }

    @Test
    fun reminderAtCurrentInstantIsNotRescheduled() {
        val schedule = schedule(day(9, 28))
        val triggerClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-09T11:45:00Z")
        }
        assertTrue(
            NotificationPlanner.plan(listOf(PlaceSchedule(place, schedule)), triggerClock).isEmpty()
        )
    }

    private fun day(dayOfMonth: Int, vararg offSlots: Int) = DaySchedule(
        LocalDate(2026, 11, dayOfMonth),
        List(48) { if (it in offSlots) PowerState.OFF else PowerState.ON },
    )

    private fun schedule(today: DaySchedule, tomorrow: DaySchedule? = null) = OutageSchedule(
        group = place.selection.group,
        today = today,
        tomorrow = tomorrow,
        sourceUpdatedAt = null,
        fetchedAt = clock.now(),
    )
}
