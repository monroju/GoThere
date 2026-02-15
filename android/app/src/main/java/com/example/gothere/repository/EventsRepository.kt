package com.example.gothere.repository

import android.util.Log
import com.example.gothere.model.EventItem
import com.example.gothere.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * EventsRepository handles the persistence of calendar events.
 * Data is stored at: users / {userId} / events
 */
class EventsRepository {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private fun col(): CollectionReference? {
        val uid = auth.currentUser?.uid
        return if (uid != null) {
            db.collection("users").document(uid).collection("events")
        } else {
            Log.w("EventsRepository", "Accessing events without sign-in")
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun eventsFlow(): Flow<List<EventItem>> = authStateFlow().flatMapLatest { user ->
        val uid = user?.uid
        if (uid == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val collection = db.collection("users").document(uid).collection("events")
                val now = System.currentTimeMillis() - 24L * 60 * 60 * 1000

                val listener = collection
                    .whereGreaterThanOrEqualTo("dateMillis", now)
                    .orderBy("dateMillis", Query.Direction.ASCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        val items = snap?.documents
                            ?.mapNotNull { d -> d.toObject(EventItem::class.java)?.copy(id = d.id) }
                            .orEmpty()
                        trySend(items)
                    }
                awaitClose { listener.remove() }
            }
        }
    }

    private fun authStateFlow() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** One-shot list of upcoming events. */
    suspend fun listUpcoming(): List<EventItem> {
        val collection = col() ?: return emptyList()
        val now = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        return try {
            val snap = collection
                .whereGreaterThanOrEqualTo("dateMillis", now)
                .orderBy("dateMillis", Query.Direction.ASCENDING)
                .get()
                .await()
            snap.documents.mapNotNull { d -> d.toObject(EventItem::class.java)?.copy(id = d.id) }
        } catch (e: Exception) {
            Log.e("EventsRepository", "List upcoming failed", e)
            emptyList()
        }
    }

    suspend fun add(title: String, dateMillis: Long) {
        val collection = col() ?: return
        try {
            val item = EventItem(title = title.trim(), dateMillis = dateMillis)
            collection.add(item).await()
        } catch (e: Exception) {
            Log.e("EventsRepository", "Add failed", e)
        }
    }

    suspend fun addFromTask(task: Task) {
        val whenMillis = task.dueAt ?: System.currentTimeMillis()
        add(task.title, whenMillis)
    }

    suspend fun updateTitle(id: String, title: String) {
        val collection = col() ?: return
        if (id.isBlank()) return
        try {
            collection.document(id).update("title", title.trim()).await()
        } catch (e: Exception) {
            Log.e("EventsRepository", "Update title failed", e)
        }
    }

    suspend fun updateDate(id: String, dateMillis: Long) {
        val collection = col() ?: return
        if (id.isBlank()) return
        try {
            collection.document(id).update("dateMillis", dateMillis).await()
        } catch (e: Exception) {
            Log.e("EventsRepository", "Update date failed", e)
        }
    }

    suspend fun delete(id: String) {
        val collection = col() ?: return
        if (id.isBlank()) return
        try {
            collection.document(id).delete().await()
        } catch (e: Exception) {
            Log.e("EventsRepository", "Delete failed", e)
        }
    }
}
