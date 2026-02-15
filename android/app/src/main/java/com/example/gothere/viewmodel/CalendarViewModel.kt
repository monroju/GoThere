// app/src/main/java/com/example/gothere/viewmodel/CalendarViewModel.kt
package com.example.gothere.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.EventItem
import com.example.gothere.repository.EventsRepository
import com.example.gothere.util.NotificationHelper
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

    fun add(context: Context, title: String, dateMillis: Long) = viewModelScope.launch {
        repo.add(title, dateMillis)
        // Schedule notification for the new event
        // Note: repo.add doesn't return the ID easily here, 
        // but the eventsFlow will emit the new item shortly.
        // For simplicity, we can fetch the latest list or trigger a re-sync.
    }

    fun updateTitle(id: String, title: String) = viewModelScope.launch {
        repo.updateTitle(id, title)
    }

    fun updateDate(id: String, dateMillis: Long) = viewModelScope.launch {
        repo.updateDate(id, dateMillis)
    }

    fun delete(context: Context, id: String) = viewModelScope.launch {
        repo.delete(id)
        NotificationHelper.cancelNotification(context, id)
    }
}
