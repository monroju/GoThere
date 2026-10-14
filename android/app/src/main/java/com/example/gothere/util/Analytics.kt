package com.example.gothere.util

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.gothere.BuildConfig
import com.posthog.PostHog
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields

/**
 * Named analytics events, same names as iOS `Analytics.swift` and the canonical list in
 * `Projects/growth-plan/02-metrics.md`, so one PostHog dashboard covers both platforms.
 */
object Analytics {
    // Onboarding and identity
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val SIGNUP_COMPLETED = "signup_completed"
    const val LOGIN_COMPLETED = "login_completed"
    const val INSTALL_ATTRIBUTED = "install_attributed"

    // Activation candidates. The first one fired also fires ACTIVATED.
    const val WIZARD_STARTED = "wizard_started"
    const val WIZARD_COMPLETED = "wizard_completed"
    const val DECISION_TREE_COMPLETED = "decision_tree_completed"
    const val ANCESTRY_RESULT_VIEWED = "ancestry_result_viewed"
    const val VISA_COMPARE_VIEWED = "visa_compare_viewed"
    const val ACTIVATED = "activated"

    // Monetization
    const val PAYWALL_VIEWED = "paywall_viewed"
    const val PAYWALL_DISMISSED = "paywall_dismissed"
    const val PURCHASE_STARTED = "purchase_started"
    const val PURCHASE_COMPLETED = "purchase_completed"
    const val PURCHASE_FAILED = "purchase_failed"
    const val PURCHASE_RESTORED = "purchase_restored"
    const val LEGACY_INSTALL_CONFIRMED = "legacy_install_confirmed"
    const val FREE_LIMIT_REACHED = "free_limit_reached"

    // Retention
    const val REVIEW_PROMPT_REQUESTED = "review_prompt_requested"
    const val REFERRAL_REDEEMED = "referral_redeemed"
    const val DOCUMENT_SCAN_COMPLETED = "document_scan_completed"
    const val CONCIERGE_WAITLIST_JOINED = "concierge_waitlist_joined"

    private const val PREFS = "analytics_prefs"
    private const val KEY_FIRST_OPEN = "first_open_at"
    private const val KEY_ACTIVATED = "activated_v1"
    const val KEY_ACQ_SOURCE = "acq_source_v1"

    private val activationEvents = setOf(
        WIZARD_COMPLETED, DECISION_TREE_COMPLETED, ANCESTRY_RESULT_VIEWED, VISA_COMPARE_VIEWED
    )

    private lateinit var appContext: Context

    /** Which screen opened the paywall now on screen; purchase events carry it. */
    @Volatile var paywallSource: String = "unknown"
    @Volatile private var lastPurchaseAt: Long = 0L

    /** Call once at launch, right after PostHogAndroid.setup. */
    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = prefs()
        if (!prefs.contains(KEY_FIRST_OPEN)) {
            prefs.edit().putLong(KEY_FIRST_OPEN, System.currentTimeMillis()).apply()
        }
        registerSuperProperties()
    }

    fun log(event: String, properties: Map<String, Any?> = emptyMap()) {
        runCatching { PostHog.capture(event, properties = properties.filterValues { it != null }.mapValues { it.value!! }) }
        if (event == PURCHASE_COMPLETED) lastPurchaseAt = System.currentTimeMillis()
        if (event in activationEvents) markActivated(event)
    }

    /** True when a purchase completed at or after [sinceMillis] (closing the paywall then is not a dismissal). */
    fun purchasedSince(sinceMillis: Long): Boolean = lastPurchaseAt >= sinceMillis

    private fun markActivated(via: String) {
        if (!::appContext.isInitialized) return
        val prefs = prefs()
        if (prefs.getBoolean(KEY_ACTIVATED, false)) return
        prefs.edit().putBoolean(KEY_ACTIVATED, true).apply()
        runCatching {
            PostHog.capture(
                ACTIVATED,
                properties = mapOf("via" to via),
                userPropertiesSetOnce = mapOf("activated_at" to Instant.now().toString())
            )
        }
    }

    fun setAcquisitionSource(source: String) {
        runCatching { PostHog.register("acq_source", source) }
    }

    /** Ties events to the Firebase uid so server-side (RTDN) and app events join. */
    fun identify(uid: String) {
        runCatching {
            if (PostHog.distinctId() == uid) return
            PostHog.identify(uid, userPropertiesSetOnce = mapOf("acq_source" to storedSource()))
        }
    }

    fun reset() {
        runCatching { PostHog.reset() }
        registerSuperProperties()
    }

    fun storedSource(): String =
        if (::appContext.isInitialized) prefs().getString(KEY_ACQ_SOURCE, null) ?: "pending" else "pending"

    private fun registerSuperProperties() {
        if (!::appContext.isInitialized) return
        val firstOpen = Instant.ofEpochMilli(prefs().getLong(KEY_FIRST_OPEN, System.currentTimeMillis()))
            .atZone(ZoneOffset.UTC)
        val installWeek = "%04d-W%02d".format(
            firstOpen.get(IsoFields.WEEK_BASED_YEAR), firstOpen.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )
        runCatching {
            PostHog.register("app", "gothere")
            PostHog.register("platform", "android")
            PostHog.register("app_version", BuildConfig.VERSION_NAME)
            PostHog.register("install_week", installWeek)
            PostHog.register("acq_source", storedSource())
            PostHog.register("is_internal", isInternalBuild())
        }
    }

    /** Debug builds and installs not from Play (sideload, emulator) are internal traffic. */
    private fun isInternalBuild(): Boolean {
        if (BuildConfig.DEBUG) return true
        if (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return true
        val installer = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                appContext.packageManager.getInstallSourceInfo(appContext.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getInstallerPackageName(appContext.packageName)
            }
        }.getOrNull()
        return installer != "com.android.vending"
    }

    fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
