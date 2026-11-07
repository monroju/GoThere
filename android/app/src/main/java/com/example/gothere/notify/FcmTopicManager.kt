package com.example.gothere.notify

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging

object FcmTopicManager {
    const val TOPIC_US_POLICY_ALERTS = "us_policy_alerts"
    private const val PREFS = "fcm_prefs"
    private const val KEY_SUBSCRIBED_US_POLICY = "subscribed_us_policy_alerts"
    private const val KEY_RULE_TOPIC = "rule_alert_topic"

    /** Destinations with verified rule-change alerts. Topic is `rules_<code>`;
     *  the sender is shared/alerts/alerts.py in the parent repo. */
    val RULE_ALERT_CODES = mapOf(
        "spain" to "es", "portugal" to "pt", "ireland" to "ie", "italy" to "it", "mexico" to "mx",
    )

    fun ruleAlertTopic(countryId: String): String? = RULE_ALERT_CODES[countryId]?.let { "rules_$it" }

    fun subscribeToUSPolicyAlertsIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SUBSCRIBED_US_POLICY, false)) return
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_US_POLICY_ALERTS)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.edit().putBoolean(KEY_SUBSCRIBED_US_POLICY, true).apply()
                }
            }
    }

    private const val KEY_RULE_TOPICS = "rule_alert_topics"
    private const val KEY_DESTINATION = "rule_alert_destination"

    /**
     * Rule-alert topics the user should hold. Free: one country, the first tracked Plan B
     * path with an alert list, otherwise the selected destination. All-Access: destination
     * plus every tracked path. Pure so it is unit tested; mirrors iOS desiredRuleTopics.
     */
    fun desiredRuleTopics(destinationId: String, planBCodes: List<String>, allAccess: Boolean): List<String> {
        val available = RULE_ALERT_CODES.values.toSet()
        val planB = planBCodes.filter { it in available }.map { "rules_$it" }
        val destination = ruleAlertTopic(destinationId)
        val out = when {
            allAccess -> listOfNotNull(destination) + planB
            planB.isNotEmpty() -> listOf(planB.first())
            else -> listOfNotNull(destination)
        }
        return out.distinct()
    }

    /** Destination changed. Remembers it, then recomputes the whole topic set. */
    fun syncRuleAlertTopic(context: Context, countryId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DESTINATION, countryId).apply()
        syncRuleAlertTopics(context)
    }

    /** Subscribe and unsubscribe so FCM holds exactly desiredRuleTopics.
     *  Topics need no permission; delivery still follows POST_NOTIFICATIONS. */
    fun syncRuleAlertTopics(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val destination = prefs.getString(KEY_DESTINATION, null) ?: "spain"
        val wanted = desiredRuleTopics(
            destinationId = destination,
            planBCodes = com.example.gothere.planb.PlanBStore.get(context).alertCountryCodes,
            allAccess = com.example.gothere.billing.PurchaseManager.getInstance(context).hasAllAccess(),
        )
        val current = (prefs.getStringSet(KEY_RULE_TOPICS, null) ?: emptySet()).toMutableSet()
        // Migrate the single-topic key from 1.14.x.
        prefs.getString(KEY_RULE_TOPIC, null)?.let {
            current += it
            prefs.edit().remove(KEY_RULE_TOPIC).apply()
        }
        val messaging = FirebaseMessaging.getInstance()
        (current - wanted.toSet()).forEach { messaging.unsubscribeFromTopic(it) }
        prefs.edit().putStringSet(KEY_RULE_TOPICS, current.intersect(wanted.toSet())).apply()
        wanted.filter { it !in current }.forEach { topic ->
            messaging.subscribeToTopic(topic).addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val held = (prefs.getStringSet(KEY_RULE_TOPICS, null) ?: emptySet()) + topic
                prefs.edit().putStringSet(KEY_RULE_TOPICS, held).apply()
            }
        }
    }
}
