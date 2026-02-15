package com.example.gothere.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.gothere.model.Document //
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class DocumentsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage
    private val root: StorageReference get() = storage.reference

    // --- PRIVATE DOCUMENTS (User Specific) ---

    private fun userFolder(): StorageReference {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Not signed in: Cannot access user-specific storage.")
        return root.child("users/$uid/documents")
    }

    suspend fun upload(context: Context, uri: Uri, mime: String? = null) {
        try {
            val folder = userFolder()
            val fileName = getFileName(context, uri) ?: System.currentTimeMillis().toString()
            val ref = folder.child(fileName)

            val meta = StorageMetadata.Builder()
                .setContentType(mime ?: context.contentResolver.getType(uri) ?: "application/octet-stream")
                .build()

            Log.d("DocumentsRepository", "Starting upload for '$fileName'")
            ref.putFile(uri, meta).await()
            Log.d("DocumentsRepository", "Upload successful")
        } catch (e: Exception) {
            Log.e("DocumentsRepository", "Upload failed", e)
        }
    }

    fun documentsFlow(): Flow<List<Document>> = flow {
        try {
            val folder = userFolder()
            val result = folder.listAll().await()

            val items = result.items
                .map { ref ->
                    val url = ref.downloadUrl.await().toString()
                    Document(
                        id = ref.name,
                        name = ref.name,
                        downloadUrl = url,
                        path = ref.path
                    )
                }
                .sortedBy { it.name.lowercase() }

            emit(items)
        } catch (e: Exception) {
            Log.e("DocumentsRepository", "Error getting documents list", e)
            emit(emptyList())
        }
    }

    suspend fun delete(document: Document) {
        val p = document.path.trim()
        if (p.isBlank()) return
        try {
            storage.getReference(p).delete().await()
        } catch (e: Exception) {
            Log.e("DocumentsRepository", "Delete failed for $p", e)
        }
    }

    // --- PUBLIC DOCUMENTS (Resources) ---

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPublicDocumentsFlow(): Flow<List<Document>> {
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                flow { emit(fetchPublicFolders()) }
            }
        }
    }

    private suspend fun fetchPublicFolders(): List<Document> = coroutineScope {
        // Define the public folders you created in Firebase Console
        val publicFolders = listOf("templates", "VisaForms", "Arrival")

        val allItems = publicFolders.map { folderName ->
            async {
                try {
                    root.child(folderName).listAll().await().items.map { ref ->
                        val url = ref.downloadUrl.await().toString()
                        Document(
                            id = ref.path,
                            name = ref.name,
                            downloadUrl = url,
                            path = ref.path
                        )
                    }
                } catch (e: Exception) {
                    Log.e("DocumentsRepository", "Failed to fetch folder: $folderName", e)
                    emptyList<Document>()
                }
            }
        }.awaitAll().flatten().sortedBy { it.name.lowercase() }

        return@coroutineScope allItems
    }

    // --- HELPER ---

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) return cursor.getString(index)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}