// app/src/main/java/com/example/gothere/viewmodel/DocumentsViewModel.kt
package com.example.gothere.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Document
import com.example.gothere.repository.DocumentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch



class DocumentsViewModel : ViewModel() {
    private val repo = DocumentsRepository()

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repo.documentsFlow()
                .catch { e ->
                    Log.e("DocumentsViewModel", "Error fetching documents", e)
                    _documents.value = emptyList()
                }
                .collect { 
                    _documents.value = it 
                }
        }
    }

    fun upload(context: Context, uri: Uri, mime: String? = null) {
        viewModelScope.launch {
            _uploading.value = true
            runCatching { repo.upload(context, uri, mime) }
                .onSuccess { 
                    Log.d("DocumentsViewModel", "Upload sequence finished")
                }
                .onFailure { 
                    Log.e("DocumentsViewModel", "Upload failed", it) 
                }
            _uploading.value = false
            refresh()
        }
    }

    fun delete(document: Document) {
        viewModelScope.launch {
            runCatching { repo.delete(document) }
                .onFailure { Log.e("DocumentsViewModel", "Delete failed", it) }
            refresh()
        }
    }


    /** Open a document’s URL with an ACTION_VIEW intent. */
    fun open(context: Context, url: String) {
        if (url.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Log.e("DocumentsViewModel", "Could not open URL: $url", it)
        }
    }
}
