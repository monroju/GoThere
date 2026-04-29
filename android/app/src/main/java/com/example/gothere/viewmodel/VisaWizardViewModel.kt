package com.example.gothere.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Task
import com.example.gothere.model.WizardConfig
import com.example.gothere.model.WizardQuestion
import com.example.gothere.model.WizardStep
import com.example.gothere.model.WizardTrack
import com.example.gothere.repository.TaskRepository
import com.example.gothere.repository.WizardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VisaWizardViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext
    private val taskRepo = TaskRepository()

    private val _config = MutableStateFlow<WizardConfig?>(null)
    val config: StateFlow<WizardConfig?> = _config

    // Available tracks for current country
    private val _availableTracks = MutableStateFlow<List<Pair<String, WizardTrack>>>(emptyList())
    val availableTracks: StateFlow<List<Pair<String, WizardTrack>>> = _availableTracks

    // Currently selected track
    private val _selectedTrackId = MutableStateFlow<String?>(null)
    val selectedTrackId: StateFlow<String?> = _selectedTrackId

    private val _selectedTrack = MutableStateFlow<WizardTrack?>(null)
    val selectedTrack: StateFlow<WizardTrack?> = _selectedTrack

    // Current step index (0 = track selection, 1+ = wizard steps)
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep

    // User answers: questionId -> value
    private val _answers = MutableStateFlow<Map<String, Any>>(emptyMap())
    val answers: StateFlow<Map<String, Any>> = _answers

    // Generated tasks (shown on summary screen)
    private val _generatedTasks = MutableStateFlow<List<Task>>(emptyList())
    val generatedTasks: StateFlow<List<Task>> = _generatedTasks

    // State flags
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete

    fun loadConfig(countryId: String) {
        val cfg = WizardRepository.loadConfig(context)
        _config.value = cfg
        _availableTracks.value = WizardRepository.getTracksForCountry(cfg, countryId)

        // Auto-select if only one track available
        if (_availableTracks.value.size == 1) {
            selectTrack(_availableTracks.value.first().first)
        }
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
        _selectedTrack.value = _config.value?.tracks?.get(trackId)
        _currentStep.value = 1
        _answers.value = emptyMap()
        _generatedTasks.value = emptyList()
        _saveComplete.value = false
    }

    fun setAnswer(questionId: String, value: Any) {
        _answers.value = _answers.value.toMutableMap().apply { put(questionId, value) }
    }

    fun nextStep() {
        val track = _selectedTrack.value ?: return
        val totalSteps = track.steps.size
        val current = _currentStep.value

        if (current < totalSteps) {
            _currentStep.value = current + 1
        } else {
            // Last step -> generate preview
            generateTaskPreview()
            _currentStep.value = current + 1 // summary step
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value = _currentStep.value - 1
        } else if (_currentStep.value == 1 && _availableTracks.value.size > 1) {
            // Go back to track selection
            _currentStep.value = 0
            _selectedTrackId.value = null
            _selectedTrack.value = null
        }
    }

    /** Total steps including track selection (0) and summary (steps.size + 1) */
    fun totalStepCount(): Int {
        val track = _selectedTrack.value ?: return 1
        return track.steps.size + 2 // track selection + N steps + summary
    }

    fun getCurrentWizardStep(): WizardStep? {
        val track = _selectedTrack.value ?: return null
        val stepIndex = _currentStep.value - 1 // step 1 = index 0
        return track.steps.getOrNull(stepIndex)
    }

    fun isOnSummary(): Boolean {
        val track = _selectedTrack.value ?: return false
        return _currentStep.value > track.steps.size
    }

    /** Check if a question should be visible based on showIf conditions. */
    fun isQuestionVisible(question: WizardQuestion): Boolean {
        val showIf = question.showIf ?: return true
        val currentAnswers = _answers.value

        for ((qId, expected) in showIf) {
            val actual = currentAnswers[qId] ?: return false
            val matches = when (expected) {
                is Boolean -> actual == expected
                is String -> actual.toString() == expected
                else -> actual.toString() == expected.toString()
            }
            if (!matches) return false
        }
        return true
    }

    private fun generateTaskPreview() {
        val track = _selectedTrack.value ?: return
        val ans = _answers.value
        val targetWeeks = WizardRepository.targetWeeksFromChoice(
            ans["target_move"]?.toString() ?: "6_months"
        )

        _generatedTasks.value = WizardRepository.generateTasks(
            track = track,
            answers = ans,
            countryId = track.countryId,
            targetWeeksFromNow = targetWeeks
        )

        Log.d("VisaWizard", "Generated ${_generatedTasks.value.size} tasks from ${track.taskRules.size} rules")
    }

    fun saveTasksToFirestore() {
        val tasks = _generatedTasks.value
        if (tasks.isEmpty()) return

        _isSaving.value = true
        viewModelScope.launch {
            val result = taskRepo.insertTasks(tasks)
            _isSaving.value = false
            if (result.isSuccess) {
                _saveComplete.value = true
                // Save target move date for countdown widget
                val targetChoice = _answers.value["target_move"]?.toString() ?: "6_months"
                val weeks = WizardRepository.targetWeeksFromChoice(targetChoice)
                val targetMillis = System.currentTimeMillis() + weeks * 7L * 24 * 60 * 60 * 1000
                context.getSharedPreferences("wizard_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putLong("target_move_millis", targetMillis).apply()
                Log.d("VisaWizard", "Saved ${tasks.size} tasks + countdown ($weeks weeks)")
            } else {
                Log.e("VisaWizard", "Failed to save tasks", result.exceptionOrNull())
            }
        }
    }
}
