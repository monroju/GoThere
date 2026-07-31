package com.example.gothere.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.BuildConfig
import com.example.gothere.ai.DocumentAnalysis
import com.example.gothere.viewmodel.DocumentScanViewModel
import java.io.File

private fun createImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val file = File.createTempFile("scan_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
}

@Composable
fun DocumentScanScreen(
    countryId: String? = null,
    onRequestUpgrade: () -> Unit
) {
    val context = LocalContext.current
    val vm: DocumentScanViewModel = viewModel()

    val analysis by vm.analysis.collectAsState()
    val isAnalyzing by vm.isAnalyzing.collectAsState()
    val error by vm.error.collectAsState()
    val scanCount by vm.scanCount.collectAsState()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun analyzeUri(uri: Uri) {
        val bmp = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bmp != null) vm.analyze(bmp, countryId)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingUri?.let { analyzeUri(it) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { analyzeUri(it) } }

    fun startCamera() {
        if (vm.isScanGated) { onRequestUpgrade(); return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val uri = createImageUri(context)
            pendingUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun startPhoto() {
        if (vm.isScanGated) { onRequestUpgrade(); return }
        photoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val current = analysis
        when {
            isAnalyzing -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Reading your document…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            current != null -> {
                DocumentResultCard(current)
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan another document")
                }
            }
            else -> {
                Intro()
                Button(
                    onClick = { startCamera() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan with camera")
                }
                OutlinedButton(
                    onClick = { startPhoto() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose a photo")
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (!vm.hasAllAccess()) {
            val remaining = (DocumentScanViewModel.MAX_FREE_SCANS - scanCount).coerceAtLeast(0)
            Text(
                if (remaining > 0) "$remaining free scan${if (remaining == 1) "" else "s"} left"
                else "Free scans used — upgrade for unlimited",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "GoThere explains documents to help you understand them. It is not legal, tax, or immigration advice. Always verify with the office that sent it, or a qualified professional, before acting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Intro() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text("Got a letter you can't read?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Photograph any official letter or form — from immigration, tax, your landlord, the bank, or the health service. We explain it in plain English and flag any deadline, even if it's in another language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DocumentResultCard(a: DocumentAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        when {
            a.unreadable -> StateMessage(
                "Couldn't read this clearly",
                "Try again with good light, the document flat, and the camera straight on. Make sure the whole page is in frame."
            )
            !a.isDocument -> StateMessage(
                "That doesn't look like an official document",
                "Point the camera at a letter or form you received — from immigration, tax, your landlord, the bank, or the health service."
            )
            else -> {
                // Header
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(a.docType ?: "Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        a.sender?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        a.originalLanguage?.takeIf { it.isNotEmpty() }?.let {
                            Text("Original language: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    a.confidence?.let { ConfidenceBadge(it) }
                }

                // Deadlines first
                if (a.deadlines.isEmpty()) {
                    Text("No deadline stated in this document.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    a.deadlines.forEach { DeadlineCard(it.dateAsWritten, it.action, it.sourceQuote) }
                }

                a.summary?.takeIf { it.isNotEmpty() }?.let { TintedSection("What this says", it) }
                a.nextStep?.takeIf { it.isNotEmpty() }?.let { TintedSection("Your next step", it) }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: String) {
    val color = when (confidence) {
        "high" -> Color(0xFF34D399)
        "low" -> MaterialTheme.colorScheme.error
        else -> Color(0xFFF59E0B)
    }
    Text(
        confidence.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun DeadlineCard(dateAsWritten: String, action: String, sourceQuote: String) {
    val orange = Color(0xFFF59E0B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(orange.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, orange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⏰ $dateAsWritten", style = MaterialTheme.typography.titleSmall, color = orange, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) Text(action, style = MaterialTheme.typography.bodyMedium)
        if (sourceQuote.isNotEmpty()) {
            Text("From the document:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "“$sourceQuote”",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TintedSection(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StateMessage(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
