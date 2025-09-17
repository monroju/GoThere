package com.example.gothere.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.example.gothere.model.FormDoc
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-user custom forms stored at: users/{uid}/forms
 * Shown in the "My Forms" section of Resources.
 */
class UserFormsRepository {
    private val auth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage
    private val root = storage.reference

    private fun userUid(): String =
        auth.currentUser?.uid ?: error("Not signed in")

    private fun userFormsRef() =
        root.child("users/${userUid()}/forms")

    // --- simple on-device cache (by uid) ---
    private fun cacheKey() = "my_forms_${userUid()}"
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("forms_cache", Context.MODE_PRIVATE)

    suspend fun listMyForms(ctx: Context): List<FormDoc> {
        // try cache
        prefs(ctx).getString(cacheKey(), null)?.let { s ->
            try {
                val arr = JSONArray(s)
                if (arr.length() > 0) {
                    return (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        FormDoc(
                            name = o.getString("name"),
                            url = o.getString("url"),
                            path = o.getString("path"),
                            sizeBytes = o.optLong("sizeBytes", 0),
                            updatedAt = o.optLong("updatedAt", 0)
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // fetch
        val listing = userFormsRef().listAll().await()
        val items = listing.items.map { ref ->
            val meta = ref.metadata.await()
            val url = ref.downloadUrl.await().toString()
            FormDoc(
                name = ref.name,
                url = url,
                path = ref.path.removePrefix("/"),
                sizeBytes = meta.sizeBytes,
                updatedAt = meta.updatedTimeMillis
            )
        }.sortedBy { it.name.lowercase() }

        // cache
        try {
            val arr = JSONArray()
            items.forEach { f ->
                arr.put(JSONObject().apply {
                    put("name", f.name)
                    put("url", f.url)
                    put("path", f.path)
                    put("sizeBytes", f.sizeBytes)
                    put("updatedAt", f.updatedAt)
                })
            }
            prefs(ctx).edit { putString(cacheKey(), arr.toString()) }
        } catch (_: Exception) {}

        return items
    }

    /** Upload a PDF (or any file). Optionally pass MIME to set contentType. */
    suspend fun upload(uri: Uri, mime: String? = null) {
        val fileName = System.currentTimeMillis().toString() + ".pdf"
        val ref = userFormsRef().child(fileName)
        val meta = storageMetadata { if (mime != null) contentType = mime }
        ref.putFile(uri, meta).await()
    }

    suspend fun delete(path: String) {
        if (path.isBlank()) return
        root.child(path).delete().await()
    }

    /** Call after upload/delete to force a fresh list next time. */
    fun invalidateCache(ctx: Context) = prefs(ctx).edit { remove(cacheKey()) }
}
