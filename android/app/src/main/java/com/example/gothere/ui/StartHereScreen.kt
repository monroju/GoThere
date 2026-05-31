@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * "Where do I start?" triage funnel. Mirror of iOS StartHereView. Routes users into
 * the tool matching their situation/tier via callbacks handled by ResourcesScreen.
 */
@Composable
fun StartHereScreen(
    onDismiss: () -> Unit,
    onCostCalc: () -> Unit,
    onFamily: () -> Unit,
    onRemote: () -> Unit,
    onAncestry: () -> Unit,
    onInvestment: () -> Unit,
    onRights: () -> Unit,
    onCompare: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where do I start?") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Where do I start?", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Pick what fits your situation — we'll take you straight to the right tool.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            route("Money's tight — is this even possible?",
                "See the real cost to move + the cheapest visa paths.", onCostCalc)
            route("I'm moving with kids",
                "Schooling, healthcare & child visas, country by country.", onFamily)
            route("I work remotely",
                "Employer-letter templates + the tax traps to avoid.", onRemote)
            route("I might have EU or Latin heritage",
                "You could already be a citizen — check your eligibility.", onAncestry)
            route("I want a Plan B / second passport",
                "Golden visas & citizenship by investment.", onInvestment)
            route("I'm worried about rights or safety",
                "Compare destinations on the protections that matter to you.", onRights)
            route("Just show me my options",
                "Compare every visa side by side.", onCompare)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun route(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
