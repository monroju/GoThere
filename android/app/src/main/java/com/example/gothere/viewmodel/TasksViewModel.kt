package com.example.gothere.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Task
import com.example.gothere.repository.EventsRepository
import com.example.gothere.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel is allowed to be constructed with or without an Android Context.
 */
class TasksViewModel(
    @Suppress("unused") private val context: Context? = null
) : ViewModel() {

    private val repo = TaskRepository()
    private val events = EventsRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _dueThisWeek = MutableStateFlow(false)
    val dueThisWeek: StateFlow<Boolean> = _dueThisWeek

    // Country filter - defaults to "spain"
    private val _countryFilter = MutableStateFlow("spain")
    val countryFilter: StateFlow<String> = _countryFilter

    /**
     * If dueThisWeek is ON, most tasks likely have dueAt == null.
     * Default true: keep undated tasks visible even when "Due this week" is enabled.
     */
    private val _includeUndatedWhenDueThisWeek = MutableStateFlow(true)
    val includeUndatedWhenDueThisWeek: StateFlow<Boolean> = _includeUndatedWhenDueThisWeek

    // All tasks from repository
    val allTasks: StateFlow<List<Task>> =
        repo.tasksFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI-filtered list (includes country filter)
    val filtered: StateFlow<List<Task>> =
        combine(allTasks, query, dueThisWeek, includeUndatedWhenDueThisWeek, countryFilter) { list, q, onlyWeek, includeUndated, country ->
            val qn = q.trim().lowercase()

            // First filter by country
            val countryFiltered = list.filter { t ->
                val taskCountry = t.countryId ?: "spain" // Default to spain for legacy tasks
                taskCountry == country
            }

            val searched = if (qn.isEmpty()) {
                countryFiltered
            } else {
                countryFiltered.filter { t ->
                    val titleHit = t.title.lowercase().contains(qn)
                    val descHit = (t.description ?: "").lowercase().contains(qn)
                    val linkHit = (t.links ?: emptyList()).any { link ->
                        val lab = (link.label ?: "").lowercase()
                        val url = (link.url ?: "").lowercase()
                        lab.contains(qn) || url.contains(qn)
                    }
                    titleHit || descHit || linkHit
                }
            }

            val result = if (!onlyWeek) {
                searched
            } else {
                val (start, end) = weekBounds()
                searched.filter { t ->
                    val d = t.dueAt
                    (d != null && d in start..end) || (includeUndated && d == null)
                }
            }

            result.sortedWith(
                compareBy<Task>(
                    { it.completed },                         // false first
                    { if (it.dueAt == null) 1 else 0 },       // dated first
                    { it.dueAt ?: Long.MAX_VALUE },           // soonest first
                    { it.title.lowercase() }
                )
            ).also {
                Log.d(
                    "TasksViewModel",
                    "all=${list.size} country=$country countryFiltered=${countryFiltered.size} searched=${searched.size} onlyWeek=$onlyWeek includeUndated=$includeUndated result=${it.size} query='$qn'"
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Intentionally empty.
        // Task seeding/import is handled once via importSeedFromAssetsIfMissing()
        // in TasksScreen after successful authentication.
        Log.d("TasksViewModel", "init: seeding handled by TasksScreen import")
    }

    fun setSearch(v: String) {
        _query.value = v
    }

    fun toggleWeek(checked: Boolean) {
        _dueThisWeek.value = checked
    }

    fun setIncludeUndatedWhenDueThisWeek(v: Boolean) {
        _includeUndatedWhenDueThisWeek.value = v
    }

    /** Set country filter */
    fun setCountryFilter(countryId: String) {
        Log.d("TasksViewModel", "setCountryFilter: $countryId")
        _countryFilter.value = countryId
    }

    fun toggle(task: Task) = viewModelScope.launch {
        val id = task.id
        if (id.isNullOrBlank()) {
            Log.w("TasksViewModel", "toggle: task has no id (seed/local item?) title='${task.title}'")
            return@launch
        }

        val res = repo.toggleCompleted(id, !task.completed)
        if (res.isFailure) {
            Log.e("TasksViewModel", "toggleCompleted FAILED id=$id", res.exceptionOrNull())
        } else {
            Log.d("TasksViewModel", "toggleCompleted OK id=$id completed=${!task.completed}")
        }
    }

    /** Update the due date in Firestore (nullable to clear it). */
    fun setDue(task: Task, dateMillisUtcMidnight: Long?) = viewModelScope.launch {
        val id = task.id
        if (id.isNullOrBlank()) {
            Log.w("TasksViewModel", "setDue: task has no id title='${task.title}'")
            return@launch
        }

        val res = repo.setDue(id, dateMillisUtcMidnight)
        if (res.isFailure) {
            Log.e("TasksViewModel", "setDue FAILED id=$id dueAt=$dateMillisUtcMidnight", res.exceptionOrNull())
        } else {
            Log.d("TasksViewModel", "setDue OK id=$id dueAt=$dateMillisUtcMidnight")
        }
    }

    fun addToCalendar(task: Task) = viewModelScope.launch {
        try {
            events.addFromTask(task)
            Log.d("TasksViewModel", "addToCalendar OK title='${task.title}'")
        } catch (e: Exception) {
            Log.e("TasksViewModel", "addToCalendar FAILED title='${task.title}'", e)
        }
    }

    // ---- helpers ----
    private fun weekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 7)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return start to end
    }
}
