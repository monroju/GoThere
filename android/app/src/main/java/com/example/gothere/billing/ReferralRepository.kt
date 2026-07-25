package com.example.gothere.billing

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReferralInfo(val code: String, val shareUrl: String, val rewardDays: Int)
data class RedeemResult(val rewardDays: Int, val premiumUntilSeconds: Double)

/**
 * Thin client for the referral Cloud Functions (getReferralCode / redeemReferral).
 *
 * Uses the Firebase callable protocol (firebase-functions-ktx), which attaches the
 * signed-in user's ID token automatically — the functions read request.auth.uid.
 * The callable Task is bridged to a coroutine via suspendCancellableCoroutine so we
 * don't pull in kotlinx-coroutines-play-services just for .await().
 */
class ReferralRepository {

    private val functions = FirebaseFunctions.getInstance()

    suspend fun fetchCode(): ReferralInfo {
        val data = call("getReferralCode", emptyMap())
        return ReferralInfo(
            code = data["code"] as? String ?: "",
            shareUrl = data["shareUrl"] as? String ?: "https://getgothere.app",
            rewardDays = (data["rewardDays"] as? Number)?.toInt() ?: 30
        )
    }

    suspend fun redeem(code: String): RedeemResult {
        val data = call("redeemReferral", mapOf("code" to code))
        return RedeemResult(
            rewardDays = (data["rewardDays"] as? Number)?.toInt() ?: 30,
            premiumUntilSeconds = (data["premiumUntil"] as? Number)?.toDouble()
                ?: (System.currentTimeMillis() / 1000.0 + 30 * 86_400)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun call(name: String, params: Map<String, Any>): Map<String, Any?> =
        suspendCancellableCoroutine { cont ->
            functions.getHttpsCallable(name).call(params)
                .addOnSuccessListener { result ->
                    cont.resume((result.data as? Map<String, Any?>) ?: emptyMap())
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
