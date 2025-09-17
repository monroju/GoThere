// app/src/main/java/com/example/gothere/viewmodel/DocumentsViewModel.kt
package com.example.gothere.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.DocumentItem
import com.example.gothere.repository.DocumentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DocumentsViewModel : ViewModel() {
    private val repo = DocumentsRepository()

    private val _documents = MutableStateFlow<List<DocumentItem>>(emptyList())
    val documents: StateFlow<List<DocumentItem>> = _documents

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.documentsFlow() }
                .onSuccess { flow ->
                    flow.collect { _documents.value = it }
                }
                .onFailure {
                    _documents.value = emptyList()
                }
        }
    }

    fun upload(uri: Uri, mime: String? = null) {
        viewModelScope.launch {
            _uploading.value = true
            runCatching { repo.upload(uri, mime) }
            _uploading.value = false
            // refresh after upload
            refresh()
        }
    }

    fun delete(path: String) {
        viewModelScope.launch {
            runCatching { repo.delete(path) }
            refresh()
        }
    }

    /** Open a document’s URL with an ACTION_VIEW intent. */
    fun open(context: Context, url: String) {
        if (url.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    /** Placeholder to satisfy older UI calls; selection happens in the screen. */
    fun pickAndUpload() { /* no-op: handled by the screen’s picker */ }
}
