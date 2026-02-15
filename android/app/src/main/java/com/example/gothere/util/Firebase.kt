package com.example.gothere.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseUtil {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun uid(): String = auth.currentUser?.uid ?: "local"

    fun profilesCollection() = db.collection("users").document(uid()).collection("profiles")
    fun recommendationsCollection() = db.collection("users").document(uid()).collection("recommendations")
    fun tasksCollection() = db.collection("users").document(uid()).collection("tasks")
}
