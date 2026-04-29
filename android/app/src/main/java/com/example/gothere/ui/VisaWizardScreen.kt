@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.gothere.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gothere.model.QuestionOption
import com.example.gothere.model.Task
import com.example.gothere.model.WizardQuestion
import com.example.gothere.model.WizardStep
import com.example.gothere.model.WizardTrack
import com.example.gothere.viewmodel.VisaWizardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VisaWizardScreen(
    navController: NavHostController,
    countryId: String,
    vm: VisaWizardViewModel = viewModel()
) {
    val tracks by vm.availableTracks.collectAsState()
    val selectedTrackId by vm.selectedTrackId.collectAsState()
    val selectedTrack by vm.selectedTrack.collectAsState()
    val currentStep by vm.currentStep.collectAsState()
    val answers by vm.answers.collectAsState()
    val generatedTasks by vm.generatedTasks.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val saveComplete by vm.saveComplete.collectAsState()

    LaunchedEffect(countryId) {
        vm.loadConfig(countryId)
    }

    val countryName = when (countryId) {
        "spain" -> "Spain"
        "portugal" -> "Portugal"
        "mexico" -> "Mexico"
        else -> "Spain"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visa Wizard") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0 && !saveComplete) {
                            vm.prevStep()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            if (selectedTrack != null) {
                val total = vm.totalStepCount()
                val progress = (currentStep.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Step $currentStep of ${total - 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            AnimatedContent(
                targetState = currentStep,
                label = "wizard_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                when {
                    step == 0 -> TrackSelectionScreen(
                        countryName = countryName,
                        tracks = tracks,
                        onSelect = { vm.selectTrack(it) }
                    )
                    vm.isOnSummary() -> SummaryScreen(
                        tasks = generatedTasks,
                        isSaving = isSaving,
                        saveComplete = saveComplete,
                        onSave = { vm.saveTasksToFirestore() },
                        onDone = {
                            navController.popBackStack()
                            navController.navigate("tasks") {
                                launchSingleTop = true
                            }
                        }
                    )
                    else -> {
                        val wizardStep = vm.getCurrentWizardStep()
                        if (wizardStep != null) {
                            StepScreen(
                                step = wizardStep,
                                answers = answers,
                                isQuestionVisible = { vm.isQuestionVisible(it) },
                                onAnswer = { qId, value -> vm.setAnswer(qId, value) },
                                onNext = { vm.nextStep() },
                                onBack = { vm.prevStep() },
                                isLastStep = step == (selectedTrack?.steps?.size ?: 0)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Track Selection ====================

@Composable
private fun TrackSelectionScreen(
    countryName: String,
    tracks: List<Pair<String, WizardTrack>>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Choose Your Visa Path",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Select the visa type you're pursuing in $countryName. We'll create a personalized step-by-step checklist.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        tracks.forEach { (trackId, track) ->
            ElevatedCard(
                onClick = { onSelect(trackId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        track.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${track.shortName} \u2022 ${track.steps.size} steps \u2022 ${track.taskRules.size} tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== Step Screen ====================

@Composable
private fun StepScreen(
    step: WizardStep,
    answers: Map<String, Any>,
    isQuestionVisible: (WizardQuestion) -> Boolean,
    onAnswer: (String, Any) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLastStep: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        step.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        step.questions.forEach { question ->
            if (isQuestionVisible(question)) {
                QuestionRenderer(
                    question = question,
                    currentAnswer = answers[question.id],
                    onAnswer = { onAnswer(question.id, it) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) { Text("Back") }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) { Text(if (isLastStep) "Generate Checklist" else "Next") }
        }
    }
}

// ==================== Question Renderers ====================

@Composable
private fun QuestionRenderer(
    question: WizardQuestion,
    currentAnswer: Any?,
    onAnswer: (Any) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            question.label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )

        question.hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (question.type) {
            "single_choice" -> SingleChoiceQuestion(
                options = question.options ?: emptyList(),
                selectedId = currentAnswer?.toString(),
                onSelect = { onAnswer(it) }
            )
            "boolean" -> BooleanQuestion(
                selected = currentAnswer as? Boolean,
                onSelect = { onAnswer(it) }
            )
            "number" -> NumberQuestion(
                value = (currentAnswer as? Int) ?: question.min ?: 1,
                min = question.min ?: 1,
                max = question.max ?: 10,
                onValueChange = { onAnswer(it) }
            )
        }
    }
}

@Composable
private fun SingleChoiceQuestion(
    options: List<QuestionOption>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedId == option.id,
                onClick = { onSelect(option.id) },
                label = { Text(option.label, modifier = Modifier.padding(vertical = 4.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun BooleanQuestion(
    selected: Boolean?,
    onSelect: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == true,
            onClick = { onSelect(true) },
            label = { Text("Yes", modifier = Modifier.padding(vertical = 4.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        FilterChip(
            selected = selected == false,
            onClick = { onSelect(false) },
            label = { Text("No", modifier = Modifier.padding(vertical = 4.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
private fun NumberQuestion(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ==================== Summary Screen ====================

@Composable
private fun SummaryScreen(
    tasks: List<Task>,
    isSaving: Boolean,
    saveComplete: Boolean,
    onSave: () -> Unit,
    onDone: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (saveComplete) {
            // Success state
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "${tasks.size} tasks added to your checklist!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your personalized visa checklist is ready. Tasks with due dates have been scheduled based on your target move date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Go to Tasks") }
            }
        } else {
            // Preview state
            Text(
                "Your Personalized Checklist",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${tasks.size} tasks tailored to your situation. Review and add them to your Tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Group by phase for preview
            val grouped = tasks.groupBy { it.category ?: "Other" }
            grouped.forEach { (phase, phaseTasks) ->
                Text(
                    phase,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                phaseTasks.forEach { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                task.dueAt?.let { millis ->
                                    Text(
                                        "Due: ${dateFormat.format(Date(millis))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add ${tasks.size} Tasks to My Checklist")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
