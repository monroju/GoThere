@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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
            out += CareSection("adhd", context.getString(R.string.cc_adhd), Icons.Outlined.Psychology, it)
        }
        profile?.hrt?.let {
            out += CareSection("hrt", context.getString(R.string.cc_hrt), Icons.Outlined.Medication, it)
        }
        profile?.insulin?.let {
            out += CareSection("insulin", context.getString(R.string.cc_insulin), Icons.Outlined.MonitorHeart, it)
        }
        profile?.bringIn?.let {
            out += CareSection("bring_in", context.getString(R.string.cc_bring_in), Icons.Outlined.FlightLand, it)
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
                title = { Text(stringResource(R.string.res_tool_meds)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Text(stringResource(R.string.cc_header), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.cc_intro),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(stringResource(R.string.mc_destination), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FamilyMoveData.profiles.forEach { dest ->
                    val on = countryId == dest.countryId
                    FilterChip(
                        selected = on,
                        onClick = { countryId = dest.countryId },
                        label = { Text("${dest.flag} ${countryDisplayName(dest.countryId, dest.name)}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (sections.isEmpty()) {
                Text(stringResource(R.string.cc_no_data),
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
            Text(stringResource(R.string.cc_sources),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
