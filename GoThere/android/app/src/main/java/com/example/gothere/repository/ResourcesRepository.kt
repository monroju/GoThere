package com.example.gothere.repository

import com.example.gothere.model.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ResourcesRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun col() = db.collection("users")
        .document(auth.currentUser?.uid ?: "_no_user_")
        .collection("resources")

    /** Live user resources from Firestore (owned by the signed-in user). */
    fun resourcesFlow(): Flow<List<Resource>> = callbackFlow {
        val query = col()
            .orderBy("category", Query.Direction.ASCENDING)
            .orderBy("label", Query.Direction.ASCENDING)

        val reg = query.addSnapshotListener { snap, err ->
            if (err != null || snap == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snap.documents.mapNotNull { it.toObject(Resource::class.java)?.copy(id = it.id) }
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    /** Simple helpers to edit Firestore resources. */
    suspend fun add(label: String, url: String, category: String) {
        val res = Resource(label = label, url = url, category = category)
        col().add(res).await()
    }

    suspend fun remove(id: String) {
        if (id.isNotBlank()) col().document(id).delete().await()
    }
}
