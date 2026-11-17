package org.exxlexxlee.svitlo.widget.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.exxlexxlee.svitlo.widget.ScheduleWidgetReceiver
import org.exxlexxlee.svitlo.widget.installedScheduleWidgetIds

internal const val ACTION_WIDGET_TIME = "org.exxlexxlee.svitlo.widget.UPDATE_TIME"

/** A PendingIntent survives process death; periodic widget updates are not aligned to the clock. */
internal object WidgetTimeUpdates {
    @Synchronized
    fun schedule(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val operation = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ScheduleWidgetReceiver::class.java).setAction(ACTION_WIDGET_TIME),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ids = installedScheduleWidgetIds(context)
        if (ids.isEmpty()) {
            manager.cancel(operation)
            return
        }
        val triggerAt = nextWidgetTimeUpdate(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                return
            } catch (_: SecurityException) {
                // Access can be revoked between the check and scheduling.
            }
        }
        // Android may delay this fallback when exact alarm access is unavailable.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }
}
