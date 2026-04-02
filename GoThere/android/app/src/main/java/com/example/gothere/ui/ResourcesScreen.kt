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
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.FormDoc
import com.example.gothere.model.Resource
import com.example.gothere.repository.UserFormsRepository
import com.example.gothere.util.DownloadUtils
import com.example.gothere.viewmodel.ResourcesViewModel
import kotlinx.coroutines.launch

@Composable
fun ResourcesScreen() {
    val vm: ResourcesViewModel = viewModel()
    val resources by vm.resources.collectAsState()
    val grouped = remember(resources) { resources.groupBy { it.category.ifBlank { "Other" } } }

    val ctx = LocalContext.current
    val myRepo = remember { UserFormsRepository() }
    val scope = rememberCoroutineScope()

    var myForms by remember { mutableStateOf<List<FormDoc>>(emptyList()) }
    var loadingMy by remember { mutableStateOf(false) }
    var myError by remember { mutableStateOf<String?>(null) }

    // Initial load for "My Forms"
    LaunchedEffect(Unit) {
        refreshMyForms(
            ctx = ctx,
            repo = myRepo,
            set = { myForms = it },
            setBusy = { loadingMy = it },
            setErr = { myError = it }
        )
    }

    // File picker for uploads (PDFs)
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                runCatching { myRepo.upload(uri, "application/pdf") }
                    .onSuccess {
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

        // --- Visa Forms (static list using your public URLs) ---
        item { VisaFormsCard() }

        // --- Helpful Resources (from Firestore) ---
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

        // Cross-promotion: Localista
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    openUrl(ctx, "https://play.google.com/store/apps/details?id=com.localista.spain")
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Newspaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Try Localista",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Spain news in English — 30+ cities, emergency alerts, expat guides",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            Icon(Icons.Outlined.Link, contentDescription = "Link", modifier = Modifier.size(18.dp))
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

/* ---------- Visa Forms (static card with your URLs) ---------- */

private data class PdfForm(val title: String, val url: String)

@Composable
private fun VisaFormsCard(modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current

    val visaForms = listOf(
        PdfForm("EX00. Long-Term Stay",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX00.%20Long-Term%20Stay.pdf?alt=media&token=8a87b268-d9e6-43eb-afde-a03ba38dbdcc"),
        PdfForm("EX01. Non-lucrative Temp Residence",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX01.%20Non-lucrative%20Temp%20Residence.pdf?alt=media&token=be0b34b3-20a3-4b12-a767-57ee370c5a39"),
        PdfForm("EX02. Temp for Family Reunion",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX02.Temp%20for%20Family%20Reunion.pdf?alt=media&token=9c7cd171-63dd-4b5d-a386-b70857c5ee4c"),
        PdfForm("EX03. Temp Residence Employee Work",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX03.%20Temp%20Residence%20Employee%20Work.pdf?alt=media&token=7684e53d-59ef-4f6c-82a4-442683e11898"),
        PdfForm("EX04. Residence for Internships",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX04.%20Residence%20for%20Internships.pdf?alt=media&token=7c259fef-7815-41f2-81ed-52133d5797ef"),
        PdfForm("EX06. Work Residence Seasonal",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX06.%20Work%20Residence%20Seasonal.pdf?alt=media&token=c90d0498-2ad8-4dac-be57-d56a6211a134"),
        PdfForm("EX07. Temp Residence Self Employment",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX07.%20Temp%20Residence%20Self%20Employment.pdf?alt=media&token=b8a5f847-9782-4703-914c-07b39552c526"),
        PdfForm("EX09. Temp Work Exception",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX09.%20Temp%20Work%20Exception.pdf?alt=media&token=d76b2c00-9abb-4f0b-a9da-9c20de713f27"),
        PdfForm("EX10. Residence Exceptional Circumstances",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX10.%20Residence%20Exc.Circumstances.pdf?alt=media&token=a0d37bb2-9f38-403d-8730-15fdbeb68758"),
        PdfForm("EX11. Long-term Residence EU",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX11.%20Long%20term%20Residence%20EU.pdf?alt=media&token=fa5ba7f0-5859-416c-a4c0-a33cf3b5a668"),
        PdfForm("EX13. Return Authorization",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX13.%20Retrun%20Auth.pdf?alt=media&token=a32de66a-d310-4394-9d3e-c9bc2ee13dc7"),
        PdfForm("EX15. NIE and Certificates",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX15.%20NIE%20and%20Certs.pdf?alt=media&token=7a4db604-d724-4b33-8f45-9b5e0be9bc88"),
        PdfForm("EX16. Registration Card / Travel Doc",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX16.%20Registration%20Card%20Travel%20Doc.pdf?alt=media&token=5188a5a3-5145-4020-9449-56f8d14ed2fd"),
        PdfForm("EX17. RCE and EU Citizen",
            "https://firebasestorage.googleapis.com/v0/b/gothere-e5ea7.firebasestorage.app/o/Visa%20Forms%2FEX17.%20RCE%20and%20EU%20CItizen.pdf?alt=media&token=b7e45699-1a66-438b-81c5-340027dfb992"),
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Visa Forms",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            visaForms.forEachIndexed { idx, doc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uri.openUri(doc.url) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(doc.title, modifier = Modifier.weight(1f))
                    TextButton(onClick = { uri.openUri(doc.url) }) {
                        Text("View / Download")
                    }
                }
                if (idx != visaForms.lastIndex) Divider()
            }
        }
    }
}
