package org.exxlexxlee.svitlo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.PlaceSchedule

class NotificationScheduler(
    private val context: Context,
    private val clock: Clock,
) {
    private companion object {
        const val PREFS_NAME = "svitlo_notification_prefs"
        const val KEY_SCHEDULED_IDS = "scheduled_notification_ids"
    }

    @Synchronized
    fun scheduleNotifications(
        placeSchedules: List<PlaceSchedule>,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                as? AlarmManager ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousScheduledIds = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet())
            ?: emptySet()

        val newScheduledIds = mutableSetOf<String>()
        NotificationPlanner.plan(placeSchedules, clock).forEach { notification ->
            newScheduledIds.add(notification.id.toString())
            val intent = Intent(context, OutageNotificationReceiver::class.java).apply {
                putExtra(OutageNotificationReceiver.EXTRA_PLACE_NAME, notification.placeName)
                putExtra(OutageNotificationReceiver.EXTRA_OUTAGE_TIME, notification.outageTime)
                putExtra(
                    OutageNotificationReceiver.EXTRA_REMINDER_MINUTES,
                    notification.reminderMinutes
                )
                putExtra(OutageNotificationReceiver.EXTRA_NOTIFICATION_ID, notification.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            scheduleAlarm(alarmManager, notification.triggerEpochMs, pendingIntent)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent,
            )
        }
    }
}
