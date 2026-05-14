@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.data.VisaCatalog
import com.example.gothere.data.VisaCategory
import com.example.gothere.data.VisaInfo

private const val MAX_SELECTION = 3

@Composable
fun VisaCompareScreen(
    initialCountryId: String? = null,
    onDismiss: () -> Unit,
    onStartWizard: (String) -> Unit
) {
    var countryFilter by remember { mutableStateOf<String?>(initialCountryId) }
    var categoryFilter by remember { mutableStateOf<VisaCategory?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showComparison by remember { mutableStateOf(false) }

    val filtered = remember(countryFilter, categoryFilter) {
        VisaCatalog.all.filter { v ->
            (countryFilter == null || v.countryId == countryFilter) &&
            (categoryFilter == null || v.category == categoryFilter)
        }
    }

    val selectedVisas = remember(selectedIds) {
        selectedIds.mapNotNull { VisaCatalog.byId(it) }.sortedBy { it.countryName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showComparison) "Side-by-Side" else "Visa Comparison") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showComparison) showComparison = false else onDismiss()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (!showComparison && selectedVisas.size >= 2) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { showComparison = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Compare ${selectedVisas.size} Visas",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (showComparison) {
            ComparisonTable(
                visas = selectedVisas,
                onStartWizard = { trackId ->
                    onStartWizard(trackId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Compare Visa Types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Select 2-3 visas to see eligibility, cost, and citizenship paths side-by-side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilterRow(
                    label = "Country",
                    items = listOf(null to "All") + countryOptions(),
                    selected = countryFilter,
                    onSelect = { countryFilter = it }
                )

                FilterRow(
                    label = "Category",
                    items = listOf<Pair<VisaCategory?, String>>(null to "All") +
                        VisaCategory.entries.map { it to it.displayName },
                    selected = categoryFilter,
                    onSelect = { categoryFilter = it }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${filtered.size} visa types",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text("Clear (${selectedIds.size})")
                        }
                    }
                }

                filtered.forEach { v ->
                    VisaRow(
                        visa = v,
                        isSelected = v.id in selectedIds,
                        atLimit = selectedIds.size >= MAX_SELECTION && v.id !in selectedIds,
                        onToggle = {
                            selectedIds = if (v.id in selectedIds) selectedIds - v.id
                                else if (selectedIds.size < MAX_SELECTION) selectedIds + v.id
                                else selectedIds
                        }
                    )
                }
            }
        }
    }
}

private fun countryOptions(): List<Pair<String?, String>> = listOf(
    "spain" to "🇪🇸 Spain",
    "portugal" to "🇵🇹 Portugal",
    "mexico" to "🇲🇽 Mexico",
    "canada" to "🇨🇦 Canada",
    "ireland" to "🇮🇪 Ireland",
    "italy" to "🇮🇹 Italy",
    "germany" to "🇩🇪 Germany",
    "poland" to "🇵🇱 Poland",
    "argentina" to "🇦🇷 Argentina",
    "hungary" to "🇭🇺 Hungary",
    "uk_ancestry" to "🇬🇧 UK (Ancestry)"
)

@Composable
private fun <T> FilterRow(
    label: String,
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (value, display) ->
                val isSelected = value == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) (items.first().first) else value) },
                    label = { Text(display, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun VisaRow(
    visa: VisaInfo,
    isSelected: Boolean,
    atLimit: Boolean,
    onToggle: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = !atLimit, onClick = onToggle)
            .padding(12.dp)
            .alpha(if (atLimit) 0.4f else 1f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(visa.countryFlag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        visa.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${visa.shortName} · ${visa.category.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    visa.income,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonTable(
    visas: List<VisaInfo>,
    onStartWizard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        "Income / Funds" to { v: VisaInfo -> v.income },
        "Processing time" to { v: VisaInfo -> v.processingTime },
        "Duration" to { v: VisaInfo -> v.duration },
        "Work allowed?" to { v: VisaInfo -> v.workAllowed },
        "Path to PR" to { v: VisaInfo -> v.pathToPR },
        "Path to citizenship" to { v: VisaInfo -> v.pathToCitizenship },
        "Cost estimate" to { v: VisaInfo -> v.costEstimate }
    )

    val labelColumnWidth = 130.dp
    val dataColumnWidth = 200.dp

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontal-scroll table
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp)
                )
        ) {
            // Header row
            Row(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                TableCell(width = labelColumnWidth) {
                    Text(
                        "Feature",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                visas.forEach { v ->
                    TableCell(width = dataColumnWidth) {
                        Column {
                            Text(
                                "${v.countryFlag} ${v.shortName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                v.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            rows.forEachIndexed { idx, (label, accessor) ->
                Row(
                    modifier = Modifier.background(
                        if (idx % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    TableCell(width = labelColumnWidth) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    visas.forEach { v ->
                        TableCell(width = dataColumnWidth) {
                            Text(
                                accessor(v),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Pros/Cons per visa
        visas.forEach { v ->
            ProsConsCard(v)
        }

        // Action buttons
        visas.forEach { v ->
            ActionButton(v, onStartWizard)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TableCell(width: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .padding(2.dp)
    ) {
        content()
    }
}

@Composable
private fun ProsConsCard(v: VisaInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${v.countryFlag} ${v.shortName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Pros",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Pros",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    v.pros.forEach { p ->
                        Text(
                            "• $p",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Cons",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Cons",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    v.cons.forEach { c ->
                        Text(
                            "• $c",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(v: VisaInfo, onStartWizard: (String) -> Unit) {
    val context = LocalContext.current
    val trackId = v.wizardTrackId
    if (trackId != null) {
        Button(
            onClick = { onStartWizard(trackId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Start Wizard — ${v.shortName}", fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(v.officialUrl)
                )
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open Official — ${v.shortName}")
        }
    }
}

