package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Task
import com.example.gothere.repository.EventsRepository
import com.example.gothere.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TasksViewModel : ViewModel() {
    private val repo = TaskRepository()
    private val events = EventsRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // NEW: filter for "due this week"
    private val _dueThisWeek = MutableStateFlow(false)
    val dueThisWeek: StateFlow<Boolean> = _dueThisWeek

    private val tasks: StateFlow<List<Task>> =
        repo.tasksFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filtered: StateFlow<List<Task>> =
        combine(tasks, query, dueThisWeek) { list, q, onlyWeek ->
            val qn = q.trim().lowercase()
            val searched = if (qn.isEmpty()) list else list.filter { t ->
                t.title.lowercase().contains(qn) ||
                (t.description ?: "").lowercase().contains(qn) ||
                (t.links ?: emptyList()).any { it.label.lowercase().contains(qn) }
            }
            if (!onlyWeek) searched else {
                val (start, end) = weekBounds()
                searched.filter { it.dueAt != null && it.dueAt!! in start..end }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { repo.seedIfEmpty() } }

    fun setSearch(v: String) { _query.value = v }
    fun toggleWeek(checked: Boolean) { _dueThisWeek.value = checked }

    fun toggle(task: Task) = viewModelScope.launch {
        repo.toggleCompleted(task.id.orEmpty(), !task.completed)
    }

    fun setDue(task: Task, dateMillisUtcMidnight: Long?) = viewModelScope.launch {
        repo.setDue(task.id.orEmpty(), dateMillisUtcMidnight)
    }

    fun addToCalendar(task: Task) = viewModelScope.launch {
        events.addFromTask(task)
    }

    // ---- helpers ----
    private fun weekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 7); cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return start to end
    }
}
