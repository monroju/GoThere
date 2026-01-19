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

    fun isImported(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IMPORTED, false)
    }

    fun markImported(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IMPORTED, true).apply()
    }
}
