@file:OptIn(ExperimentalLayoutApi::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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

    val countryName = stringResource(
        when (countryId) {
            "portugal" -> R.string.country_portugal
            "mexico" -> R.string.country_mexico
            else -> R.string.country_spain
        }
    )

    // Language label changes by country
    val languageLabel = when (countryId) {
        "portugal" -> stringResource(R.string.decide_portuguese_comfort)
        else -> stringResource(R.string.decide_spanish_comfort)
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
                    stringResource(R.string.decide_best_places, countryName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        contentDescription = stringResource(R.string.cost_calculator)
                    )
                }
            }

            LabeledChoiceRow(stringResource(R.string.decide_household), Household.entries.toList(), household) { household = it }

            // Personal Considerations (multi-select)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.decide_personal),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    stringResource(R.string.decide_personal_hint),
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
                                        PersonalConsideration.LGBTQ -> stringResource(R.string.consideration_lgbtq)
                                        PersonalConsideration.Trans -> stringResource(R.string.consideration_trans)
                                        PersonalConsideration.Disabled -> stringResource(R.string.consideration_disabled)
                                        PersonalConsideration.Veteran -> stringResource(R.string.consideration_veteran)
                                        PersonalConsideration.Pregnant -> stringResource(R.string.consideration_pregnant)
                                        PersonalConsideration.Neurodivergent -> stringResource(R.string.consideration_neurodivergent)
                                        PersonalConsideration.Senior -> stringResource(R.string.consideration_senior)
                                        PersonalConsideration.Poc -> stringResource(R.string.consideration_poc)
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

            LabeledChoiceRow(stringResource(R.string.decide_budget), Budget.entries.toList(), budget) { budget = it }
            LabeledChoiceRow(stringResource(R.string.decide_climate), ClimatePref.entries.toList(), climate) { climate = it }

            LabeledSwitch(stringResource(R.string.decide_coastal), wantsCoastal) { wantsCoastal = it }
            LabeledSwitch(stringResource(R.string.decide_big_city), wantsBigCity) { wantsBigCity = it }

            LabeledChoiceRow(languageLabel, Language.entries.toList(), language) { language = it }

            LabeledChoiceRowNullable(
                stringResource(R.string.decide_expat_density),
                Density.entries.toList(),
                expatPref
            ) { expatPref = it }

            LabeledSwitch(stringResource(R.string.decide_car), carOwner) { carOwner = it }
            LabeledSwitch(stringResource(R.string.decide_airport), airportImportant) { airportImportant = it }

            LabeledChoiceRowNullable(
                stringResource(R.string.decide_business),
                BusinessFocus.entries.toList(),
                businessFocus
            ) { businessFocus = it }

            LabeledChoiceRow(stringResource(R.string.decide_nightlife), Nightlife.entries.toList(), nightlife) { nightlife = it }

            SliderWithLabel(stringResource(R.string.decide_safety), safetyPriority) { safetyPriority = it }

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
                        com.example.gothere.util.Analytics.log(com.example.gothere.util.Analytics.DECISION_TREE_COMPLETED, mapOf(
                            "country_id" to countryId,
                            "top_destination" to ranked.firstOrNull()?.destination?.id
                        ))
                        // Persist personas so ResourcesScreen can render For You section
                        // without re-running the wizard.
                        com.example.gothere.repository.UserConsiderationsStore.save(
                            shareContext, considerations, household
                        )
                        scope.launch {
                            try {
                                val id = ProfileRepository.saveProfile(profile)
                                profileId = id
                                RecommendationRepository.saveRecommendations(id, ranked)
                                snackbarHostState.showSnackbar(shareContext.getString(R.string.decide_saved))
                            } catch (e: Exception) {
                                // Firestore save failed (e.g. permissions) — results still show
                                snackbarHostState.showSnackbar(shareContext.getString(R.string.decide_save_failed, e.message?.take(50) ?: ""))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text(stringResource(R.string.decide_evaluate)) }

                if (results.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                appendLine(shareContext.getString(R.string.decide_share_header, countryName))
                                appendLine()
                                results.forEachIndexed { i, r ->
                                    appendLine(shareContext.getString(R.string.decide_share_line, i + 1, r.destination.name, r.destination.region, r.score))
                                }
                                appendLine()
                                appendLine(shareContext.getString(R.string.decide_share_footer))
                            }
                            clipboard.setText(AnnotatedString(text))
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, shareContext.getString(R.string.decide_share_subject, countryName))
                            }
                            shareContext.startActivity(Intent.createChooser(shareIntent, shareContext.getString(R.string.decide_share_chooser)))
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text(stringResource(R.string.decide_share_top5)) }
                }
            }

            if (results.isNotEmpty()) {
                Text(
                    stringResource(R.string.decide_top_recs),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Inclusion notes for active personas + single-parent households.
                // Renders only when at least one matches.
                InclusionNotesCard(
                    countryId = countryId,
                    considerations = considerations,
                    isSingleParent = household == Household.SingleParent
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
                                shareContext.getString(R.string.decide_starter_created, top.destination.name)
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
                    Text(stringResource(R.string.decide_create_starter))
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

/** Display label for the wizard enums. English keeps the enum name it always showed; the enum value itself is what gets saved. */
@Composable
private fun enumLabel(e: Enum<*>): String = when (e) {
    Household.Singles -> stringResource(R.string.enum_household_singles)
    Household.Couple -> stringResource(R.string.enum_household_couple)
    Household.FamilyKids -> stringResource(R.string.enum_household_familykids)
    Household.SingleParent -> stringResource(R.string.enum_household_singleparent)
    Household.Retiree -> stringResource(R.string.enum_household_retiree)
    Budget.Low -> stringResource(R.string.enum_budget_low)
    Budget.Medium -> stringResource(R.string.enum_budget_medium)
    Budget.High -> stringResource(R.string.enum_budget_high)
    ClimatePref.WarmCoastal -> stringResource(R.string.enum_climatepref_warmcoastal)
    ClimatePref.Temperate -> stringResource(R.string.enum_climatepref_temperate)
    ClimatePref.Mountain -> stringResource(R.string.enum_climatepref_mountain)
    ClimatePref.Continental -> stringResource(R.string.enum_climatepref_continental)
    Language.None -> stringResource(R.string.enum_language_none)
    Language.Basic -> stringResource(R.string.enum_language_basic)
    Language.Intermediate -> stringResource(R.string.enum_language_intermediate)
    BusinessFocus.Tech -> stringResource(R.string.enum_businessfocus_tech)
    BusinessFocus.Tourism -> stringResource(R.string.enum_businessfocus_tourism)
    BusinessFocus.Logistics -> stringResource(R.string.enum_businessfocus_logistics)
    BusinessFocus.Agriculture -> stringResource(R.string.enum_businessfocus_agriculture)
    BusinessFocus.RemoteWork -> stringResource(R.string.enum_businessfocus_remotework)
    BusinessFocus.Finance -> stringResource(R.string.enum_businessfocus_finance)
    Nightlife.Low -> stringResource(R.string.enum_nightlife_low)
    Nightlife.Medium -> stringResource(R.string.enum_nightlife_medium)
    Nightlife.High -> stringResource(R.string.enum_nightlife_high)
    Density.Low -> stringResource(R.string.enum_density_low)
    Density.Medium -> stringResource(R.string.enum_density_medium)
    Density.High -> stringResource(R.string.enum_density_high)
    else -> e.name
}

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
                            enumLabel(opt),
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
                label = { Text(stringResource(R.string.no_preference), modifier = Modifier.padding(vertical = 4.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(enumLabel(opt), modifier = Modifier.padding(vertical = 4.dp)) },
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
                        contentDescription = stringResource(R.string.decide_view_map, ranked.destination.name),
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
                        contentDescription = stringResource(R.string.decide_calc_costs, ranked.destination.name),
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
                    label = { Text(stringResource(R.string.decide_chip_cost, ranked.destination.costLevel)) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.decide_chip_safety, ranked.destination.safety)) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.decide_chip_expat, ranked.destination.expatDensity)) }
                )
            }

            Text(
                stringResource(R.string.decide_why_fits),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            ranked.reasons.take(4).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                stringResource(R.string.decide_neighborhood_tips),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            advice.take(3).forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
