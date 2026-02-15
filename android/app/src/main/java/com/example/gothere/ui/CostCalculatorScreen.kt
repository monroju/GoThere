package com.example.gothere.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

/**
 * Cost data for different cities
 */
data class CityCostData(
    val cityName: String,
    val countryId: String,
    val currency: String,
    val currencySymbol: String,
    val exchangeRateToUSD: Double, // 1 local currency = X USD
    val avgRentStudio: Int,        // Monthly rent for studio/1BR in city center
    val avgRentOneBR: Int,         // Monthly rent for 1BR outside center
    val avgRentTwoBR: Int,         // Monthly rent for 2BR
    val utilities: Int,            // Monthly utilities (electricity, water, gas, internet)
    val groceries: Int,            // Monthly groceries for 1 person
    val publicTransport: Int,      // Monthly transport pass
    val healthInsurance: Int,      // Monthly private health insurance
    val diningOut: Int,            // Average meal at mid-range restaurant
    val coffee: Double,            // Cappuccino price
    val gym: Int                   // Monthly gym membership
)

// Cost data for major cities (approximate 2024-2025 data)
val cityData = mapOf(
    // Spain
    "madrid" to CityCostData("Madrid", "spain", "EUR", "€", 1.08, 1200, 900, 1600, 150, 300, 55, 100, 15, 1.80, 40),
    "barcelona" to CityCostData("Barcelona", "spain", "EUR", "€", 1.08, 1300, 950, 1700, 140, 320, 45, 100, 16, 1.90, 45),
    "valencia" to CityCostData("Valencia", "spain", "EUR", "€", 1.08, 900, 700, 1200, 120, 280, 40, 90, 12, 1.60, 35),
    "sevilla" to CityCostData("Seville", "spain", "EUR", "€", 1.08, 800, 650, 1100, 110, 260, 38, 85, 11, 1.50, 32),
    "malaga" to CityCostData("Málaga", "spain", "EUR", "€", 1.08, 950, 750, 1300, 115, 270, 40, 90, 13, 1.70, 35),

    // Portugal
    "lisbon" to CityCostData("Lisbon", "portugal", "EUR", "€", 1.08, 1400, 1000, 1800, 130, 280, 40, 80, 12, 1.50, 35),
    "porto" to CityCostData("Porto", "portugal", "EUR", "€", 1.08, 1000, 750, 1300, 110, 250, 35, 75, 10, 1.30, 30),
    "faro" to CityCostData("Faro", "portugal", "EUR", "€", 1.08, 800, 600, 1000, 100, 240, 30, 70, 10, 1.20, 28),
    "coimbra" to CityCostData("Coimbra", "portugal", "EUR", "€", 1.08, 600, 450, 800, 90, 220, 25, 70, 8, 1.10, 25),
    "braga" to CityCostData("Braga", "portugal", "EUR", "€", 1.08, 650, 500, 850, 95, 230, 28, 70, 9, 1.15, 27),

    // Mexico
    "mexico_city" to CityCostData("Mexico City", "mexico", "MXN", "$", 0.058, 18000, 12000, 25000, 2500, 5000, 800, 3000, 250, 60.0, 800),
    "guadalajara" to CityCostData("Guadalajara", "mexico", "MXN", "$", 0.058, 14000, 10000, 20000, 2200, 4500, 600, 2500, 200, 50.0, 700),
    "monterrey" to CityCostData("Monterrey", "mexico", "MXN", "$", 0.058, 16000, 11000, 22000, 2800, 5500, 700, 2800, 220, 55.0, 750),
    "merida" to CityCostData("Mérida", "mexico", "MXN", "$", 0.058, 12000, 8000, 16000, 2000, 4000, 500, 2200, 180, 45.0, 600),
    "playa_del_carmen" to CityCostData("Playa del Carmen", "mexico", "MXN", "$", 0.058, 20000, 14000, 28000, 2600, 5500, 0, 3500, 280, 70.0, 900),
    "san_miguel" to CityCostData("San Miguel de Allende", "mexico", "MXN", "$", 0.058, 18000, 13000, 24000, 2400, 5000, 0, 3000, 250, 65.0, 850),
    "oaxaca" to CityCostData("Oaxaca City", "mexico", "MXN", "$", 0.058, 10000, 7000, 14000, 1800, 3500, 400, 2000, 150, 40.0, 500),
    "puerto_vallarta" to CityCostData("Puerto Vallarta", "mexico", "MXN", "$", 0.058, 16000, 11000, 22000, 2400, 5000, 0, 3200, 230, 60.0, 800)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCalculatorScreen(
    countryId: String = "spain",
    preselectedCity: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    // Filter cities for current country
    val availableCities = remember(countryId) {
        cityData.filter { it.value.countryId == countryId }
    }

    // Selected city
    var selectedCityKey by remember {
        mutableStateOf(preselectedCity ?: availableCities.keys.firstOrNull() ?: "")
    }
    val selectedCity = availableCities[selectedCityKey]

    // User inputs for lifestyle customization
    var rentType by remember { mutableStateOf(1) } // 0=studio, 1=1BR, 2=2BR
    var includeGym by remember { mutableStateOf(false) }
    var diningOutPerWeek by remember { mutableStateOf("4") }
    var includeHealthInsurance by remember { mutableStateOf(true) }
    var usePublicTransport by remember { mutableStateOf(true) }
    var showInUSD by remember { mutableStateOf(false) }

    // City selector dropdown state
    var cityDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cost Calculator",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Estimate your monthly expenses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }

        // City Selector
        item {
            ExposedDropdownMenuBox(
                expanded = cityDropdownExpanded,
                onExpandedChange = { cityDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCity?.cityName ?: "Select a city",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("City") },
                    leadingIcon = { Icon(Icons.Outlined.LocationCity, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = cityDropdownExpanded,
                    onDismissRequest = { cityDropdownExpanded = false }
                ) {
                    availableCities.forEach { (key, city) ->
                        DropdownMenuItem(
                            text = { Text(city.cityName) },
                            onClick = {
                                selectedCityKey = key
                                cityDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Currency Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show in USD",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = showInUSD,
                    onCheckedChange = { showInUSD = it }
                )
            }
        }

        if (selectedCity != null) {
            // Lifestyle Options Card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Customize Your Lifestyle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))

                        // Housing Type
                        Text(
                            text = "Housing Type",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = rentType == 0,
                                onClick = { rentType = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                label = { Text("Studio") }
                            )
                            SegmentedButton(
                                selected = rentType == 1,
                                onClick = { rentType = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                label = { Text("1 BR") }
                            )
                            SegmentedButton(
                                selected = rentType == 2,
                                onClick = { rentType = 2 },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                label = { Text("2 BR") }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Dining out frequency
                        OutlinedTextField(
                            value = diningOutPerWeek,
                            onValueChange = { diningOutPerWeek = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Meals out per week") },
                            leadingIcon = { Icon(Icons.Outlined.Restaurant, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))

                        // Toggle options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.DirectionsBus, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Public Transport")
                            }
                            Switch(checked = usePublicTransport, onCheckedChange = { usePublicTransport = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Private Health Insurance")
                            }
                            Switch(checked = includeHealthInsurance, onCheckedChange = { includeHealthInsurance = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gym Membership")
                            }
                            Switch(checked = includeGym, onCheckedChange = { includeGym = it })
                        }
                    }
                }
            }

            // Cost Breakdown Card
            item {
                val rent = when (rentType) {
                    0 -> selectedCity.avgRentStudio
                    1 -> selectedCity.avgRentOneBR
                    else -> selectedCity.avgRentTwoBR
                }
                val mealsOut = (diningOutPerWeek.toIntOrNull() ?: 0) * 4 * selectedCity.diningOut
                val transport = if (usePublicTransport) selectedCity.publicTransport else 0
                val health = if (includeHealthInsurance) selectedCity.healthInsurance else 0
                val gym = if (includeGym) selectedCity.gym else 0

                val total = rent + selectedCity.utilities + selectedCity.groceries + mealsOut + transport + health + gym

                CostBreakdownCard(
                    city = selectedCity,
                    rent = rent,
                    utilities = selectedCity.utilities,
                    groceries = selectedCity.groceries,
                    diningOut = mealsOut,
                    transport = transport,
                    healthInsurance = health,
                    gym = gym,
                    total = total,
                    showInUSD = showInUSD
                )
            }

            // Comparison Tips
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Tips for ${selectedCity.cityName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getCityTips(selectedCityKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Disclaimer
            item {
                Text(
                    text = "* Estimates based on 2024-2025 data. Actual costs may vary based on lifestyle, location within the city, and market conditions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom spacing
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun CostBreakdownCard(
    city: CityCostData,
    rent: Int,
    utilities: Int,
    groceries: Int,
    diningOut: Int,
    transport: Int,
    healthInsurance: Int,
    gym: Int,
    total: Int,
    showInUSD: Boolean
) {
    val currencyFormatter = remember(showInUSD, city) {
        if (showInUSD) {
            NumberFormat.getCurrencyInstance(Locale.US)
        } else {
            NumberFormat.getCurrencyInstance(
                when (city.currency) {
                    "EUR" -> Locale.GERMANY
                    "MXN" -> Locale("es", "MX")
                    else -> Locale.US
                }
            )
        }
    }

    fun formatAmount(amount: Int): String {
        val value = if (showInUSD) (amount * city.exchangeRateToUSD) else amount.toDouble()
        return currencyFormatter.format(value)
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Cost Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            CostLineItem(Icons.Outlined.Home, "Rent", formatAmount(rent))
            CostLineItem(Icons.Outlined.Bolt, "Utilities", formatAmount(utilities))
            CostLineItem(Icons.Outlined.ShoppingCart, "Groceries", formatAmount(groceries))
            if (diningOut > 0) {
                CostLineItem(Icons.Outlined.Restaurant, "Dining Out", formatAmount(diningOut))
            }
            if (transport > 0) {
                CostLineItem(Icons.Outlined.DirectionsBus, "Transport", formatAmount(transport))
            }
            if (healthInsurance > 0) {
                CostLineItem(Icons.Outlined.HealthAndSafety, "Health Insurance", formatAmount(healthInsurance))
            }
            if (gym > 0) {
                CostLineItem(Icons.Outlined.FitnessCenter, "Gym", formatAmount(gym))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formatAmount(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Annual estimate
            Spacer(Modifier.height(8.dp))
            Text(
                text = "≈ ${formatAmount(total * 12)} / year",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun CostLineItem(
    icon: ImageVector,
    label: String,
    amount: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getCityTips(cityKey: String): String {
    return when (cityKey) {
        // Spain
        "madrid" -> "Look in neighborhoods like Lavapiés, Malasaña, or Tetuán for better deals. Consider a monthly metro pass (€55) - it covers unlimited travel."
        "barcelona" -> "Areas like Hospitalet, Sant Andreu, or Badalona offer lower rents. The T-Casual card is great for occasional travelers."
        "valencia" -> "One of the best value cities in Spain. Ruzafa and Benimaclet are popular expat areas with reasonable rents."
        "sevilla" -> "Very affordable. Triana and Nervión offer good value. Summer utilities can spike due to AC needs."
        "malaga" -> "Growing expat hub. El Palo and Pedregalejo have beach access at lower prices than city center."

        // Portugal
        "lisbon" -> "Most expensive in Portugal. Consider Almada (across the river) or Amadora for significant savings. NHR tax regime may reduce your tax burden."
        "porto" -> "More affordable than Lisbon. Matosinhos and Vila Nova de Gaia offer seaside living at lower costs."
        "faro" -> "Gateway to the Algarve. Very affordable base with easy access to beautiful beaches."
        "coimbra" -> "University town with low costs. Great healthcare and walkable historic center."
        "braga" -> "One of Portugal's most affordable cities. Young, vibrant atmosphere."

        // Mexico
        "mexico_city" -> "Huge price variation by neighborhood. Roma, Condesa are pricey but popular. Coyoacán and San Ángel offer charm at lower cost. IMSS healthcare is very affordable."
        "guadalajara" -> "Tech hub with growing expat community. Chapultepec and Providencia are popular areas."
        "monterrey" -> "Business city with higher costs but also higher salaries. San Pedro is upscale; Centro and Cumbres are more affordable."
        "merida" -> "One of Mexico's safest cities. Excellent value, great food scene. No public transport - budget for taxis/Uber."
        "playa_del_carmen" -> "Tourist prices but great beach lifestyle. Consider Tulum or Puerto Morelos for lower costs."
        "san_miguel" -> "Artistic expat favorite. Higher costs due to popularity but incredible culture and community."
        "oaxaca" -> "Cultural gem with very low costs. Amazing food scene. Limited but growing infrastructure."
        "puerto_vallarta" -> "Beach town with year-round warmth. Pitillal and 5 de Diciembre areas are more affordable."

        else -> "Research neighborhoods carefully - prices can vary dramatically within the same city."
    }
}

/**
 * Bottom sheet wrapper for Cost Calculator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCalculatorBottomSheet(
    countryId: String,
    cityKey: String? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        CostCalculatorScreen(
            countryId = countryId,
            preselectedCity = cityKey,
            onDismiss = onDismiss
        )
    }
}
