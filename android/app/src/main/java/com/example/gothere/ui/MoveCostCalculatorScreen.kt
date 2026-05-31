@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.data.MoveCostData
import com.example.gothere.data.MoveCostEstimate
import com.example.gothere.data.MoveLifestyle

/**
 * "Can I afford to move?" calculator — free top-of-funnel hook for lower/middle-class
 * users who assume relocation is out of reach. Mirrors iOS MoveCostCalculatorView.
 */
@Composable
fun MoveCostCalculatorScreen(
    initialCountryId: String? = null,
    onDismiss: () -> Unit
) {
    var countryId by remember {
        mutableStateOf(initialCountryId ?: MoveCostData.profiles.firstOrNull()?.countryId ?: "spain")
    }
    var adults by remember { mutableIntStateOf(1) }
    var children by remember { mutableIntStateOf(0) }
    var lifestyle by remember { mutableStateOf(MoveLifestyle.MODERATE) }
    var monthsRunway by remember { mutableIntStateOf(3) }

    val profile = MoveCostData.profile(countryId)
    val estimate = profile?.let {
        MoveCostEstimate.compute(it, adults, children, lifestyle, monthsRunway)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cost to Move") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "What it really costs to land",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "A realistic one-time budget to get there and stay afloat your first few months. Most people overestimate this wildly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Destination picker
            Text("Destination", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoveCostData.profiles.forEach { p ->
                    FilterChip(
                        selected = p.countryId == countryId,
                        onClick = { countryId = p.countryId },
                        label = { Text("${p.flag} ${p.name}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Household steppers
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper("Adults", adults, 1..6, { adults = it }, Modifier.weight(1f))
                Stepper("Children", children, 0..8, { children = it }, Modifier.weight(1f))
            }

            // Lifestyle
            Text("Lifestyle", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MoveLifestyle.entries.forEachIndexed { idx, l ->
                    SegmentedButton(
                        selected = l == lifestyle,
                        onClick = { lifestyle = l },
                        shape = SegmentedButtonDefaults.itemShape(idx, MoveLifestyle.entries.size)
                    ) { Text(l.label) }
                }
            }

            // Runway
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Cash runway", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Months to budget before income stabilises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StepperControl(monthsRunway, 1..12) { monthsRunway = it }
                    Spacer(Modifier.width(8.dp))
                    Text("$monthsRunway mo", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // Result
            if (estimate != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estimated landing budget", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%,d".format(estimate.total)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Ballpark range: $${"%,d".format(estimate.lowBand)} – $${"%,d".format(estimate.highBand)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        LineItem("✈️ Flights (${adults + children} ppl)", estimate.flights)
                        LineItem("📄 Government fees", estimate.govtFees)
                        LineItem("⚖️ Legal / gestor", estimate.legal)
                        LineItem("🏠 Deposit + first month", estimate.upfrontRent)
                        if (estimate.firstMonthsLiving > 0) {
                            LineItem("🍽️ Living runway (${maxOf(0, monthsRunway - 1)} mo)", estimate.firstMonthsLiving)
                        }
                        LineItem("📦 Setup & shipping", estimate.setup)
                    }
                }
            }

            Text(
                "Estimates only — actual costs vary by city, season, and visa track. Figures refreshed for 2025-2026. Not financial advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$value", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                StepperControl(value, range, onChange)
            }
        }
    }
}

@Composable
private fun StepperControl(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease")
        }
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun LineItem(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$${"%,d".format(amount)}", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
