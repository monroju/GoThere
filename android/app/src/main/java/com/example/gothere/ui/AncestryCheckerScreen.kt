package com.example.gothere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.repository.AncestryCatalog
import com.example.gothere.repository.AncestryPath
import com.example.gothere.repository.AncestryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncestryCheckerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val catalog: AncestryCatalog = remember { AncestryRepository.load(context) }
    var selectedPath by remember { mutableStateOf<AncestryPath?>(null) }
    val ruleAnswers = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ancestry Citizenship") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPath != null) {
                            selectedPath = null
                            ruleAnswers.clear()
                        } else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Header()
            val path = selectedPath
            if (path == null) {
                CountryPicker(catalog) { selectedPath = it; ruleAnswers.clear() }
                Disclaimer(catalog.disclaimer)
            } else {
                Detail(path = path, answers = ruleAnswers) { id, v -> ruleAnswers[id] = v }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "You might already be a citizen.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Six EU countries pass citizenship down to grandchildren or further. No income test, no residency requirement, no language test (mostly). The hardest part is the paperwork.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Upper-tier "second passport / Plan B" framing — the same route a CBI buyer
        // pays $200k+ for, available for the cost of document research if you qualify.
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "A second passport for the cost of paperwork. What investors pay \$200k+ for via citizenship-by-investment, you may inherit — full EU mobility, a tax-residency option, and a real Plan B.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun CountryPicker(catalog: AncestryCatalog, onPick: (AncestryPath) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pick the ancestor's country", style = MaterialTheme.typography.titleMedium)
        catalog.paths.forEach { p ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(p) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.countryName, fontWeight = FontWeight.SemiBold)
                        Text(
                            p.shortName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun Disclaimer(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Detail(
    path: AncestryPath,
    answers: Map<String, Boolean>,
    onAnswer: (String, Boolean) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(path.fullName, style = MaterialTheme.typography.titleLarge)
        Text(path.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Cost", "$${path.estCostLowUSD}–$${path.estCostHighUSD}", Modifier.weight(1f))
            StatCard("Timeline", "${path.estTimelineLowMonths}–${path.estTimelineHighMonths} mo", Modifier.weight(1f))
            StatCard("Income test", if (path.incomeRequired) "Yes" else "None", Modifier.weight(1f))
        }

        Text("Do you qualify?", style = MaterialTheme.typography.titleMedium)
        path.eligibilityRules.forEach { rule ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(rule.label, style = MaterialTheme.typography.bodyMedium)
                    rule.explanation?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = answers[rule.id] == true,
                            onClick = { onAnswer(rule.id, true) },
                            label = { Text("Yes") }
                        )
                        FilterChip(
                            selected = answers[rule.id] == false,
                            onClick = { onAnswer(rule.id, false) },
                            label = { Text("No") }
                        )
                    }
                    if (answers[rule.id] == false && rule.openWorkaround != null) {
                        Text(
                            "Workaround available: ${rule.openWorkaround}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Verdict(path, answers)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Documents you'll need", style = MaterialTheme.typography.titleMedium)
                path.documents.forEach { d ->
                    Text("• $d", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("If money is tight", style = MaterialTheme.typography.titleMedium)
                Text(path.lowIncomeNotes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (path.officialUrl.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(path.officialUrl) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Official government source",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Verdict(path: AncestryPath, answers: Map<String, Boolean>) {
    val required = path.eligibilityRules.filter { it.required }
    val answered = required.all { answers.containsKey(it.id) }
    if (!answered) return
    val allYes = required.all { answers[it.id] == true }
    val bg = if (allYes) Color(0x3320C997) else Color(0x33FF9800)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (allYes) {
                Text("Likely eligible ✓", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B7F3F))
                Text(path.outcome, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Direct path blocked", style = MaterialTheme.typography.titleMedium, color = Color(0xFF7A4A00))
                Text(
                    "One or more required rules failed. Check the workaround notes above — some failed rules have court-petition paths that still work.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
