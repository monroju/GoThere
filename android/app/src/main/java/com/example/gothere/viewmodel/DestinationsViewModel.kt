package com.example.gothere.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.data.DestinationConfig
import com.example.gothere.repository.DestinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the DestinationsScreen.
 * Manages destination selection and visa track preferences.
 */
class DestinationsViewModel(
    private val repo: DestinationRepository = DestinationRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "DestinationsViewModel"
    }

    private val _activeDestinationId = MutableStateFlow(DestinationConfig.SPAIN)
    val activeDestinationId: StateFlow<String> = _activeDestinationId.asStateFlow()

    private val _activeVisaTrackId = MutableStateFlow("")
    val activeVisaTrackId: StateFlow<String> = _activeVisaTrackId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load the user's current active destination and visa track
     */
    fun loadActiveDestination() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val destId = repo.getActiveDestinationId()
                _activeDestinationId.value = destId

                val trackId = repo.getActiveVisaTrackId(destId)
                _activeVisaTrackId.value = trackId.ifBlank {
                    DestinationConfig.getDestination(destId)?.defaultVisaTrackId ?: ""
                }

                Log.d(TAG, "Loaded active destination: $destId, track: ${_activeVisaTrackId.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load active destination", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Select a destination and visa track.
     * This will seed tasks if needed and update the user's preferences.
     */
    fun selectDestination(context: Context, destinationId: String, visaTrackId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Set active destination (this also triggers seeding)
                val result = repo.setActiveDestination(destinationId, context)
                
                if (result.isSuccess) {
                    _activeDestinationId.value = destinationId
                    
                    // Set visa track
                    repo.setActiveVisaTrack(destinationId, visaTrackId)
                    _activeVisaTrackId.value = visaTrackId
                    
                    Log.d(TAG, "Selected destination: $destinationId, track: $visaTrackId")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to select destination"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to select destination", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Change only the visa track within the current destination
     */
    fun changeVisaTrack(visaTrackId: String) {
        viewModelScope.launch {
            try {
                val destId = _activeDestinationId.value
                repo.setActiveVisaTrack(destId, visaTrackId)
                _activeVisaTrackId.value = visaTrackId
                Log.d(TAG, "Changed visa track to: $visaTrackId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change visa track", e)
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
