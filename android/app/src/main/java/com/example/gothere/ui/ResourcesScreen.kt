package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.MenuBook
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.data.RealJourney
import com.example.gothere.data.RealJourneys
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

/** Tier-targeting tools hosted via state-swap inside ResourcesScreen. Mirror of the
 *  iOS Resources CTA stack. */
enum class ResourceTool {
    StartHere, CostCalc, Family, RemoteWork, Healthcare, Timeline,
    Investment, Concierge, PolicyWatch, RightsSafety, CareContinuity, Ancestry, Compare
}

/** Reusable CTA row for a tier-targeting tool. */
@Composable
private fun ToolCta(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    countryId: String = "spain",
    vm: ResourcesViewModel = viewModel()
) {
    val context = LocalContext.current
    val purchaseManager = remember { PurchaseManager.getInstance(context) }
    val purchasedCountries by purchaseManager.purchasedCountries.collectAsState()
    val isUnlocked = purchasedCountries.contains(countryId)
    var showPaywall by remember { mutableStateOf(false) }
    var presentedJourney by remember { mutableStateOf<RealJourney?>(null) }
    // Tier-targeting tool hub — state-swap navigation (mirror of iOS Resources CTAs).
    var activeTool by remember { mutableStateOf<ResourceTool?>(null) }

    // Hardware-back dismisses the active tool back to the Resources list (iOS gets this
    // free via NavigationLink).
    BackHandler(enabled = activeTool != null) { activeTool = null }

    when (activeTool) {
        ResourceTool.StartHere -> {
            StartHereScreen(
                onDismiss = { activeTool = null },
                onCostCalc = { activeTool = ResourceTool.CostCalc },
                onFamily = { activeTool = ResourceTool.Family },
                onRemote = { activeTool = ResourceTool.RemoteWork },
                onAncestry = { activeTool = ResourceTool.Ancestry },
                onInvestment = { activeTool = ResourceTool.Investment },
                onRights = { activeTool = ResourceTool.RightsSafety },
                onCompare = { activeTool = ResourceTool.Compare }
            ); return
        }
        ResourceTool.CostCalc -> { MoveCostCalculatorScreen(initialCountryId = countryId, onDismiss = { activeTool = null }); return }
        ResourceTool.Family -> { FamilyMoveScreen(initialCountryId = countryId, onDismiss = { activeTool = null }); return }
        ResourceTool.RemoteWork -> { RemoteWorkScreen(onDismiss = { activeTool = null }); return }
        ResourceTool.Healthcare -> { HealthcareCompareScreen(initialCountryId = countryId, onDismiss = { activeTool = null }); return }
        ResourceTool.Timeline -> { RelocationTimelineScreen(onDismiss = { activeTool = null }); return }
        ResourceTool.Investment -> { InvestmentMigrationScreen(onDismiss = { activeTool = null }, onOpenAncestry = { activeTool = ResourceTool.Ancestry }); return }
        ResourceTool.Concierge -> { ConciergeScreen(onDismiss = { activeTool = null }); return }
        ResourceTool.PolicyWatch -> { PolicyWatchScreen(onDismiss = { activeTool = null }); return }
        ResourceTool.RightsSafety -> { RightsSafetyScreen(onDismiss = { activeTool = null }, onOpenCare = { activeTool = ResourceTool.CareContinuity }); return }
        ResourceTool.CareContinuity -> { CareContinuityScreen(initialCountryId = countryId, onDismiss = { activeTool = null }); return }
        ResourceTool.Ancestry -> { AncestryCheckerScreen(onBack = { activeTool = null }); return }
        ResourceTool.Compare -> { VisaCompareScreen(initialCountryId = countryId, onDismiss = { activeTool = null }, onStartWizard = { activeTool = null }); return }
        null -> {}
    }

    // Observe ViewModel state
    val documentsByFolder by vm.documentsByFolder.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    // Load documents when countryId changes (only when unlocked)
    LaunchedEffect(countryId, isUnlocked) {
        if (isUnlocked) {
            vm.loadDocumentsForCountry(countryId)
        }
    }

    if (showPaywall) {
        PaywallDialog(
            countryId = countryId,
            onDismiss = { showPaywall = false },
            purchaseManager = purchaseManager
        )
    }

    presentedJourney?.let { journey ->
        // Full-screen dialog hosting the Real Journey premium content. Paywalled by
        // PurchaseManager.hasAllAccess() internally.
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { presentedJourney = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            RealJourneyScreen(
                journey = journey,
                purchaseManager = purchaseManager,
                onDismiss = { presentedJourney = null },
                onOpenPaywall = {
                    presentedJourney = null
                    showPaywall = true
                }
            )
        }
    }
    
    // Country name for display
    val countryName = when (countryId) {
        "spain" -> "Spain 🇪🇸"
        "portugal" -> "Portugal 🇵🇹"
        "mexico" -> "Mexico 🇲🇽"
        "canada" -> "Canada 🇨🇦"
        "ireland" -> "Ireland 🇮🇪"
        "italy" -> "Italy 🇮🇹"
        "germany" -> "Germany 🇩🇪"
        "poland" -> "Poland 🇵🇱"
        "argentina" -> "Argentina 🇦🇷"
        "hungary" -> "Hungary 🇭🇺"
        "uk_ancestry" -> "UK (Ancestry) 🇬🇧"
        else -> "Spain 🇪🇸"
    }

    // Hardcoded web resources
    val webResourceCategories = remember(countryId) { getResourcesForCountry(countryId) }
    val realJourney = remember(countryId) { RealJourneys.forCountry(countryId).firstOrNull() }

    // "For You" inclusivity state — hoisted to composable scope. `remember` cannot be
    // called inside the LazyListScope builder lambda (not a @Composable context).
    val forYouState = remember(countryId) {
        com.example.gothere.repository.UserConsiderationsStore.load(context)
    }
    val forYouCats = remember(countryId, forYouState) {
        InclusivityResources.categories(
            considerations = forYouState.considerations,
            isSingleParent = forYouState.isSingleParent,
            countryId = countryId
        )
    }

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

            // Tier-targeting tool CTAs — all free, non-paywalled hooks. StartHere first.
            item {
                ToolCta(Icons.Outlined.Explore, "Where do I start?",
                    "Answer one question — we'll point you to the right tool",
                    highlight = true) { activeTool = ResourceTool.StartHere }
            }
            item {
                ToolCta(Icons.Outlined.Paid, "Can I afford to move?",
                    "Estimate your real cost to land in $countryName") { activeTool = ResourceTool.CostCalc }
            }
            item {
                ToolCta(Icons.Outlined.FamilyRestroom, "Moving with kids",
                    "Schooling, healthcare & visas for your children in $countryName") { activeTool = ResourceTool.Family }
            }
            item {
                ToolCta(Icons.Outlined.Computer, "Bring your job abroad",
                    "Employer-letter templates + the tax-residency traps to avoid") { activeTool = ResourceTool.RemoteWork }
            }
            item {
                ToolCta(Icons.Outlined.LocalHospital, "Healthcare costs vs the US",
                    "See $countryName premiums and public coverage next to US prices") { activeTool = ResourceTool.Healthcare }
            }
            item {
                ToolCta(Icons.Outlined.CalendarMonth, "Build my move timeline",
                    "\"Gone in N months\" → a personalized month-by-month plan") { activeTool = ResourceTool.Timeline }
            }
            item {
                ToolCta(Icons.Outlined.TrendingUp, "Golden visas & second passports",
                    "Residency & citizenship by investment — incl. Caribbean CBI") { activeTool = ResourceTool.Investment }
            }
            item {
                ToolCta(Icons.Outlined.Favorite, "Rights & Safety",
                    "Compare destinations on LGBTQ+, disability, reproductive & senior protections") { activeTool = ResourceTool.RightsSafety }
            }
            item {
                ToolCta(Icons.Outlined.Medication, "Meds & Care Abroad",
                    "ADHD meds, HRT, insulin — availability and what you can carry in") { activeTool = ResourceTool.CareContinuity }
            }
            item {
                ToolCta(Icons.Outlined.Notifications, "Policy Watch",
                    "US policy changes that affect your move — with alerts") { activeTool = ResourceTool.PolicyWatch }
            }
            item {
                ToolCta(Icons.Outlined.Star, "GoThere Concierge (soon)",
                    "White-glove relocation: vetted lawyers, done-with-you prep") { activeTool = ResourceTool.Concierge }
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

            // Locked country gate: show paywall card instead of resources
            if (!isUnlocked) {
                item {
                    LockedCountryCard(
                        countryName = countryName,
                        onUnlockClick = { showPaywall = true }
                    )
                }
            }

            // Real Journey CTA — paywalled premium content authored from anonymized
            // real client correspondence. Only surfaces when a journey exists for this
            // country. The country must be unlocked first (matches iOS gating order).
            if (isUnlocked && realJourney != null) {
                item {
                    RealJourneyCtaCard(
                        journey = realJourney,
                        isProUnlocked = purchaseManager.hasAllAccess(),
                        onClick = { presentedJourney = realJourney }
                    )
                }
            }

            // "For You" — inclusivity-aware resources driven by the wizard's
            // PersonalConsiderations + Household. Renders only when at least one
            // persona was selected (or Single Parent household).
            if (isUnlocked) {
                if (forYouCats.isNotEmpty() || forYouState.considerations.isNotEmpty() || forYouState.isSingleParent) {
                    item {
                        InclusionNotesCard(
                            countryId = countryId,
                            considerations = forYouState.considerations,
                            isSingleParent = forYouState.isSingleParent,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (forYouCats.isNotEmpty()) {
                        item {
                            Text(
                                text = "For You",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(forYouCats) { category ->
                            ResourceCategoryCard(
                                category = category,
                                onResourceClick = { resource ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // Web Resource categories (only when country is unlocked)
            if (isUnlocked) {
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
            }

            // Firebase Storage Documents Section (only when country is unlocked)
            if (isUnlocked && documentsByFolder.isNotEmpty()) {
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
            if (isUnlocked && isLoading) {
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
            if (isUnlocked && !isLoading && documentsByFolder.isEmpty()) {
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
private fun LockedCountryCard(
    countryName: String,
    onUnlockClick: () -> Unit
) {
    val displayName = countryName.removeSuffix(" 🇪🇸")
        .removeSuffix(" 🇵🇹")
        .removeSuffix(" 🇲🇽")
        .removeSuffix(" 🇨🇦")
        .removeSuffix(" 🇮🇪")
        .removeSuffix(" 🇮🇹")
        .removeSuffix(" 🇩🇪")
        .removeSuffix(" 🇵🇱")
        .removeSuffix(" 🇦🇷")
        .removeSuffix(" 🇭🇺")
        .removeSuffix(" 🇬🇧")
        .trim()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$displayName is locked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Unlock the $displayName Pack to access immigration portals, housing sites, banking, healthcare, and expat communities — plus downloadable PDF guides.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onUnlockClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock $displayName")
            }
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
        "canada" -> getCanadaResources()
        "ireland" -> getIrelandResources()
        "italy" -> getItalyResources()
        "germany" -> getGermanyResources()
        "poland" -> getPolandResources()
        "argentina" -> getArgentinaResources()
        "hungary" -> getHungaryResources()
        "uk_ancestry" -> getUkAncestryResources()
        else -> getSpainResources()
    }
}

private fun getSpainResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "visas",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("spain-consulate", "Spanish Consulates in USA", "Find your assigned consulate", "https://www.exteriores.gob.es/en/EmbajadasConsulados/Paginas/index.aspx", "official"),
            ResourceItem("spain-nie", "NIE/TIE Application Guide", "Foreigner identification numbers", "https://www.inclusion.gob.es/web/migraciones/w/extranjeria", "official"),
            ResourceItem("spain-migr", "Spanish Immigration Portal", "Official Inclusión y Migraciones", "https://www.inclusion.gob.es/web/migraciones", "official")
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
            ResourceItem("pt-mne", "Portuguese MFA Visas Portal", "All national visas (D7, D8, D2, etc.)", "https://vistos.mne.gov.pt/en/national-visas/general-information/type-of-visa", "official")
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

private fun getCanadaResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("ircc", "IRCC (Immigration Canada)", "Immigration, Refugees and Citizenship Canada", "https://www.canada.ca/en/immigration-refugees-citizenship.html", "official"),
            ResourceItem("consulates-ca", "Canadian Consulates in USA", "Find your assigned consulate", "https://travel.gc.ca/assistance/embassies-consulates", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "ID & Tax Registration",
        icon = "badge",
        resources = listOf(
            ResourceItem("sin", "SIN (Social Insurance Number)", "Required to work and pay taxes", "https://www.canada.ca/en/employment-social-development/services/sin.html", "official"),
            ResourceItem("cra", "Canada Revenue Agency", "Federal taxes for residents", "https://www.canada.ca/en/revenue-agency.html", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("realtor-ca", "Realtor.ca", "MLS national property search", "https://www.realtor.ca", "marketplace"),
            ResourceItem("rentals-ca", "Rentals.ca", "Apartment and home rentals", "https://rentals.ca", "marketplace"),
            ResourceItem("kijiji", "Kijiji", "Classifieds with rentals", "https://www.kijiji.ca", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("rbc", "RBC Royal Bank", "Largest Canadian bank", "https://www.rbc.com", "service"),
            ResourceItem("td", "TD Canada Trust", "Cross-border friendly", "https://www.td.com", "service"),
            ResourceItem("wise-ca", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("health-canada", "Health Canada", "Provincial healthcare overview", "https://www.canada.ca/en/health-canada/services/health-care-system/canada-health-care-system-medicare.html", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("reddit-can", "r/PersonalFinanceCanada", "Reddit community", "https://www.reddit.com/r/PersonalFinanceCanada/", "community"),
            ResourceItem("internations-ca", "InterNations Canada", "Expat network", "https://www.internations.org/canada-expats", "community")
        )
    )
)

private fun getIrelandResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("dfa", "Department of Foreign Affairs", "Citizenship + foreign births portal", "https://www.ireland.ie/en/dfa/citizenship/", "official"),
            ResourceItem("isd", "Irish Immigration Service", "Visas, Stamps, residence permissions", "https://www.irishimmigration.ie/", "official"),
            ResourceItem("consulates-ie", "Irish Consulates in USA", "Find your assigned consulate", "https://www.ireland.ie/en/usa/", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "ID & Tax Registration",
        icon = "badge",
        resources = listOf(
            ResourceItem("pps", "PPS Number Service", "Personal Public Service Number", "https://www.gov.ie/en/service/12e6de-get-a-personal-public-service-pps-number/", "official"),
            ResourceItem("revenue-ie", "Revenue (Irish Tax)", "Tax registration for residents", "https://www.revenue.ie", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("daft", "Daft.ie", "Ireland's largest property portal", "https://www.daft.ie", "marketplace"),
            ResourceItem("myhome-ie", "MyHome.ie", "Property and rentals", "https://www.myhome.ie", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("boi", "Bank of Ireland", "Largest retail bank", "https://www.bankofireland.com", "service"),
            ResourceItem("aib", "AIB", "Allied Irish Banks", "https://www.aib.ie", "service"),
            ResourceItem("revolut-ie", "Revolut", "Digital banking, popular in Ireland", "https://www.revolut.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("hse", "HSE (Health Service Executive)", "Public healthcare", "https://www.hse.ie", "official"),
            ResourceItem("vhi", "Vhi Healthcare", "Largest private insurer", "https://www.vhi.ie", "service")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("boards-ie", "Boards.ie", "Ireland's largest forum", "https://www.boards.ie", "community"),
            ResourceItem("internations-ie", "InterNations Ireland", "Expat network", "https://www.internations.org/ireland-expats", "community")
        )
    )
)

private fun getItalyResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("esteri", "Italian MFA Citizenship", "Jure sanguinis + visa types portal", "https://www.esteri.it/en/servizi-consolari-e-visti/italiani-all-estero/cittadinanza/", "official"),
            ResourceItem("vistoperitalia", "Visto per l'Italia", "Italian visa portal — all national visas", "https://vistoperitalia.esteri.it/", "official"),
            ResourceItem("prenotami", "Prenot@mi", "Book consulate appointments", "https://prenotami.esteri.it/", "official"),
            ResourceItem("consulates-it", "Italian Consulates in USA", "Find your assigned consulate", "https://www.esteri.it/en/ministero/laministero/lerappresentanze/", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "Codice Fiscale & Anagrafe",
        icon = "badge",
        resources = listOf(
            ResourceItem("codice-fiscale", "Agenzia delle Entrate (Codice Fiscale)", "Italian tax code", "https://www.agenziaentrate.gov.it/portale/codice-fiscale-tessera-sanitaria", "official"),
            ResourceItem("anagrafe", "Anagrafe Nazionale", "National residents registry", "https://www.anagrafenazionale.interno.it/", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("immobiliare-it", "Immobiliare.it", "Italy's largest property portal", "https://www.immobiliare.it", "marketplace"),
            ResourceItem("idealista-it", "Idealista Italia", "Property and rentals", "https://www.idealista.it", "marketplace"),
            ResourceItem("subito-it", "Subito.it", "Classifieds with rentals", "https://www.subito.it", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("intesa", "Intesa Sanpaolo", "Largest Italian bank", "https://www.intesasanpaolo.com", "service"),
            ResourceItem("unicredit", "UniCredit", "Major bank, English support", "https://www.unicredit.it", "service"),
            ResourceItem("wise-it", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("ssn", "SSN (Servizio Sanitario Nazionale)", "National Health Service", "https://www.salute.gov.it", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations-it", "InterNations Italy", "Expat network", "https://www.internations.org/italy-expats", "community"),
            ResourceItem("reddit-it", "r/ItalyExpat", "Reddit community", "https://www.reddit.com/r/ItalyExpat/", "community")
        )
    )
)

private fun getGermanyResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("bva", "Bundesverwaltungsamt (BVA)", "Citizenship restoration (Art. 116, StAG §15)", "https://www.bva.bund.de/EN/Services/Citizens/Migration-Citizenship/Citizenship/Restoration-of-citizenship/restoration-of-citizenship_node.html", "official"),
            ResourceItem("make-it-de", "Make it in Germany", "Official visa portal — Blue Card, Freelancer, Chancenkarte, etc.", "https://www.make-it-in-germany.com/en/visa-residence/", "official"),
            ResourceItem("consulates-de", "German Consulates in USA", "Find your assigned consulate", "https://www.germany.info/us-en", "official"),
            ResourceItem("yadvashem", "Yad Vashem", "Persecution evidence research (StAG §15)", "https://www.yadvashem.org/", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "Anmeldung & Tax",
        icon = "badge",
        resources = listOf(
            ResourceItem("anmeldung", "Anmeldung Registration", "Required address registration", "https://service.berlin.de/dienstleistung/120335/", "official"),
            ResourceItem("elster", "ELSTER (Finanzamt)", "German tax portal", "https://www.elster.de", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("immoscout", "ImmoScout24", "Germany's largest property portal", "https://www.immobilienscout24.de", "marketplace"),
            ResourceItem("immowelt", "Immowelt", "Property and rentals", "https://www.immowelt.de", "marketplace"),
            ResourceItem("wg-gesucht", "WG-Gesucht", "Shared apartments and rentals", "https://www.wg-gesucht.de", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("n26-de", "N26", "Berlin-based digital bank", "https://n26.com/en-de", "service"),
            ResourceItem("dkb", "DKB", "Online bank, free account", "https://www.dkb.de", "service"),
            ResourceItem("deutsche-bank", "Deutsche Bank", "Major retail bank", "https://www.deutsche-bank.de", "service"),
            ResourceItem("wise-de", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("tk", "TK (Techniker Krankenkasse)", "Largest public health insurer", "https://www.tk.de/en", "official"),
            ResourceItem("aok", "AOK", "Public health insurance", "https://www.aok.de", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("toytown", "Toytown Germany", "English-speaking expat forum", "https://www.toytowngermany.com", "community"),
            ResourceItem("internations-de", "InterNations Germany", "Expat network", "https://www.internations.org/germany-expats", "community")
        )
    )
)

private fun getPolandResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("gov-pl", "Polish Government Citizenship Portal", "Confirmation of Polish citizenship", "https://www.gov.pl/web/usa-en/citizenship", "official"),
            ResourceItem("udsc", "Office for Foreigners (UDSC)", "Residence permits + work permits portal", "https://www.gov.pl/web/udsc", "official"),
            ResourceItem("voivode", "Mazowiecki Voivode", "Diaspora applicant office", "https://www.gov.pl/web/uw-mazowiecki", "official"),
            ResourceItem("consulates-pl", "Polish Consulates in USA", "Find your assigned consulate", "https://www.gov.pl/web/usa-en", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "PESEL & Tax",
        icon = "badge",
        resources = listOf(
            ResourceItem("pesel", "PESEL Number Application", "Polish personal ID number", "https://www.gov.pl/web/gov/uzyskaj-numer-pesel-dla-cudzoziemcow", "official"),
            ResourceItem("us-pl", "Urząd Skarbowy (Tax Office)", "Tax registration in Poland", "https://www.podatki.gov.pl", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("otodom", "Otodom", "Poland's largest property portal", "https://www.otodom.pl", "marketplace"),
            ResourceItem("olx-pl", "OLX Nieruchomości", "Classifieds with rentals", "https://www.olx.pl/nieruchomosci/", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("mbank", "mBank", "Online bank, English app", "https://www.mbank.pl", "service"),
            ResourceItem("pko", "PKO Bank Polski", "Largest Polish bank", "https://www.pkobp.pl", "service"),
            ResourceItem("revolut-pl", "Revolut", "Digital banking", "https://www.revolut.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("nfz", "NFZ (National Health Fund)", "Public healthcare", "https://www.nfz.gov.pl", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations-pl", "InterNations Poland", "Expat network", "https://www.internations.org/poland-expats", "community"),
            ResourceItem("reddit-pl", "r/poland", "Reddit community", "https://www.reddit.com/r/poland/", "community")
        )
    )
)

private fun getArgentinaResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("cancilleria", "Argentine MFA Citizenship", "Citizenship by option (Ley 346) portal", "https://www.cancilleria.gob.ar/en/services/argentinians-abroad/argentine-citizenship", "official"),
            ResourceItem("ar-migraciones", "Argentine Migrations Portal", "Residence permits — Rentista, Pensionado, Investor", "https://www.argentina.gob.ar/migraciones", "official"),
            ResourceItem("consulates-ar", "Argentine Consulates in USA", "Find your assigned consulate", "https://www.cancilleria.gob.ar/en/foreign-policy/embassies-and-consulates", "official"),
            ResourceItem("renaper", "RENAPER", "National Persons Registry (DNI)", "https://www.argentina.gob.ar/interior/renaper", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "DNI & Tax",
        icon = "badge",
        resources = listOf(
            ResourceItem("dni", "DNI Application", "Argentine national ID card", "https://www.argentina.gob.ar/interior/renaper/dni", "official"),
            ResourceItem("afip", "AFIP", "Tax authority (CUIT/CUIL)", "https://www.afip.gob.ar/", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("zonaprop", "ZonaProp", "Argentina's largest property portal", "https://www.zonaprop.com.ar", "marketplace"),
            ResourceItem("argenprop", "ArgenProp", "Property and rentals", "https://www.argenprop.com", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("santander-ar", "Santander Argentina", "International bank", "https://www.santander.com.ar", "service"),
            ResourceItem("galicia", "Banco Galicia", "Major private bank", "https://www.bancogalicia.com", "service"),
            ResourceItem("wise-ar", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("obras-sociales", "Public Healthcare Argentina", "Universal coverage overview", "https://www.argentina.gob.ar/salud", "official"),
            ResourceItem("osde", "OSDE", "Largest private health plan", "https://www.osde.com.ar", "service")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations-ar", "InterNations Argentina", "Expat network", "https://www.internations.org/argentina-expats", "community"),
            ResourceItem("reddit-ba", "r/buenosaires", "Reddit community", "https://www.reddit.com/r/buenosaires/", "community")
        )
    )
)

private fun getHungaryResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "citizenship",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("allampolgarsag", "Hungarian Citizenship Portal", "Simplified naturalization application", "https://allampolgarsag.gov.hu/", "official"),
            ResourceItem("oif-hu", "Office for Immigration (OIF)", "D-Visa, White Card, residence permits", "https://oif.gov.hu/", "official"),
            ResourceItem("nemzetikartya", "Nemzeti Kártya", "Guest Investor + White Card portal", "https://nemzetikartya.gov.hu/", "official"),
            ResourceItem("kormanyablak", "Kormányablak (Govt Office)", "One-stop government office", "https://kormanyablak.hu/en", "official"),
            ResourceItem("embassy-hu", "Hungarian Embassy USA", "Washington DC consulate", "https://washington.mfa.gov.hu/eng", "official")
        )
    ),
    ResourceCategory(
        id = "documents",
        title = "Documents & Translation",
        icon = "description",
        resources = listOf(
            ResourceItem("offi", "OFFI (Translation Office)", "Official Hungarian translations", "https://www.offi.hu/", "service"),
            ResourceItem("balassi", "Hungarian Cultural Institute", "Language courses & resources", "https://newyork.balassiintezet.hu/en/", "official"),
            ResourceItem("familysearch-hu", "FamilySearch — Hungary", "Ancestor records research", "https://www.familysearch.org/en/search/collection/list?page=1&place=Hungary", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "ID Cards & Tax",
        icon = "badge",
        resources = listOf(
            ResourceItem("nyilvantarto", "Nyilvántartó", "ID card and address registry", "https://nyilvantarto.hu/", "official"),
            ResourceItem("nav-hu", "NAV (Tax Authority)", "Hungarian taxation", "https://www.nav.gov.hu/", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("ingatlan", "Ingatlan.com", "Hungary's largest property portal", "https://ingatlan.com", "marketplace"),
            ResourceItem("otthonterkep", "Otthontérkép", "Property listings", "https://www.otthonterkep.hu", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("otp", "OTP Bank", "Largest Hungarian bank", "https://www.otpbank.hu", "service"),
            ResourceItem("revolut-hu", "Revolut", "Digital banking, popular in Hungary", "https://www.revolut.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("neak", "NEAK (Health Insurance Fund)", "Public healthcare (TAJ card)", "https://neak.gov.hu/", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations-hu", "InterNations Hungary", "Expat network", "https://www.internations.org/hungary-expats", "community"),
            ResourceItem("xpatloop", "XpatLoop", "English-language Hungary news/community", "https://xpatloop.com", "community")
        )
    )
)

private fun getUkAncestryResources(): List<ResourceCategory> = listOf(
    ResourceCategory(
        id = "visa",
        title = "Government & Immigration Portals",
        icon = "passport",
        resources = listOf(
            ResourceItem("ukvi", "UK Visas and Immigration (UKVI)", "Official UKVI portal — all visa categories", "https://www.gov.uk/government/organisations/uk-visas-and-immigration", "official"),
            ResourceItem("ilr", "ILR (Settlement) Guidance", "Indefinite Leave to Remain after 5yr", "https://www.gov.uk/ancestry-visa/settle-in-the-uk", "official"),
            ResourceItem("ihs-info", "Immigration Health Surcharge", "Paid as part of visa application", "https://www.gov.uk/healthcare-immigration-application", "official")
        )
    ),
    ResourceCategory(
        id = "documents",
        title = "Birth Certificates & Records",
        icon = "description",
        resources = listOf(
            ResourceItem("gro", "GRO (England & Wales)", "Order birth/marriage certificates", "https://www.gro.gov.uk/gro/content/certificates/", "official"),
            ResourceItem("scotlandspeople", "ScotlandsPeople", "Scottish family records", "https://www.scotlandspeople.gov.uk/", "official"),
            ResourceItem("groni", "GRONI (Northern Ireland)", "Order NI certificates", "https://www.nidirect.gov.uk/articles/general-register-office", "official"),
            ResourceItem("tb-test", "TB Test Country List", "Required pre-visa health check", "https://www.gov.uk/tb-test-visa", "official")
        )
    ),
    ResourceCategory(
        id = "id",
        title = "NIN & Tax",
        icon = "badge",
        resources = listOf(
            ResourceItem("nin", "National Insurance Number", "Apply post-arrival", "https://www.gov.uk/apply-national-insurance-number", "official"),
            ResourceItem("hmrc", "HMRC", "UK tax authority", "https://www.gov.uk/government/organisations/hm-revenue-customs", "official")
        )
    ),
    ResourceCategory(
        id = "housing",
        title = "Housing & Rentals",
        icon = "home",
        resources = listOf(
            ResourceItem("rightmove", "Rightmove", "UK's largest property portal", "https://www.rightmove.co.uk", "marketplace"),
            ResourceItem("zoopla", "Zoopla", "Property and rentals", "https://www.zoopla.co.uk", "marketplace"),
            ResourceItem("spareroom", "SpareRoom", "Flatshare and rooms", "https://www.spareroom.co.uk", "marketplace")
        )
    ),
    ResourceCategory(
        id = "banking",
        title = "Banking & Finance",
        icon = "account_balance",
        resources = listOf(
            ResourceItem("monzo", "Monzo", "Popular UK digital bank", "https://monzo.com", "service"),
            ResourceItem("starling", "Starling Bank", "Digital bank, easy setup", "https://www.starlingbank.com", "service"),
            ResourceItem("hsbc-uk", "HSBC UK", "Major retail bank", "https://www.hsbc.co.uk", "service"),
            ResourceItem("wise-uk", "Wise", "Multi-currency transfers", "https://wise.com", "service")
        )
    ),
    ResourceCategory(
        id = "healthcare",
        title = "Healthcare",
        icon = "local_hospital",
        resources = listOf(
            ResourceItem("nhs", "NHS", "National Health Service", "https://www.nhs.uk", "official"),
            ResourceItem("ihs", "Immigration Health Surcharge", "Paid as part of visa fee", "https://www.gov.uk/healthcare-immigration-application", "official")
        )
    ),
    ResourceCategory(
        id = "community",
        title = "Expat Communities",
        icon = "groups",
        resources = listOf(
            ResourceItem("internations-uk", "InterNations UK", "Expat network", "https://www.internations.org/united-kingdom-expats", "community"),
            ResourceItem("reddit-uk", "r/AskUK", "Reddit community", "https://www.reddit.com/r/AskUK/", "community")
        )
    )
)

/**
 * Entry-point card for a Real Journey. Tapping it opens [RealJourneyScreen]
 * which renders either the full content (subscribers) or a paywall preview.
 */
@Composable
private fun RealJourneyCtaCard(
    journey: RealJourney,
    isProUnlocked: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isProUnlocked) Icons.Outlined.MenuBook else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Real Journey",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isProUnlocked) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(
                    text = journey.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = journey.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

