package com.example.gothere.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.data.DestinationConfig
import com.example.gothere.data.Phases
import com.example.gothere.model.DestinationTask
import com.example.gothere.viewmodel.DestinationTasksViewModel
import com.example.gothere.viewmodel.ProgressStats
import com.example.gothere.viewmodel.TaskPhaseGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationTasksScreen(
    onNavigateToDestinations: () -> Unit = {},
    vm: DestinationTasksViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeDestinationId by vm.activeDestinationId.collectAsState()
    val tasksByPhase by vm.tasksByPhase.collectAsState()
    val progressStats by vm.progressStats.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val selectedPhaseId by vm.selectedPhaseId.collectAsState()
    val showCompleted by vm.showCompletedTasks.collectAsState()

    // Ensure tasks are seeded on first load
    LaunchedEffect(activeDestinationId) {
        vm.ensureSeeded(context)
    }

    val destination = remember(activeDestinationId) {
        DestinationConfig.getDestination(activeDestinationId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with destination indicator
        DestinationHeader(
            destination = destination,
            onChangeDestination = onNavigateToDestinations
        )

        // Progress bar
        ProgressSection(stats = progressStats)

        // Search and filters
        FilterSection(
            searchQuery = searchQuery,
            onSearchChange = vm::setSearchQuery,
            selectedPhaseId = selectedPhaseId,
            onPhaseSelect = vm::setPhaseFilter,
            showCompleted = showCompleted,
            onShowCompletedChange = vm::setShowCompleted
        )

        // Task list
        if (tasksByPhase.isEmpty()) {
            EmptyTasksState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                tasksByPhase.forEach { group ->
                    item(key = "header_${group.phaseId}") {
                        PhaseHeader(
                            group = group,
                            isExpanded = true
                        )
                    }

                    items(
                        items = group.tasks,
                        key = { it.id ?: "${it.phaseId}_${it.title}" }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { vm.toggleTask(task) },
                            onSetDueDate = { millis -> vm.setTaskDueDate(task, millis) }
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DestinationHeader(
    destination: com.example.gothere.model.DestinationCountry?,
    onChangeDestination: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChangeDestination() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = destination?.flagEmoji ?: "🌍",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = destination?.name ?: "Select Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap to change destination",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Change destination",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProgressSection(stats: ProgressStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${stats.completedTasks}/${stats.totalTasks} tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { stats.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${stats.percentage}% complete",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPhaseId: String?,
    onPhaseSelect: (String?) -> Unit,
    showCompleted: Boolean,
    onShowCompletedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search tasks...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Phase filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPhaseId == null,
                    onClick = { onPhaseSelect(null) },
                    label = { Text("All Phases") }
                )
            }
            items(Phases.allPhases) { phase ->
                FilterChip(
                    selected = selectedPhaseId == phase.id,
                    onClick = { onPhaseSelect(if (selectedPhaseId == phase.id) null else phase.id) },
                    label = { Text(phase.shortName) }
                )
            }
        }

        // Show completed toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show completed",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = showCompleted,
                onCheckedChange = onShowCompletedChange,
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun PhaseHeader(
    group: TaskPhaseGroup,
    isExpanded: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${group.completedCount}/${group.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: DestinationTask,
    onToggle: () -> Unit,
    onSetDueDate: (Long?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.completed) 0.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (!task.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (task.completed) 0.6f else 1f)
                    )
                }

                // Links
                task.links?.takeIf { it.isNotEmpty() }?.let { links ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        links.take(2).forEach { link ->
                            AssistChip(
                                onClick = { /* Open link */ },
                                label = { 
                                    Text(
                                        link.label ?: "Link",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No tasks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tasks will appear here once your destination is set up",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
