package com.example.gothere.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gothere.Route
import com.example.gothere.model.Task
import com.example.gothere.repository.SeedImportStore
import com.example.gothere.repository.TaskRepository
import com.example.gothere.repository.TipsRepository
import com.example.gothere.viewmodel.TasksViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    countryId: String = "spain",
    navController: NavHostController? = null,
    vm: TasksViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCategory by remember { mutableStateOf("Phase 1: Research & Planning") }
    var showImportSeed by remember { mutableStateOf(false) }

    // Date picker state
    var datePickerTask by remember { mutableStateOf<Task?>(null) }

    // Notes editor state
    var notesTask by remember { mutableStateOf<Task?>(null) }
    var notesText by remember { mutableStateOf("") }

    // Celebration state
    var celebratePhase by remember { mutableStateOf<String?>(null) }

    // Tip of the day
    val todaysTip = remember(countryId) { TipsRepository.getTodaysTip(context, countryId) }

    // Dismissed tip
    var tipDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(countryId) {
        vm.setCountryFilter(countryId)
    }

    // Auto-seed + one-shot dedupe
    LaunchedEffect(Unit) {
        try {
            val repo = TaskRepository()
            if (!SeedImportStore.isImported(context)) {
                repo.importSeedFromAssetsIfMissing(context)
                    .onSuccess { (added, _) ->
                        if (added > 0) SeedImportStore.markImported(context)
                    }
            }
            // Clean up the duplicate Spain tasks that early users accumulated when
            // two parallel imports raced. Runs at most once per install.
            if (!SeedImportStore.isDeduped(context)) {
                repo.dedupeSeedTasks().onSuccess {
                    SeedImportStore.markDeduped(context)
                }
            }
        } catch (_: Throwable) {}
    }

    // Per-country auto-seed: imports the country's task seed the first time the
    // user opens that country's checklist (no-op after that). Mirrors iOS, which
    // bundles all 11 country seeds in the app and seeds on selection.
    LaunchedEffect(countryId) {
        try {
            if (!SeedImportStore.isCountryImported(context, countryId)) {
                TaskRepository().importCountrySeedFromAssets(context, countryId)
                    .onSuccess { (added, _) ->
                        if (added > 0) SeedImportStore.markCountryImported(context, countryId)
                    }
            }
        } catch (_: Throwable) {}
    }

    val items by vm.filtered.collectAsState()
    val allItems by vm.allTasks.collectAsState()

    val filteredItems = remember(items, searchQuery, showCompleted) {
        items.filter { task ->
            val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                (task.description ?: "").contains(searchQuery, ignoreCase = true)
            val matchesCompleted = showCompleted || !task.completed
            matchesSearch && matchesCompleted
        }
    }

    val sections = remember(filteredItems) { groupTasksByPhase(filteredItems) }

    // Progress calculation
    val countryTasks = remember(items) { items }
    val totalTasks = countryTasks.size
    val completedTasks = countryTasks.count { it.completed }
    val progressFraction = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    // Detect phase completion for celebration
    LaunchedEffect(items) {
        val phases = items.groupBy { it.category ?: "Other" }
        for ((phase, tasks) in phases) {
            if (tasks.isNotEmpty() && tasks.all { it.completed }) {
                val key = "celebrated_$phase"
                val prefs = context.getSharedPreferences("celebrations", Context.MODE_PRIVATE)
                if (!prefs.getBoolean(key, false)) {
                    celebratePhase = phase
                    prefs.edit().putBoolean(key, true).apply()
                    break
                }
            }
        }
    }

    // Countdown (from SharedPrefs — set by Visa Wizard)
    val moveCountdown = remember {
        val prefs = context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
        val targetMillis = prefs.getLong("target_move_millis", 0L)
        if (targetMillis > System.currentTimeMillis()) {
            val days = ((targetMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
            days
        } else null
    }

    val countryName = when (countryId) {
        "spain" -> "Spain"; "portugal" -> "Portugal"; "mexico" -> "Mexico"
        "canada" -> "Canada"; "ireland" -> "Ireland"
        "italy" -> "Italy"; "germany" -> "Germany"; "poland" -> "Poland"
        "argentina" -> "Argentina"; "hungary" -> "Hungary"; "uk_ancestry" -> "UK (Ancestry)"
        else -> "Spain"
    }

    // Celebration dialog
    if (celebratePhase != null) {
        AlertDialog(
            onDismissRequest = { celebratePhase = null },
            title = { Text("Phase Complete!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("\uD83C\uDF89", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("You've completed all tasks in:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(celebratePhase!!, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Keep going — you're making great progress!", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { celebratePhase = null }) { Text("Awesome!") }
            }
        )
    }

    // Import seed dialog
    if (showImportSeed) {
        AlertDialog(
            onDismissRequest = { showImportSeed = false },
            title = { Text("Re-import Seed Tasks") },
            text = { Text("Add missing seed tasks for $countryName.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportSeed = false
                    scope.launch {
                        TaskRepository().importCountrySeedFromAssets(context, countryId)
                            .onSuccess { (a, s) -> snack.showSnackbar("Added $a, skipped $s") }
                            .onFailure { snack.showSnackbar("Import failed") }
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showImportSeed = false }) { Text("Cancel") } }
        )
    }

    // Date picker dialog
    if (datePickerTask != null) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = datePickerTask?.dueAt)
        DatePickerDialog(
            onDismissRequest = { datePickerTask = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerTask?.let { task ->
                        dateState.selectedDateMillis?.let { millis ->
                            vm.setDue(task, millis)
                        }
                    }
                    datePickerTask = null
                }) { Text("Set Date") }
            },
            dismissButton = {
                TextButton(onClick = {
                    datePickerTask?.let { vm.setDue(it, null) }
                    datePickerTask = null
                }) { Text("Clear") }
            }
        ) { DatePicker(state = dateState) }
    }

    // Notes editor dialog
    if (notesTask != null) {
        AlertDialog(
            onDismissRequest = { notesTask = null },
            title = { Text("Notes") },
            text = {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Add personal notes...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    notesTask?.let { vm.setNotes(it, notesText.ifBlank { null }) }
                    notesTask = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { notesTask = null }) { Text("Cancel") } }
        )
    }

    // Add task bottom sheet
    if (showAddTask) {
        ModalBottomSheet(
            onDismissRequest = { showAddTask = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("New Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = newTaskTitle, onValueChange = { newTaskTitle = it },
                    label = { Text("Task title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Phase", style = MaterialTheme.typography.labelLarge)
                val phases = listOf("Phase 1: Research & Planning", "Phase 2: Documentation",
                    "Phase 3: Application", "Phase 4: Arrival & Setup", "Phase 5: Residency Follow-up")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    phases.forEach { phase ->
                        FilterChip(selected = newTaskCategory == phase, onClick = { newTaskCategory = phase },
                            label = { Text(phase) }, colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                    }
                }
                Button(onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        scope.launch {
                            TaskRepository().addTask(Task(title = newTaskTitle.trim(), category = newTaskCategory, countryId = countryId))
                            newTaskTitle = ""; showAddTask = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = newTaskTitle.isNotBlank()) { Text("Add Task") }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTask = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { scaffoldPadding ->
        Column(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Relocation Checklist", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search") }
                if (filteredItems.isNotEmpty()) {
                    IconButton(onClick = {
                        val text = buildString {
                            appendLine("GoThere - $countryName Relocation Checklist\n")
                            sections.forEach { s ->
                                appendLine(s.phase ?: "Other Tasks")
                                s.tasks.forEach { t ->
                                    appendLine("  ${if (t.completed) "\u2705" else "\u2B1C"} ${t.title}")
                                }; appendLine()
                            }
                            appendLine("Progress: $completedTasks/$totalTasks completed")
                        }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                        }, "Share checklist"))
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Share") }
                }
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                    DropdownMenuItem(text = { Text("Re-import Seed Tasks") }, onClick = {
                        showOverflowMenu = false; showImportSeed = true })
                }
            }

            // Search bar
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), singleLine = true)
            }

            // Filter chip
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showCompleted, onClick = { showCompleted = !showCompleted },
                    label = { Text(if (showCompleted) "Hide Done" else "Show Done") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                // --- Progress Dashboard ---
                if (totalTasks > 0) {
                    item(key = "progress") {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                                        CircularProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier.size(56.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            strokeWidth = 6.dp
                                        )
                                        Text("${(progressFraction * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("$completedTasks of $totalTasks tasks completed",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        val remaining = totalTasks - completedTasks
                                        if (remaining > 0) {
                                            Text("$remaining tasks remaining",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        } else {
                                            Text("All tasks complete!",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // --- Countdown ---
                if (moveCountdown != null) {
                    item(key = "countdown") {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("\u2708\uFE0F", fontSize = 28.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("$moveCountdown days until your move",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("Stay on track with your checklist!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                // --- Tip of the Day ---
                if (todaysTip != null && !tipDismissed) {
                    item(key = "tip") {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Lightbulb, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Tip of the Day", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(todaysTip.tip, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { tipDismissed = true }, modifier = Modifier.size(24.dp)) {
                                    Text("\u2715", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // --- Empty state ---
                if (items.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No tasks for $countryName yet", style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    scope.launch {
                                        TaskRepository().importCountrySeedFromAssets(context, countryId)
                                            .onSuccess { (a, _) -> snack.showSnackbar("Added $a tasks") }
                                    }
                                }) { Text("Import $countryName Tasks") }
                            }
                        }
                    }
                }

                // --- Task list ---
                sections.forEach { section ->
                    item(key = "header_${countryId}_${section.phase}") {
                        PhaseHeader(section.phase ?: "Other Tasks")
                    }
                    items(items = section.tasks,
                        key = { it.id ?: "${countryId}_${it.category}|${it.title}" }
                    ) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { vm.toggle(task) },
                            onSetDate = { datePickerTask = task },
                            onEditNotes = {
                                notesText = task.notes ?: ""
                                notesTask = task
                            },
                            onLinkClick = { url ->
                                handleTaskLink(context, navController, countryId, url)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onSetDate: () -> Unit,
    onEditNotes: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val hasDetails = !task.description.isNullOrBlank() || !task.links.isNullOrEmpty() || !task.notes.isNullOrBlank()

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface)
                    task.dueAt?.let { millis ->
                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
                        val isOverdue = millis < System.currentTimeMillis() && !task.completed
                        Text(dateStr, style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Date picker button
                IconButton(onClick = onSetDate, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Set due date",
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Notes button
                IconButton(onClick = onEditNotes, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Add notes",
                        modifier = Modifier.size(18.dp),
                        tint = if (task.notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary)
                }

                if (hasDetails) {
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Expandable details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 48.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    task.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    task.notes?.takeIf { it.isNotBlank() }?.let {
                        Text("\uD83D\uDCDD $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    task.links?.forEach { link ->
                        val url = link.url
                        if (!url.isNullOrBlank()) {
                            val isInternal = url.startsWith("gothere://")
                            Row(modifier = Modifier.clickable { onLinkClick(url) }
                                .padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (isInternal) Icons.AutoMirrored.Outlined.ArrowForward else Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(link.label ?: url, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TaskPhaseSection(val phase: String?, val tasks: List<Task>)

private fun groupTasksByPhase(tasks: List<Task>): List<TaskPhaseSection> {
    if (tasks.isEmpty()) return emptyList()
    val grouped = tasks.groupBy { t -> t.category?.takeIf { it.isNotBlank() } ?: "Other Tasks" }
    fun phaseNumber(label: String): Int {
        val regex = Regex("""^Phase\s+(\d+)\s*:""", RegexOption.IGNORE_CASE)
        return regex.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }
    return grouped.keys.sortedWith(compareBy<String>({ phaseNumber(it) }, { it.lowercase() })).map { key ->
        TaskPhaseSection(
            phase = if (key == "Other Tasks") null else key,
            tasks = grouped[key].orEmpty().sortedWith(
                compareBy<Task>({ it.completed }, { it.dueAt ?: Long.MAX_VALUE }, { it.title.lowercase() }))
        )
    }
}

@Composable
private fun PhaseHeader(title: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

/**
 * Resolves task links. `gothere://<path>` URLs route through the nav controller for
 * in-app deep links; anything else opens externally via ACTION_VIEW. Supported paths:
 *   - gothere://wizard          → Visa Wizard for the current country
 *   - gothere://compare         → Visa Compare for the current country
 *   - gothere://resources       → Resources tab
 *   - gothere://decision        → Decision Tree tab
 *   - gothere://documents       → Documents tab
 */
private fun handleTaskLink(
    context: Context,
    navController: NavHostController?,
    countryId: String,
    url: String
) {
    if (url.startsWith("gothere://") && navController != null) {
        // gothere://compare currently aliases to the Wizard since VisaCompareScreen is
        // only wired as a sub-screen inside VisaWizardScreen, not on the top-level graph.
        when (url.removePrefix("gothere://").substringBefore('?').trim('/').lowercase()) {
            "wizard",
            "compare"      -> navController.navigate(Route.VisaWizard.create(countryId))
            "resources"    -> navController.navigate(Route.Resources.route)
            "decision",
            "decisiontree" -> navController.navigate(Route.DecisionTree.route)
            "documents"    -> navController.navigate(Route.Documents.route)
            "calendar"     -> navController.navigate(Route.Calendar.route)
            "tasks"        -> navController.navigate(Route.Tasks.route)
            else -> Log.w("TasksScreen", "Unknown gothere:// deep link: $url")
        }
        return
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
}
