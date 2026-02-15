package com.example.gothere.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")
private val KEY_DARK = booleanPreferencesKey("is_dark")

class ThemePrefs(private val context: Context) {
    val isDarkFlow = context.dataStore.data.map { it[KEY_DARK] ?: false }
    suspend fun setDark(on: Boolean) = context.dataStore.edit { it[KEY_DARK] = on }
}
