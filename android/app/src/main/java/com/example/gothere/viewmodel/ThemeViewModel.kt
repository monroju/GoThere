package com.example.gothere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.data.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = ThemePreferences(app)
    val darkTheme = prefs.themeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggleTheme() = viewModelScope.launch {
        prefs.setDarkTheme(!darkTheme.value)
    }
}
