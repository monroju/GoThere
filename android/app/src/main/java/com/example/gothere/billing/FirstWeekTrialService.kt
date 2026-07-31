package com.example.gothere.billing

import android.content.Context
import kotlin.math.ceil

object FirstWeekTrialService {
    const val SAMPLE_COUNTRY_ID = "portugal"
    private const val PREFS = "first_week_trial"
    private const val KEY_INSTALLED_AT = "installed_at_ms"
    private const val TRIAL_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    fun bootstrap(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_INSTALLED_AT, 0L) == 0L) {
            prefs.edit().putLong(KEY_INSTALLED_AT, System.currentTimeMillis()).apply()
        }
    }

    fun isActive(context: Context): Boolean {
        val installed = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_INSTALLED_AT, 0L)
        if (installed == 0L) return false
        return System.currentTimeMillis() - installed < TRIAL_DAYS_MS
    }

    fun daysRemaining(context: Context): Int {
        val installed = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_INSTALLED_AT, 0L)
        if (installed == 0L) return 0
        val msLeft = TRIAL_DAYS_MS - (System.currentTimeMillis() - installed)
        return if (msLeft <= 0) 0 else ceil(msLeft.toDouble() / (24 * 60 * 60 * 1000)).toInt()
    }

    fun unlocksCountry(context: Context, countryId: String): Boolean {
        return isActive(context) && countryId == SAMPLE_COUNTRY_ID
    }
}
