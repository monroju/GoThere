package com.example.gothere.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gothere.billing.PurchaseManager

/**
 * Paywall dialog shown when user tries to access a locked country
 */
@Composable
fun PaywallDialog(
    countryId: String,
    onDismiss: () -> Unit,
    purchaseManager: PurchaseManager
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val productDetails by purchaseManager.productDetails.collectAsState()
    
    val countryName = when (countryId) {
        "portugal" -> "Portugal"
        "mexico" -> "Mexico"
        "ireland" -> "Ireland"
        "italy" -> "Italy"
        "germany" -> "Germany"
        "poland" -> "Poland"
        "argentina" -> "Argentina"
        "hungary" -> "Hungary"
        "uk_ancestry" -> "UK (Ancestry)"
        else -> "Country"
    }

    val countryFlag = when (countryId) {
        "portugal" -> "🇵🇹"
        "mexico" -> "🇲🇽"
        "ireland" -> "🇮🇪"
        "italy" -> "🇮🇹"
        "germany" -> "🇩🇪"
        "poland" -> "🇵🇱"
        "argentina" -> "🇦🇷"
        "hungary" -> "🇭🇺"
        "uk_ancestry" -> "🇬🇧"
        else -> "🌍"
    }

    val countryProductId = when (countryId) {
        "portugal" -> PurchaseManager.PRODUCT_PORTUGAL
        "mexico" -> PurchaseManager.PRODUCT_MEXICO
        "ireland" -> PurchaseManager.PRODUCT_IRELAND
        "italy" -> PurchaseManager.PRODUCT_ITALY
        "germany" -> PurchaseManager.PRODUCT_GERMANY
        "poland" -> PurchaseManager.PRODUCT_POLAND
        "argentina" -> PurchaseManager.PRODUCT_ARGENTINA
        "hungary" -> PurchaseManager.PRODUCT_HUNGARY
        "uk_ancestry" -> PurchaseManager.PRODUCT_UK_ANCESTRY
        else -> null
    }
    
    val singlePrice = countryProductId?.let { purchaseManager.getFormattedPrice(it) } ?: "$3.99"
    val bundlePrice = purchaseManager.getFormattedPrice(PurchaseManager.PRODUCT_ALL_COUNTRIES) ?: "$5.99"

    // Item 11 mirror — new freemium SKUs surface as additional CTAs when Play Console
    // has them configured (Item 13 pending). Hidden when not yet loaded so the dialog
    // degrades cleanly to the original two-button layout.
    val regionBundleProductId: String? = when {
        countryId in PurchaseManager.EUROPE_BUNDLE_COUNTRIES -> PurchaseManager.PRODUCT_EUROPE_BUNDLE
        countryId in PurchaseManager.AMERICAS_BUNDLE_COUNTRIES -> PurchaseManager.PRODUCT_AMERICAS_BUNDLE
        else -> null
    }
    val regionBundleCount = when (regionBundleProductId) {
        PurchaseManager.PRODUCT_EUROPE_BUNDLE -> PurchaseManager.EUROPE_BUNDLE_COUNTRIES.size
        PurchaseManager.PRODUCT_AMERICAS_BUNDLE -> PurchaseManager.AMERICAS_BUNDLE_COUNTRIES.size
        else -> 0
    }
    val regionBundleLabel = when (regionBundleProductId) {
        PurchaseManager.PRODUCT_EUROPE_BUNDLE -> "Europe Bundle"
        PurchaseManager.PRODUCT_AMERICAS_BUNDLE -> "Americas Bundle"
        else -> "Region Bundle"
    }
    val regionBundleAvailable = regionBundleProductId != null && productDetails.containsKey(regionBundleProductId)
    val regionBundlePrice = regionBundleProductId?.let { purchaseManager.getFormattedPrice(it) } ?: ""

    val preferredSubProductId: String? = when {
        productDetails.containsKey(PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL) -> PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL
        productDetails.containsKey(PurchaseManager.PRODUCT_ALL_ACCESS_MONTHLY) -> PurchaseManager.PRODUCT_ALL_ACCESS_MONTHLY
        else -> null
    }
    val subPriceSuffix = if (preferredSubProductId == PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL) "/yr" else "/mo"
    val subPrice = preferredSubProductId?.let { purchaseManager.getFormattedPrice(it) } ?: ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Flag and title
                Text(
                    text = countryFlag,
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Unlock $countryName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get the complete $countryName relocation toolkit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features list
                FeaturesList(countryName)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Single country purchase button
                if (countryProductId != null) {
                    Button(
                        onClick = {
                            activity?.let { act ->
                                purchaseManager.launchPurchaseFlow(act, countryProductId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "$singlePrice — Unlock $countryName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Region bundle button — contextual to the country being viewed.
                // Hidden when the Play Console product isn't loaded yet (Item 13).
                if (regionBundleAvailable && regionBundleProductId != null) {
                    OutlinedButton(
                        onClick = {
                            activity?.let { act ->
                                purchaseManager.launchPurchaseFlow(act, regionBundleProductId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "$regionBundlePrice — $regionBundleLabel · $regionBundleCount countries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Lifetime All-Access (legacy all_countries SKU repositioned).
                // The misleading "Save over 80%" copy was dropped — subscriptions and region
                // bundles now exist so a single fixed percentage no longer makes sense.
                OutlinedButton(
                    onClick = {
                        activity?.let { act ->
                            purchaseManager.launchPurchaseFlow(act, PurchaseManager.PRODUCT_ALL_COUNTRIES)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$bundlePrice — Lifetime All-Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pay once · every country, current and future",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // All-Access subscription — surface the recommended plan price.
                // Hidden when no Play Console subscription product is loaded.
                if (preferredSubProductId != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            activity?.let { act ->
                                purchaseManager.launchPurchaseFlow(act, preferredSubProductId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Or subscribe — $subPrice$subPriceSuffix",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Maybe later link
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Maybe Later",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturesList(countryName: String) {
    val features = listOf(
        "20+ visa & documentation tasks",
        "Decision tree for 15+ cities",
        "Neighborhood guides & tips",
        "Country-specific resources",
        "Pay once or subscribe — your call"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Compact paywall banner shown in-screen (alternative to dialog)
 */
@Composable
fun PaywallBanner(
    countryId: String,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countryName = when (countryId) {
        "portugal" -> "Portugal"
        "mexico" -> "Mexico"
        "ireland" -> "Ireland"
        "italy" -> "Italy"
        "germany" -> "Germany"
        "poland" -> "Poland"
        "argentina" -> "Argentina"
        "hungary" -> "Hungary"
        "uk_ancestry" -> "UK (Ancestry)"
        else -> "Country"
    }

    val countryFlag = when (countryId) {
        "portugal" -> "🇵🇹"
        "mexico" -> "🇲🇽"
        "ireland" -> "🇮🇪"
        "italy" -> "🇮🇹"
        "germany" -> "🇩🇪"
        "poland" -> "🇵🇱"
        "argentina" -> "🇦🇷"
        "hungary" -> "🇭🇺"
        "uk_ancestry" -> "🇬🇧"
        else -> "🌍"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = countryFlag,
                fontSize = 32.sp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock $countryName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Get full access to $countryName tasks & guides",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Unlock")
            }
        }
    }
}
