@file:OptIn(ExperimentalLayoutApi::class)

package com.example.gothere.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Map
import com.example.gothere.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.gothere.data.SpainDestinations
import com.example.gothere.data.PortugalDestinations
import com.example.gothere.data.MexicoDestinations
import com.example.gothere.data.TasksRepository
import com.example.gothere.decision.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionTreeScreen(
    navController: NavHostController,
    countryId: String = "spain"
) {
    val scroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    val shareContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for Cost Calculator bottom sheet
    var showCostCalculator by remember { mutableStateOf(false) }
    var selectedCityForCalculator by remember { mutableStateOf<String?>(null) }

    var household by remember { mutableStateOf(Household.FamilyKids) }
    var budget by remember { mutableStateOf(Budget.Medium) }
    var climate by remember { mutableStateOf(ClimatePref.WarmCoastal) }
    var wantsCoastal by remember { mutableStateOf(true) }
    var wantsBigCity by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(Language.Basic) }
    var carOwner by remember { mutableStateOf(false) }
    var airportImportant by remember { mutableStateOf(true) }
    var businessFocus by remember { mutableStateOf<BusinessFocus?>(null) }
    var nightlife by remember { mutableStateOf(Nightlife.Medium) }
    var expatPref by remember { mutableStateOf<Density?>(null) }
    var safetyPriority by remember { mutableStateOf(3) }
    var considerations by remember { mutableStateOf<Set<PersonalConsideration>>(emptySet()) }

    var results by remember { mutableStateOf<List<RankedDestination>>(emptyList()) }
    var profileId by remember { mutableStateOf<String?>(null) }

    // Get destinations based on country
    val destinations = remember(countryId) {
        when (countryId) {
            "portugal" -> PortugalDestinations.all
            "mexico" -> MexicoDestinations.all
            else -> SpainDestinations.all
        }
    }

    val countryName = when (countryId) {
        "portugal" -> "Portugal"
        "mexico" -> "Mexico"
        else -> "Spain"
    }

    // Language label changes by country
    val languageLabel = when (countryId) {
        "portugal" -> "Portuguese Comfort"
        "mexico" -> "Spanish Comfort"
        else -> "Spanish Comfort"
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Header with Cost Calculator button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Decision Tree: Best Places in $countryName",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Cost Calculator FAB
                FilledTonalIconButton(
                    onClick = {
                        selectedCityForCalculator = null
                        showCostCalculator = true
                    }
                ) {
                    Icon(
                        Icons.Outlined.Calculate,
                        contentDescription = "Cost Calculator"
                    )
                }
            }

            LabeledChoiceRow("Household", Household.entries.toList(), household) { household = it }

            // Personal Considerations (multi-select)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Personal Considerations",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    "Select any that apply — affects safety, healthcare & rights scoring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PersonalConsideration.entries.forEach { opt ->
                        val selected = opt in considerations
                        FilterChip(
                            selected = selected,
                            onClick = {
                                considerations = if (selected) considerations - opt else considerations + opt
                            },
                            label = {
                                Text(
                                    when (opt) {
                                        PersonalConsideration.LGBTQ -> "LGBTQ+"
                                        PersonalConsideration.Disabled -> "Disabled / Accessibility"
                                        PersonalConsideration.Veteran -> "Veteran"
                                        PersonalConsideration.Pregnant -> "Pregnant / Expecting"
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            LabeledChoiceRow("Budget", Budget.entries.toList(), budget) { budget = it }
            LabeledChoiceRow("Preferred Climate", ClimatePref.entries.toList(), climate) { climate = it }

            LabeledSwitch("Prefer Coastal Living", wantsCoastal) { wantsCoastal = it }
            LabeledSwitch("Prefer Big City", wantsBigCity) { wantsBigCity = it }

            LabeledChoiceRow(languageLabel, Language.entries.toList(), language) { language = it }

            LabeledChoiceRowNullable(
                "Expat Density Preference (optional)",
                Density.entries.toList(),
                expatPref
            ) { expatPref = it }

            LabeledSwitch("Own a Car", carOwner) { carOwner = it }
            LabeledSwitch("Airport Proximity Important", airportImportant) { airportImportant = it }

            LabeledChoiceRowNullable(
                "Business Focus (optional)",
                BusinessFocus.entries.toList(),
                businessFocus
            ) { businessFocus = it }

            LabeledChoiceRow("Nightlife Preference", Nightlife.entries.toList(), nightlife) { nightlife = it }

            SliderWithLabel("Safety Priority", safetyPriority) { safetyPriority = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val profile = UserProfile(
                            household, budget, climate, wantsCoastal, wantsBigCity,
                            language, carOwner, airportImportant, businessFocus, nightlife, expatPref, safetyPriority,
                            considerations
                        )
                        val ranked = DecisionEngine.rank(destinations, profile, countryId).take(5)
                        results = ranked
                        scope.launch {
                            try {
                                val id = ProfileRepository.saveProfile(profile)
                                profileId = id
                                RecommendationRepository.saveRecommendations(id, ranked)
                                snackbarHostState.showSnackbar("Profile and recommendations saved")
                            } catch (e: Exception) {
                                // Firestore save failed (e.g. permissions) — results still show
                                snackbarHostState.showSnackbar("Results ready (save failed: ${e.message?.take(50)})")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Evaluate & Save") }

                if (results.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                appendLine("GoThere - Top $countryName Recommendations:")
                                appendLine()
                                results.forEachIndexed { i, r ->
                                    appendLine("${i + 1}. ${r.destination.name} — ${r.destination.region} (score ${r.score})")
                                }
                                appendLine()
                                appendLine("Find your perfect relocation destination at https://getgothere.app")
                            }
                            clipboard.setText(AnnotatedString(text))
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "GoThere - Top $countryName Picks")
                            }
                            shareContext.startActivity(Intent.createChooser(shareIntent, "Share recommendations"))
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Share Top 5") }
                }
            }

            if (results.isNotEmpty()) {
                Text(
                    "Top Recommendations",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 16.dp)
                )

                results.forEachIndexed { idx, ranked ->
                    RecommendationCard(
                        ranked = ranked,
                        index = idx + 1,
                        countryId = countryId,
                        onViewMap = { cityKey ->
                            navController.navigate(Route.CityMap.create(countryId, cityKey))
                        },
                        onCalculateCost = { cityId ->
                            selectedCityForCalculator = cityId
                            showCostCalculator = true
                        }
                    )
                }

                val top = results.first()
                Button(
                    onClick = {
                        scope.launch {
                            val tasks = TasksRepository.starterTasksForCity(
                                top.destination.id,
                                top.destination.name,
                                countryId
                            )
                            TasksRepository.insertTasks(tasks)
                            snackbarHostState.showSnackbar(
                                "Starter tasks created for ${top.destination.name}"
                            )
                            navController.navigate("tasks") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Create Starter Tasks for Top Pick")
                }
            }

            // Add extra space at the bottom for better scrolling feel
            Spacer(Modifier.height(40.dp))
        }
    }

    // Cost Calculator Bottom Sheet
    if (showCostCalculator) {
        CostCalculatorBottomSheet(
            countryId = countryId,
            cityKey = selectedCityForCalculator,
            onDismiss = { showCostCalculator = false }
        )
    }
}

// ----- UI helpers -----

@Composable
private fun <T : Enum<T>> LabeledChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = {
                        Text(
                            opt.toString(),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
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
private fun <T : Enum<T>> LabeledChoiceRowNullable(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("No preference", modifier = Modifier.padding(vertical = 4.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(opt.toString(), modifier = Modifier.padding(vertical = 4.dp)) },
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
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$label: $value",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun RecommendationCard(
    ranked: RankedDestination,
    index: Int,
    countryId: String,
    onCalculateCost: (String) -> Unit,
    onViewMap: (String) -> Unit = {}
) {
    val advice = MicroAdvice.tipsFor(ranked.destination.id, countryId)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$index) ${ranked.destination.name} — ${ranked.destination.region}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // View Map button
                FilledTonalIconButton(
                    onClick = {
                        val cityKey = ranked.destination.id.lowercase().replace(" ", "_")
                        onViewMap(cityKey)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Map,
                        contentDescription = "View map for ${ranked.destination.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cost Calculator button for this city
                FilledTonalIconButton(
                    onClick = {
                        val cityKey = ranked.destination.id.lowercase().replace(" ", "_")
                        onCalculateCost(cityKey)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Calculate,
                        contentDescription = "Calculate costs for ${ranked.destination.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(ranked.destination.type) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("Cost ${ranked.destination.costLevel}/3") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Safety ${ranked.destination.safety}/5") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("Expat ${ranked.destination.expatDensity}/5") }
                )
            }

            Text(
                "Why it fits:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            ranked.reasons.take(4).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "Neighborhood tips:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            advice.take(3).forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
