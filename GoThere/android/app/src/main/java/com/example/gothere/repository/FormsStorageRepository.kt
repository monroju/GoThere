// app/src/main/java/com/example/gothere/repository/FormsStorageRepository.kt
package com.example.gothere.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gothere.model.FormDoc
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lists read-only Visa forms from Firebase Storage under "Visa Forms/".
 * Adds lightweight caching using SharedPreferences to avoid hitting
 * getDownloadUrl() every open.
 */
class FormsStorageRepository {
    private val storage = Firebase.storage
    private val root = storage.reference
    private val formsFolder = root.child("Visa Forms")

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("forms_cache", Context.MODE_PRIVATE)

    suspend fun listVisaForms(ctx: Context): List<FormDoc> {
        // 1. Try cache first
        prefs(ctx).getString("visa_forms", null)?.let { cached ->
            try {
                val arr = JSONArray(cached)
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
            } catch (_: Exception) {
                // ignore malformed cache
            }
        }

        // 2. Otherwise fetch from Firebase Storage
        val listing = formsFolder.listAll().await()
        val items = listing.items.map { ref ->
            val path = ref.path.removePrefix("/") // "Visa Forms/XYZ.pdf"
            val meta = ref.metadata.await()
            val url = ref.downloadUrl.await().toString()
            FormDoc(
                name = ref.name,
                url = url,
                path = path,
                sizeBytes = meta.sizeBytes,
                updatedAt = meta.updatedTimeMillis
            )
        }.sortedBy { it.name.lowercase() }

        // 3. Save into cache
        try {
            val arr = JSONArray()
            items.forEach { f ->
                arr.put(
                    JSONObject().apply {
                        put("name", f.name)
                        put("url", f.url)
                        put("path", f.path)
                        put("sizeBytes", f.sizeBytes)
                        put("updatedAt", f.updatedAt)
                    }
                )
            }
            prefs(ctx).edit { putString("visa_forms", arr.toString()) }
        } catch (_: Exception) { }

        return items
    }
}
