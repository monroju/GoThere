package com.example.gothere.data

/**
 * Healthcare cost comparison vs the US. Mirror of iOS HealthcareCostData.swift.
 * Typical private-insurance premiums in USD for 2025-2026. Informational only.
 */
data class HealthcareProfile(
    val countryId: String,
    val flag: String,
    val name: String,
    val publicSystem: String,
    val privateMonthlySingleUSD: Int,
    val privateMonthlyFamilyUSD: Int,
    val note: String
)

object HealthcareCostData {
    // US reference points (2024 KFF / ACA marketplace averages).
    const val usFamilyMonthlyTotalUSD = 2125
    const val usFamilyWorkerShareMonthlyUSD = 525
    const val usSingleMarketplaceMonthlyUSD = 500

    val profiles: List<HealthcareProfile> = listOf(
        HealthcareProfile("spain", "🇪🇸", "Spain",
            "SNS public health — free/low-cost once you're a registered resident paying social security (or via the convenio especial pay-in for non-workers).",
            90, 280,
            "Visa applicants must hold private insurance with no copays (e.g. Sanitas, Adeslas). Excellent quality, short waits."),
        HealthcareProfile("portugal", "🇵🇹", "Portugal",
            "SNS public health — open to registered residents at near-zero cost.",
            60, 220,
            "Private insurance (Médis, Multicare) is cheap and widely used to skip public waits."),
        HealthcareProfile("mexico", "🇲🇽", "Mexico",
            "IMSS public health — voluntary enrollment open to residents for a low annual fee (~\$400/yr).",
            70, 250,
            "Private care is high-quality and inexpensive; many expats pay cash for routine visits (~\$30–50)."),
        HealthcareProfile("canada", "🇨🇦", "Canada",
            "Provincial medicare — free for residents after a waiting period (up to ~3 months in some provinces).",
            70, 200,
            "Public covers core care; private 'extended' plans cover dental/vision/drugs. Insure the initial waiting period."),
        HealthcareProfile("ireland", "🇮🇪", "Ireland",
            "Public health for residents; under-8s get free GP care. Public waits can be long.",
            130, 420,
            "Private insurance (Vhi, Laya) is common to bypass waits. Still well below US cost."),
        HealthcareProfile("italy", "🇮🇹", "Italy",
            "SSN public health — register for a low annual contribution; excellent in the north.",
            80, 260,
            "Elective Residency visa requires private cover; SSN enrollment available once resident."),
        HealthcareProfile("germany", "🇩🇪", "Germany",
            "Statutory health insurance (GKV) — mandatory, ~14.6% of income; children covered free under a parent.",
            250, 250,
            "GKV family coverage adds spouse/kids at no extra premium — a huge saving vs US family plans. Private (PKV) is an option for high earners."),
        HealthcareProfile("poland", "🇵🇱", "Poland",
            "NFZ public health for insured residents.",
            40, 130,
            "Private packages (Medicover, LUX MED) are very cheap and skip public queues."),
        HealthcareProfile("argentina", "🇦🇷", "Argentina",
            "Universal public healthcare — free to everyone, including residents-in-process.",
            60, 180,
            "Private prepagas (OSDE, Swiss Medical) are affordable; peso means USD income stretches far."),
        HealthcareProfile("hungary", "🇭🇺", "Hungary",
            "Public health (TAJ) for insured residents.",
            50, 160,
            "Private clinics in Budapest are inexpensive and English-friendly."),
        HealthcareProfile("uk_ancestry", "🇬🇧", "UK (Ancestry)",
            "NHS — covers residents. Visa holders pre-pay the Immigration Health Surcharge (~\$1,000/person/yr).",
            90, 300,
            "The IHS is the real cost — paid up front for the full visa term. After that, NHS care is free at point of use.")
    )

    fun profile(countryId: String): HealthcareProfile? = profiles.firstOrNull { it.countryId == countryId }
}
