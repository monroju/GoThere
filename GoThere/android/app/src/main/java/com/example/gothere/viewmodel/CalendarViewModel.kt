// app/src/main/java/com/example/gothere/viewmodel/CalendarViewModel.kt
package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.EventItem
import com.example.gothere.repository.EventsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel : ViewModel() {
    private val repo = EventsRepository()

    // Live upcoming events list for the UI
    val events: StateFlow<List<EventItem>> =
        repo.eventsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, dateMillis: Long) = viewModelScope.launch {
        repo.add(title, dateMillis)
    }

    fun updateTitle(id: String, title: String) = viewModelScope.launch {
        repo.updateTitle(id, title)
    }

    fun updateDate(id: String, dateMillis: Long) = viewModelScope.launch {
        repo.updateDate(id, dateMillis)
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id)
    }
}
