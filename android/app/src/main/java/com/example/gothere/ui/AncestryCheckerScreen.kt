package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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
import com.example.gothere.util.ReviewHelper
import com.example.gothere.util.ReviewMilestone
import com.example.gothere.repository.AncestryPath
import com.example.gothere.repository.AncestryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncestryCheckerScreen(onBack: () -> Unit, onOpenPlanB: (() -> Unit)? = null) {
    val context = LocalContext.current
    val catalog: AncestryCatalog = remember { AncestryRepository.load(context) }
    var selectedPath by remember { mutableStateOf<AncestryPath?>(null) }
    val ruleAnswers = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_ancestry)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPath != null) {
                            selectedPath = null
                            ruleAnswers.clear()
                        } else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                OfficialSourceNotice(countryId = null)
            } else {
                Detail(path = path, answers = ruleAnswers, onOpenPlanB = onOpenPlanB) { id, v -> ruleAnswers[id] = v }
                OfficialSourceNotice(countryId = null)
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.anc_header),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.anc_intro),
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
                stringResource(R.string.anc_planb),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun CountryPicker(catalog: AncestryCatalog, onPick: (AncestryPath) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.anc_pick_country), style = MaterialTheme.typography.titleMedium)
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
    onOpenPlanB: (() -> Unit)? = null,
    onAnswer: (String, Boolean) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(path.fullName, style = MaterialTheme.typography.titleLarge)
        Text(path.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.anc_cost), "$${path.estCostLowUSD}–$${path.estCostHighUSD}", Modifier.weight(1f))
            StatCard(stringResource(R.string.anc_timeline), stringResource(R.string.anc_months_range, path.estTimelineLowMonths, path.estTimelineHighMonths), Modifier.weight(1f))
            StatCard(stringResource(R.string.anc_income_test), stringResource(if (path.incomeRequired) R.string.yes else R.string.none), Modifier.weight(1f))
        }

        Text(stringResource(R.string.anc_qualify), style = MaterialTheme.typography.titleMedium)
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
                            label = { Text(stringResource(R.string.yes)) }
                        )
                        FilterChip(
                            selected = answers[rule.id] == false,
                            onClick = { onAnswer(rule.id, false) },
                            label = { Text(stringResource(R.string.no)) }
                        )
                    }
                    if (answers[rule.id] == false && rule.openWorkaround != null) {
                        Text(
                            stringResource(R.string.anc_workaround, rule.openWorkaround ?: ""),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Verdict(path, answers, onOpenPlanB)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.anc_docs_needed), style = MaterialTheme.typography.titleMedium)
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
                Text(stringResource(R.string.anc_money_tight), style = MaterialTheme.typography.titleMedium)
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
                        stringResource(R.string.anc_official_source),
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
private fun Verdict(path: AncestryPath, answers: Map<String, Boolean>, onOpenPlanB: (() -> Unit)? = null) {
    val required = path.eligibilityRules.filter { it.required }
    val answered = required.all { answers.containsKey(it.id) }
    if (!answered) return
    val allYes = required.all { answers[it.id] == true }
    LaunchedEffect(path.id, allYes) {
        com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.ANCESTRY_RESULT_VIEWED, mapOf(
            "country_id" to path.countryId,
            "path_id" to path.id,
            "likely_eligible" to allYes
        ))
    }
    val bg = if (allYes) Color(0x3320C997) else Color(0x33FF9800)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (allYes) {
                Text(stringResource(R.string.anc_likely), style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B7F3F))
                Text(path.outcome, style = MaterialTheme.typography.bodyMedium)
                TrackThisPath(path, onOpenPlanB)
            } else {
                Text(stringResource(R.string.anc_blocked), style = MaterialTheme.typography.titleMedium, color = Color(0xFF7A4A00))
                Text(
                    stringResource(R.string.anc_failed_rules),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Plan B: turn a "Likely eligible" result into a tracked path (PLAN_B_SPEC 4.2). */
@Composable
private fun TrackThisPath(path: AncestryPath, onOpenPlanB: (() -> Unit)?) {
    val context = LocalContext.current
    val chain = remember(path.id) {
        com.example.gothere.planb.DescentChainRepository.load(context)?.chainForAncestryPath(path.id)
    } ?: return
    val store = remember { com.example.gothere.planb.PlanBStore.get(context) }
    val paths by store.paths.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var offerPath by remember { mutableStateOf<com.example.gothere.planb.PlanBPath?>(null) }
    val name = countryDisplayName(path.countryId, path.countryName)
    val afterTracked = {
        onOpenPlanB?.invoke()
        // Ask for a review after the user acts on the result, not on the verdict itself.
        ReviewHelper.onMilestone(context, ReviewMilestone.PLANB_PATH_TRACKED)
    }
    if (paths.any { it.pathwayId == chain.pathwayId }) {
        TextButton(onClick = { onOpenPlanB?.invoke() }, enabled = onOpenPlanB != null) {
            Text(stringResource(R.string.planb_tracking))
        }
    } else {
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.planb_track))
        }
    }
    if (showDialog) {
        PlanBTrackDialog(chain = chain, countryName = name, onDismiss = { showDialog = false },
            onTracked = {
                showDialog = false
                val tracked = store.paths.value.firstOrNull { it.pathwayId == chain.pathwayId }
                val pm = com.example.gothere.billing.PurchaseManager.getInstance(context)
                if (tracked != null && PlanBUnlockOffer.claim(context, tracked.id, path.countryId, pm)) {
                    offerPath = tracked
                } else {
                    afterTracked()
                }
            })
    }
    offerPath?.let { p ->
        PlanBUnlockOfferDialog(path = p, countryId = path.countryId, countryName = name,
            onDone = { offerPath = null; afterTracked() })
    }
}
