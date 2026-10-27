@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.util.FirebaseUtil
import com.google.firebase.firestore.FieldValue

/**
 * Premium "concierge" tier. Mirror of iOS ConciergeView. SCAFFOLD ONLY: no live IAP/
 * pricing (operator decision). Captures a waitlist signal in Firestore
 * `conciergeWaitlist/{uid}`. Flip [isLiveTier] once the SKU + advisor network exist.
 */
@Composable
fun ConciergeScreen(onDismiss: () -> Unit) {
    val isLiveTier = false
    var joined by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val benefits = listOf(
        stringResource(R.string.con_f1) to stringResource(R.string.con_f1_sub),
        stringResource(R.string.con_f2) to stringResource(R.string.con_f2_sub),
        stringResource(R.string.con_f3) to stringResource(R.string.con_f3_sub),
        stringResource(R.string.con_f4) to stringResource(R.string.con_f4_sub)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.con_title)) },
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Concierge", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(if (isLiveTier) R.string.con_premium else R.string.con_soon),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(stringResource(R.string.con_intro),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            benefits.forEach { (title, detail) ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(detail, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (joined) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.con_on_list),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(onClick = {
                    submitting = true
                    val uid = FirebaseUtil.auth.currentUser?.uid
                    if (uid != null) {
                        com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.CONCIERGE_WAITLIST_JOINED)
                        FirebaseUtil.db.collection("conciergeWaitlist").document(uid)
                            .set(mapOf(
                                "uid" to uid,
                                "platform" to "android",
                                "createdAt" to FieldValue.serverTimestamp()
                            ), com.google.firebase.firestore.SetOptions.merge())
                            .addOnCompleteListener { submitting = false; joined = true }
                    } else {
                        submitting = false; joined = true
                    }
                }, enabled = !submitting, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (submitting) { CircularProgressIndicator(modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                    Text(stringResource(if (isLiveTier) R.string.con_talk else R.string.con_join),
                        fontWeight = FontWeight.SemiBold)
                }
                Text(stringResource(R.string.con_no_charge),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(stringResource(R.string.con_disclaimer),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
