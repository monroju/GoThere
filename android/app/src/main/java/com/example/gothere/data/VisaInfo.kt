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
    val wizardTrackId: String?
)
