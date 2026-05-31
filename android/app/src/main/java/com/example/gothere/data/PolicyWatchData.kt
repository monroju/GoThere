package com.example.gothere.data

/**
 * In-app "Policy Watch". Mirror of iOS PolicyWatchData.swift. Standing US
 * emigration-policy factors, each framed "what it means → who it's for → fastest route."
 */
data class PolicyWatchItem(
    val id: String,
    val icon: String,           // material icon name hint (mapped in the screen)
    val headline: String,
    val whoAffected: String,
    val fastestRoute: String
)

object PolicyWatchData {
    val items: List<PolicyWatchItem> = listOf(
        PolicyWatchItem("worldwide_tax", "public",
            "The US taxes citizens on worldwide income — forever",
            "Everyone who leaves while keeping US citizenship or a green card.",
            "You almost never pay double thanks to the FEIE (~\$126k) + Foreign Tax Credit — but you must keep filing. Pick a destination with a US tax treaty; lean on a preferential regime (Beckham, IFICI)."),
        PolicyWatchItem("exit_tax", "logout",
            "Renouncing citizenship can trigger an exit tax",
            "High-net-worth individuals considering giving up US citizenship.",
            "Secure a second nationality FIRST (citizenship by descent if you qualify — far cheaper than CBI), then take expatriation advice. Never renounce without a tax attorney."),
        PolicyWatchItem("passport_backlog", "book",
            "Passport demand keeps processing times volatile",
            "Anyone whose passport expires within ~18 months of moving.",
            "Renew every family member's passport now — it's the cheapest, highest-leverage first step. Build it into your move timeline."),
        PolicyWatchItem("totalization", "shield",
            "Social Security totalization protects your benefits abroad",
            "Retirees and workers who've paid into US Social Security.",
            "Most GoThere destinations have a totalization agreement so you don't pay into two systems and don't lose credits. Mexico and Argentina are the gaps — verify before relying on it."),
        PolicyWatchItem("rights_shifts", "groups",
            "State-level rights changes are driving relocation",
            "LGBTQ+ families, those needing reproductive care, and others affected by shifting state law.",
            "Use the rights & safety filter to weight destinations on the protections that matter to you — several GoThere countries lead the US on these.")
    )

    const val footer = "Policy Watch is educational, not legal or tax advice. Enable alerts to get notified when a US policy change materially affects moving abroad."
}
