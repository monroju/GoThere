package com.example.gothere.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("gothere_prefs")

class ThemePreferences(private val context: Context) {
    companion object {
        private val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val themeFlow = context.dataStore.data.map { it[DARK_THEME] ?: true } // default = dark

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = value }
    }
}
