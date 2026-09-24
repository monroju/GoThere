package com.example.gothere.billing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.gothere.BuildConfig

/**
 * Grandfathers people who BOUGHT GoThere while it was a paid Play app, ahead of the
 * paid -> free flip. Android port of iOS `LegacyEntitlementService`.
 *
 * Play has no AppTransaction equivalent, and once the listing is free every install
 * looks "licensed", so the only discriminating signal is when the app first landed on
 * the device: `firstInstallTime` survives updates and is not user-editable without
 * root. An install from the Play Store before [FREEMIUM_CUTOFF_MS] was a paid install.
 * Promo-code redeemers and license testers also qualify, which is fine.
 *
 * Grant-only: once true it is cached (SharedPreferences here, `users/{uid}.legacyPaidInstall`
 * via PurchaseManager) and never revoked, so a reinstall or new device keeps access
 * through the Firestore mirror.
 *
 * ⚠️ FREEMIUM_CUTOFF_MS must be AFTER the moment the Play price is set to Free.
 * If the flip slips past it, ship a new build with a later cutoff BEFORE flipping,
 * otherwise buyers who installed between the cutoff and the flip lose access.
 * See ANDROID_FREEMIUM_PLAN.md (go/no-go gate).
 */
object LegacyEntitlementService {
    /** 2026-10-16 00:00:00 UTC. Planned flip is on or before 2026-10-15. */
    const val FREEMIUM_CUTOFF_MS = 1_792_108_800_000L

    private const val PREFS = "legacy_entitlement"
    private const val KEY_GRANTED = "legacy_paid_install"
    private const val PLAY_STORE = "com.android.vending"

    /** Debug-only switch so the grandfathered path can be exercised on a sideloaded build. */
    var debugForceLegacy: Boolean = false

    fun isLegacyPaidInstall(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_GRANTED, false)) return true
        if (BuildConfig.DEBUG && debugForceLegacy) return true

        val granted = installedBeforeCutoff(context) && installedFromPlay(context)
        if (granted) markGranted(context)
        return granted
    }

    /** Called when Firestore already says this account is grandfathered (e.g. a new device). */
    fun markGranted(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GRANTED, true).apply()
    }

    private fun installedBeforeCutoff(context: Context): Boolean = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.firstInstallTime in 1 until FREEMIUM_CUTOFF_MS
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun installedFromPlay(context: Context): Boolean {
        val pm = context.packageManager
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        } catch (e: Exception) {
            null
        }
        return installer == PLAY_STORE
    }
}
