@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.EventItem
import com.example.gothere.util.NotificationHelper
import com.example.gothere.viewmodel.CalendarViewModel
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

private enum class CalendarMode { WEEK, MONTH }

@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    val context = LocalContext.current

    // date state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var currentWeekStart by remember { mutableStateOf(selectedDate.with(DayOfWeek.MONDAY)) }

    // dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }

    // events from the view model
    val events by vm.events.collectAsState()
    
    val eventsByDate = remember(events) {
        events.groupBy { event ->
            Instant.ofEpochMilli(event.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    SegmentedButtons(
                        mode = mode,
                        onModeChange = { mode = it }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Appointment")
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header row: month/week label + arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val headerText = when (mode) {
                    CalendarMode.MONTH -> {
                        val m = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        "$m ${currentMonth.year}"
                    }
                    CalendarMode.WEEK -> {
                        val end = currentWeekStart.plusDays(6)
                        "${currentWeekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} " +
                                "${currentWeekStart.dayOfMonth} – " +
                                "${end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${end.dayOfMonth}"
                    }
                }
                Text(headerText, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            when (mode) {
                                CalendarMode.MONTH -> currentMonth = currentMonth.minusMonths(1)
                                CalendarMode.WEEK -> currentWeekStart = currentWeekStart.minusWeeks(1)
                            }
                        },
                        content = { Text("◀") },
                    )
                    OutlinedButton(
                        onClick = {
                            when (mode) {
                                CalendarMode.MONTH -> currentMonth = currentMonth.plusMonths(1)
                                CalendarMode.WEEK -> currentWeekStart = currentWeekStart.plusWeeks(1)
                            }
                        },
                        content = { Text("▶") },
                    )
                }
            }

            // Day-of-week labels
            DaysOfWeekRow()

            // Calendar grid
            when (mode) {
                CalendarMode.MONTH ->
                    MonthGrid(
                        month = currentMonth,
                        selectedDate = selectedDate,
                        events = eventsByDate,
                        onSelect = { selectedDate = it }
                    )
                CalendarMode.WEEK ->
                    WeekRow(
                        weekStart = currentWeekStart,
                        selectedDate = selectedDate,
                        events = eventsByDate,
                        onSelect = { selectedDate = it }
                    )
            }

            // List for the selected day
            Text(
                text = "Schedule for ${selectedDate}",
                style = MaterialTheme.typography.titleSmall
            )

            val dayEvents = eventsByDate[selectedDate.toEpochDay()] ?: emptyList()
            if (dayEvents.isEmpty()) {
                Text("No events for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayEvents) { event ->
                        ElevatedCard {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(event.title, modifier = Modifier.weight(1f))
                                IconButton(onClick = { vm.delete(context, event.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Appointment / Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date: $selectedDate", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newEventTitle,
                        onValueChange = { newEventTitle = it },
                        label = { Text("Appointment or Note details") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEventTitle.isNotBlank()) {
                            val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            vm.add(context, newEventTitle, millis)
                            newEventTitle = ""
                            showAddDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SegmentedButtons(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit
) {
    val options = listOf("Month" to CalendarMode.MONTH, "Week" to CalendarMode.WEEK)
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onModeChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(label) }
        }
    }
}

@Composable
private fun DaysOfWeekRow() {
    val days = DayOfWeek.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    events: Map<Long, List<EventItem>>,
    onSelect: (LocalDate) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(7) { i ->
            val date = weekStart.plusDays(i.toLong())
            DayCell(date, date.month == weekStart.month, selectedDate, events, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    events: Map<Long, List<EventItem>>,
    onSelect: (LocalDate) -> Unit
) {
    val firstOfMonth = month.atDay(1)
    val firstDisplay = firstOfMonth.with(DayOfWeek.MONDAY)
    val daysInMonth = month.lengthOfMonth()
    val lastOfMonth = month.atDay(daysInMonth)
    val lastDisplay = lastOfMonth.with(DayOfWeek.SUNDAY)

    val totalDays = Duration.between(firstDisplay.atStartOfDay(), lastDisplay.plusDays(1).atStartOfDay()).toDays()
    val weeks = (totalDays / 7).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(weeks) { w ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { d ->
                    val date = firstDisplay.plusDays((w * 7 + d).toLong())
                    DayCell(
                        date = date,
                        isCurrentMonth = date.month == month.month,
                        selectedDate = selectedDate,
                        events = events,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    selectedDate: LocalDate,
    events: Map<Long, List<EventItem>>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = date == selectedDate
    val eventCount = events[date.toEpochDay()]?.size ?: 0

    Box(
        modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(MaterialTheme.shapes.small)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onSelect(date) }
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(Modifier.height(4.dp))
            if (eventCount > 0) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = eventCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
