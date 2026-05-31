@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.data.HealthcareCostData
import com.example.gothere.data.HealthcareProfile

/** Healthcare cost comparison vs the US. Mirror of iOS HealthcareCompareView. */
@Composable
fun HealthcareCompareScreen(initialCountryId: String?, onDismiss: () -> Unit) {
    var countryId by remember {
        mutableStateOf(initialCountryId ?: HealthcareCostData.profiles.firstOrNull()?.countryId ?: "spain")
    }
    val profile = HealthcareCostData.profile(countryId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Healthcare Costs") },
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
            Text("What healthcare really costs", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Typical private premiums vs the US — plus what the public system covers once you're a resident.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ToolCountryPicker(HealthcareCostData.profiles.map { it.countryId to "${it.flag} ${it.name}" }, countryId) { countryId = it }

            profile?.let { p ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Private insurance — monthly", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        compareRow("${p.flag} Single", p.privateMonthlySingleUSD,
                            "🇺🇸 US marketplace", HealthcareCostData.usSingleMarketplaceMonthlyUSD)
                        HorizontalDivider()
                        compareRow("${p.flag} Family of 4", p.privateMonthlyFamilyUSD,
                            "🇺🇸 US family (total)", HealthcareCostData.usFamilyMonthlyTotalUSD)
                        val savings = (HealthcareCostData.usFamilyMonthlyTotalUSD - p.privateMonthlyFamilyUSD) * 12
                        if (savings > 0) {
                            Text("≈ $${"%,d".format(savings)}/yr less than a US family plan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🏥 Public system", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(p.publicSystem, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ToolSection("ℹ️ Note", p.note)
            }
            Text("Estimates only — premiums vary by age, health, and plan. US figures from 2024 KFF/marketplace averages. Not insurance advice.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun compareRow(destLabel: String, destUSD: Int, usLabel: String, usUSD: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(destLabel, style = MaterialTheme.typography.bodyMedium)
            Text("$${"%,d".format(destUSD)}/mo", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(usLabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$${"%,d".format(usUSD)}/mo", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
