package com.example.gothere.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val TAG = "AuthRepository"

    /**
     * Single source of truth for auth state.
     * Emits immediately and on every auth change.
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val user = fa.currentUser
            Log.d(TAG, "Auth state changed: uid=${user?.uid}, email=${user?.email}")
            trySend(user).isSuccess
        }
        auth.addAuthStateListener(listener)

        // Emit current state immediately
        val user = auth.currentUser
        Log.d(TAG, "Auth state initial: uid=${user?.uid}, email=${user?.email}")
        trySend(user).isSuccess

        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged { old, new -> old?.uid == new?.uid }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user ?: error("No user returned from Firebase")
    }

    suspend fun createUser(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("No user returned from Firebase")

        // Optional: ensure a basic user doc exists for downstream reads
        val doc = db.collection("users").document(user.uid)
        val snapshot = doc.get().await()
        if (!snapshot.exists()) {
            doc.set(
                mapOf(
                    "email" to (user.email ?: email.trim()),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        user
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        Log.d(TAG, "Signing out user uid=${auth.currentUser?.uid}")
        auth.signOut()
    }
}
