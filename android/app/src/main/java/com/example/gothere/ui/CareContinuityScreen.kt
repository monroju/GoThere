@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FlightLand
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.data.FamilyMoveData
import com.example.gothere.decision.PersonalConsideration
import com.example.gothere.repository.CareContinuityProfiles
import com.example.gothere.repository.UserConsiderationsStore

private data class CareSection(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val note: String
)

/**
 * "Meds & Care Abroad" — per-country medication & treatment availability.
 * Mirror of iOS CareContinuityView. Section order adapts to stored
 * PersonalConsiderations (Trans → HRT first, Neurodivergent → ADHD first).
 */
@Composable
fun CareContinuityScreen(initialCountryId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var countryId by remember { mutableStateOf(initialCountryId) }

    val sections = remember(countryId) {
        val profile = CareContinuityProfiles.profile(context, countryId)
        val out = mutableListOf<CareSection>()
        profile?.adhd?.let {
            out += CareSection("adhd", "ADHD medication", Icons.Outlined.Psychology, it)
        }
        profile?.hrt?.let {
            out += CareSection("hrt", "Hormone therapy (HRT)", Icons.Outlined.Medication, it)
        }
        profile?.insulin?.let {
            out += CareSection("insulin", "Insulin & diabetes care", Icons.Outlined.MonitorHeart, it)
        }
        profile?.bringIn?.let {
            out += CareSection("bring_in", "What you can carry in", Icons.Outlined.FlightLand, it)
        }
        val considerations = UserConsiderationsStore.load(context).considerations
        when {
            PersonalConsideration.Trans in considerations ->
                out.sortedByDescending { it.id == "hrt" }
            PersonalConsideration.Neurodivergent in considerations ->
                out.sortedByDescending { it.id == "adhd" }
            else -> out
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meds & Care Abroad") },
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
            Text("Will your treatment travel with you?", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Availability of your meds, who can prescribe them, and what you're allowed to bring through the border.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Destination", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FamilyMoveData.profiles.forEach { dest ->
                    val on = countryId == dest.countryId
                    FilterChip(
                        selected = on,
                        onClick = { countryId = dest.countryId },
                        label = { Text("${dest.flag} ${dest.name}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (sections.isEmpty()) {
                Text("No continuity data for this country yet.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                sections.forEach { section ->
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(section.icon, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(section.title, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(section.note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Text("Sourced from INCB country regulations, national medicine agencies, and the CDC Yellow Book. Informational only — always confirm with the destination's customs authority and a clinician before travelling with medication.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
