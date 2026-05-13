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

class PurchaseManager private constructor(context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PurchaseManager"
        
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

        // Spain and Canada are always free.
        // Spain is GoThere's original launch destination.
        // Canada is free for the v1 launch of the Fast-Track Eligibility module to ride the Bill C-3 news cycle.
        private val FREE_COUNTRIES = setOf("spain", "canada")
        
        @Volatile
        private var INSTANCE: PurchaseManager? = null
        
        fun getInstance(context: Context): PurchaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PurchaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PORTUGAL)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MEXICO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_IRELAND)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ITALY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_GERMANY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_POLAND)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ARGENTINA)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_HUNGARY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_UK_ANCESTRY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ALL_COUNTRIES)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

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

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
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

    private fun processPurchases(purchases: List<Purchase>) {
        val unlockedCountries = FREE_COUNTRIES.toMutableSet()

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                purchase.products.forEach { productId ->
                    when (productId) {
                        PRODUCT_PORTUGAL -> unlockedCountries.add("portugal")
                        PRODUCT_MEXICO -> unlockedCountries.add("mexico")
                        PRODUCT_IRELAND -> unlockedCountries.add("ireland")
                        PRODUCT_ITALY -> unlockedCountries.add("italy")
                        PRODUCT_GERMANY -> unlockedCountries.add("germany")
                        PRODUCT_POLAND -> unlockedCountries.add("poland")
                        PRODUCT_ARGENTINA -> unlockedCountries.add("argentina")
                        PRODUCT_HUNGARY -> unlockedCountries.add("hungary")
                        PRODUCT_UK_ANCESTRY -> unlockedCountries.add("uk_ancestry")
                        PRODUCT_ALL_COUNTRIES -> {
                            unlockedCountries.add("portugal")
                            unlockedCountries.add("mexico")
                            unlockedCountries.add("ireland")
                            unlockedCountries.add("italy")
                            unlockedCountries.add("germany")
                            unlockedCountries.add("poland")
                            unlockedCountries.add("argentina")
                            unlockedCountries.add("hungary")
                            unlockedCountries.add("uk_ancestry")
                        }
                    }
                }
            }
        }
        
        _purchasedCountries.value = unlockedCountries
        savePurchasesToFirestore(unlockedCountries)
        if (BuildConfig.DEBUG) Log.d(TAG, "Unlocked countries: $unlockedCountries")
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

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun isCountryUnlocked(countryId: String): Boolean {
        return _purchasedCountries.value.contains(countryId)
    }

    fun getFormattedPrice(productId: String): String? {
        return _productDetails.value[productId]
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }

    private fun savePurchasesToFirestore(countries: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("unlockedCountries", countries.toList())
            .addOnFailureListener { e ->
                firestore.collection("users").document(uid)
                    .set(mapOf("unlockedCountries" to countries.toList()), com.google.firebase.firestore.SetOptions.merge())
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
                
                val countries = snapshot?.get("unlockedCountries") as? List<*>
                if (countries != null) {
                    val countrySet = countries.filterIsInstance<String>().toMutableSet()
                    countrySet.addAll(FREE_COUNTRIES)
                    _purchasedCountries.value = countrySet
                    if (BuildConfig.DEBUG) Log.d(TAG, "Loaded countries from Firestore: $countrySet")
                }
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
