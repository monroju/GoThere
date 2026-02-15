package com.example.gothere.decision

import com.example.gothere.data.Destination
import kotlin.math.max

object DecisionEngine {

    fun rank(destinations: List<Destination>, p: UserProfile): List<RankedDestination> {
        return destinations.map { d ->
            var score = 0
            val reasons = mutableListOf<String>()

            // Budget vs cost
            when (p.budget) {
                Budget.Low -> {
                    val s = when (d.costLevel) { 1 -> 9; 2 -> 4; else -> -5 }
                    score += s; if (s > 0) reasons += "More affordable"
                    if ("affordable" in d.tags) { score += 3; reasons += "Affordable neighborhood options" }
                }
                Budget.Medium -> {}
                Budget.High -> {
                    val s = if (d.costLevel == 3) 5 else 0
                    score += s; if (s > 0) reasons += "Premium amenities available"
                }
            }

            // Household fit
            when (p.household) {
                Household.FamilyKids -> {
                    if ("families" in d.tags) { score += 9; reasons += "Family-friendly" }
                    if ("intl_schools" in d.tags) { score += 6; reasons += "International schools" }
                    score += (d.safety * 2)
                    if ("green" in d.tags) { score += 2; reasons += "Parks & green space" }
                }
                Household.Singles -> {
                    if ("singles" in d.tags) { score += 8; reasons += "Good for singles" }
                    if ("culture" in d.tags) { score += 4; reasons += "Vibrant culture" }
                    if (p.nightlifePreference != Nightlife.Low && (d.type == "Big City" || "tourism" in d.tags)) {
                        score += 4; reasons += "Active nightlife"
                    }
                }
                Household.Couple -> {
                    if ("culture" in d.tags) score += 3
                    if ("coastal" in d.tags && p.wantsCoastal) score += 3
                    score += d.safety
                }
                Household.Retiree -> {
                    if ("retiree_friendly" in d.tags || "affordable" in d.tags) { score += 6; reasons += "Retiree-friendly pace/cost" }
                    score += (d.safety * 2)
                }
            }

            // Coastal / climate
            if (p.wantsCoastal && "coastal" in d.tags) { score += 6; reasons += "Coastal living" }
            if (matchesClimate(p.climate, d.climate)) { score += 4; reasons += "Matches preferred climate" }

            // City size preference
            when {
                p.wantsBigCity && d.type == "Big City" -> { score += 6; reasons += "Big-city amenities" }
                !p.wantsBigCity && (d.type == "Town" || d.type == "Mid City") -> { score += 3; reasons += "Manageable size" }
            }

            // Language comfort
            if (p.languageComfort == Language.None && d.expatDensity >= 4) { score += 4; reasons += "Strong English-speaking expat base" }
            if (p.languageComfort == Language.Intermediate && d.expatDensity <= 3) { score += 2; reasons += "More local immersion possible" }

            // Airport proximity
            if (p.airportProximityImportant && "airport" in d.tags) { score += 5; reasons += "Major airport access" }

            // Business focus
            p.businessFocus?.let { focus ->
                val add = when (focus) {
                    BusinessFocus.Tech        -> if ("tech" in d.tags || "startup" in d.tags) 7 else 0
                    BusinessFocus.Tourism     -> if ("tourism" in d.tags || "expat_hub" in d.tags || "coastal" in d.tags) 7 else 0
                    BusinessFocus.Logistics   -> if ("logistics" in d.tags || "well_connected" in d.tags || "port" in d.tags || "airport" in d.tags) 7 else 0
                    BusinessFocus.Agriculture -> if ("agriculture" in d.tags || "food" in d.tags || "green" in d.tags) 6 else 0
                    BusinessFocus.RemoteWork  -> if ("remote_work" in d.tags || "expat_hub" in d.tags || d.safety >= 4) 6 else 0
                    BusinessFocus.Finance     -> if ("business" in d.tags) 5 else 0
                }
                score += add
                if (add > 0) reasons += "Aligns with business focus"
            }

            // Nightlife
            if (p.nightlifePreference == Nightlife.High && ("singles" in d.tags || d.type == "Big City" || "tourism" in d.tags)) {
                score += 3; reasons += "Nightlife options"
            }
            if (p.nightlifePreference == Nightlife.Low && ("slower_pace" in d.tags || d.type != "Big City")) {
                score += 2; reasons += "Quieter lifestyle"
            }

            // Safety priority
            val safetyBoost = max(0, d.safety - 3) * (p.safetyPriority + if (p.household == Household.FamilyKids) 1 else 0)
            if (safetyBoost > 0) reasons += "High perceived safety"
            score += safetyBoost

            RankedDestination(d, score, reasons.distinct())
        }.sortedByDescending { it.score }
    }

    private fun matchesClimate(pref: ClimatePref, climate: String): Boolean =
        when (pref) {
            ClimatePref.WarmCoastal -> climate == "warm_coastal"
            ClimatePref.Temperate   -> climate == "temperate"
            ClimatePref.Mountain    -> climate == "mountain"
            ClimatePref.Continental -> climate == "continental"
        }
}
