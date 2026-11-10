package org.exxlexxlee.svitlo.messaging

data class PushMessage(val title: String?, val body: String?, val tag: String?) {
    companion object {
        fun fromPayload(
            notificationTitle: String?,
            notificationBody: String?,
            notificationTag: String?,
            data: Map<String, String>,
            messageId: String?,
        ): PushMessage? {
            val title = notificationTitle.nonBlank() ?: data["title"].nonBlank()
            val body = notificationBody.nonBlank() ?: data["body"].nonBlank()
            if (title == null && body == null) return null
            return PushMessage(title, body, notificationTag.nonBlank() ?: messageId.nonBlank())
        }

        private fun String?.nonBlank(): String? = this?.takeUnless { it.isBlank() }
    }
}
