package com.example.gothere.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.repository.CalendarNote
import com.example.gothere.repository.CalendarRepository
import com.example.gothere.notify.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val note: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isSaving: Boolean = false,
    val message: String? = null
)

class CalendarViewModel(
    private val repo: CalendarRepository = CalendarRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarUiState())
    val ui: StateFlow<CalendarUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { loadNoteFor(_ui.value.selected) }
    }

    fun prevMonth() = updateMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = updateMonth(_ui.value.month.plusMonths(1))

    private fun updateMonth(m: YearMonth) {
        _ui.value = _ui.value.copy(month = m)
    }

    fun selectDate(date: LocalDate) {
        _ui.value = _ui.value.copy(selected = date, message = null)
        viewModelScope.launch { loadNoteFor(date) }
    }

    private suspend fun loadNoteFor(date: LocalDate) {
        val key = date.toString() // yyyy-MM-dd
        val note = repo.getNote(key)
        _ui.value = _ui.value.copy(
            note = note?.note.orEmpty(),
            reminderEnabled = note?.remindAt != null,
            reminderHour = note?.remindAt?.let { millis ->
                ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()).hour
            } ?: 9,
            reminderMinute = note?.remindAt?.let { millis ->
                ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()).minute
            } ?: 0
        )
    }

    fun onNoteChange(v: String) { _ui.value = _ui.value.copy(note = v, message = null) }
    fun toggleReminder(enabled: Boolean) { _ui.value = _ui.value.copy(reminderEnabled = enabled) }
    fun setTime(hour: Int, minute: Int) { _ui.value = _ui.value.copy(reminderHour = hour, reminderMinute = minute) }

    fun save(context: Context) {
        val s = _ui.value
        _ui.value = s.copy(isSaving = true, message = null)
        viewModelScope.launch {
            val dateKey = s.selected.toString()
            val remindAtMillis = if (s.reminderEnabled) {
                val zdt = ZonedDateTime.of(
                    s.selected.year, s.selected.monthValue, s.selected.dayOfMonth,
                    s.reminderHour, s.reminderMinute, 0, 0, ZoneId.systemDefault()
                )
                zdt.toInstant().toEpochMilli()
            } else null

            repo.upsertNote(CalendarNote(dateKey, s.note.trim(), remindAtMillis))

            if (remindAtMillis != null) {
                ReminderScheduler.schedule(context, remindAtMillis, dateKey, s.note.ifBlank { "Calendar reminder" })
            } else {
                ReminderScheduler.cancel(context, dateKey)
            }

            _ui.value = _ui.value.copy(isSaving = false, message = "Saved")
        }
    }
}
