package com.example.gothere.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class CalendarNote(
    val date: String = "",         // yyyy-MM-dd
    val note: String = "",
    val remindAt: Long? = null     // epoch millis (local time)
)

class CalendarRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun col(uid: String) =
        db.collection("users").document(uid).collection("calendar")

    suspend fun getNote(dateStr: String): CalendarNote? {
        val uid = auth.currentUser?.uid ?: return null
        val snap = col(uid).document(dateStr).get().await()
        return if (snap.exists()) snap.toObject(CalendarNote::class.java) else null
    }

    suspend fun upsertNote(note: CalendarNote) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No user")
        col(uid).document(note.date).set(note).await()
    }
}
