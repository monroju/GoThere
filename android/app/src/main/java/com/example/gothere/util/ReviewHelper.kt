package com.example.gothere.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manages in-app review prompts using Google Play's ReviewManager API.
 * Triggers a review prompt after the user completes a threshold of tasks.
 */
object ReviewHelper {
    private const val TAG = "ReviewHelper"
    private const val PREFS_NAME = "review_prefs"
    private const val KEY_COMPLETED_COUNT = "completed_task_count"
    private const val KEY_REVIEW_REQUESTED = "review_requested"
    private const val TASK_THRESHOLD = 5

    /** Call this each time a user completes a task. */
    fun onTaskCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REVIEW_REQUESTED, false)) return

        val count = prefs.getInt(KEY_COMPLETED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COMPLETED_COUNT, count).apply()

        if (count >= TASK_THRESHOLD && context is Activity) {
            requestReview(context)
        }
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    // Mark as requested regardless of outcome (Google throttles this)
                    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_REVIEW_REQUESTED, true)
                        .apply()
                    Log.d(TAG, "Review flow completed")
                }
            } else {
                Log.w(TAG, "Review flow request failed", task.exception)
            }
        }
    }
}
