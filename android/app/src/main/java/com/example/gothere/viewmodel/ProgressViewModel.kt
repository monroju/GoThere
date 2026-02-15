package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Task
import com.example.gothere.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel : ViewModel() {
    private val repo = TaskRepository()
    val tasks: StateFlow<List<Task>> =
        repo.tasksFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
