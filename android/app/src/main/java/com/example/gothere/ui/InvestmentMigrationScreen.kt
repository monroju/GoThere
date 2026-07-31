@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.data.InvestmentMigration

/** Investment migration + CBI. Mirror of iOS InvestmentMigrationView. */
@Composable
fun InvestmentMigrationScreen(onDismiss: () -> Unit, onOpenAncestry: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment Routes") },
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
            Text("Buy your optionality", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Residency and second-passport routes via investment — for diversification, mobility, and a real Plan B.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("🏛️ Residency by investment (EU & LatAm)", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold)
            InvestmentMigration.residencyByInvestment.forEach { v ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${v.countryFlag} ${v.name}", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text(v.income, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Citizenship: ${v.pathToCitizenship} · Processing: ${v.processingTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { open(v.officialUrl) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Official program ↗", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Text("🛂 Citizenship by investment (second passport)", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold)

            // The $0 version → ancestry.
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpenAncestry() }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Have EU or Latin heritage? Claim it for ~\$0",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Citizenship by descent is the free version of a second passport — check your eligibility first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            InvestmentMigration.cbiPrograms.forEach { p ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.flag} ${p.country}", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("from $${"%,d".format(p.minInvestmentUSD)}",
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Text(p.route, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("⏱ ${p.timelineMonths} · ${p.perks}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { open(p.officialUrl) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Official program ↗", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Surface(color = Color(0xFFFF9800).copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📉 Recently ended / changed", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    InvestmentMigration.endedPrograms.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(InvestmentMigration.disclaimer, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
