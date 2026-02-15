package com.example.gothere.repository

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/**
 * Tiny SharedPreferences-backed cache:
 * key = b64(path) + ":" + updatedAt
 * val = url
 */
object FormsUrlCache {
    private const val PREFS = "forms_url_cache"

    private fun key(path: String, updatedAt: Long): String =
        Base64.encodeToString(path.toByteArray(), Base64.NO_WRAP) + ":" + updatedAt

    fun get(context: Context, path: String, updatedAt: Long): String? {
        val k = key(path, updatedAt)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(k, null)
    }

    fun put(context: Context, path: String, updatedAt: Long, url: String) {
        val k = key(path, updatedAt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(k, url)
        }
    }
}
