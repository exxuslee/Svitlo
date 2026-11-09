package org.exxlexxlee.svitlo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OutageNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME) ?: return
        val outageTime = intent.getStringExtra(EXTRA_OUTAGE_TIME) ?: return
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 15)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        NotificationHelper.showOutageNotification(
            context = context,
            placeName = placeName,
            outageTime = outageTime,
            reminderMinutes = reminderMinutes,
            notificationId = notificationId,
        )
    }

    companion object {
        const val EXTRA_PLACE_NAME = "extra_place_name"
        const val EXTRA_OUTAGE_TIME = "extra_outage_time"
        const val EXTRA_REMINDER_MINUTES = "extra_reminder_minutes"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
