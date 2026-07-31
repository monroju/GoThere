package com.example.gothere.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.billing.ReferralInfo
import com.example.gothere.billing.ReferralRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Give-a-month / get-a-month referral dialog. Mirrors iOS ReferralView.
 *
 * Shows the signed-in user's shareable code (with a system share sheet) and a
 * redeem field. Redeeming grants both users a 30-day server-side All-Access window
 * (users/{uid}.promoAccessUntil), mirrored locally via PurchaseManager.applyPromoGrant
 * for an instant unlock. Requires a signed-in Firebase user.
 */
@Composable
fun ReferralDialog(
    onDismiss: () -> Unit,
    purchaseManager: PurchaseManager
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repo = remember { ReferralRepository() }
    val signedIn = remember { FirebaseAuth.getInstance().currentUser != null }

    var info by remember { mutableStateOf<ReferralInfo?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var enteredCode by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }
    var redeemMessage by remember { mutableStateOf<String?>(null) }
    var redeemSucceeded by remember { mutableStateOf(false) }

    fun loadCode() {
        isLoading = true
        loadError = null
        scope.launch {
            try {
                info = repo.fetchCode()
            } catch (e: Exception) {
                loadError = "Couldn't load your code. Check your connection and try again."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (signedIn && info == null) loadCode()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("🎁", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Give a month, get a month",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Share your code. When a friend redeems it, you both get 30 days of All-Access — every country, every visa path — free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                if (!signedIn) {
                    Text(
                        "Create a free account to invite friends and redeem codes. Referral months are tied to your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Your code
                    when {
                        isLoading -> CircularProgressIndicator()
                        info != null -> {
                            val current = info!!
                            Text(
                                current.code,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { clipboard.setText(AnnotatedString(current.code)) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Copy") }
                                Button(
                                    onClick = {
                                        val msg = "I'm using GoThere to plan my move abroad — it's genuinely useful. " +
                                            "Use my code ${current.code} and we both get a free month of All-Access: ${current.shareUrl}"
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Invite a friend"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Share") }
                            }
                        }
                        loadError != null -> {
                            Text(loadError!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                            TextButton(onClick = { loadCode() }) { Text("Try again") }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // Redeem
                    Text(
                        "Got a code from a friend?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (redeemSucceeded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                redeemMessage ?: "You're in — enjoy your free month!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = enteredCode,
                            onValueChange = { enteredCode = it.uppercase() },
                            label = { Text("Enter code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (redeemMessage != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(redeemMessage!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isRedeeming = true
                                redeemMessage = null
                                scope.launch {
                                    try {
                                        val result = repo.redeem(enteredCode.trim())
                                        purchaseManager.applyPromoGrant(result.premiumUntilSeconds)
                                        redeemSucceeded = true
                                        redeemMessage = "You're in — ${result.rewardDays} days of All-Access unlocked."
                                    } catch (e: Exception) {
                                        redeemMessage = e.message ?: "Couldn't redeem that code. Please try again."
                                    }
                                    isRedeeming = false
                                }
                            },
                            enabled = enteredCode.isNotBlank() && !isRedeeming,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRedeeming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Redeem")
                            }
                        }
                    }
                }
            }
        }
    }
}
