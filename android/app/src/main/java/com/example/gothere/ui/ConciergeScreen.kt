@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

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
        "Vetted advisor matching" to "Hand-picked immigration lawyers, tax advisors, and relocation agents in your target country — pre-screened, not a directory dump.",
        "Done-with-you visa prep" to "We help assemble and sanity-check your application packet against the consulate's exact checklist.",
        "Priority support" to "Direct line for time-sensitive questions during your move window.",
        "Settling-in concierge" to "Warm intros for housing, banking, and schools — the things that take locals to unlock."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoThere Concierge") },
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Concierge", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                    Text(if (isLiveTier) "PREMIUM" else "COMING SOON",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text("White-glove relocation support for people who'd rather pay to get it right than spend months figuring it out alone.",
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
                    Text("You're on the list — we'll be in touch.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(onClick = {
                    submitting = true
                    val uid = FirebaseUtil.auth.currentUser?.uid
                    if (uid != null) {
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
                    Text(if (isLiveTier) "Talk to a concierge" else "Join the waitlist",
                        fontWeight = FontWeight.SemiBold)
                }
                Text("No charge to express interest. We'll only reach out when it launches.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("Concierge connects you with independent, licensed professionals. GoThere is not a law firm and does not provide legal or tax advice.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}
