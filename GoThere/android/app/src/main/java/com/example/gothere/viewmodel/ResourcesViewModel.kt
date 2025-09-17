// app/src/main/java/com/example/gothere/viewmodel/ResourcesViewModel.kt
package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.model.Resource
import com.example.gothere.repository.ResourcesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ResourcesViewModel : ViewModel() {
    private val repo = ResourcesRepository()

    val resources: StateFlow<List<Resource>> =
        repo.resourcesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
