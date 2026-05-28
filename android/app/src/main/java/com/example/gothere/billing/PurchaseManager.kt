package com.example.gothere.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.gothere.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine for the all-access subscription introduced in the freemium pivot.
 * Mirrors iOS `SubscriptionStatus` exactly so the Firestore document format is shared.
 *
 * Note: Android Play Billing does not expose subscription expiration timestamps via
 * BillingClient.Purchase. Until Item 12 ships server-side validation via
 * Real-Time Developer Notifications, `Active.expiresAtSeconds` will typically be null
 * on this client — the iOS side populates it because StoreKit returns it directly.
 */
sealed class SubscriptionStatus {
    object None : SubscriptionStatus()
    data class Active(val expiresAtSeconds: Double?) : SubscriptionStatus()
    object BillingRetry : SubscriptionStatus()
    object GracePeriod : SubscriptionStatus()
    object Expired : SubscriptionStatus()

    val isActive: Boolean get() = this is Active

    /** Firestore-friendly representation. Wire format matches iOS `dictionaryRepresentation`. */
    fun toFirestoreMap(): Map<String, Any?> = when (this) {
        None -> mapOf("state" to "none")
        Expired -> mapOf("state" to "expired")
        BillingRetry -> mapOf("state" to "billingRetry")
        GracePeriod -> mapOf("state" to "gracePeriod")
        is Active -> {
            val base = mutableMapOf<String, Any?>("state" to "active")
            if (expiresAtSeconds != null) base["expiresAt"] = expiresAtSeconds
            base
        }
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>?): SubscriptionStatus {
            if (map == null) return None
            return when (map["state"] as? String) {
                "active" -> Active((map["expiresAt"] as? Number)?.toDouble())
                "expired" -> Expired
                "billingRetry" -> BillingRetry
                "gracePeriod" -> GracePeriod
                else -> None
            }
        }
    }
}

/**
 * Manages in-app purchases and the all-access subscription via Google Play Billing.
 *
 * Two product families coexist during the freemium pivot:
 *   • Legacy per-country one-time IAPs (`com.gothere.<country>_pack`) — pre-freemium
 *     buyers keep their entitlements via the existing `purchasedCountries` set.
 *   • New all-access subscriptions (`com.gothere.all_access_*`) and region bundles
 *     (`com.gothere.europe_bundle`, `americas_bundle`) — drive `hasAllAccess` and
 *     surface in the new PaywallScreen sections (Item 11 mirrored from iOS).
 *
 * Call sites that read `purchasedCountries` keep working unchanged — whenever the
 * user has all-access through a subscription, the `all_countries` IAP, or both
 * region bundles, every paid country is automatically added to the set.
 */
class PurchaseManager private constructor(private val appContext: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PurchaseManager"

        // DEPRECATED 2026-Q3 — kept for owners pre-freemium. Existing buyers continue to
        // unlock the corresponding country via these one-time IAPs. New users see the
        // subscription + bundle SKUs below as the headline offering on PaywallScreen.
        const val PRODUCT_PORTUGAL = "com.gothere.portugal_pack"
        const val PRODUCT_MEXICO = "com.gothere.mexico_pack"
        const val PRODUCT_IRELAND = "com.gothere.ireland_pack"
        const val PRODUCT_ITALY = "com.gothere.italy_pack"
        const val PRODUCT_GERMANY = "com.gothere.germany_pack"
        const val PRODUCT_POLAND = "com.gothere.poland_pack"
        const val PRODUCT_ARGENTINA = "com.gothere.argentina_pack"
        const val PRODUCT_HUNGARY = "com.gothere.hungary_pack"
        const val PRODUCT_UK_ANCESTRY = "com.gothere.uk_ancestry_pack"
        const val PRODUCT_ALL_COUNTRIES = "com.gothere.all_countries"

        // New SKUs for the freemium pivot. Runtime-safe — queryProductDetails tolerates
        // products that aren't yet configured in Play Console; they simply don't appear
        // in `productDetails` until they're created.
        const val PRODUCT_ALL_ACCESS_MONTHLY = "com.gothere.all_access_monthly"
        const val PRODUCT_ALL_ACCESS_ANNUAL = "com.gothere.all_access_annual"
        const val PRODUCT_EUROPE_BUNDLE = "com.gothere.europe_bundle"
        const val PRODUCT_AMERICAS_BUNDLE = "com.gothere.americas_bundle"

        /** Countries unlocked by the Europe region bundle. Excludes Spain (always free). */
        val EUROPE_BUNDLE_COUNTRIES = listOf(
            "portugal", "ireland", "italy", "germany", "poland", "hungary", "uk_ancestry"
        )

        /** Countries unlocked by the Americas region bundle. Excludes Canada (always free). */
        val AMERICAS_BUNDLE_COUNTRIES = listOf("mexico", "argentina")

        val ALL_PAID_COUNTRIES = EUROPE_BUNDLE_COUNTRIES + AMERICAS_BUNDLE_COUNTRIES

        // Spain and Canada are always free.
        // Spain is GoThere's original launch destination.
        // Canada is free for the v1 launch of the Fast-Track Eligibility module to ride the Bill C-3 news cycle.
        private val FREE_COUNTRIES = setOf("spain", "canada")

        // SKU sets for fast type lookups in processPurchases.
        private val LEGACY_INAPP_SKUS = setOf(
            PRODUCT_PORTUGAL, PRODUCT_MEXICO, PRODUCT_IRELAND, PRODUCT_ITALY,
            PRODUCT_GERMANY, PRODUCT_POLAND, PRODUCT_ARGENTINA, PRODUCT_HUNGARY,
            PRODUCT_UK_ANCESTRY, PRODUCT_ALL_COUNTRIES
        )
        private val BUNDLE_INAPP_SKUS = setOf(PRODUCT_EUROPE_BUNDLE, PRODUCT_AMERICAS_BUNDLE)
        private val SUBSCRIPTION_SKUS = setOf(PRODUCT_ALL_ACCESS_MONTHLY, PRODUCT_ALL_ACCESS_ANNUAL)

        @Volatile
        private var INSTANCE: PurchaseManager? = null

        fun getInstance(context: Context): PurchaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PurchaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _purchasedCountries = MutableStateFlow<Set<String>>(FREE_COUNTRIES)
    val purchasedCountries: StateFlow<Set<String>> = _purchasedCountries.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.None)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    /**
     * Tracks owned non-consumable product IDs (legacy packs + region bundles + all_countries).
     * Subscriptions are reflected via `subscriptionStatus` instead.
     */
    private val _ownedSKUs = MutableStateFlow<Set<String>>(emptySet())
    val ownedSKUs: StateFlow<Set<String>> = _ownedSKUs.asStateFlow()

    init {
        connectToPlayBilling()
        loadPurchasesFromFirestore()
    }

    private fun connectToPlayBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Billing client connected")
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchases()
                } else {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                if (BuildConfig.DEBUG) Log.w(TAG, "Billing service disconnected")
                _isConnected.value = false
                connectToPlayBilling()
            }
        })
    }

    private fun queryProductDetails() {
        // Legacy + bundle SKUs are INAPP; subscription SKUs are SUBS. Both are queried
        // in one call — Play Billing handles mixed product types.
        val productList = buildList {
            (LEGACY_INAPP_SKUS + BUNDLE_INAPP_SKUS).forEach { sku ->
                add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            }
            SUBSCRIPTION_SKUS.forEach { sku ->
                add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            }
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsMap = productDetailsList.associateBy { it.productId }
                _productDetails.value = detailsMap
                if (BuildConfig.DEBUG) Log.d(TAG, "Product details loaded: ${detailsMap.keys}")
            } else {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Queries both INAPP and SUBS purchase sets. Each result is merged in
     * [reconcileFromPurchases] so the user's full entitlement picture is recomputed
     * atomically. Subscription expiration (sub gone from currentEntitlements) is
     * inferred by passing `hadPreviousSub` into the SUBS handler.
     */
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { inappResult, inappPurchases ->
            if (inappResult.responseCode != BillingClient.BillingResponseCode.OK) {
                if (BuildConfig.DEBUG) Log.e(TAG, "INAPP queryPurchases failed: ${inappResult.debugMessage}")
            }
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { subsResult, subsPurchases ->
                if (subsResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "SUBS queryPurchases failed: ${subsResult.debugMessage}")
                }
                reconcileFromPurchases(
                    inappPurchases = inappPurchases.orEmpty(),
                    subsPurchases = subsPurchases.orEmpty(),
                    fromQueryReconcile = true
                )
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "User canceled purchase")
            }
            else -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Handles purchases arriving from a fresh transaction (onPurchasesUpdated).
     * Splits the list by product type and delegates to [reconcileFromPurchases].
     * Note: a single onPurchasesUpdated callback only contains one product type at
     * a time (Play Billing returns INAPP and SUBS through separate paths), so we
     * partition by whether the SKU appears in [SUBSCRIPTION_SKUS].
     */
    private fun processPurchases(purchases: List<Purchase>) {
        val (subsPurchases, inappPurchases) = purchases.partition { purchase ->
            purchase.products.any { it in SUBSCRIPTION_SKUS }
        }
        reconcileFromPurchases(
            inappPurchases = inappPurchases,
            subsPurchases = subsPurchases,
            fromQueryReconcile = false
        )
    }

    /**
     * Single source of truth for translating Play Billing purchases into our state.
     * Walks the INAPP + SUBS purchase lists, computes the unlocked-countries set
     * (including all-access expansion), updates owned SKUs and subscription status,
     * and syncs to Firestore.
     *
     * @param fromQueryReconcile true when called from launch-time queryPurchases —
     *   enables the "sub disappeared → mark expired" transition. False during fresh
     *   onPurchasesUpdated calls (which may legitimately contain INAPP only).
     */
    private fun reconcileFromPurchases(
        inappPurchases: List<Purchase>,
        subsPurchases: List<Purchase>,
        fromQueryReconcile: Boolean
    ) {
        val unlockedCountries = FREE_COUNTRIES.toMutableSet()
        val skus = mutableSetOf<String>()
        var sawSubscription = false

        // INAPP — legacy packs + region bundles + all_countries lifetime.
        for (purchase in inappPurchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)

            purchase.products.forEach { productId ->
                when (productId) {
                    PRODUCT_PORTUGAL -> { unlockedCountries.add("portugal"); skus.add(productId) }
                    PRODUCT_MEXICO -> { unlockedCountries.add("mexico"); skus.add(productId) }
                    PRODUCT_IRELAND -> { unlockedCountries.add("ireland"); skus.add(productId) }
                    PRODUCT_ITALY -> { unlockedCountries.add("italy"); skus.add(productId) }
                    PRODUCT_GERMANY -> { unlockedCountries.add("germany"); skus.add(productId) }
                    PRODUCT_POLAND -> { unlockedCountries.add("poland"); skus.add(productId) }
                    PRODUCT_ARGENTINA -> { unlockedCountries.add("argentina"); skus.add(productId) }
                    PRODUCT_HUNGARY -> { unlockedCountries.add("hungary"); skus.add(productId) }
                    PRODUCT_UK_ANCESTRY -> { unlockedCountries.add("uk_ancestry"); skus.add(productId) }
                    PRODUCT_ALL_COUNTRIES -> {
                        unlockedCountries.addAll(ALL_PAID_COUNTRIES)
                        skus.add(productId)
                    }
                    PRODUCT_EUROPE_BUNDLE -> {
                        unlockedCountries.addAll(EUROPE_BUNDLE_COUNTRIES)
                        skus.add(productId)
                    }
                    PRODUCT_AMERICAS_BUNDLE -> {
                        unlockedCountries.addAll(AMERICAS_BUNDLE_COUNTRIES)
                        skus.add(productId)
                    }
                }
            }
        }

        // SUBS — all-access subscription.
        for (purchase in subsPurchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
            if (purchase.products.any { it in SUBSCRIPTION_SKUS }) {
                sawSubscription = true
                // Write the iapPurchases/{purchaseToken} lookup so the Firebase
                // IAP function (Item 12) can map Play RTDN events back to this
                // Firebase user on renewal / refund / expiration.
                writePurchaseLookup(purchase)
            }
        }

        // Subscription state machine.
        // Play Billing client doesn't expose expiration timestamps — until Item 12
        // wires server-side RTDN, we mark the sub Active with null expiration while
        // it appears in queryPurchases, and Expired the first time a reconcile
        // doesn't return it.
        val newSubStatus: SubscriptionStatus = when {
            sawSubscription -> SubscriptionStatus.Active(expiresAtSeconds = null)
            fromQueryReconcile && _subscriptionStatus.value.isActive -> SubscriptionStatus.Expired
            else -> _subscriptionStatus.value  // preserve when a non-reconcile call processed only INAPP
        }

        // hasAllAccess expansion — make purchasedCountries reflect everything the user
        // can reach, so call sites that already gate on .contains(country) keep working.
        val effectivelyAllAccess = newSubStatus.isActive ||
            PRODUCT_ALL_COUNTRIES in skus ||
            (PRODUCT_EUROPE_BUNDLE in skus && PRODUCT_AMERICAS_BUNDLE in skus)
        if (effectivelyAllAccess) {
            unlockedCountries.addAll(ALL_PAID_COUNTRIES)
        }

        _purchasedCountries.value = unlockedCountries
        _ownedSKUs.value = skus
        _subscriptionStatus.value = newSubStatus

        savePurchasesToFirestore(unlockedCountries, skus, newSubStatus)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Reconciled: countries=$unlockedCountries, skus=$skus, sub=$newSubStatus")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Purchase acknowledged: ${purchase.products}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String): Boolean {
        val details = _productDetails.value[productId]
        if (details == null) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Product details not found for: $productId")
            return false
        }

        // Subscriptions require an offerToken; one-time purchases do not.
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        if (productId in SUBSCRIPTION_SKUS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                if (BuildConfig.DEBUG) Log.e(TAG, "No offer token for subscription: $productId")
                return false
            }
            paramsBuilder.setOfferToken(offerToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Effective country gate. ORs the legacy per-country set with the derived
     * all-access signal. Existing call sites that read `purchasedCountries.contains(...)`
     * continue working because `_purchasedCountries` is auto-expanded whenever
     * [hasAllAccess] is true.
     */
    fun isCountryUnlocked(countryId: String): Boolean {
        if (hasAllAccess()) return true
        if (FirstWeekTrialService.unlocksCountry(appContext, countryId)) return true
        return _purchasedCountries.value.contains(countryId)
    }

    /**
     * True when the user has full content access through any of: an active
     * subscription, the legacy `all_countries` lifetime IAP, OR both region bundles.
     * Includes a legacy-compat fallback: if Firestore restored a pre-freemium doc
     * (no `ownedSKUs` field) but every paid country sits in `purchasedCountries`,
     * treat as all-access. Mirrors iOS `hasAllAccess`.
     */
    fun hasAllAccess(): Boolean {
        if (_subscriptionStatus.value.isActive) return true
        val skus = _ownedSKUs.value
        if (PRODUCT_ALL_COUNTRIES in skus) return true
        if (PRODUCT_EUROPE_BUNDLE in skus && PRODUCT_AMERICAS_BUNDLE in skus) return true
        if (ALL_PAID_COUNTRIES.all { it in _purchasedCountries.value }) return true
        return false
    }

    fun getFormattedPrice(productId: String): String? {
        val details = _productDetails.value[productId] ?: return null
        // Subscriptions use subscriptionOfferDetails > pricingPhases > formattedPrice.
        details.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice?.let { return it }
        return details.oneTimePurchaseOfferDetails?.formattedPrice
    }

    /**
     * Writes the `iapPurchases/{purchaseToken}` lookup row used by the Firebase
     * IAP function (Item 12) to map Play RTDN events back to this Firebase user.
     * Only called for SUBS purchases — Play sends RTDN for subscriptions only.
     */
    private fun writePurchaseLookup(purchase: Purchase) {
        val uid = auth.currentUser?.uid ?: return
        val productId = purchase.products.firstOrNull() ?: return
        val payload = mapOf(
            "uid" to uid,
            "productId" to productId,
            "platform" to "android",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        firestore.collection("iapPurchases").document(purchase.purchaseToken)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                if (BuildConfig.DEBUG) Log.e(TAG, "iapPurchases lookup write failed", e)
            }
    }

    private fun savePurchasesToFirestore(
        countries: Set<String>,
        skus: Set<String>,
        subStatus: SubscriptionStatus
    ) {
        val uid = auth.currentUser?.uid ?: return
        val payload: Map<String, Any?> = mapOf(
            "unlockedCountries" to countries.toList(),
            "ownedSKUs" to skus.toList(),
            "subscriptionStatus" to subStatus.toFirestoreMap(),
            "hasAllAccess" to hasAllAccess()
        )
        firestore.collection("users").document(uid)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                if (BuildConfig.DEBUG) Log.e(TAG, "Firestore sync failed", e)
            }
    }

    private fun loadPurchasesFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Firestore listen failed", error)
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: return@addSnapshotListener

                (data["unlockedCountries"] as? List<*>)?.let { countries ->
                    val countrySet = countries.filterIsInstance<String>().toMutableSet()
                    countrySet.addAll(FREE_COUNTRIES)
                    _purchasedCountries.value = countrySet
                }
                (data["ownedSKUs"] as? List<*>)?.let { skuList ->
                    _ownedSKUs.value = skuList.filterIsInstance<String>().toSet()
                }
                @Suppress("UNCHECKED_CAST")
                (data["subscriptionStatus"] as? Map<String, Any?>)?.let { subMap ->
                    _subscriptionStatus.value = SubscriptionStatus.fromFirestoreMap(subMap)
                }
                if (BuildConfig.DEBUG) Log.d(TAG, "Loaded from Firestore: $data")
            }
    }

    fun restorePurchases() {
        if (_isConnected.value) {
            queryPurchases()
        } else {
            connectToPlayBilling()
        }
    }
}
