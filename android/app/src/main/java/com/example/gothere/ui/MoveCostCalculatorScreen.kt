@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
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
                title = { Text(stringResource(R.string.mc_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    stringResource(R.string.mc_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.mc_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Destination picker
            Text(stringResource(R.string.mc_destination), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoveCostData.profiles.forEach { p ->
                    FilterChip(
                        selected = p.countryId == countryId,
                        onClick = { countryId = p.countryId },
                        label = { Text("${p.flag} ${countryDisplayName(p.countryId, p.name)}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Household steppers
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper(stringResource(R.string.mc_adults), adults, 1..6, { adults = it }, Modifier.weight(1f))
                Stepper(stringResource(R.string.mc_children), children, 0..8, { children = it }, Modifier.weight(1f))
            }

            // Lifestyle
            Text(stringResource(R.string.mc_lifestyle), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MoveLifestyle.entries.forEachIndexed { idx, l ->
                    SegmentedButton(
                        selected = l == lifestyle,
                        onClick = { lifestyle = l },
                        shape = SegmentedButtonDefaults.itemShape(idx, MoveLifestyle.entries.size)
                    ) { Text(stringResource(when (l) {
                        com.example.gothere.data.MoveLifestyle.LEAN -> R.string.mc_lean
                        com.example.gothere.data.MoveLifestyle.MODERATE -> R.string.mc_moderate
                        com.example.gothere.data.MoveLifestyle.COMFORTABLE -> R.string.mc_comfortable
                    })) }
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
                        Text(stringResource(R.string.mc_runway), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.mc_runway_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StepperControl(monthsRunway, 1..12) { monthsRunway = it }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mc_months_short, monthsRunway), fontWeight = FontWeight.Bold,
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
                        Text(stringResource(R.string.mc_estimated), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%,d".format(estimate.total)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.mc_ballpark, "%,d".format(estimate.lowBand), "%,d".format(estimate.highBand)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        LineItem(stringResource(R.string.mc_flights, adults + children), estimate.flights)
                        LineItem(stringResource(R.string.mc_gov_fees), estimate.govtFees)
                        LineItem(stringResource(R.string.mc_legal), estimate.legal)
                        LineItem(stringResource(R.string.mc_deposit), estimate.upfrontRent)
                        if (estimate.firstMonthsLiving > 0) {
                            LineItem(stringResource(R.string.mc_living, maxOf(0, monthsRunway - 1)), estimate.firstMonthsLiving)
                        }
                        LineItem(stringResource(R.string.mc_setup), estimate.setup)
                    }
                }
            }

            Text(
                stringResource(R.string.mc_disclaimer),
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
            Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.decrease))
        }
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.increase))
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
