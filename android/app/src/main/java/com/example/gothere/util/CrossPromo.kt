package com.example.gothere.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Our other apps, promoted from Settings and Resources. Mirrors iOS CrossPromo. */
enum class PromoApp(val key: String, val packageId: String, val title: String, val subtitle: String) {
    LOCALISTA("localista", "com.localista.spain", "Localista", "Spain news, alerts and trámites in English"),
    PASSDGT("passdgt", "app.passdgt.twa", "PassDGT", "Practice the Spanish DGT theory test in English. Free, offline."),
}

/**
 * Opens a sister app's Play listing with an install referrer so the other app can
 * attribute the install, and logs `cross_promo_tapped`.
 */
object CrossPromo {
    const val PLACEMENT_SETTINGS = "settings"
    const val PLACEMENT_RESOURCES = "resources"

    fun storeUrl(app: PromoApp, placement: String): String =
        "https://play.google.com/store/apps/details?id=${app.packageId}" +
            "&referrer=utm_source%3Dgothere%26utm_medium%3Dcross_promo%26utm_campaign%3D$placement"

    fun open(context: Context, app: PromoApp, placement: String, countryId: String? = null) {
        Analytics.log(
            Analytics.CROSS_PROMO_TAPPED,
            mapOf("target_app" to app.key, "placement" to placement, "country_id" to countryId)
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl(app, placement)))
        runCatching { context.startActivity(intent) }
    }
}
