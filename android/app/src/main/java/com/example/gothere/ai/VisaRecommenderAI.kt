package com.example.gothere.ai

import com.example.gothere.data.VisaCatalog
import com.example.gothere.data.VisaCategory
import com.example.gothere.data.VisaInfo
import com.example.gothere.decision.Budget
import com.example.gothere.decision.Household

/**
 * Visa-side recommender backing the AI `recommend_visas` tool. Kotlin port of
 * iOS `VisaRecommender.swift` so both platforms surface the same ranked tracks
 * for a given (country, household, budget, ancestry).
 *
 * Pure function — no I/O, safe to call inside the conversation loop.
 *
 * Personal Considerations (LGBTQ+/Disabled/etc.) are intentionally NOT scored
 * here: VisaCatalog carries no visa-level data for them. DecisionEngine handles
 * those at the country level. Per brief: if data is missing, don't synthesize.
 */
object VisaRecommenderAI {

    data class RankedVisa(val visa: VisaInfo, val score: Double, val reasons: List<String>)

    /** ancestry slug → the VisaCatalog id of the descent/citizenship track. */
    private val ancestryDescentVisaId = mapOf(
        "italian" to "it_jure_sanguinis",
        "irish" to "ie_fbr",
        "polish" to "pl_confirmation",
        "german" to "de_stag_15",
        "hungarian" to "hu_simplified",
        "argentine" to "ar_by_option",
        "british" to "uk_ancestry"
    )

    fun recommend(
        countryId: String,
        household: Household,
        budget: Budget,
        ancestrySlug: String = "none"
    ): List<RankedVisa> {
        val pool = VisaCatalog.byCountry(countryId)
        if (pool.isEmpty()) return emptyList()

        val budgetTarget = budgetTargetEUR(budget)
        val descentId = ancestryDescentVisaId[ancestrySlug.lowercase()]

        return pool.map { visa ->
            var score = 0.0
            val reasons = mutableListOf<String>()

            // 1. Income match (±25% bands vs a conservative budget-tier target).
            val visaIncome = visa.monthlyIncomeEUR
            if (visaIncome != null && budgetTarget != null) {
                val ratio = visaIncome.toDouble() / budgetTarget.toDouble()
                when {
                    ratio <= 0.75 -> { score += 6; reasons += "Comfortable income fit" }
                    ratio <= 1.0 -> { score += 10; reasons += "Income matches budget" }
                    ratio <= 1.25 -> { score += 4; reasons += "Income tight" }
                    // ratio > 1.25 → 0; surfaces only if other factors carry it
                }
            } else if (visaIncome == null) {
                // Non-income visas (points / employer / business / ancestry) get a
                // small neutral boost so they aren't crowded out.
                score += 3; reasons += "Non-income criteria"
            }

            // 2. Household compatibility.
            score += householdContribution(household, visa, reasons)

            // 3. Ancestry override.
            if (descentId != null && visa.id == descentId) {
                score += 50; reasons.add(0, "Matches your ancestry")
            }

            RankedVisa(visa, score, reasons)
        }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(5)
    }

    /** "What monthly income must the user clear?" target per budget tier. */
    private fun budgetTargetEUR(budget: Budget): Int? = when (budget) {
        Budget.Low -> 1_500
        Budget.Medium -> 3_000
        Budget.High -> 6_000
    }

    private fun householdContribution(
        household: Household,
        visa: VisaInfo,
        reasons: MutableList<String>
    ): Double = when (household) {
        Household.Retiree -> when (visa.category) {
            VisaCategory.PassiveIncome -> { reasons += "Built for retirees"; 12.0 }
            VisaCategory.Ancestry -> 4.0
            else -> 0.0
        }
        Household.FamilyKids, Household.SingleParent -> when (visa.category) {
            VisaCategory.Family -> { reasons += "Family reunification route"; 10.0 }
            VisaCategory.Work -> { reasons += "Work route with family inclusion"; 8.0 }
            VisaCategory.PassiveIncome -> 5.0
            VisaCategory.DigitalNomad -> { reasons += "Remote-work flexibility"; 6.0 }
            VisaCategory.Ancestry -> 6.0
            else -> 0.0
        }
        Household.Couple -> when (visa.category) {
            VisaCategory.DigitalNomad -> 8.0
            VisaCategory.PassiveIncome -> 6.0
            VisaCategory.Work -> 6.0
            VisaCategory.Ancestry -> 5.0
            else -> 0.0
        }
        Household.Singles -> when (visa.category) {
            VisaCategory.DigitalNomad -> { reasons += "Digital nomad path"; 10.0 }
            VisaCategory.Work -> 8.0
            VisaCategory.Student -> 6.0
            VisaCategory.Ancestry -> 4.0
            else -> 0.0
        }
    }
}
