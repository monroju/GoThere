package com.example.gothere.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.Task
import com.example.gothere.viewmodel.TasksViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val query by vm.query.collectAsState()
    val onlyWeek by vm.dueThisWeek.collectAsState()
    val items by vm.filtered.collectAsState()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::setSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search tasks…") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        FilterChips(onlyWeek = onlyWeek, onToggleWeek = vm::toggleWeek)

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id ?: it.title }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { vm.toggle(task) },
                    onPickDate = { millis -> vm.setDue(task, millis) },
                    onClearDate = { vm.setDue(task, null) },
                    onAddToCalendar = { vm.addToCalendar(task) }
                )
            }
        }
    }
}

@Composable
private fun FilterChips(onlyWeek: Boolean, onToggleWeek: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = onlyWeek,
            onClick = { onToggleWeek(!onlyWeek) },
            label = { Text("Due this week") },
            leadingIcon = {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onPickDate: (Long?) -> Unit,
    onClearDate: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val dateText = task.dueAt?.let { formatDate(it) } ?: "No date"

    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    task.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showPicker = true },
                    label = { Text(dateText) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) }
                )
                AssistChip(
                    onClick = onAddToCalendar,
                    label = { Text("Add to Calendar") },
                    leadingIcon = { Icon(Icons.Outlined.EventAvailable, null) }
                )
                if (task.dueAt != null) {
                    AssistChip(
                        onClick = onClearDate,
                        label = { Text("Clear date") },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) }
                    )
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = task.dueAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onPickDate(state.selectedDateMillis?.let { midn(it) })
                    showPicker = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton({ showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

private fun midn(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatDate(millis: Long): String {
    val fmt = if (DateFormat.is24HourFormat(null)) "d MMM yyyy" else "MMM d, yyyy"
    return java.text.SimpleDateFormat(fmt, Locale.getDefault()).format(Date(millis))
}
