package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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
    var showReferral by remember { mutableStateOf(false) }

    val productDetails by purchaseManager.productDetails.collectAsState()
    
    val countryName = stringResource(
        when (countryId) {
            "portugal", "mexico", "ireland", "italy", "germany", "poland",
            "argentina", "hungary", "uk_ancestry" -> countryNameRes(countryId)
            "all" -> R.string.paywall_all_countries
            else -> R.string.paywall_country
        }
    )

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
    
    // No hardcoded fallback prices: a button whose Play price didn't load is hidden rather
    // than showing a number that may not match what Play charges.
    val singlePrice = countryProductId?.let { purchaseManager.getFormattedPrice(it) }
    val bundlePrice = purchaseManager.getFormattedPrice(PurchaseManager.PRODUCT_ALL_COUNTRIES)

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
        PurchaseManager.PRODUCT_EUROPE_BUNDLE -> stringResource(R.string.paywall_europe_bundle)
        PurchaseManager.PRODUCT_AMERICAS_BUNDLE -> stringResource(R.string.paywall_americas_bundle)
        else -> stringResource(R.string.paywall_region_bundle)
    }
    val regionBundleAvailable = regionBundleProductId != null && productDetails.containsKey(regionBundleProductId)
    val regionBundlePrice = regionBundleProductId?.let { purchaseManager.getFormattedPrice(it) } ?: ""

    val preferredSubProductId: String? = when {
        productDetails.containsKey(PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL) -> PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL
        productDetails.containsKey(PurchaseManager.PRODUCT_ALL_ACCESS_MONTHLY) -> PurchaseManager.PRODUCT_ALL_ACCESS_MONTHLY
        else -> null
    }
    val subPriceSuffix = stringResource(if (preferredSubProductId == PurchaseManager.PRODUCT_ALL_ACCESS_ANNUAL) R.string.paywall_per_year else R.string.paywall_per_month)
    val subPrice = preferredSubProductId?.let { purchaseManager.getFormattedPrice(it) } ?: ""

    // 02-metrics.md: paywall_viewed with source + products, paywall_dismissed when closed without buying.
    DisposableEffect(Unit) {
        val openedAt = System.currentTimeMillis()
        val source = com.example.gothere.util.Analytics.paywallSource
        com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.PAYWALL_VIEWED, mapOf(
            "source" to source,
            "variant" to "v1",
            "country_id" to countryId,
            "products_shown" to productDetails.keys.sorted(),
            "default_product" to (preferredSubProductId ?: "none")
        ))
        onDispose {
            if (!com.example.gothere.util.Analytics.purchasedSince(openedAt)) {
                com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.PAYWALL_DISMISSED, mapOf(
                    "source" to source,
                    "seconds_on_screen" to ((System.currentTimeMillis() - openedAt) / 1000).toInt()
                ))
            }
        }
    }

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
                            contentDescription = stringResource(R.string.close),
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
                    text = stringResource(R.string.paywall_unlock_country, countryName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.paywall_subtitle, countryName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features list
                FeaturesList(countryName)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Single country purchase button
                if (countryProductId != null && singlePrice != null) {
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
                            text = stringResource(R.string.paywall_single_cta, singlePrice, countryName),
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
                            text = pluralRes(R.plurals.paywall_region_cta, regionBundleCount, regionBundlePrice, regionBundleLabel, regionBundleCount),
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
                if (bundlePrice != null) OutlinedButton(
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
                                text = stringResource(R.string.paywall_lifetime_cta, bundlePrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.paywall_lifetime_sub),
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
                            text = stringResource(R.string.paywall_subscribe_cta, subPrice, subPriceSuffix),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.paywall_renews, subPrice, subPriceSuffix),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Referral cross-sell — unlock a month free instead of paying.
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { showReferral = true }) {
                    Text(
                        text = stringResource(R.string.paywall_invite),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Maybe later link
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.maybe_later),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showReferral) {
                    ReferralDialog(
                        onDismiss = { showReferral = false },
                        purchaseManager = purchaseManager
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturesList(countryName: String) {
    val features = listOf(
        stringResource(R.string.paywall_feat_tasks),
        stringResource(R.string.paywall_feat_tree),
        stringResource(R.string.paywall_feat_guides),
        stringResource(R.string.paywall_feat_resources),
        stringResource(R.string.paywall_feat_pay)
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
    val countryName = stringResource(
        when (countryId) {
            "portugal", "mexico", "ireland", "italy", "germany", "poland",
            "argentina", "hungary", "uk_ancestry" -> countryNameRes(countryId)
            else -> R.string.paywall_country
        }
    )

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
                    text = stringResource(R.string.paywall_unlock_country, countryName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.paywall_banner_sub, countryName),
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
                Text(stringResource(R.string.unlock))
            }
        }
    }
}
