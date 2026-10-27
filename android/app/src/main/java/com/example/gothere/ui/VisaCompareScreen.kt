@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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

/** Ordering for the visa list. LOWEST_INCOME is the lower-class "what can I afford"
 *  win — floats the cheapest income-bar tracks to the top. No-income-test visas
 *  (ancestry/work/points-based) sink to the bottom of that sort. Mirrors iOS. */
enum class SortMode(val label: String) {
    DEFAULT("Default"),
    LOWEST_INCOME("Lowest income")
}

@Composable
fun VisaCompareScreen(
    initialCountryId: String? = null,
    onDismiss: () -> Unit,
    onStartWizard: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.VISA_COMPARE_VIEWED, mapOf("country_id" to (initialCountryId ?: "all")))
    }
    var countryFilter by remember { mutableStateOf<String?>(initialCountryId) }
    var categoryFilter by remember { mutableStateOf<VisaCategory?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showComparison by remember { mutableStateOf(false) }
    var maxMonthlyIncomeUSD by remember { mutableFloatStateOf(INCOME_SLIDER_MAX) }
    var sortMode by remember { mutableStateOf(SortMode.DEFAULT) }
    var noDegreeOnly by remember { mutableStateOf(false) }
    var independentIncomeOnly by remember { mutableStateOf(false) }

    val filtered = remember(countryFilter, categoryFilter, maxMonthlyIncomeUSD, sortMode, noDegreeOnly, independentIncomeOnly) {
        val list = VisaCatalog.all.filter { v ->
            (countryFilter == null || v.countryId == countryFilter) &&
            (categoryFilter == null || v.category == categoryFilter) &&
            passesIncomeFilter(v, maxMonthlyIncomeUSD) &&
            (!noDegreeOnly || v.requiresNoDegree) &&
            (!independentIncomeOnly || v.acceptsIndependentIncome)
        }
        when (sortMode) {
            SortMode.DEFAULT -> list
            // Cheapest income bar first; no-income-test visas (null) sort last.
            SortMode.LOWEST_INCOME -> list.sortedWith(
                compareBy({ it.monthlyIncomeEUR ?: Int.MAX_VALUE }, { it.countryName })
            )
        }
    }

    val selectedVisas = remember(selectedIds) {
        selectedIds.mapNotNull { VisaCatalog.byId(it) }.sortedBy { it.countryName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (showComparison) R.string.cmp_side_by_side else R.string.cmp_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showComparison) showComparison = false else onDismiss()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                            stringResource(R.string.cmp_compare_n, selectedVisas.size),
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
                    stringResource(R.string.wiz_compare),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.cmp_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilterRow(
                    label = stringResource(R.string.cmp_country),
                    items = listOf(null to stringResource(R.string.cmp_all_countries)) + countryOptions(),
                    selected = countryFilter,
                    onSelect = { countryFilter = it }
                )

                FilterRow(
                    label = stringResource(R.string.cmp_category),
                    items = listOf<Pair<VisaCategory?, String>>(null to stringResource(R.string.cmp_all_categories)) +
                        VisaCategory.entries.map { it to it.displayName },
                    selected = categoryFilter,
                    onSelect = { categoryFilter = it }
                )

                IncomeFilter(
                    value = maxMonthlyIncomeUSD,
                    onChange = { maxMonthlyIncomeUSD = it }
                )

                // Lower-income unlock filters: no-degree + independent-income paths.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = noDegreeOnly,
                        onClick = { noDegreeOnly = !noDegreeOnly },
                        label = { Text(stringResource(R.string.cmp_no_degree), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = independentIncomeOnly,
                        onClick = { independentIncomeOnly = !independentIncomeOnly },
                        label = { Text(stringResource(R.string.cmp_freelance), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                SortControl(selected = sortMode, onSelect = { sortMode = it })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pluralRes(R.plurals.cmp_visa_types, filtered.size, filtered.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text(stringResource(R.string.cmp_clear_n, selectedIds.size))
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
                        contentDescription = stringResource(R.string.cmp_selected),
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
        stringResource(R.string.cmp_row_income) to { v: VisaInfo -> v.income },
        stringResource(R.string.cmp_row_processing) to { v: VisaInfo -> v.processingTime },
        stringResource(R.string.cmp_row_duration) to { v: VisaInfo -> v.duration },
        stringResource(R.string.cmp_row_work) to { v: VisaInfo -> v.workAllowed },
        stringResource(R.string.cmp_row_pr) to { v: VisaInfo -> v.pathToPR },
        stringResource(R.string.cmp_row_citizenship) to { v: VisaInfo -> v.pathToCitizenship },
        stringResource(R.string.cmp_row_cost) to { v: VisaInfo -> v.costEstimate }
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
                        stringResource(R.string.cmp_feature),
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

        // Tax-optimization overlay — surfaces preferential regimes for visas carrying
        // them (mirror of iOS). Ends in an advisor handoff.
        val taxableVisas = visas.filter { com.example.gothere.data.TaxRegimes.forVisa(it.id).isNotEmpty() }
        if (taxableVisas.isNotEmpty()) {
            taxableVisas.forEach { v -> TaxRegimeCard(v) }
            Text(
                stringResource(R.string.cmp_tax_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            contentDescription = stringResource(R.string.cmp_pros),
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.cmp_pros),
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
                            contentDescription = stringResource(R.string.cmp_cons),
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.cmp_cons),
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
private fun TaxRegimeCard(v: VisaInfo) {
    val regimes = com.example.gothere.data.TaxRegimes.forVisa(v.id)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.cmp_tax_regimes, v.countryFlag, v.shortName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            regimes.forEach { r ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        r.flatRatePercent?.let { rate ->
                            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
                                Text(stringResource(R.string.cmp_flat, (rate * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                    }
                    r.eligibilityCriteria.forEach { c ->
                        Text("• $c", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    r.applicationWindow?.let { w ->
                        Text("⏱ $w", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
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
            Text(stringResource(R.string.cmp_start_wizard, v.shortName), fontWeight = FontWeight.SemiBold)
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
            Text(stringResource(R.string.cmp_open_official, v.shortName))
        }
    }
}

// Income-tier filter — surfaces low/no-income visas (ancestry, work, points-based)
// alongside passive-income visas under the slider value. Mirrors iOS implementation.
private const val EUR_TO_USD = 1.08
private const val INCOME_SLIDER_MIN = 500f
private const val INCOME_SLIDER_MAX = 10000f

private fun passesIncomeFilter(v: VisaInfo, maxUSD: Float): Boolean {
    if (maxUSD >= INCOME_SLIDER_MAX) return true
    val eur = v.monthlyIncomeEUR ?: return true
    return eur * EUR_TO_USD <= maxUSD
}

@Composable
private fun SortControl(selected: SortMode, onSelect: (SortMode) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.cmp_sort),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SortMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(if (mode == SortMode.LOWEST_INCOME) R.string.cmp_sort_lowest_income else R.string.cmp_sort_default), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun IncomeFilter(value: Float, onChange: (Float) -> Unit) {
    val active = value < INCOME_SLIDER_MAX
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.cmp_max_income),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (active) stringResource(R.string.cmp_income_le, value.toInt()) else stringResource(R.string.cmp_any),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = INCOME_SLIDER_MIN..INCOME_SLIDER_MAX,
                steps = ((INCOME_SLIDER_MAX - INCOME_SLIDER_MIN) / 100).toInt() - 1
            )
            Text(
                stringResource(R.string.cmp_income_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

