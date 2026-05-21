package com.example.gothere.repository

import android.content.Context

/**
 * Tracks whether the seed JSON import has been performed on this device install.
 *
 * - Per-install (SharedPreferences), not per-user
 * - If you ever want per-user behavior, include UID in the key
 */
object SeedImportStore {

    private const val PREFS = "seed_import_store"
    private const val KEY_IMPORTED = "tasks_seed_imported_v1"
    private const val KEY_DEDUPED = "tasks_deduped_v1"

    fun isImported(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IMPORTED, false)
    }

    fun markImported(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IMPORTED, true).apply()
    }

    /** True once the one-shot duplicate-task cleanup has run for this install. */
    fun isDeduped(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEDUPED, false)
    }

    fun markDeduped(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEDUPED, true).apply()
    }

    /**
     * Tracks per-country seed import so each country's tasks get imported the
     * first time the user opens that country's Tasks tab — Spain still uses the
     * legacy KEY_IMPORTED flag above for backwards compatibility.
     */
    fun isCountryImported(context: Context, countryId: String): Boolean {
        if (countryId == "spain") return isImported(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean("country_imported_$countryId", false)
    }

    fun markCountryImported(context: Context, countryId: String) {
        if (countryId == "spain") { markImported(context); return }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("country_imported_$countryId", true).apply()
    }
}
