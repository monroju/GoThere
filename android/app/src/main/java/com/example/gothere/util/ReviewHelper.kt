package com.example.gothere.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/** Moments where the user just got a useful answer. Mirrors iOS ReviewMilestone. */
enum class ReviewMilestone(val key: String) {
    ANCESTRY_LIKELY_ELIGIBLE("rp_ancestry_likely_eligible"),
    WIZARD_COMPLETED("rp_wizard_completed"),
}

/**
 * Manages in-app review prompts using Google Play's ReviewManager API.
 * Asks right after the first positive result (eligible verdict, saved checklist),
 * at most once per 120 days. Google applies its own quota on top.
 */
object ReviewHelper {
    private const val TAG = "ReviewHelper"
    private const val PREFS_NAME = "review_prefs"
    private const val KEY_LAST_PROMPT = "rp_last_prompt_at"
    private const val MIN_MILLIS_BETWEEN = 120L * 24 * 60 * 60 * 1000

    /** Call when the user reaches [milestone]. Each milestone counts once. */
    fun onMilestone(context: Context, milestone: ReviewMilestone) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(milestone.key, false)) return
        prefs.edit().putBoolean(milestone.key, true).apply()

        val last = prefs.getLong(KEY_LAST_PROMPT, 0L)
        if (last > 0 && System.currentTimeMillis() - last < MIN_MILLIS_BETWEEN) return

        val activity = context.findActivity() ?: return
        requestReview(activity)
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                Analytics.log(Analytics.REVIEW_PROMPT_REQUESTED)
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    // Mark as requested regardless of outcome (Google throttles this)
                    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
                        .apply()
                    Log.d(TAG, "Review flow completed")
                }
            } else {
                Log.w(TAG, "Review flow request failed", task.exception)
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
