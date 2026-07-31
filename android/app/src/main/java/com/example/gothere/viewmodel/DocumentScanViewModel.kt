package com.example.gothere.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.ai.DocumentAnalysis
import com.example.gothere.ai.DocumentAnalysisClient
import com.example.gothere.billing.PurchaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Drives the "Scan a Document" screen. Mirrors iOS DocumentAnalysisService:
 * separate free-scan quota (3 free, then paywall via PurchaseManager.hasAllAccess),
 * counter persisted in its own SharedPreferences, JPEG downscale+compress, and a
 * scan only "spent" when the result is actually readable.
 */
class DocumentScanViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val MAX_FREE_SCANS = 3
        private const val PREFS = "doc_scan"
        private const val COUNTER_KEY = "doc_scan_count"
        private const val MAX_DIM = 1600f
    }

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val purchaseManager = PurchaseManager.getInstance(app)

    private val _scanCount = MutableStateFlow(prefs.getInt(COUNTER_KEY, 0))
    val scanCount: StateFlow<Int> = _scanCount.asStateFlow()

    private val _analysis = MutableStateFlow<DocumentAnalysis?>(null)
    val analysis: StateFlow<DocumentAnalysis?> = _analysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun hasAllAccess(): Boolean = purchaseManager.hasAllAccess()

    val isScanGated: Boolean
        get() = _scanCount.value >= MAX_FREE_SCANS && !purchaseManager.hasAllAccess()

    fun remainingFreeScans(): Int = (MAX_FREE_SCANS - _scanCount.value).coerceAtLeast(0)

    fun reset() {
        _analysis.value = null
        _error.value = null
    }

    fun analyze(bitmap: Bitmap, country: String?) {
        _error.value = null
        _analysis.value = null
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val jpeg = compress(bitmap)
                val result = DocumentAnalysisClient.analyze(jpeg, country)
                _analysis.value = result
                if (!result.unreadable) {
                    val next = _scanCount.value + 1
                    _scanCount.value = next
                    prefs.edit().putInt(COUNTER_KEY, next).apply()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Document analysis failed. Please try again."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /** Downscale (longest side ≤ 1600px) + JPEG-compress, keeping payloads small. */
    private fun compress(bitmap: Bitmap): ByteArray {
        val longest = maxOf(bitmap.width, bitmap.height).toFloat()
        val scaled = if (longest > MAX_DIM) {
            val s = MAX_DIM / longest
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * s).toInt(), (bitmap.height * s).toInt(), true)
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
        return out.toByteArray()
    }
}
