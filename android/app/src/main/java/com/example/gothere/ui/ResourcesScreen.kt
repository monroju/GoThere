package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.model.Document
import com.example.gothere.viewmodel.ResourcesViewModel

/**
 * Resource data classes for web links
 */
data class ResourceItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val type: String = "guide" // official, service, marketplace, community, guide, emergency
)

data class ResourceCategory(
    val id: String = "",
    val title: String = "",
    val icon: String = "description",
    val resources: List<ResourceItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    countryId: String = "spain",
    vm: ResourcesViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Observe ViewModel state
    val documentsByFolder by vm.documentsByFolder.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    
    // Load documents when countryId changes
    LaunchedEffect(countryId) {
        vm.loadDocumentsForCountry(countryId)
    }
    
    // Country name for display
    val countryName = when (countryId) {
        "spain" -> "Spain 🇪🇸"
        "portugal" -> "Portugal 🇵🇹"
        "mexico" -> "Mexico 🇲🇽"
        else -> "Spain 🇪🇸"
    }

    // Hardcoded web resources
    val webResourceCategories = remember(countryId) { getResourcesForCountry(countryId) }

    // Map Firebase folder names to display names
    val folderDisplayNames = mapOf(
        "Arrival" to "Arrival Guides",
        "Resources" to "Downloadable Resources",
        "Templates" to "Document Templates",
        "VisaForms" to "Visa & Immigration Forms"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Header with refresh button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$countryName Resources",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Error message
            error?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMsg,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Web Resource categories (always shown)
            item {
                Text(
                    text = "Quick Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(webResourceCategories) { category ->
                ResourceCategoryCard(
                    category = category,
                    onResourceClick = { resource ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.url))
                        context.startActivity(intent)
                    }
                )
            }

            // Firebase Storage Documents Section
            if (documentsByFolder.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Downloadable Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Show each folder's documents
                documentsByFolder.forEach { (folderName, documents) ->
                    // Filter out non-document files (e.g. .json config files)
                    val filteredDocs = documents.filter { doc ->
                        !doc.name.endsWith(".json", ignoreCase = true)
                    }
                    if (filteredDocs.isNotEmpty()) {
                        item {
                            StorageDocumentsCard(
                                title = folderDisplayNames[folderName] ?: folderName,
                                icon = getFolderIcon(folderName),
                                documents = filteredDocs,
                                onDocumentClick = { doc ->
                                    // Use ACTION_VIEW with PDF MIME type so Android opens
                                    // in a PDF viewer instead of downloading directly
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(doc.downloadUrl), "application/pdf")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    // Fall back to browser if no PDF viewer installed
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.downloadUrl))
                                        context.startActivity(browserIntent)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Loading indicator at bottom
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Empty state for documents
            if (!isLoading && documentsByFolder.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No downloadable documents yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Check back later for PDF guides and forms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cross-promotion: Localista
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.localista.app")
                            )
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Newspaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Try Localista",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Spain news in English \u2014 30+ cities, emergency alerts, expat guides",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StorageDocumentsCard(
    title: String,
    icon: ImageVector,
    documents: List<Document>,
    onDocumentClick: (Document) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Document count badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${documents.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Documents list
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    documents.forEach { doc ->
                        DocumentRow(
                            document = doc,
                            onClick = { onDocumentClick(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: Document,
    onClick: () -> Unit
) {
    val fileIcon = getFileIcon(document.name)
    val displayName = document.name
        .removeSuffix(".pdf")
        .removeSuffix(".PDF")
        .replace("_", " ")
        .replace("-", " ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = fileIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error, // Red for PDF icon
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        TextButton(onClick = onClick) {
            Text("View")
        }
    }
}

@Composable
private fun ResourceCategoryCard(
    category: ResourceCategory,
    onResourceClick: (ResourceItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) } // Start collapsed for cleaner UI

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Header (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getIconForCategory(category.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Resources list
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    category.resources.forEach { resource ->
                        ResourceRow(
                            resource = resource,
                            onClick = { onResourceClick(resource) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(
    resource: ResourceItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type indicator
        Icon(
            imageVector = getIconForResourceType(resource.type),
            contentDescription = null,
            tint = getColorForResourceType(resource.type),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resource.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (resource.description.isNotBlank()) {
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.OpenInNew,
            contentDescription = "Open link",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// Helper functions
private fun getFolderIcon(folderName: String): ImageVector {
    return when (folderName) {
        "Arrival" -> Icons.Outlined.FlightLand
        "Resources" -> Icons.Outlined.Folder
        "Templates" -> Icons.Outlined.Description
        "VisaForms" -> Icons.Outlined.Badge
        else -> Icons.Outlined.Folder
    }
}

private fun getFileIcon(fileName: String): ImageVector {
    return when {
        fileName.endsWith(".pdf", ignoreCase = true) -> Icons.Outlined.PictureAsPdf
        fileName.endsWith(".doc", ignoreCase = true) || 
        fileName.endsWith(".docx", ignoreCase = true) -> Icons.Outlined.Description
        fileName.endsWith(".xls", ignoreCase = true) || 
        fileName.endsWith(".xlsx", ignoreCase = true) -> Icons.Outlined.TableChart
        else -> Icons.Outlined.InsertDriveFile
    }
}

@Composable
private fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "passport" -> Icons.Outlined.Badge
        "home" -> Icons.Outlined.Home
        "account_balance" -> Icons.Outlined.AccountBalance
        "local_hospital" -> Icons.Outlined.LocalHospital
        "gavel" -> Icons.Outlined.Gavel
        "groups" -> Icons.Outlined.Groups
        "bolt" -> Icons.Outlined.Bolt
        "directions_car" -> Icons.Outlined.DirectionsCar
        "badge" -> Icons.Outlined.Badge
        "security" -> Icons.Outlined.Security
        else -> Icons.Outlined.Description
    }
}

@Composable
private fun getIconForResourceType(type: String): ImageVector {
    return when (type) {
        "official" -> Icons.Outlined.VerifiedUser
        "service" -> Icons.Outlined.Business
        "marketplace" -> Icons.Outlined.Storefront
        "community" -> Icons.Outlined.Forum
        "emergency" -> Icons.Outlined.Warning
        else -> Icons.Outlined.Link
    }
}

@Composable
private fun getColorForResourceType(type: String): androidx.compose.ui.graphics.Color {
    return when (type) {
        "official" -> MaterialTheme.colorScheme.primary
        "service" -> MaterialTheme.colorScheme.tertiary
        "marketplace" -> MaterialTheme.colorScheme.secondary
        "community" -> MaterialTheme.colorScheme.onSurfaceVariant
        "emergency" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// ============================================================================
// COUNTRY-SPECIFIC WEB RESOURCES (Hardcoded quick links)
// ============================================================================

private fun getResourcesForCountry(countryId: String): List<ResourceCategory> {
    return when (countryId) {
        "portugal" -> getPortugalResources()
        "mexico" -> getMexicoResources()
        else -> getSpainResources()
    }
}

private fun getSpainResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "visas",
        title = "Visas & Immigration",
        icon = "passport",
        resources = listOf(
            ResourceItem("spain-consulate", "Spanish Consulates in USA", "Find your assigned consulate", "https://www.exteriores.gob.es/en/EmbajadasConsulados/Paginas/index.aspx", "official"),
            ResourceItem("spain-nie", "NIE/TIE Application Guide", "Foreigner identification numbers", "https://www.inclusion.gob.es/web/migraciones/w/extranjeria", "official"),
            ResourceItem("spain-nomad", "Digital Nomad Visa", "Remote worker visa requirements", "https://www.exteriores.gob.es", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("idealista", "Idealista", "Spain's largest property portal", "https://www.idealista.com", "marketplace"),
            ResourceItem("fotocasa", "Fotocasa", "Popular real estate website", "https://www.fotocasa.es", "marketplace"),
            ResourceItem("spotahome", "Spotahome", "Medium-term rentals with virtual tours", "https://www.spotahome.com", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("n26", "N26 Bank", "Digital bank, easy online setup", "https://n26.com/en-es", "service"),
            ResourceItem("bbva", "BBVA", "Major Spanish bank", "https://www.bbva.es", "service"),
            ResourceItem("wise", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("seg-social", "Spanish Social Security", "Public healthcare registration", "https://www.seg-social.es", "official"),
            ResourceItem("sanitas", "Sanitas", "Popular private insurance", "https://www.sanitas.es", "service")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations", "InterNations Spain", "Expat network with events", "https://www.internations.org/spain-expats", "community"),
            ResourceItem("expat-forum", "Spain Expat Forum", "Discussion forum", "https://www.expatforum.com/forums/spain-expat-forum.22/", "community")
        )
    )
)

private fun getPortugalResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "visas",
        title = "Visas & Immigration",
        icon = "passport",
        resources = listOf(
            ResourceItem("aima", "AIMA (Immigration Agency)", "Portuguese immigration portal", "https://www.aima.gov.pt", "official"),
            ResourceItem("d7", "D7 Passive Income Visa", "Visa for retirees and passive income", "https://vistos.mne.gov.pt/en/national-visas/general-information/type-of-visa", "official"),
            ResourceItem("d8", "D8 Digital Nomad Visa", "Remote worker visa", "https://www.aima.gov.pt", "official"),
            ResourceItem("golden", "Golden Visa Program", "Residency by investment", "https://www.sef.pt/en/pages/conteudo-detalhe.aspx?nID=21", "official")
        )
    ),
    ResourceCategory(
        id = "nif",
        title = "NIF & Tax Registration",
        icon = "badge",
        resources = listOf(
            ResourceItem("nif", "Portal das Finanças", "Get your NIF tax number", "https://www.portaldasfinancas.gov.pt", "official"),
            ResourceItem("bordr", "Bordr (NIF Service)", "Get NIF remotely", "https://bordr.io", "service")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("idealista-pt", "Idealista Portugal", "Largest property portal", "https://www.idealista.pt", "marketplace"),
            ResourceItem("imovirtual", "Imovirtual", "Popular real estate site", "https://www.imovirtual.com", "marketplace"),
            ResourceItem("casa-sapo", "Casa Sapo", "Portuguese listings", "https://casa.sapo.pt", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("activobank", "ActivoBank", "No-fee digital bank", "https://www.activobank.pt", "service"),
            ResourceItem("millennium", "Millennium BCP", "Largest private bank", "https://www.millenniumbcp.pt", "service"),
            ResourceItem("wise", "Wise", "Multi-currency transfers", "https://wise.com", "service"),
            ResourceItem("revolut", "Revolut", "Digital banking app", "https://www.revolut.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("sns", "SNS (National Health Service)", "Public healthcare", "https://www.sns.gov.pt", "official"),
            ResourceItem("medis", "Médis", "Private health insurance", "https://www.medis.pt", "service")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("americans-pt", "Americans & FriendsPT", "Facebook community", "https://www.facebook.com/groups/AmericansAndFriendsPT", "community"),
            ResourceItem("internations-pt", "InterNations Portugal", "Expat network", "https://www.internations.org/portugal-expats", "community"),
            ResourceItem("reddit-pt", "r/PortugalExpats", "Reddit community", "https://www.reddit.com/r/PortugalExpats/", "community")
        )
    ),
    ResourceCategory(
        id = "utilities",
        title = "Utilities & Services",
        icon = "bolt",
        resources = listOf(
            ResourceItem("edp", "EDP (Electricity)", "Main electricity provider", "https://www.edp.pt", "service"),
            ResourceItem("meo", "MEO", "Phone, internet, TV", "https://www.meo.pt", "service"),
            ResourceItem("nos", "NOS", "Mobile and home services", "https://www.nos.pt", "service")
        )
    )
)

private fun getMexicoResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "visas",
        title = "Visas & Immigration",
        icon = "passport",
        resources = listOf(
            ResourceItem("inm", "INM (Immigration Agency)", "Instituto Nacional de Migración", "https://www.gob.mx/inm", "official"),
            ResourceItem("temp-resident", "Temporary Resident Visa", "1-4 year residency", "https://www.gob.mx/tramites/ficha/visa-de-residente-temporal/SRE273", "official"),
            ResourceItem("inm-citas", "INM Appointments", "Schedule card exchange", "https://citas.inm.gob.mx/", "official")
        )
    ),
    ResourceCategory(
        id = "curp",
        title = "CURP & RFC Registration",
        icon = "badge",
        resources = listOf(
            ResourceItem("curp", "CURP (Population Registry)", "Get your unique ID number", "https://www.gob.mx/curp/", "official"),
            ResourceItem("sat", "SAT - RFC (Tax ID)", "Tax registration", "https://www.sat.gob.mx", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("inmuebles24", "Inmuebles24", "Mexico's largest property portal", "https://www.inmuebles24.com", "marketplace"),
            ResourceItem("segundamano", "Segundamano", "Classifieds with property listings", "https://www.segundamano.mx", "marketplace"),
            ResourceItem("vivanuncios", "Vivanuncios", "Property listings", "https://www.vivanuncios.com.mx", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("bbva-mx", "BBVA México", "Major bank, English app", "https://www.bbva.mx", "service"),
            ResourceItem("santander-mx", "Santander México", "International bank", "https://www.santander.com.mx", "service"),
            ResourceItem("nu-mx", "Nu México", "Digital bank", "https://www.nu.com.mx", "service"),
            ResourceItem("wise", "Wise", "International transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("imss", "IMSS (Public Health)", "Mexican Social Security", "http://www.imss.gob.mx", "official"),
            ResourceItem("imss-vol", "IMSS Voluntario", "Voluntary enrollment for expats", "http://www.imss.gob.mx/tramites/imss02025a", "official"),
            ResourceItem("gnp", "GNP Seguros", "Private health insurance", "https://www.gnp.com.mx", "service")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("expats-cdmx", "Expats in Mexico City", "Facebook community", "https://www.facebook.com/groups/expatsinmexicocity", "community"),
            ResourceItem("internations-mx", "InterNations Mexico", "Expat network", "https://www.internations.org/mexico-expats", "community"),
            ResourceItem("sma-civil", "San Miguel Civil List", "Famous SMA expat list", "https://www.civillist.org", "community")
        )
    ),
    ResourceCategory(
        id = "utilities",
        title = "Utilities & Services",
        icon = "bolt",
        resources = listOf(
            ResourceItem("cfe", "CFE (Electricity)", "National electricity company", "https://www.cfe.mx", "service"),
            ResourceItem("telmex", "Telmex", "Home internet and phone", "https://www.telmex.com", "service"),
            ResourceItem("telcel", "Telcel", "Largest mobile carrier", "https://www.telcel.com", "service")
        )
    ),
    ResourceCategory(
        id = "safety",
        title = "Safety & Emergency",
        icon = "security",
        resources = listOf(
            ResourceItem("us-embassy", "US Embassy Mexico", "Consular services", "https://mx.usembassy.gov", "official"),
            ResourceItem("step", "STEP Enrollment", "Smart Traveler Enrollment", "https://step.state.gov", "official")
        )
    )
)
