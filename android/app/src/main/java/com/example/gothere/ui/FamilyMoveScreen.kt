@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.data.FamilyMoveData

/** Moving-with-kids guide. Mirror of iOS FamilyMoveView. */
@Composable
fun FamilyMoveScreen(initialCountryId: String?, onDismiss: () -> Unit) {
    var countryId by remember {
        mutableStateOf(initialCountryId ?: FamilyMoveData.profiles.firstOrNull()?.countryId ?: "spain")
    }
    val profile = FamilyMoveData.profile(countryId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.res_tool_family_title)) },
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
            Text(stringResource(R.string.fam_header), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.fam_intro),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ToolCountryPicker(FamilyMoveData.profiles.map { it.countryId to "${it.flag} ${countryDisplayName(it.countryId, it.name)}" }, countryId) { countryId = it }

            profile?.let { p ->
                ToolSection("🎒 Public schooling", p.publicSchooling)
                ToolSection("🏫 International schools", p.internationalSchooling)
                ToolSection("🏠 Homeschooling", p.homeschooling)
                ToolSection("🩺 Children's healthcare", p.childHealthcare)
                ToolSection("🛂 Kids on your visa", p.childVisaNote)
                ToolTipsCard("👪 ${p.name} parent tips", p.tips)
            }
            ToolTipsCard(stringResource(R.string.fam_before_leave), FamilyMoveData.universalTips)
            Text(stringResource(R.string.fam_disclaimer),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---- Shared tool UI helpers (used by the tier-targeting tool screens) ----

@Composable
fun ToolCountryPicker(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = id == selected,
                onClick = { onSelect(id) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun ToolSection(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ToolTipsCard(title: String, tips: List<String>) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            tips.forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
