package com.example.gothere.data

/**
 * "Gone in N months" timeline generator. Mirror of iOS RelocationTimeline.swift.
 */
object RelocationTimeline {

    data class Milestone(
        val id: String,
        val title: String,
        val detail: String,
        val monthsBefore: Int,
        val familyOnly: Boolean
    )

    val milestones: List<Milestone> = listOf(
        Milestone("decide_visa", "Choose your visa pathway",
            "Use the Visa Compare + Wizard to lock in the track you qualify for. Everything else hangs off this.", 12, false),
        Milestone("budget", "Set your move budget & start saving",
            "Run the Cost-to-Move calculator. Build the landing fund + a 3-month runway.", 12, false),
        Milestone("passport", "Renew passports (6+ months validity)",
            "Every family member needs a passport valid well beyond your intended stay.", 10, false),
        Milestone("documents", "Gather & apostille core documents",
            "Birth/marriage certificates, FBI background check, diplomas — apostilled and translated as the visa requires.", 9, false),
        Milestone("kids_docs", "Gather children's documents",
            "Apostille kids' birth certificates, vaccination + school records. Single parents: notarized consent from the other parent.", 9, true),
        Milestone("employer", "Sort your income / employer authorization",
            "Remote workers: get the employer remote-work letter (see Bring Your Job). Freelancers: assemble client contracts + invoices.", 7, false),
        Milestone("visa_apply", "Submit your visa application",
            "Book the consulate appointment and file. This is the long pole — start the moment documents are ready.", 6, false),
        Milestone("schools", "Research & contact schools",
            "Shortlist public vs international schools, check enrollment windows, and email admissions. (See Moving with Kids.)", 5, true),
        Milestone("housing", "Research housing & neighborhoods",
            "Identify target areas; line up short-term housing for your first 1–2 months on the ground.", 4, false),
        Milestone("healthcare", "Arrange health insurance",
            "Secure the private policy your visa requires + bridge cover for the residency-registration gap.", 3, false),
        Milestone("downsize", "Sell, store, or ship belongings",
            "Decide what comes with you. Get shipping quotes early; sell big items while you have time.", 3, false),
        Milestone("flights", "Book one-way flights",
            "Lock in dates once your visa is approved (or appointment confirmed).", 2, false),
        Milestone("wind_down", "Wind down US life",
            "Mail forwarding, cancel/transfer subscriptions & utilities, notify the IRS of your new address, set up a US mailing solution.", 1, false),
        Milestone("pack", "Final packing & document folder",
            "Carry originals of all apostilled docs, visa approval, and proof of funds in your hand luggage.", 1, false),
        Milestone("arrival", "Arrival: register as a resident",
            "Empadronamiento / address registration, local tax ID, residency card appointment, open a bank account, register for healthcare.", 0, false)
    )

    data class MonthBucket(val index: Int, val label: String, val milestones: List<Milestone>)

    fun generate(totalMonths: Int, hasKids: Boolean): List<MonthBucket> {
        val n = totalMonths.coerceIn(1, 18)
        val relevant = milestones.filter { !it.familyOnly || hasKids }
        val byIndex = mutableMapOf<Int, MutableList<Milestone>>()
        for (m in relevant) {
            val idx = minOf(m.monthsBefore, n)
            byIndex.getOrPut(idx) { mutableListOf() }.add(m)
        }
        return byIndex.keys.sortedDescending().map { idx ->
            val label = when (idx) {
                n -> "Now — start here"
                0 -> "Move month 🎉"
                else -> "~$idx month${if (idx == 1) "" else "s"} before"
            }
            MonthBucket(idx, label, byIndex[idx] ?: emptyList())
        }
    }
}
