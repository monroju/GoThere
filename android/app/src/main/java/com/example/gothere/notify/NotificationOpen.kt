package com.example.gothere.notify

import android.content.Intent
import com.example.gothere.util.Analytics

/**
 * `notification_opened`: fired when MainActivity is launched from a notification.
 * Local notifications put [EXTRA_TYPE] / [EXTRA_ID] on their content intent. FCM
 * notification messages shown by the system while the app is in the background put
 * `google.message_id` and `from` on the launch intent. Only an internal id and the
 * kind of notification are logged, never the notification text. Same event as iOS.
 */
object NotificationOpen {
    const val EXTRA_TYPE = "notif_type"
    const val EXTRA_ID = "notif_id"

    const val TYPE_TASK_REMINDER = "task_reminder"
    const val TYPE_CALENDAR_EVENT = "calendar_event"
    const val TYPE_CALENDAR_NOTE = "calendar_note"
    const val TYPE_WEEKLY_DIGEST = "weekly_digest"

    private const val FCM_MESSAGE_ID = "google.message_id"
    private const val FCM_FROM = "from"
    private const val TOPICS = "/topics/"

    /** Properties for the event, or null when the launch did not come from a notification. */
    fun properties(extras: Map<String, String?>): Map<String, Any>? {
        val localType = extras[EXTRA_TYPE]
        if (!localType.isNullOrEmpty()) {
            val props = mutableMapOf<String, Any>(
                "type" to localType.take(40),
                "source" to "local",
                "platform" to "android"
            )
            extras[EXTRA_ID]?.takeIf { it.isNotEmpty() }?.let { props["id"] = it.take(64) }
            return props
        }
        val messageId = extras[FCM_MESSAGE_ID]
        if (messageId.isNullOrEmpty()) return null
        val topic = extras[FCM_FROM]
            ?.takeIf { it.startsWith(TOPICS) }
            ?.removePrefix(TOPICS)
            ?.takeIf { it.isNotEmpty() }
        val explicit = extras["type"]?.takeIf { it.isNotEmpty() }
        val type = when {
            explicit != null -> explicit.take(40)
            topic != null && topic.startsWith("rules_") -> "rule_alert"
            topic == FcmTopicManager.TOPIC_US_POLICY_ALERTS -> "us_policy_alert"
            else -> "push"
        }
        val props = mutableMapOf<String, Any>(
            "type" to type,
            "source" to "push",
            "platform" to "android",
            "id" to messageId.take(64)
        )
        topic?.let { props["topic"] = it }
        return props
    }

    /** Logs the event if [intent] came from a notification. Safe to call with any intent. */
    fun track(intent: Intent?) {
        val bundle = intent?.extras ?: return
        val extras = runCatching {
            bundle.keySet().associateWith { key ->
                @Suppress("DEPRECATION")
                bundle.get(key)?.toString()
            }
        }.getOrNull() ?: return
        val props = properties(extras) ?: return
        Analytics.log(Analytics.NOTIFICATION_OPENED, props)
        // Consume so a later onNewIntent or re-delivery does not log the same open twice.
        intent.removeExtra(EXTRA_TYPE)
        intent.removeExtra(FCM_MESSAGE_ID)
    }
}
