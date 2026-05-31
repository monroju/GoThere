package com.example.gothere.data

/**
 * Preferential tax regimes for the tax-optimization overlay (#11). Mirror of iOS
 * TaxRegimes.swift + the VisaInfo.taxRegimes attachments. Android's VisaInfo has no
 * taxRegimes field, so this keeps a standalone visaId → regimes lookup to avoid
 * editing the 47-row catalog. Surfaced in VisaCompare's comparison table.
 * Informational only — eligibility is decided by the tax authority, not this app.
 */
data class TaxRegime(
    val name: String,
    val flatRatePercent: Double?,       // e.g. 0.24 for 24%; null when not a single rate
    val eligibilityCriteria: List<String>,
    val applicationWindow: String?
)

object TaxRegimes {
    private val beckhamLaw = TaxRegime(
        "Beckham Law", 0.24,
        listOf(
            "Not Spanish tax resident in the prior 5 years",
            "Move triggered by an employment contract or by DNV-eligible remote work",
            "Most professional activity must be carried out in Spain"
        ),
        "Modelo 149 must be filed within 6 months of becoming Spanish tax resident"
    )
    private val beckhamLawAutonomo = TaxRegime(
        "Beckham Law (entrepreneur route)", 0.24,
        listOf(
            "Business activity certified innovative by ENISA or the DGT",
            "Not Spanish tax resident in the prior 5 years",
            "Activity carried out mainly in Spain"
        ),
        "Modelo 149 within 6 months of becoming Spanish tax resident"
    )
    private val tarifaPlanaAutonomo = TaxRegime(
        "Tarifa Plana (new autónomo)", null,
        listOf(
            "First-time autónomo registration with the RETA",
            "No autónomo registration in the prior 2 years (3 years if previously claimed the benefit)",
            "No outstanding social-security or tax debts"
        ),
        "Elect at the time of alta with the Tesorería General de la Seguridad Social"
    )
    private val ifici = TaxRegime(
        "IFICI (NHR successor)", 0.20,
        listOf(
            "New Portuguese tax resident (not resident in the prior 5 years)",
            "Activity in research, higher education, innovation, certified startups, or other listed high-value-added sectors",
            "Registration with the eligible-activity authority (e.g. ANI, IAPMEI, AICEP)"
        ),
        "Register with AT by 15 January of the year following Portuguese tax residency"
    )

    /** Maps VisaCatalog visa id → applicable regimes. Mirrors iOS catalog attachments. */
    private val byVisaId: Map<String, List<TaxRegime>> = mapOf(
        "es_dnv" to listOf(beckhamLaw),
        "es_autonomo" to listOf(beckhamLawAutonomo, tarifaPlanaAutonomo),
        "pt_d2" to listOf(ifici)
    )

    fun forVisa(visaId: String): List<TaxRegime> = byVisaId[visaId] ?: emptyList()
}
