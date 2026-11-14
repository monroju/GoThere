package com.example.gothere.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gothere.R
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.planb.PlanBPath
import com.example.gothere.planb.PlanBScore
import com.example.gothere.util.Analytics

/**
 * One-time offer shown right after a descent path is tracked: the country's plan first
 * (one purchase), All-Access second. At most once per path, never when the country is
 * already unlocked. The checklist and score stay free.
 */
object PlanBUnlockOffer {
    private const val PREFS = "planb_offer"

    fun productId(countryId: String) = "com.gothere.${countryId}_pack"

    /** True (and marks it shown) when the offer should appear for this path now. */
    fun claim(context: Context, pathId: String, countryId: String, purchaseManager: PurchaseManager): Boolean {
        if (countryId !in PurchaseManager.ALL_PAID_COUNTRIES) return false
        if (purchaseManager.isCountryUnlocked(countryId)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(pathId, false)) return false
        prefs.edit().putBoolean(pathId, true).apply()
        return true
    }
}

@Composable
fun PlanBUnlockOfferDialog(
    path: PlanBPath,
    countryId: String,
    countryName: String,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val purchaseManager = remember { PurchaseManager.getInstance(context) }
    val productDetails by purchaseManager.productDetails.collectAsState()
    val unlockedCountries by purchaseManager.purchasedCountries.collectAsState()
    val productId = PlanBUnlockOffer.productId(countryId)
    val price = remember(productDetails) { purchaseManager.getFormattedPrice(productId) }
    var showAllAccess by remember { mutableStateOf(false) }
    val props = mapOf("cc" to path.countryCode, "country_id" to countryId)

    LaunchedEffect(Unit) { Analytics.log(Analytics.PLANB_OFFER_SHOWN, props) }
    LaunchedEffect(unlockedCountries) { if (countryId in unlockedCountries) onDone() }

    if (showAllAccess) {
        PaywallDialog(countryId = "all", onDismiss = { showAllAccess = false; onDone() }, purchaseManager = purchaseManager)
        return
    }

    AlertDialog(
        onDismissRequest = {
            Analytics.log(Analytics.PLANB_OFFER_TAPPED, props + ("choice" to "dismiss"))
            onDone()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score = PlanBScore.path(path), size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.planb_offer_title, countryName), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.planb_offer_progress, path.doneCount, path.activeItems.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.planb_offer_heading, countryName), style = MaterialTheme.typography.titleSmall)
                Text("• " + stringResource(R.string.planb_offer_b1))
                Text("• " + stringResource(R.string.planb_offer_b2, countryName))
                Text("• " + stringResource(R.string.planb_offer_b3))
                Button(
                    onClick = {
                        Analytics.log(Analytics.PLANB_OFFER_TAPPED, props + ("choice" to "country"))
                        Analytics.paywallSource = "planb_offer"
                        activity?.let { purchaseManager.launchPurchaseFlow(it, productId) }
                    },
                    enabled = price != null && activity != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(price?.let { stringResource(R.string.planb_offer_buy_price, countryName, it) }
                        ?: stringResource(R.string.planb_offer_buy, countryName))
                }
                TextButton(onClick = {
                    Analytics.log(Analytics.PLANB_OFFER_TAPPED, props + ("choice" to "all_access"))
                    Analytics.paywallSource = "planb_offer"
                    showAllAccess = true
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.planb_offer_all_access)) }
                Text(stringResource(R.string.planb_offer_free_note), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                Analytics.log(Analytics.PLANB_OFFER_TAPPED, props + ("choice" to "dismiss"))
                onDone()
            }) { Text(stringResource(R.string.planb_offer_not_now)) }
        },
    )
}
