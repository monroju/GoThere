// app/src/main/java/com/example/gothere/repository/EventsRepository.kt
package com.example.gothere.repository

import com.example.gothere.model.EventItem
import com.example.gothere.model.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EventsRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun col() = db.collection("users")
        .document(auth.currentUser?.uid ?: error("Not signed in"))
        .collection("events")

    /** Live stream of upcoming events (ordered asc). */
    fun eventsFlow(): Flow<List<EventItem>> = callbackFlow {
        val now = System.currentTimeMillis() - 24L * 60 * 60 * 1000 // 1-day buffer
        val reg = col()
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
        awaitClose { reg.remove() }
    }

    /** One-shot list for a date range. */
    suspend fun listRange(startMillis: Long, endMillis: Long): List<EventItem> {
        val snap = col()
            .whereGreaterThanOrEqualTo("dateMillis", startMillis)
            .whereLessThan("dateMillis", endMillis)
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .get()
            .await()
        return snap.documents.mapNotNull { d -> d.toObject(EventItem::class.java)?.copy(id = d.id) }
    }

    /** One-shot list of upcoming events (used by older VM code). */
    suspend fun listUpcoming(): List<EventItem> {
        val now = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val snap = col()
            .whereGreaterThanOrEqualTo("dateMillis", now)
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .get()
            .await()
        return snap.documents.mapNotNull { d -> d.toObject(EventItem::class.java)?.copy(id = d.id) }
    }

    suspend fun add(title: String, dateMillis: Long) {
        val item = EventItem(title = title.trim(), dateMillis = dateMillis)
        col().add(item).await()
    }

    /** Creates an event from a task (uses task.dueAt or "now" if missing). */
    suspend fun addFromTask(task: Task) {
        val whenMillis = task.dueAt ?: System.currentTimeMillis()
        add(task.title, whenMillis)
    }

    suspend fun updateTitle(id: String, title: String) {
        if (id.isBlank()) return
        col().document(id).update("title", title.trim()).await()
    }

    suspend fun updateDate(id: String, dateMillis: Long) {
        if (id.isBlank()) return
        col().document(id).update("dateMillis", dateMillis).await()
    }

    suspend fun delete(id: String) {
        if (id.isBlank()) return
        col().document(id).delete().await()
    }
}
