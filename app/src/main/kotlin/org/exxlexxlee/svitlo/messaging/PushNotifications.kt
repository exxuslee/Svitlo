package org.exxlexxlee.svitlo.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.exxlexxlee.svitlo.MainActivity
import org.exxlexxlee.svitlo.R

class PushNotifications(private val context: Context) {
    private val channelId = context.getString(R.string.push_channel_id)

    fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.push_channel_description) },
        )
    }

    fun show(message: PushMessage) {
        if (message.title.isNullOrBlank() && message.body.isNullOrBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        createChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title ?: context.getString(R.string.app_name))
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        // A separate tag namespace prevents collisions with local outage reminders.
        context.getSystemService(NotificationManager::class.java)
            .notify("push:${message.tag ?: java.util.UUID.randomUUID()}", 0, notification)
    }
}
