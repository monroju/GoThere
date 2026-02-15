package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Document
import com.example.gothere.repository.ResourcesStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Resources screen that loads documents from Firebase Storage
 * based on the selected country.
 */
class ResourcesViewModel : ViewModel() {
    
    private val repository = ResourcesStorageRepository()

    // Current country being viewed
    private val _currentCountryId = MutableStateFlow("spain")
    val currentCountryId: StateFlow<String> = _currentCountryId.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Documents organized by folder (Arrival, Resources, Templates, VisaForms)
    private val _documentsByFolder = MutableStateFlow<Map<String, List<Document>>>(emptyMap())
    val documentsByFolder: StateFlow<Map<String, List<Document>>> = _documentsByFolder.asStateFlow()

    // Flat list of all documents for backward compatibility
    private val _publicDocuments = MutableStateFlow<List<Document>>(emptyList())
    val publicDocuments: StateFlow<List<Document>> = _publicDocuments.asStateFlow()

    init {
        // Load Spain documents by default
        loadDocumentsForCountry("spain")
    }

    /**
     * Load all documents for a specific country.
     * Called when user switches countries.
     */
    fun loadDocumentsForCountry(countryId: String) {
        if (countryId == _currentCountryId.value && _documentsByFolder.value.isNotEmpty()) {
            // Already loaded this country
            return
        }

        _currentCountryId.value = countryId
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val results = repository.fetchDocumentsForCountry(countryId)
                _documentsByFolder.value = results
                
                // Flatten for backward compatibility
                _publicDocuments.value = results.values.flatten().sortedBy { it.name.lowercase() }
                
            } catch (e: Exception) {
                _error.value = "Failed to load documents: ${e.message}"
                _documentsByFolder.value = emptyMap()
                _publicDocuments.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh documents for current country
     */
    fun refresh() {
        val currentCountry = _currentCountryId.value
        _documentsByFolder.value = emptyMap() // Force reload
        loadDocumentsForCountry(currentCountry)
    }

    /**
     * Get documents for a specific folder
     */
    fun getDocumentsForFolder(folderName: String): List<Document> {
        return _documentsByFolder.value[folderName] ?: emptyList()
    }
}
