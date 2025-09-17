package com.example.gothere.repository

import android.net.Uri
import com.example.gothere.model.DocumentItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class DocumentsRepository {
    private val auth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage
    private val root = storage.reference

    private fun userFolder(): StorageReference {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        return root.child("documents/$uid")
    }

    suspend fun upload(uri: Uri, mime: String? = null) {
        val ref = userFolder().child(System.currentTimeMillis().toString())
        val meta = storageMetadata { mime?.let { contentType = it } }
        ref.putFile(uri, meta).await()
    }

    fun documentsFlow(): Flow<List<DocumentItem>> = flow {
        val folder = userFolder()
        val result: ListResult = folder.listAll().await()
       val items = result.items.map { ref ->
    val url = ref.downloadUrl.await().toString()
    DocumentItem(
        name = ref.name,
        path = ref.path,   // e.g., "documents/{uid}/12345"
        url  = url         // <-- use url, not downloadUrl
    )
}.sortedBy { it.name.lowercase() }
        emit(items)
    }

    suspend fun delete(path: String) {
        if (path.isBlank()) return
        root.child(path).delete().await()
    }
}
