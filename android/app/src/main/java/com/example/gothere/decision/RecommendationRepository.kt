package com.example.gothere.decision

import com.example.gothere.util.FirebaseUtil
import kotlinx.coroutines.tasks.await

object RecommendationRepository {
    suspend fun saveRecommendations(profileId: String, ranked: List<RankedDestination>) {
        val parent = FirebaseUtil.recommendationsCollection().document()
        val batch = FirebaseUtil.db.batch()
        batch.set(parent, mapOf("profileId" to profileId, "createdAt" to System.currentTimeMillis()))
        val sub = parent.collection("items")
        ranked.forEachIndexed { idx, r ->
            val doc = sub.document()
            batch.set(doc, mapOf(
                "rank" to idx + 1,
                "destinationId" to r.destination.id,
                "name" to r.destination.name,
                "region" to r.destination.region,
                "score" to r.score,
                "reasons" to r.reasons
            ))
        }
        batch.commit().await()
    }
}
