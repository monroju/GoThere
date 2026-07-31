package com.example.gothere.notify

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging

object FcmTopicManager {
    const val TOPIC_US_POLICY_ALERTS = "us_policy_alerts"
    private const val PREFS = "fcm_prefs"
    private const val KEY_SUBSCRIBED_US_POLICY = "subscribed_us_policy_alerts"

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
}
