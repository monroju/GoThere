package com.example.gothere.data

/**
 * Investment-migration reference. Mirror of iOS InvestmentMigration.swift.
 * Residency-by-investment visas (pulled from VisaCatalog) + Caribbean CBI programs.
 */
object InvestmentMigration {

    /** In-catalog residency-by-investment visas. */
    val residencyByInvestment: List<VisaInfo>
        get() = VisaCatalog.all.filter { it.category == VisaCategory.Investment }

    data class CBIProgram(
        val id: String,
        val flag: String,
        val country: String,
        val minInvestmentUSD: Int,
        val route: String,
        val timelineMonths: String,
        val perks: String,
        val officialUrl: String
    )

    val cbiPrograms: List<CBIProgram> = listOf(
        CBIProgram("kn", "🇰🇳", "St. Kitts & Nevis", 250_000,
            "Sustainable Island State Contribution (donation) or real estate", "4–6 mo",
            "Oldest CBI (since 1984); visa-free ~150 countries incl. UK/Schengen",
            "https://www.ciu.gov.kn/"),
        CBIProgram("dm", "🇩🇲", "Dominica", 200_000,
            "Economic Diversification Fund (donation) or real estate", "4–6 mo",
            "Often the lowest-cost CBI; visa-free ~140 countries",
            "https://cbiu.gov.dm/"),
        CBIProgram("ag", "🇦🇬", "Antigua & Barbuda", 230_000,
            "National Development Fund (donation) or real estate", "4–6 mo",
            "Family-friendly pricing; visa-free ~150 countries",
            "https://cip.gov.ag/"),
        CBIProgram("gd", "🇬🇩", "Grenada", 235_000,
            "National Transformation Fund (donation) or real estate", "4–8 mo",
            "Only Caribbean CBI with a US E-2 treaty (path to US business visa); visa-free China",
            "https://cbi.gov.gd/"),
        CBIProgram("lc", "🇱🇨", "St. Lucia", 240_000,
            "National Economic Fund (donation), real estate, or govt bonds", "4–6 mo",
            "Bond option refundable after hold period; visa-free ~145 countries",
            "https://www.cipsaintlucia.com/")
    )

    val endedPrograms: List<String> = listOf(
        "🇪🇸 Spain Golden Visa — ended April 2025 (no longer available; use Spain's NLV or DNV instead).",
        "🇵🇹 Portugal Golden Visa — real-estate route killed Oct 2023; €500k fund/cultural routes only.",
        "🇮🇪 Ireland IIP — closed to new applicants in 2023."
    )

    const val disclaimer = "Investment migration is high-stakes and lawyer-led. Programs, prices, and due-diligence rules change frequently. GoThere is informational — engage a licensed investment-migration advisor before committing funds."
}
