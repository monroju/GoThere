package com.example.gothere.data

enum class VisaCategory(val displayName: String) {
    PassiveIncome("Passive Income / Retiree"),
    DigitalNomad("Digital Nomad"),
    Work("Work / Skilled"),
    SelfEmployed("Self-Employed"),
    Ancestry("Ancestry / Descent"),
    Investment("Investment"),
    Student("Student"),
    Family("Family")
}

data class VisaInfo(
    val id: String,
    val countryId: String,
    val countryFlag: String,
    val countryName: String,
    val name: String,
    val shortName: String,
    val category: VisaCategory,
    val income: String,
    val processingTime: String,
    val duration: String,
    val workAllowed: String,
    val pathToPR: String,
    val pathToCitizenship: String,
    val costEstimate: String,
    val pros: List<String>,
    val cons: List<String>,
    val officialUrl: String,
    val wizardTrackId: String?,
    val inclusivityFlags: List<String> = emptyList(),
    /**
     * Structured monthly income threshold in EUR, for the income-tier filter on
     * VisaCompare. Null when the visa uses non-income criteria (ancestry,
     * points-based, employer-sponsored, no test). Mirrors iOS `VisaInfo.monthlyIncomeEUR`.
     */
    val monthlyIncomeEUR: Int? = null
) {
    /// Inclusivity flags merged with category- and country-implied defaults.
    /// Mirrors iOS `VisaInfo.effectiveInclusivityFlags`.
    val effectiveInclusivityFlags: Set<String>
        get() {
            val flags = inclusivityFlags.toMutableSet()
            when (category) {
                VisaCategory.Family        -> flags += InclusivityFlag.FAMILY_REUNIFICATION
                VisaCategory.Ancestry      -> flags += InclusivityFlag.ANCESTRY_FAST_TRACK
                VisaCategory.PassiveIncome -> flags += InclusivityFlag.SENIOR_FRIENDLY
                else -> {}
            }
            val marriageEquality = setOf(
                "spain", "portugal", "canada", "ireland", "germany", "uk_ancestry",
                "argentina", "mexico"
            )
            if (countryId in marriageEquality) flags += InclusivityFlag.LGBTQ_MARRIAGE_RECOGNIZED
            val usTotalization = setOf(
                "spain", "portugal", "italy", "germany", "ireland",
                "poland", "hungary", "uk_ancestry", "canada"
            )
            if (countryId in usTotalization) flags += InclusivityFlag.VETERAN_BENEFITS
            return flags
        }
}

/// Canonical inclusivity-flag values. Kept in sync with iOS `InclusivityFlag`
/// and `shared/country_safety_profiles.json` personas.
object InclusivityFlag {
    const val FAMILY_REUNIFICATION     = "family_reunification"
    const val SINGLE_PARENT_FRIENDLY   = "single_parent_friendly"
    const val LGBTQ_MARRIAGE_RECOGNIZED = "lgbtq_marriage_recognized"
    const val CAREGIVER_PATHWAY        = "caregiver_pathway"
    const val DISABILITY_ACCOMMODATION = "disability_accommodation"
    const val SENIOR_FRIENDLY          = "senior_friendly"
    const val VETERAN_BENEFITS         = "veteran_benefits"
    const val ANCESTRY_FAST_TRACK      = "ancestry_fast_track"
}
