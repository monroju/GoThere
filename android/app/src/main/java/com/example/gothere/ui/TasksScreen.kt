package com.example.gothere.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.Task
import com.example.gothere.repository.SeedImportStore
import com.example.gothere.repository.TaskRepository
import com.example.gothere.viewmodel.TasksViewModel
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    countryId: String = "spain",
    vm: TasksViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    // Update the ViewModel's country filter when countryId changes
    LaunchedEffect(countryId) {
        Log.d("TasksScreen", "Country changed to: $countryId")
        vm.setCountryFilter(countryId)
    }

    // ✅ Runs once per install (after login)
    LaunchedEffect(Unit) {
        try {
            val alreadyImported = SeedImportStore.isImported(context)

            if (alreadyImported) {
                Log.d("SeedImport", "Seed already imported -> skipping")
                return@LaunchedEffect
            }

            Log.e("SeedImport", "Seed import not yet done -> running import")

            val repo = TaskRepository()

            // suspend call — LaunchedEffect is already a coroutine
            val result = repo.importSeedFromAssetsIfMissing(context)

            result
                .onSuccess { (added, skipped) ->
                    Log.e(
                        "SeedImport",
                        "Seed import DONE added=$added skipped=$skipped"
                    )
                    SeedImportStore.markImported(context)
                }
                .onFailure { e ->
                    Log.e("SeedImport", "Seed import FAILED", e)
                }

        } catch (t: Throwable) {
            Log.e("SeedImport", "Seed import crashed", t)
        }
    }


    val items by vm.filtered.collectAsState()

    LaunchedEffect(items, countryId) {
        Log.d("TasksScreen", "Rendering tasks count = ${items.size} for country = $countryId")
    }

    val sections = remember(items) { groupTasksByPhase(items) }

    // Get country display name
    val countryName = when (countryId) {
        "spain" -> "Spain"
        "portugal" -> "Portugal"
        "mexico" -> "Mexico"
        else -> "Spain"
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Relocation Checklist",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Optional debug button: re-import seed (does NOT delete existing tasks; adds missing)
            Button(
                onClick = {
                    scope.launch {
                        val repo = TaskRepository()
                        val result = repo.importSeedFromAssetsIfMissing(context)
                        result.onSuccess { pair: Pair<Int, Int> ->
                            val (added, skipped) = pair
                            snack.showSnackbar("Seed import: added=$added skipped=$skipped")
                        }.onFailure { e ->
                            snack.showSnackbar("Seed import failed: ${e.message}")
                        }
                    }
                }
            ) {
                Text("Import Seed")
            }
        }

        Spacer(Modifier.height(12.dp))

        SnackbarHost(hostState = snack)

        Spacer(Modifier.height(12.dp))

        // Show message if no tasks for this country yet
        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tasks for $countryName yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tasks for $countryName will appear here once imported.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val repo = TaskRepository()
                                val result = repo.importCountrySeedFromAssets(context, countryId)
                                result.onSuccess { (added, skipped) ->
                                    snack.showSnackbar("$countryName tasks: added=$added skipped=$skipped")
                                }.onFailure { e ->
                                    snack.showSnackbar("Import failed: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text("Import $countryName Tasks")
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            sections.forEach { section ->
                val phaseTitle = section.phase ?: "Other Tasks"

                item(key = "header_${countryId}_$phaseTitle") {
                    PhaseHeader(phaseTitle)
                }

                items(
                    items = section.tasks,
                    key = { it.id ?: "${countryId}_${it.category}|${it.title}" }
                ) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { vm.toggle(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (task.completed)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                )
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (task.completed) 0.5f else 1f
                        )
                    )
                }
            }
        }
    }
}

/* ---------- Phase grouping helpers ---------- */

private data class TaskPhaseSection(
    val phase: String?,
    val tasks: List<Task>
)

private fun groupTasksByPhase(tasks: List<Task>): List<TaskPhaseSection> {
    if (tasks.isEmpty()) return emptyList()

    val grouped: Map<String, List<Task>> = tasks.groupBy { t ->
        t.category?.takeIf { it.isNotBlank() } ?: "Other Tasks"
    }

    fun phaseNumber(label: String): Int {
        val regex = Regex("""^Phase\s+(\d+)\s*:""", RegexOption.IGNORE_CASE)
        return regex.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    val sortedKeys = grouped.keys.sortedWith(
        compareBy<String>({ phaseNumber(it) }, { it.lowercase() })
    )

    return sortedKeys.map { key ->
        val tasksForKey = grouped[key].orEmpty()
            .sortedWith(
                compareBy<Task>(
                    { it.completed },
                    { it.dueAt ?: Long.MAX_VALUE },
                    { it.title.lowercase() }
                )
            )

        TaskPhaseSection(
            phase = if (key == "Other Tasks") null else key,
            tasks = tasksForKey
        )
    }
}

@Composable
private fun PhaseHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
