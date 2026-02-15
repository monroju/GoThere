package com.example.gothere.repository

import android.util.Log
import com.example.gothere.model.Document
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching public documents from Firebase Storage
 * organized by country folders.
 * 
 * Firebase Storage Structure:
 * gs://gothere-e5ea7.firebasestorage.app/
 * ├── Arrival/
 * │   ├── spain/
 * │   ├── portugal/
 * │   └── mexico/
 * ├── Resources/
 * │   ├── spain/
 * │   ├── portugal/
 * │   └── mexico/
 * ├── Templates/
 * │   ├── spain/
 * │   ├── portugal/
 * │   └── mexico/
 * └── VisaForms/
 *     ├── spain/
 *     ├── portugal/
 *     └── mexico/
 */
class ResourcesStorageRepository {

    private val storage = Firebase.storage
    private val root: StorageReference get() = storage.reference

    companion object {
        private const val TAG = "ResourcesStorageRepo"
        
        // Main resource folders in Firebase Storage
        val RESOURCE_FOLDERS = listOf("Arrival", "Resources", "Templates", "VisaForms")
    }

    /**
     * Fetch all documents for a specific country from all resource folders.
     * 
     * @param countryId The country folder name (e.g., "spain", "portugal", "mexico")
     * @return Map of folder name to list of documents
     */
    suspend fun fetchDocumentsForCountry(countryId: String): Map<String, List<Document>> = coroutineScope {
        val results = mutableMapOf<String, List<Document>>()
        
        val deferredResults = RESOURCE_FOLDERS.map { folderName ->
            async {
                folderName to fetchDocumentsFromFolder(folderName, countryId)
            }
        }
        
        deferredResults.awaitAll().forEach { (folder, docs) ->
            if (docs.isNotEmpty()) {
                results[folder] = docs
            }
        }
        
        results
    }

    /**
     * Fetch documents from a specific folder/country combination.
     * Example: VisaForms/spain/
     */
    private suspend fun fetchDocumentsFromFolder(
        folderName: String,
        countryId: String
    ): List<Document> {
        return try {
            val folderRef = root.child("$folderName/$countryId")
            val result = folderRef.listAll().await()
            
            result.items.map { ref ->
                val url = ref.downloadUrl.await().toString()
                Document(
                    id = ref.path,
                    name = ref.name,
                    downloadUrl = url,
                    path = ref.path
                )
            }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.d(TAG, "No documents found in $folderName/$countryId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch all documents from a single folder for a country.
     * Useful for specific screens like DocumentsScreen.
     */
    suspend fun fetchFolderDocuments(folderName: String, countryId: String): List<Document> {
        return fetchDocumentsFromFolder(folderName, countryId)
    }

    /**
     * Fetch all documents across all countries for a specific folder.
     * Returns a map of countryId to documents.
     */
    suspend fun fetchAllCountriesForFolder(folderName: String): Map<String, List<Document>> = coroutineScope {
        val countries = listOf("spain", "portugal", "mexico")
        
        val results = countries.map { country ->
            async {
                country to fetchDocumentsFromFolder(folderName, country)
            }
        }.awaitAll().toMap()
        
        results.filterValues { it.isNotEmpty() }
    }
}
