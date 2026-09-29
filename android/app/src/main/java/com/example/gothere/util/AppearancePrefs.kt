package com.example.gothere.util

import android.content.Context

/**
 * App appearance: follow the phone ("system"), or pin "light" / "dark".
 * Plain SharedPreferences so the first frame already has the right theme.
 */
object AppearancePrefs {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFS = "appearance_prefs"
    private const val KEY = "appearance"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
    }
}
