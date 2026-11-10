package org.exxlexxlee.svitlo.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class SvitloMessagingService : FirebaseMessagingService() {
    private val pushNotifications: PushNotifications by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        // Background notification messages are displayed by the FCM SDK itself.
        // Foreground notifications and title/body data messages are displayed here.
        val push = PushMessage.fromPayload(
            message.notification?.title,
            message.notification?.body,
            message.notification?.tag,
            message.data,
            message.messageId,
        ) ?: return
        pushNotifications.show(push)
    }
}
