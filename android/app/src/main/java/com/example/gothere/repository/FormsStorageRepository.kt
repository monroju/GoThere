package com.example.gothere.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gothere.model.FormDoc
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lists read-only Visa forms from Firebase Storage.
 * Primary folder:  "VisaForms"   (no space)
 * Legacy fallback: "Visa Forms"  (with space)
 *
 * Uses a small SharedPreferences cache to avoid re-fetching download URLs on every open.
 */
class FormsStorageRepository {

    private val storage = Firebase.storage
    private val primaryRef: StorageReference = storage.reference.child("VisaForms")
    private val legacyRef:  StorageReference = storage.reference.child("Visa Forms")

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("forms_cache", Context.MODE_PRIVATE)

    /** Call this after you change files/folders so UI reloads fresh. */
    fun invalidateCache(ctx: Context) {
        prefs(ctx).edit { remove(CACHE_KEY) }
    }

    suspend fun listVisaForms(ctx: Context): List<FormDoc> {
        // 1) Try cache first
        prefs(ctx).getString(CACHE_KEY, null)?.let { cached ->
            runCatching {
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
            }
        }

        // 2) Fetch from Storage: prefer primary, fall back to legacy
        val items = tryList(primaryRef).ifEmpty { tryList(legacyRef) }
            .sortedBy { it.name.lowercase() }

        // 3) Cache it
        runCatching {
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
            prefs(ctx).edit { putString(CACHE_KEY, arr.toString()) }
        }

        return items
    }

    private suspend fun tryList(folder: StorageReference): List<FormDoc> {
        return runCatching {
            val listing = folder.listAll().await()
            listing.items.map { ref ->
                val meta = runCatching { ref.metadata.await() }.getOrNull()
                val url = ref.downloadUrl.await().toString()
                FormDoc(
                    name = ref.name,
                    url = url,
                    path = ref.path.removePrefix("/"),
                    sizeBytes = meta?.sizeBytes ?: 0L,
                    updatedAt = meta?.updatedTimeMillis ?: 0L
                )
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        private const val CACHE_KEY = "visa_forms_cache_v2" // v2 so old cache (with space) won’t collide
    }
}
