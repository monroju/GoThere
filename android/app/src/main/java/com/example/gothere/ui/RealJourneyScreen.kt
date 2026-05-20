package com.example.gothere.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightbulbCircle
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.data.JourneyPhase
import com.example.gothere.data.RealJourney

/**
 * Premium-only screen showing a sanitized, real-world visa journey end-to-end.
 * Gated by [PurchaseManager.hasAllAccess]. Free users see a paywall preview;
 * subscribers see the full content. Mirrors iOS `RealJourneyView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealJourneyScreen(
    journey: RealJourney,
    purchaseManager: PurchaseManager,
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    // Re-derive on any subscription/SKU change so the lock state flips live.
    val subStatus by purchaseManager.subscriptionStatus.collectAsState()
    val ownedSkus by purchaseManager.ownedSKUs.collectAsState()
    val purchasedCountries by purchaseManager.purchasedCountries.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val gateInputs = listOf(subStatus, ownedSkus, purchasedCountries) // forces recomposition
    val isUnlocked = purchaseManager.hasAllAccess()

    var expandedPhase by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Real Journey") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item { JourneyHeader(journey) }
            item { DisclaimerBanner(journey.disclaimer) }

            if (isUnlocked) {
                item { EligibilityCard(journey.eligibilitySummary) }
                item { FeeCard(journey.feeSummary) }
                item {
                    SectionHeader("Phase-by-phase walkthrough", Icons.Outlined.ListAlt)
                }
                items(journey.phases.size, key = { idx -> journey.phases[idx].id }) { idx ->
                    val phase = journey.phases[idx]
                    PhaseCard(
                        phase = phase,
                        expanded = expandedPhase == phase.id,
                        onToggle = {
                            expandedPhase = if (expandedPhase == phase.id) null else phase.id
                        }
                    )
                }
                item { CrossGotchasCard(journey) }
            } else {
                item { LockedPreview(journey, onUnlockClick = onOpenPaywall) }
            }
        }
    }
}

@Composable
private fun JourneyHeader(journey: RealJourney) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = journey.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = journey.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = journey.totalDuration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisclaimerBanner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EligibilityCard(items: List<String>) {
    ElevatedCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Eligibility snapshot", Icons.Outlined.CheckCircle)
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Text(text = item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun FeeCard(summary: String) {
    ElevatedCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Fees & payment structure", Icons.Outlined.Euro)
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PhaseCard(phase: JourneyPhase, expanded: Boolean, onToggle: () -> Unit) {
    ElevatedCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = phase.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = phase.timeframe,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()
                    Text(text = phase.summary, style = MaterialTheme.typography.bodyMedium)

                    if (phase.documents.isNotEmpty()) {
                        SubSection("Documents") {
                            phase.documents.forEach { doc ->
                                IconRow(icon = Icons.Outlined.Description, text = doc)
                            }
                        }
                    }

                    if (phase.lawyerPatterns.isNotEmpty()) {
                        SubSection("What the lawyer typically says") {
                            phase.lawyerPatterns.forEach { p ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = p.situation,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = p.phrasing,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    if (phase.gotchas.isNotEmpty()) {
                        SubSection("Gotchas") {
                            phase.gotchas.forEach { g ->
                                IconRow(icon = Icons.Outlined.Warning, text = g, tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrossGotchasCard(journey: RealJourney) {
    ElevatedCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Cross-phase gotchas worth knowing", Icons.Outlined.LightbulbCircle)
            journey.crossPhaseGotchas.forEach { g ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = g.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = g.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedPreview(journey: RealJourney, onUnlockClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElevatedCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Inside this guide",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                journey.phases.forEach { phase ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = phase.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        ElevatedCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "What you'll get",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                listOf(
                    "Every document the lawyer requested, by phase",
                    "Real fee ranges + payment-split structure",
                    "Sanitized correspondence patterns (what the lawyer actually said)",
                    "Province-specific timing expectations",
                    "Subsanación playbook + 30-day deadline interpretation"
                ).forEach { item ->
                    IconRow(icon = Icons.Outlined.CheckCircle, text = item)
                }
            }
        }

        Button(
            onClick = onUnlockClick,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Unlock all Real Journeys with GoThere Pro", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SubSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun IconRow(icon: ImageVector, text: String, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
