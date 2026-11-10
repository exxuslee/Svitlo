package org.exxlexxlee.svitlo.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushMessageTest {
    @Test
    fun notificationFieldsTakePrecedenceOverData() {
        val message = PushMessage.fromPayload(
            "Notification title", "Notification body", "tag",
            mapOf("title" to "Data title", "body" to "Data body"), "message-id",
        )

        assertEquals(PushMessage("Notification title", "Notification body", "tag"), message)
    }

    @Test
    fun missingAndBlankNotificationFieldsFallBackIndependently() {
        val message = PushMessage.fromPayload(
            " ", "Notification body", "",
            mapOf("title" to "Data title", "body" to "Data body"), "message-id",
        )

        assertEquals(PushMessage("Data title", "Notification body", "message-id"), message)
    }

    @Test
    fun dataOnlyMessageMayHaveOnlyBodyAndNoTag() {
        val message = PushMessage.fromPayload(null, null, null, mapOf("body" to "Body"), null)

        assertEquals(PushMessage(null, "Body", null), message)
    }

    @Test
    fun titleOnlyMessageIsDisplayed() {
        val message = PushMessage.fromPayload("Title", null, null, emptyMap(), "message-id")

        assertEquals(PushMessage("Title", null, "message-id"), message)
    }

    @Test
    fun emptyPayloadAndUnknownDataAreIgnored() {
        assertNull(PushMessage.fromPayload(null, null, null, emptyMap(), "message-id"))
        assertNull(PushMessage.fromPayload(" ", "\n", null, mapOf("body" to " "), null))
        assertNull(PushMessage.fromPayload(null, null, null, mapOf("url" to "https://example.test"), null))
    }
}
