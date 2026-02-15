package com.example.gothere.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.data.DestinationConfig
import com.example.gothere.data.Phases
import com.example.gothere.model.DestinationTask
import com.example.gothere.repository.DestinationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for displaying destination-scoped tasks.
 * Replaces TasksViewModel when destination-aware task management is enabled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DestinationTasksViewModel(
    private val repo: DestinationRepository = DestinationRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "DestinationTasksVM"
    }

    // Current active destination
    val activeDestinationId: StateFlow<String> = repo.activeDestinationFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DestinationConfig.SPAIN)

    // Filter controls
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPhaseId = MutableStateFlow<String?>(null)
    val selectedPhaseId: StateFlow<String?> = _selectedPhaseId.asStateFlow()

    private val _showCompletedTasks = MutableStateFlow(true)
    val showCompletedTasks: StateFlow<Boolean> = _showCompletedTasks.asStateFlow()

    private val _dueThisWeek = MutableStateFlow(false)
    val dueThisWeek: StateFlow<Boolean> = _dueThisWeek.asStateFlow()

    // All tasks for current destination (raw from Firestore)
    private val allTasks: Flow<List<DestinationTask>> = activeDestinationId
        .flatMapLatest { destId ->
            Log.d(TAG, "Loading tasks for destination: $destId")
            repo.tasksFlow(destId)
        }

    // Filtered and sorted tasks for UI
    val tasks: StateFlow<List<DestinationTask>> = combine(
        allTasks,
        searchQuery,
        selectedPhaseId,
        showCompletedTasks,
        dueThisWeek
    ) { taskList, query, phaseFilter, showCompleted, onlyDueThisWeek ->
        
        var filtered = taskList

        // Filter by search query
        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            filtered = filtered.filter { task ->
                task.title.lowercase().contains(q) ||
                (task.description?.lowercase()?.contains(q) == true)
            }
        }

        // Filter by phase
        if (phaseFilter != null) {
            filtered = filtered.filter { it.phaseId == phaseFilter }
        }

        // Filter completed
        if (!showCompleted) {
            filtered = filtered.filter { !it.completed }
        }

        // Filter by due this week
        if (onlyDueThisWeek) {
            val (weekStart, weekEnd) = weekBounds()
            filtered = filtered.filter { task ->
                val due = task.dueAt
                // Include tasks due this week OR tasks without due date
                (due != null && due in weekStart..weekEnd) || due == null
            }
        }

        // Sort: by phase order, then by task order, then incomplete first
        filtered.sortedWith(
            compareBy(
                { Phases.getOrder(it.phaseId) },
                { it.completed }, // false (incomplete) first
                { it.order },
                { it.title.lowercase() }
            )
        ).also {
            Log.d(TAG, "Filtered tasks: ${taskList.size} -> ${it.size}")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped by phase for section headers
    val tasksByPhase: StateFlow<List<TaskPhaseGroup>> = tasks.map { taskList ->
        taskList
            .groupBy { it.phaseId }
            .map { (phaseId, phaseTasks) ->
                TaskPhaseGroup(
                    phaseId = phaseId,
                    displayName = Phases.getDisplayName(phaseId),
                    tasks = phaseTasks,
                    completedCount = phaseTasks.count { it.completed },
                    totalCount = phaseTasks.size
                )
            }
            .sortedBy { Phases.getOrder(it.phaseId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Progress stats
    val progressStats: StateFlow<ProgressStats> = tasks.map { taskList ->
        val total = taskList.size
        val completed = taskList.count { it.completed }
        ProgressStats(
            totalTasks = total,
            completedTasks = completed,
            percentage = if (total > 0) (completed * 100) / total else 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressStats())

    // ==================== Actions ====================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPhaseFilter(phaseId: String?) {
        _selectedPhaseId.value = phaseId
    }

    fun setShowCompleted(show: Boolean) {
        _showCompletedTasks.value = show
    }

    fun setDueThisWeek(enabled: Boolean) {
        _dueThisWeek.value = enabled
    }

    fun toggleTask(task: DestinationTask) {
        viewModelScope.launch {
            val taskId = task.id ?: return@launch
            val destId = activeDestinationId.value
            
            val result = repo.toggleTaskCompleted(destId, taskId, !task.completed)
            if (result.isFailure) {
                Log.e(TAG, "Failed to toggle task", result.exceptionOrNull())
            }
        }
    }

    fun setTaskDueDate(task: DestinationTask, dueAtMillis: Long?) {
        viewModelScope.launch {
            val taskId = task.id ?: return@launch
            val destId = activeDestinationId.value
            
            val result = repo.setTaskDueDate(destId, taskId, dueAtMillis)
            if (result.isFailure) {
                Log.e(TAG, "Failed to set due date", result.exceptionOrNull())
            }
        }
    }

    /**
     * Ensure current destination is seeded (call on screen load)
     */
    fun ensureSeeded(context: Context) {
        viewModelScope.launch {
            val destId = activeDestinationId.value
            repo.ensureDestinationSeeded(destId, context)
        }
    }

    // ==================== Helpers ====================

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

/**
 * Grouping of tasks by phase for UI sections
 */
data class TaskPhaseGroup(
    val phaseId: String,
    val displayName: String,
    val tasks: List<DestinationTask>,
    val completedCount: Int,
    val totalCount: Int
)

/**
 * Overall progress statistics
 */
data class ProgressStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val percentage: Int = 0
)
