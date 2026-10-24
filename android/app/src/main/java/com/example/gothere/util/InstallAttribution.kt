package com.example.gothere.util

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

/**
 * Reads the Play Install Referrer once and fires `install_attributed`.
 * Store links on getgothere.app carry `referrer=utm_source=...&utm_campaign=...`;
 * Play reports untagged installs as `utm_source=google-play&utm_medium=organic`.
 */
object InstallAttribution {
    private const val KEY_DONE = "install_attributed_v1"

    fun resolveOnce(context: Context) {
        val prefs = Analytics.prefs()
        if (prefs.getBoolean(KEY_DONE, false)) return
        val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val referrer = runCatching { client.installReferrer.installReferrer }.getOrNull()
                        record(fromReferrer(referrer))
                    }
                    // Not a Play install (sideload, emulator) or Play too old: nothing will ever come.
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                    InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR ->
                        record(mapOf("acq_source" to "unknown"))
                    // SERVICE_UNAVAILABLE etc: retry next launch.
                    else -> Unit
                }
                runCatching { client.endConnection() }
            }

            override fun onInstallReferrerServiceDisconnected() = Unit
        })
    }

    private fun fromReferrer(referrer: String?): Map<String, Any?> {
        if (referrer.isNullOrBlank()) return mapOf("acq_source" to "organic")
        val params = Uri.parse("?$referrer")
        val utmSource = params.getQueryParameter("utm_source")
        val utmMedium = params.getQueryParameter("utm_medium")
        val utmCampaign = params.getQueryParameter("utm_campaign")
        val source = when {
            utmSource == null || utmMedium == "organic" -> "organic"
            params.getQueryParameter("gclid") != null || utmSource == "google" && utmMedium == "cpc" -> "google_ads"
            else -> "play_referrer"
        }
        return mapOf(
            "acq_source" to source,
            "utm_source" to utmSource,
            "utm_medium" to utmMedium,
            "utm_campaign" to utmCampaign,
            "utm_content" to params.getQueryParameter("utm_content"),
            "referrer" to referrer.take(200)
        )
    }

    private fun record(props: Map<String, Any?>) {
        val source = props["acq_source"] as? String ?: "unknown"
        Analytics.prefs().edit()
            .putString(Analytics.KEY_ACQ_SOURCE, source)
            .putBoolean(KEY_DONE, true)
            .apply()
        Analytics.setAcquisitionSource(source)
        Analytics.log(Analytics.INSTALL_ATTRIBUTED, props + ("\$set_once" to mapOf("acq_source" to source)))
    }
}
