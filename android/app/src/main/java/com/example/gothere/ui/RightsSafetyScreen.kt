@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.data.FamilyMoveData
import com.example.gothere.decision.Household
import com.example.gothere.decision.PersonalConsideration
import com.example.gothere.repository.CountrySafetyProfiles
import com.example.gothere.repository.UserConsiderationsStore

private fun PersonalConsideration.label(): String = when (this) {
    PersonalConsideration.LGBTQ -> "LGBTQ+"
    PersonalConsideration.Trans -> "Transgender"
    PersonalConsideration.Disabled -> "Disabled / Accessibility"
    PersonalConsideration.Veteran -> "Veteran"
    PersonalConsideration.Pregnant -> "Pregnant / Expecting"
    PersonalConsideration.Neurodivergent -> "Neurodivergent"
    PersonalConsideration.Senior -> "Senior (60+)"
    PersonalConsideration.Poc -> "Person of Color"
}

private val DISPLAY_ORDER = listOf(
    PersonalConsideration.LGBTQ, PersonalConsideration.Trans, PersonalConsideration.Disabled,
    PersonalConsideration.Veteran, PersonalConsideration.Pregnant,
    PersonalConsideration.Neurodivergent, PersonalConsideration.Senior, PersonalConsideration.Poc
)

/** Rights & Safety comparison. Mirror of iOS RightsSafetyView. */
@Composable
fun RightsSafetyScreen(onDismiss: () -> Unit, onOpenCare: (() -> Unit)? = null) {
    val context = LocalContext.current
    val initial = remember {
        val stored = UserConsiderationsStore.load(context).considerations
        if (stored.isEmpty()) setOf(PersonalConsideration.LGBTQ) else stored
    }
    var selected by remember { mutableStateOf(initial) }

    // Persist on dispose so the Resources "For You" section + DecisionTree stay in sync.
    DisposableEffect(Unit) {
        onDispose {
            val st = UserConsiderationsStore.load(context)
            val household = if (st.isSingleParent) Household.SingleParent else Household.Singles
            UserConsiderationsStore.save(context, selected, household)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rights & Safety") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Weight your move on what matters", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Compare destinations on the protections most relevant to you and your family — not just cost and visas.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("This applies to me / my family", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonalConsideration.entries.forEach { c ->
                    val on = c in selected
                    FilterChip(
                        selected = on,
                        onClick = { selected = if (on) selected - c else selected + c },
                        label = { Text(c.label(), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (selected.isEmpty()) {
                Text("Pick at least one consideration to compare destinations.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                DISPLAY_ORDER.filter { it in selected }.forEach { c ->
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(c.label(), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            FamilyMoveData.profiles.forEach { dest ->
                                val note = CountrySafetyProfiles.profile(context, dest.countryId)?.note(c)
                                if (note != null) {
                                    Card(colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("${dest.flag} ${dest.name}",
                                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text(note, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            onOpenCare?.let { open ->
                Card(
                    onClick = open,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Your meds & care abroad", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("ADHD meds, HRT, insulin — what's available and what you can carry in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Sourced from ILGA-Europe, EU directives, OECD, and SSA data. Informational only — laws change; verify current protections before deciding.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
