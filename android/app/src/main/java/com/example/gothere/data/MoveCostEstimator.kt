package com.example.gothere.data

/**
 * One-time "cost to actually get there" estimator. Built for the lower/middle-class
 * user whose biggest blocker is the belief that moving abroad is only for the rich.
 * Every figure is a deliberately conservative *estimate in USD* — refresh annually
 * alongside VisaCatalog. The UI shows a ±20% band so users treat them as ballpark.
 * Mirrors iOS MoveCostEstimator.swift.
 */
data class MoveCostProfile(
    val countryId: String,
    val flag: String,
    val name: String,
    val flightPerPersonUSD: Int,
    val govtFeesPerPersonUSD: Int,
    val legalUSD: Int,
    val monthlyRentUSD: Int,
    val monthlyLivingPerAdultUSD: Int,
    val setupUSD: Int
) {
    /** First month + 1 month deposit up front = 2× monthly rent. */
    val upfrontRentUSD: Int get() = monthlyRentUSD * 2
}

object MoveCostData {
    /** 2025-2026 conservative estimates. Refresh each Q1. */
    val profiles: List<MoveCostProfile> = listOf(
        MoveCostProfile("spain", "🇪🇸", "Spain", 550, 150, 1600, 1100, 800, 1500),
        MoveCostProfile("portugal", "🇵🇹", "Portugal", 600, 120, 1600, 1000, 750, 1400),
        MoveCostProfile("mexico", "🇲🇽", "Mexico", 350, 250, 600, 700, 600, 900),
        MoveCostProfile("canada", "🇨🇦", "Canada", 400, 1100, 1500, 1600, 1000, 1800),
        MoveCostProfile("ireland", "🇮🇪", "Ireland", 600, 300, 1500, 1900, 1000, 1800),
        MoveCostProfile("italy", "🇮🇹", "Italy", 650, 130, 1400, 950, 800, 1500),
        MoveCostProfile("germany", "🇩🇪", "Germany", 600, 110, 1200, 1300, 950, 1600),
        MoveCostProfile("poland", "🇵🇱", "Poland", 650, 90, 1000, 750, 650, 1200),
        MoveCostProfile("argentina", "🇦🇷", "Argentina", 750, 200, 700, 550, 550, 900),
        MoveCostProfile("hungary", "🇭🇺", "Hungary", 650, 110, 900, 700, 650, 1100),
        MoveCostProfile("uk_ancestry", "🇬🇧", "UK (Ancestry)", 550, 800, 1200, 2000, 1100, 1800),
    )

    fun profile(countryId: String): MoveCostProfile? = profiles.firstOrNull { it.countryId == countryId }
}

/** Lifestyle dial — scales the recurring living-cost portion only. */
enum class MoveLifestyle(val label: String, val multiplier: Double) {
    LEAN("Lean", 0.75),
    MODERATE("Moderate", 1.0),
    COMFORTABLE("Comfortable", 1.5)
}

data class MoveCostEstimate(
    val flights: Int,
    val govtFees: Int,
    val legal: Int,
    val upfrontRent: Int,
    val firstMonthsLiving: Int,
    val setup: Int
) {
    val total: Int get() = flights + govtFees + legal + upfrontRent + firstMonthsLiving + setup
    val lowBand: Int get() = (total * 0.8).toInt()
    val highBand: Int get() = (total * 1.2).toInt()

    companion object {
        fun compute(
            profile: MoveCostProfile,
            adults: Int,
            children: Int,
            lifestyle: MoveLifestyle,
            monthsRunway: Int
        ): MoveCostEstimate {
            val people = maxOf(1, adults + children)
            val m = lifestyle.multiplier

            val flights = profile.flightPerPersonUSD * people
            // Children usually pay reduced/no consular fee on family tracks — count at 50%.
            val govtFees = profile.govtFeesPerPersonUSD * adults +
                (profile.govtFeesPerPersonUSD * 0.5 * children).toInt()
            val legal = profile.legalUSD
            val upfrontRent = (profile.upfrontRentUSD * m).toInt()
            val extraMonths = maxOf(0, monthsRunway - 1)
            val monthlyLiving = profile.monthlyLivingPerAdultUSD.toDouble() * maxOf(1, adults) +
                profile.monthlyLivingPerAdultUSD * 0.6 * children +
                profile.monthlyRentUSD
            val firstMonthsLiving = (monthlyLiving * m * extraMonths).toInt()
            val setup = (profile.setupUSD * m).toInt()

            return MoveCostEstimate(flights, govtFees, legal, upfrontRent, firstMonthsLiving, setup)
        }
    }
}
