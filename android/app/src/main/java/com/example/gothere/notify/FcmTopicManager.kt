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

    /** Keep exactly one `rules_<code>` topic, matching the selected destination.
     *  Topics need no permission; delivery still follows POST_NOTIFICATIONS. */
    fun syncRuleAlertTopic(context: Context, countryId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wanted = ruleAlertTopic(countryId)
        val current = prefs.getString(KEY_RULE_TOPIC, null)
        if (wanted == current) return
        val messaging = FirebaseMessaging.getInstance()
        if (current != null) {
            messaging.unsubscribeFromTopic(current)
            prefs.edit().remove(KEY_RULE_TOPIC).apply()
        }
        if (wanted == null) return
        messaging.subscribeToTopic(wanted).addOnCompleteListener { task ->
            if (task.isSuccessful) prefs.edit().putString(KEY_RULE_TOPIC, wanted).apply()
        }
    }
}
