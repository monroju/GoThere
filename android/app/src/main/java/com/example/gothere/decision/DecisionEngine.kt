package com.example.gothere.decision

import com.example.gothere.data.Destination
import kotlin.math.max

object DecisionEngine {

    fun rank(
        destinations: List<Destination>,
        p: UserProfile,
        countryId: String = "spain"
    ): List<RankedDestination> {
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
                Household.SingleParent -> {
                    if ("families" in d.tags) { score += 8; reasons += "Family-friendly" }
                    if ("intl_schools" in d.tags) { score += 6; reasons += "International schools" }
                    if ("public_transit" in d.tags || d.type == "Big City") { score += 4; reasons += "Strong public transit" }
                    score += (d.safety * 2)
                }
                Household.Retiree -> {
                    if ("retiree_friendly" in d.tags || "affordable" in d.tags) { score += 6; reasons += "Retiree-friendly pace/cost" }
                    score += (d.safety * 2)
                }
            }

            // Personal Considerations (country-level + tag bonus)
            p.considerations.forEach { c ->
                when (c) {
                    PersonalConsideration.LGBTQ -> {
                        when (countryId) {
                            "spain", "portugal", "canada", "ireland", "germany", "uk_ancestry" -> { score += 12; reasons += "Strong LGBTQ+ protections" }
                            "italy", "argentina", "mexico" -> { score += 6; reasons += "LGBTQ+ legal but uneven" }
                            "poland", "hungary" -> { score -= 4; reasons += "LGBTQ+ rights limited" }
                        }
                        if ("lgbtq_friendly" in d.tags) score += 5
                    }
                    PersonalConsideration.Trans -> {
                        // Legal gender recognition + gender-affirming care access. Self-ID countries
                        // and Argentina's Ley 26.743 care guarantee score highest; Hungary's
                        // recognition ban is a hard negative.
                        when (countryId) {
                            "spain", "portugal", "germany", "canada", "argentina" -> { score += 12; reasons += "Self-ID gender recognition + care access" }
                            "ireland" -> { score += 8; reasons += "Self-ID recognition (care waits long)" }
                            "mexico", "italy" -> { score += 5; reasons += "Trans recognition varies by region" }
                            "uk_ancestry" -> { score += 4; reasons += "Long NHS gender-care waits" }
                            "poland" -> { score -= 2; reasons += "Gender recognition needs court process" }
                            "hungary" -> { score -= 8; reasons += "Legal gender recognition banned" }
                        }
                        if ("lgbtq_friendly" in d.tags) score += 5
                    }
                    PersonalConsideration.Disabled -> {
                        when (countryId) {
                            "germany", "spain", "portugal", "ireland", "italy", "poland", "hungary" -> { score += 8; reasons += "EU Disability Card recognised" }
                            "canada", "uk_ancestry" -> { score += 7; reasons += "Strong accessibility law" }
                            "mexico", "argentina" -> { score += 2 }
                        }
                        if (d.type == "Big City") { score += 4; reasons += "Better accessibility in city" }
                        if ("public_transit" in d.tags) score += 4
                    }
                    PersonalConsideration.Veteran -> {
                        when (countryId) {
                            "spain", "portugal", "italy", "germany", "ireland", "poland", "hungary", "uk_ancestry", "canada" -> { score += 6; reasons += "US totalization agreement" }
                            "mexico" -> { score += 4; reasons += "VA FMP coverage available" }
                            "argentina" -> { score += 3 }
                        }
                    }
                    PersonalConsideration.Pregnant -> {
                        when (countryId) {
                            "germany", "ireland", "canada", "italy", "spain", "portugal", "uk_ancestry" -> { score += 10; reasons += "Strong maternity care" }
                            "poland", "hungary", "argentina" -> { score += 5 }
                            "mexico" -> { score += 4 }
                        }
                        if (d.type == "Big City") { score += 3; reasons += "Top-tier hospitals" }
                    }
                    PersonalConsideration.Neurodivergent -> {
                        when (countryId) {
                            "germany", "canada", "ireland", "uk_ancestry" -> { score += 9; reasons += "Adult ND assessment via public system" }
                            "spain", "portugal", "italy" -> { score += 6; reasons += "Growing ND support networks" }
                            "poland", "hungary", "argentina", "mexico" -> { score += 3 }
                        }
                        if ("walkable" in d.tags) { score += 4; reasons += "Walkable — lower sensory load" }
                        if (d.expatDensity >= 4) { score += 3; reasons += "English-speaking ND community" }
                        if (d.type == "Big City" && "walkable" !in d.tags) score -= 2
                    }
                    PersonalConsideration.Senior -> {
                        when (countryId) {
                            "portugal", "spain", "italy" -> { score += 10; reasons += "Top retiree healthcare & climate" }
                            "ireland", "germany", "canada", "uk_ancestry" -> { score += 8; reasons += "Strong public healthcare" }
                            "mexico", "argentina" -> { score += 6; reasons += "Lower cost of senior living" }
                            "poland", "hungary" -> { score += 4 }
                        }
                        if ("retiree_friendly" in d.tags) score += 5
                        if ("walkable" in d.tags) score += 3
                        if ("warm_coastal" in d.tags || d.climate == "warm_coastal") score += 3
                    }
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
