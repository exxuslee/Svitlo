package org.exxlexxlee.svitlo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.Place
import kotlin.math.abs

object NotificationScheduler {
    private const val PREFS_NAME = "svitlo_notification_prefs"
    private const val KEY_SCHEDULED_IDS = "scheduled_notification_ids"

    fun scheduleNotifications(
        context: Context,
        placeSchedulePairs: List<Pair<Place, OutageSchedule?>>,
        clock: Clock = Clock.System,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                as? AlarmManager ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousScheduledIds = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet())
            ?: emptySet()

        val newScheduledIds = mutableSetOf<String>()
        val nowMs = clock.now().toEpochMilliseconds()

        placeSchedulePairs.forEach { (place, schedule) ->
            if (schedule == null) return@forEach
            val details = place.selection.details ?: return@forEach
            val reminderMinutes = details.reminderMinutes
            if (reminderMinutes <= 0) return@forEach // 0 = Ніколи

            val quietNight = details.quietNight
            val placeName = details.name.ifBlank { place.selection.group.name }

            val days = listOfNotNull(
                schedule.today.takeIf { it.published },
                schedule.tomorrow?.takeIf { it.published },
            )

            days.forEach { daySchedule ->
                val date = daySchedule.date
                val windows = OutageWindows.windows(daySchedule.halfHours)

                windows.forEach { window ->
                    val startMinutes = window.startMinutes
                    val hour = startMinutes / 60
                    val minute = startMinutes % 60

                    val startLocalDateTime = LocalDateTime(
                        year = date.year,
                        monthNumber = date.monthNumber,
                        dayOfMonth = date.dayOfMonth,
                        hour = hour,
                        minute = minute,
                        second = 0,
                        nanosecond = 0,
                    )
                    val startInstant = startLocalDateTime.toInstant(KyivTime.ZONE)
                    val startEpochMs = startInstant.toEpochMilliseconds()

                    val triggerEpochMs = startEpochMs - reminderMinutes * 60 * 1000L

                    if (triggerEpochMs > nowMs) {
                        // Check quiet night (23:00 to 07:00)
                        val triggerInstant = Instant.fromEpochMilliseconds(triggerEpochMs)
                        val triggerHour = triggerInstant.toLocalDateTime(KyivTime.ZONE).hour
                        if (quietNight && (triggerHour !in 7..<23)) {
                            return@forEach
                        }

                        val timeFormatted = OutageWindows.formatClock(startMinutes)
                        val notificationId =
                            abs((place.id.hashCode() * 31 + startEpochMs.hashCode()))
                        val notificationIdStr = notificationId.toString()

                        newScheduledIds.add(notificationIdStr)

                        val intent = Intent(context, OutageNotificationReceiver::class.java).apply {
                            putExtra(OutageNotificationReceiver.EXTRA_PLACE_NAME, placeName)
                            putExtra(OutageNotificationReceiver.EXTRA_OUTAGE_TIME, timeFormatted)
                            putExtra(
                                OutageNotificationReceiver.EXTRA_REMINDER_MINUTES,
                                reminderMinutes
                            )
                            putExtra(
                                OutageNotificationReceiver.EXTRA_NOTIFICATION_ID,
                                notificationId
                            )
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            notificationId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )

                        scheduleAlarm(alarmManager, triggerEpochMs, pendingIntent)
                    }
                }
            }
        }

        // Cancel alarms that are no longer scheduled
        val idsToCancel = previousScheduledIds - newScheduledIds
        idsToCancel.forEach { idStr ->
            val id = idStr.toIntOrNull() ?: return@forEach
            val intent = Intent(context, OutageNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        prefs.edit { putStringSet(KEY_SCHEDULED_IDS, newScheduledIds) }
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerEpochMs: Long,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        }
    }
}
