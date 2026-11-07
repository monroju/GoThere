@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gothere.R
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.notify.FcmTopicManager
import com.example.gothere.planb.*
import com.example.gothere.repository.AncestryRepository
import com.example.gothere.util.Analytics

/** Plan B tab. Mirror of iOS PlanBScreen: score, tracked paths, next steps, rule watch. */
@Composable
fun PlanBScreen(onOpenAncestry: () -> Unit, onOpenPath: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { PlanBStore.get(context) }
    val paths by store.paths.collectAsState()
    val bundle = remember { DescentChainRepository.load(context) }
    LaunchedEffect(Unit) {
        Analytics.log(Analytics.PLANB_SCORE_VIEWED, mapOf(
            "score_bucket" to PlanBScore.bucket(PlanBScore.overall(paths)), "paths" to paths.size))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScoreRing(PlanBScore.overall(paths), 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.planb_tagline), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.planb_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (paths.isEmpty()) {
            SectionCard {
                Text(stringResource(R.string.planb_your_paths), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.planb_empty_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onOpenAncestry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.planb_check_ancestry))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.planb_your_paths), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenAncestry) { Text(stringResource(R.string.planb_add)) }
                }
                paths.forEach { p ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenPath(p.id) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScoreRing(PlanBScore.path(p), 44.dp)
                            Column(Modifier.weight(1f)) {
                                Text(planBCountryName(p, bundle), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.planb_docs_done, p.doneCount, p.activeItems.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            val next = paths.flatMap { p -> p.activeItems.filter { it.state != PlanBDocState.DONE }.map { p to it } }
                .withIndex()
                .sortedWith(compareBy({ it.value.second.state.ordinal }, { it.index }))
                .take(3).map { it.value }
            if (next.isNotEmpty()) {
                SectionCard {
                    Text(stringResource(R.string.planb_next_steps), style = MaterialTheme.typography.titleMedium)
                    next.forEach { (p, item) ->
                        val chain = bundle?.chains?.get(p.pathwayId)
                        val doc = chain?.let { PlanBChainBuilder.document(item, it) }
                        Column(Modifier.fillMaxWidth().clickable { onOpenPath(p.id) }.padding(vertical = 4.dp)) {
                            Text(doc?.let { DataLanguage.pick(it.label, it.labelEs) } ?: item.kind,
                                style = MaterialTheme.typography.bodyMedium)
                            Text("${planBCountryName(p, bundle)} · ${personName(item.personRole)} · ${stateName(item.state)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SectionCard(tinted = true) {
                Text(stringResource(R.string.planb_rule_watch), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.planb_rule_watch_body), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        bundle?.let {
            Text(DataLanguage.pick(it.disclaimer, it.disclaimerEs), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One tracked path: documents per person, where to get them, stages, dates, alerts, sources. */
@Composable
fun PlanBPathDetailScreen(pathId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlanBStore.get(context) }
    val paths by store.paths.collectAsState()
    val bundle = remember { DescentChainRepository.load(context) }
    val purchaseManager = remember { PurchaseManager.getInstance(context) }
    val purchased by purchaseManager.purchasedCountries.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val path = paths.firstOrNull { it.id == pathId }
    val chain = path?.let { bundle?.chains?.get(it.pathwayId) }
    val ancestryPath = chain?.let { c -> AncestryRepository.load(context).paths.firstOrNull { it.id == c.ancestryPathId } }
    val name = path?.let { planBCountryName(it, bundle) } ?: ""

    Scaffold(topBar = {
        TopAppBar(title = { Text(name) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        })
    }) { padding ->
        if (path == null || chain == null || bundle == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.planb_not_found))
            }
            return@Scaffold
        }
        val unlocked = purchased.let {
            ancestryPath?.let { purchaseManager.isCountryUnlocked(it.countryId) } ?: purchaseManager.hasAllAccess()
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ScoreRing(PlanBScore.path(path), 64.dp)
                Column {
                    Text(stringResource(R.string.planb_docs_done, path.doneCount, path.activeItems.size),
                        style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.planb_score_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Documents, grouped by person.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.planb_documents), style = MaterialTheme.typography.titleMedium)
                listOf("self", "parent", "grandparent", "ggp", "ancestor").forEach { role ->
                    val items = path.items.filter { it.personRole == role }
                    if (items.isNotEmpty()) {
                        Text(personName(role), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        items.forEach { item -> ItemCard(item, path, chain, bundle, store) }
                    }
                }
            }

            // Stages
            SectionCard {
                Text(stringResource(R.string.planb_stages), style = MaterialTheme.typography.titleMedium)
                chain.stages.forEach { stage ->
                    Row(Modifier.fillMaxWidth().selectable(selected = stage.id == path.stage, role = Role.RadioButton,
                        onClick = { store.setStage(path.id, stage.id) }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = stage.id == path.stage, onClick = null)
                        Text(DataLanguage.pick(stage.label, stage.labelEs), Modifier.weight(1f).padding(start = 8.dp))
                        if (unlocked) {
                            Text(monthsText(stage), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (!unlocked) {
                    TextButton(onClick = {
                        Analytics.log(Analytics.PLANB_PAYWALL_SHOWN, mapOf("reason" to "timelines", "cc" to path.countryCode))
                        Analytics.paywallSource = "planb"
                        showPaywall = true
                    }) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.planb_unlock_timelines))
                    }
                }
            }

            // Filed + appointment (today's date when switched on).
            SectionCard {
                DateSwitch(stringResource(R.string.planb_filed), path.filedOn) { store.setFiled(path.id, it) }
                DateSwitch(stringResource(R.string.planb_appointment), path.appointmentOn) { store.setAppointment(path.id, it) }
            }

            // Rule alerts
            val alertable = FcmTopicManager.RULE_ALERT_CODES.values.toSet()
            SectionCard(tinted = true) {
                Text(stringResource(R.string.planb_rule_alerts), style = MaterialTheme.typography.titleMedium)
                if (path.countryCode in alertable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.planb_alert_me), Modifier.weight(1f))
                        Switch(checked = path.alertsOn, onCheckedChange = { store.setAlerts(path.id, it) })
                    }
                    val freeHolder = store.alertCountryCodes.firstOrNull { it in alertable }
                    if (!purchaseManager.hasAllAccess() && path.alertsOn && freeHolder != null && freeHolder != path.countryCode) {
                        TextButton(onClick = {
                            Analytics.log(Analytics.PLANB_PAYWALL_SHOWN, mapOf("reason" to "alerts", "cc" to path.countryCode))
                            Analytics.paywallSource = "planb"
                            showPaywall = true
                        }) { Text(stringResource(R.string.planb_alerts_capped)) }
                    }
                } else {
                    Text(stringResource(R.string.planb_alerts_unavailable), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Sources
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val uri = LocalUriHandler.current
                Text(stringResource(R.string.planb_sources), style = MaterialTheme.typography.titleMedium)
                chain.lastVerified?.let {
                    Text(stringResource(R.string.planb_rules_verified, it), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                chain.sources.forEach { s ->
                    Row(Modifier.clickable { runCatching { uri.openUri(s.url) } }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(s.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(DataLanguage.pick(bundle.disclaimer, bundle.disclaimerEs), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedButton(onClick = { confirmRemove = true }) { Text(stringResource(R.string.planb_stop_tracking)) }
        }
    }

    if (confirmRemove && path != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.planb_stop_tracking_q, name)) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; store.remove(path.id); onBack() }) {
                    Text(stringResource(R.string.planb_stop))
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showPaywall) {
        PaywallDialog(countryId = ancestryPath?.countryId ?: "all", onDismiss = { showPaywall = false },
            purchaseManager = purchaseManager)
    }
}

@Composable
private fun ItemCard(item: PlanBItem, path: PlanBPath, chain: DescentChain, bundle: DescentChainBundle, store: PlanBStore) {
    val doc = PlanBChainBuilder.document(item, chain)
    val hints = doc?.let { bundle.hints(chain, it.orderHintIds) }.orEmpty()
    var menu by remember { mutableStateOf(false) }
    var showHints by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(doc?.let { DataLanguage.pick(it.label, it.labelEs) } ?: item.kind, Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.skipped) TextDecoration.LineThrough else null,
                    color = if (item.skipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Box {
                    AssistChip(onClick = { menu = true }, enabled = !item.skipped, label = { Text(stateName(item.state)) })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        PlanBDocState.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(stateName(s)) }, onClick = {
                                menu = false; store.setState(path.id, item.id, s)
                            })
                        }
                    }
                }
            }
            doc?.condition?.let { c ->
                Text(DataLanguage.pick(c, doc.conditionEs), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.planb_not_needed), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = item.skipped, onCheckedChange = { store.setSkipped(path.id, item.id, it) })
                }
            }
            val badges = listOfNotNull(
                if (doc?.needsApostille == true) stringResource(R.string.planb_apostille) else null,
                if (doc?.needsTranslation == true) stringResource(R.string.planb_translation) else null,
            )
            if (badges.isNotEmpty()) {
                Text(badges.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            item.orderedOn?.let {
                Text(stringResource(R.string.planb_ordered_on, it), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hints.isNotEmpty()) {
                TextButton(onClick = { showHints = !showHints }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.planb_where_to_get))
                }
                if (showHints) hints.forEach { HintBlock(it) }
            }
        }
    }
}

@Composable
private fun HintBlock(hint: OrderHint) {
    val uri = LocalUriHandler.current
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(DataLanguage.pick(hint.label, hint.labelEs), style = MaterialTheme.typography.labelLarge)
        Text(hint.howTo?.let { DataLanguage.pick(it, hint.howToEs) } ?: stringResource(R.string.planb_not_verified),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        hint.fee?.let { Text(stringResource(R.string.planb_fee, it), style = MaterialTheme.typography.labelSmall) }
        hint.url?.let { u ->
            Text(stringResource(R.string.planb_open_official), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { runCatching { uri.openUri(u) } })
        }
        hint.lastVerified?.let {
            Text(stringResource(R.string.planb_checked, it), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DateSwitch(label: String, value: String?, onChange: (String?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            value?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = value != null, onCheckedChange = { onChange(if (it) todayIso() else null) })
    }
}

@Composable
private fun SectionCard(tinted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (tinted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun ScoreRing(score: Int, size: Dp) {
    val desc = stringResource(R.string.planb_readiness, score)
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clearAndSetSemantics { contentDescription = desc }) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            strokeWidth = size / 9
        )
        Text("$score", style = if (size > 60.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

/** Tasks card for people with no move date: the Plan B entry point. */
@Composable
fun PlanBHomeCard(onClick: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlanBStore.get(context) }
    val paths by store.paths.collectAsState()
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreRing(PlanBScore.overall(paths), 40.dp)
            Column(Modifier.weight(1f)) {
                Text(if (paths.isEmpty()) stringResource(R.string.planb_home_title)
                    else stringResource(R.string.planb_home_score, PlanBScore.overall(paths)),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.planb_home_sub), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

/** From a "Likely eligible" result: pick the ancestor's generation, then track. */
@Composable
fun PlanBTrackDialog(chain: DescentChain, countryName: String, onDismiss: () -> Unit, onTracked: () -> Unit) {
    val context = LocalContext.current
    var ancestor by remember { mutableStateOf(chain.ancestorChoices.lastOrNull { it == PlanBRole.GRANDPARENT }
        ?: chain.ancestorChoices.last()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(countryName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.planb_ancestor_is, countryName))
                chain.ancestorChoices.forEach { role ->
                    Row(Modifier.fillMaxWidth().selectable(selected = role == ancestor, role = Role.RadioButton,
                        onClick = { ancestor = role }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == ancestor, onClick = null)
                        Text(personName(role.raw), Modifier.padding(start = 8.dp))
                    }
                }
                chain.lineage.note?.let {
                    Text(DataLanguage.pick(it, chain.lineage.noteEs), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.planb_stays_on_phone), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { PlanBStore.get(context).trackDescent(chain, ancestor); onTracked() }) {
                Text(stringResource(R.string.planb_track))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

// MARK: helpers

@Composable
private fun planBCountryName(path: PlanBPath, bundle: DescentChainBundle?): String {
    val context = LocalContext.current
    val chain = bundle?.chains?.get(path.pathwayId)
    val a = chain?.let { c -> AncestryRepository.load(context).paths.firstOrNull { it.id == c.ancestryPathId } }
    return a?.let { countryDisplayName(it.countryId, it.countryName) } ?: path.countryCode.uppercase()
}

@Composable
private fun personName(role: String): String = stringResource(when (role) {
    "self" -> R.string.planb_role_self
    "parent" -> R.string.planb_role_parent
    "grandparent" -> R.string.planb_role_grandparent
    "ggp" -> R.string.planb_role_ggp
    else -> R.string.planb_role_ancestor
})

@Composable
private fun stateName(state: PlanBDocState): String = stringResource(when (state) {
    PlanBDocState.NOT_STARTED -> R.string.planb_state_not_started
    PlanBDocState.ORDERED -> R.string.planb_state_ordered
    PlanBDocState.RECEIVED -> R.string.planb_state_received
    PlanBDocState.APOSTILLED -> R.string.planb_state_apostilled
    PlanBDocState.TRANSLATED -> R.string.planb_state_translated
    PlanBDocState.DONE -> R.string.planb_state_done
})

@Composable
private fun monthsText(stage: ChainStage): String {
    val low = stage.lowMonths
    val high = stage.highMonths
    if (low == null || high == null || low.isNaN() || high.isNaN()) return stringResource(R.string.planb_not_verified)
    fun f(d: Double) = if (d == Math.floor(d)) d.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", d)
    return if (low == high) stringResource(R.string.planb_about_months, f(low))
    else stringResource(R.string.planb_months_range, f(low), f(high))
}
