@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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
                title = { Text(stringResource(R.string.res_tool_start)) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.res_tool_start), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.start_pick),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            route(stringResource(R.string.start_money),
                stringResource(R.string.start_money_sub), onCostCalc)
            route(stringResource(R.string.start_kids),
                stringResource(R.string.start_kids_sub), onFamily)
            route(stringResource(R.string.start_remote),
                stringResource(R.string.start_remote_sub), onRemote)
            route(stringResource(R.string.start_heritage),
                stringResource(R.string.start_heritage_sub), onAncestry)
            route(stringResource(R.string.start_planb),
                stringResource(R.string.start_planb_sub), onInvestment)
            route(stringResource(R.string.start_rights),
                stringResource(R.string.start_rights_sub), onRights)
            route(stringResource(R.string.start_options),
                stringResource(R.string.start_options_sub), onCompare)
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
