// app/src/main/java/com/example/gothere/ui/ResourcesScreen.kt
package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.FormDoc
import com.example.gothere.model.Resource
import com.example.gothere.repository.FormsStorageRepository
import com.example.gothere.repository.UserFormsRepository
import com.example.gothere.util.DownloadUtils
import com.example.gothere.viewmodel.ResourcesViewModel
import kotlinx.coroutines.launch

@Composable
fun ResourcesScreen() {
    val vm: ResourcesViewModel = viewModel()
    val resources by vm.resources.collectAsState()

    val ctx = LocalContext.current
    val storageRepo = remember { FormsStorageRepository() }
    val myRepo = remember { UserFormsRepository() }
    val scope = rememberCoroutineScope()

    var forms by remember { mutableStateOf<List<FormDoc>>(emptyList()) }
    var loadingForms by remember { mutableStateOf(false) }
    var formsError by remember { mutableStateOf<String?>(null) }

    var myForms by remember { mutableStateOf<List<FormDoc>>(emptyList()) }
    var loadingMy by remember { mutableStateOf(false) }
    var myError by remember { mutableStateOf<String?>(null) }

    // Initial loads
    LaunchedEffect(Unit) {
        loadingForms = true; formsError = null
        runCatching { storageRepo.listVisaForms(ctx) }
            .onSuccess { forms = it }
            .onFailure { formsError = it.message }
        loadingForms = false

        refreshMyForms(
            ctx = ctx,
            repo = myRepo,
            set = { myForms = it },
            setBusy = { loadingMy = it },
            setErr = { myError = it }
        )
    }

    // File picker for user uploads (PDFs)
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // persist permission then upload in coroutine
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                runCatching { myRepo.upload(uri, "application/pdf") }
                    .onSuccess {
                        myRepo.invalidateCache(ctx)
                        refreshMyForms(
                            ctx = ctx,
                            repo = myRepo,
                            set = { myForms = it },
                            setBusy = { loadingMy = it },
                            setErr = { myError = it }
                        )
                    }
                    .onFailure { myError = it.message }
            }
        }
    }

    val grouped = remember(resources) { resources.groupBy { it.category.ifBlank { "Other" } } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- My Forms (user uploads) ---
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "My Forms",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { uploadLauncher.launch(arrayOf("application/pdf")) }
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Upload PDF")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        loadingMy -> CircularProgressIndicator(Modifier.size(24.dp))
                        myError != null -> Text("Error: $myError")
                        myForms.isEmpty() -> Text("No uploads yet.")
                        else -> myForms.forEach { f ->
                            FormRow(
                                item = f,
                                onPreview = { openUrl(ctx, f.url) },
                                onDownload = {
                                    DownloadUtils.downloadPdf(
                                        ctx,
                                        f.url,
                                        DownloadUtils.suggestedFileName(f.url, f.name)
                                    )
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // --- Visa Forms (read-only, from Storage) ---
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Visa Forms",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        loadingForms -> CircularProgressIndicator(Modifier.size(24.dp))
                        formsError != null -> Text("Error: $formsError")
                        forms.isEmpty() -> Text("No forms available.")
                        else -> forms.forEach { form ->
                            FormRow(
                                item = form,
                                onPreview = { openUrl(ctx, form.url) },
                                onDownload = {
                                    DownloadUtils.downloadPdf(
                                        ctx,
                                        form.url,
                                        DownloadUtils.suggestedFileName(form.url, form.name)
                                    )
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // --- Firestore Resources (links) ---
        grouped.forEach { (category, list) ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            category,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        list.forEach { res ->
                            ResourceRow(res) { url -> openUrl(ctx, url) }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ctx.startActivity(i)
    }
}

@Composable
private fun ResourceRow(res: Resource, onOpen: (String) -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = res.url.isNotBlank()) { onOpen(res.url) }
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = "Link",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(res.label.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyLarge)
                if (res.url.isNotBlank()) {
                    Text(
                        res.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FormRow(
    item: FormDoc,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            val icon = when {
                item.name.endsWith(".pdf", ignoreCase = true) -> Icons.Outlined.PictureAsPdf
                item.name.endsWith(".png", true) ||
                item.name.endsWith(".jpg", true) ||
                item.name.endsWith(".jpeg", true) -> Icons.Outlined.Image
                else -> Icons.Outlined.Description
            }
            Icon(icon, contentDescription = "File", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(item.name.ifBlank { "Form" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onPreview) { Text("Preview") }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onDownload) { Text("Download") }
        }
    }
}

private suspend fun refreshMyForms(
    ctx: android.content.Context,
    repo: UserFormsRepository,
    set: (List<FormDoc>) -> Unit,
    setBusy: (Boolean) -> Unit,
    setErr: (String?) -> Unit
) {
    setBusy(true); setErr(null)
    runCatching { repo.listMyForms(ctx) }
        .onSuccess { set(it) }
        .onFailure { setErr(it.message) }
    setBusy(false)
}
