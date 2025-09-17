package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotesViewModel : ViewModel() {
    private val repo = NotesRepository()

    // Expose Flow (not StateFlow) to match the repository
    val note: Flow<String> = repo.noteFlow

    fun save(text: String) {
        viewModelScope.launch { repo.save(text) }
    }
}
