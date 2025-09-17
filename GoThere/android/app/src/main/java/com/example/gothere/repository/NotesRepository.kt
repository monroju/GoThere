package com.example.gothere.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotesRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun noteDoc() = db
        .collection("users")
        .document(auth.currentUser?.uid ?: error("Not signed in"))
        .collection("notes")
        .document("note")

    /** Live note text as a Flow<String>. */
    val noteFlow: Flow<String> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend("")
            awaitClose { }
            return@callbackFlow
        }
        val reg = noteDoc().addSnapshotListener { snap, _ ->
            val text = snap?.getString("text") ?: ""
            trySend(text).isSuccess
        }
        awaitClose { reg.remove() }
    }

    /** Save note text to Firestore. */
    suspend fun save(text: String) {
        noteDoc().set(mapOf("text" to text)).await()
    }
}
