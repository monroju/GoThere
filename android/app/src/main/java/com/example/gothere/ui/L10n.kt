package com.example.gothere.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.gothere.R

/**
 * Small helpers for the UI string resources (values / values-es).
 * Only UI chrome is localized; task titles, guides and catalog data stay English.
 */

/** Quantity string without depending on the experimental pluralStringResource. */
@Composable
fun pluralRes(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, count, *args)

/** Display name for a country id. The id itself is persisted and never changes. */
@StringRes
fun countryNameRes(id: String): Int = when (id) {
    "spain" -> R.string.country_spain
    "portugal" -> R.string.country_portugal
    "mexico" -> R.string.country_mexico
    "canada" -> R.string.country_canada
    "ireland" -> R.string.country_ireland
    "italy" -> R.string.country_italy
    "germany" -> R.string.country_germany
    "poland" -> R.string.country_poland
    "argentina" -> R.string.country_argentina
    "hungary" -> R.string.country_hungary
    "uk_ancestry" -> R.string.country_uk_ancestry
    else -> R.string.country_spain
}

private val knownCountryIds = setOf(
    "spain", "portugal", "mexico", "canada", "ireland", "italy",
    "germany", "poland", "argentina", "hungary", "uk_ancestry"
)

/** Localized country name for known ids; anything else keeps the data's own name. */
@Composable
fun countryDisplayName(id: String, fallback: String): String =
    if (id in knownCountryIds) stringResource(countryNameRes(id)) else fallback

/** Seed phase names (the part after "Phase N:"), mapped to display labels. */
@StringRes
private fun phaseNameRes(name: String): Int? = when (name.trim().lowercase()) {
    "research & planning" -> R.string.phase_research_planning
    "documentation" -> R.string.phase_documentation
    "application" -> R.string.phase_application
    "documentation & visa application" -> R.string.phase_documentation_visa
    "arrival & settling in spain" -> R.string.phase_arrival_spain
    "arrival & setup" -> R.string.phase_arrival_setup
    "pre-move preparations" -> R.string.phase_pre_move
    "settlement" -> R.string.phase_settlement
    "residency follow-up" -> R.string.phase_residency_followup
    "long-term setup & ongoing compliance" -> R.string.phase_long_term
    "arrival" -> R.string.phase_arrival
    "other tasks" -> R.string.phase_other_tasks
    else -> null
}

/** Localized phase overline ("Phase 1" / "Fase 1"), or the raw text if it is not "Phase N". */
@Composable
fun localizedPhaseOverline(overline: String): String {
    val n = Regex("""^Phase\s+(\d+)$""", RegexOption.IGNORE_CASE).find(overline.trim())
        ?.groupValues?.get(1)?.toIntOrNull()
    return if (n != null) stringResource(R.string.phase_n, n) else overline
}

/** Localized phase heading, falling back to the stored (English) text. */
@Composable
fun localizedPhaseName(name: String): String =
    phaseNameRes(name)?.let { stringResource(it) } ?: name

/** Full category label, e.g. "Phase 1: Research & Planning" -> "Fase 1: Investigación y planificación". */
@Composable
fun localizedPhaseTitle(category: String): String {
    val overline = category.substringBefore(':', "").trim()
    if (overline.isEmpty()) return localizedPhaseName(category)
    return localizedPhaseOverline(overline) + ": " + localizedPhaseName(category.substringAfter(':'))
}
