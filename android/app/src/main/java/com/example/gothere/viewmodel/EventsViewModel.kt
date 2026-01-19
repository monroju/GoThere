package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.EventItem
import com.example.gothere.model.Task
import com.example.gothere.repository.EventsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {
    private val repo = EventsRepository()

    // Live stream of upcoming events
    val events: StateFlow<List<EventItem>> =
        repo.eventsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, dateMillis: Long) = viewModelScope.launch {
        repo.add(title, dateMillis)
    }

    fun addFromTask(task: Task) = viewModelScope.launch {
        repo.addFromTask(task)
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
