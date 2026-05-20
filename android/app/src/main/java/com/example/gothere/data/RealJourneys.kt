package com.example.gothere.data

/**
 * Registry of premium-only real-world visa journeys. Mirrors iOS `RealJourneys.swift`.
 * Content parity between platforms is intentional — both apps share the same Firestore
 * user document, so visible content must match.
 */
object RealJourneys {
    val all: List<RealJourney> by lazy { listOf(spainDNV) }

    fun forVisa(visaId: String): RealJourney? = all.firstOrNull { it.visaId == visaId }

    fun forCountry(countryId: String): List<RealJourney> = all.filter { it.countryId == countryId }

    // -----------------------------------------------------------------------------
    // Spain Digital Nomad Visa
    // -----------------------------------------------------------------------------

    private val spainDNV: RealJourney by lazy { RealJourney(
        id = "spain_dnv",
        visaId = "spain_dnv",
        countryId = "spain",
        title = "Spain Digital Nomad Visa",
        subtitle = "Family of four · real 2023 case · 7-month journey",
        totalDuration = "~7 months from first consult to TIE card in hand",
        feeSummary = "Law firm retainer ≈ €4,500 for a family of four. Typical split: 60% on engagement, 40% on residence-permit pickup. Lease review and tax/Beckham Law consultations are billed separately as add-ons.",
        eligibilitySummary = listOf(
            "Employer must have been operating for at least 12 months",
            "Employment relationship at least 3 months old at submission",
            "Documented remote-work capability (employer letter or contract clause)",
            "Income floor: roughly €2,000/month base + ~€750 for first family member + ~€250 per additional dependant",
            "Clean criminal record (apostilled background check from country of origin)"
        ),
        phases = listOf(intakePhase, submissionPhase, correctionsPhase, arrivalPhase, empadronamientoPhase, tiePhase, healthcarePhase, taxPhase),
        crossPhaseGotchas = listOf(
            JourneyGotcha(
                id = "g_appointments",
                title = "Appointment volume scales by family size",
                detail = "A family of four needs 8 total in-person appointments at the police station (4 fingerprint slots + 4 card-pickup slots). Book consecutive same-day slots wherever possible — gestores often handle this."
            ),
            JourneyGotcha(
                id = "g_30day",
                title = "The '30-day TIE deadline' refers to booking, not attending",
                detail = "Spanish admin accepts a confirmed appointment receipt dated within 30 days of the resolution, even if the actual fingerprint date is months later. This relieves the pressure if local police slots are scarce."
            ),
            JourneyGotcha(
                id = "g_tasa",
                title = "Tasa 790-012 is paid in cash, day-of, before 11 AM",
                detail = "Not online. Not the day before. Walk into any Spanish bank the morning of your appointment with the printed form, pay in cash, get it stamped, then go to the police station. Miss the window and you reschedule."
            ),
            JourneyGotcha(
                id = "g_padron",
                title = "Empadronamiento generally won't accept Airbnb addresses",
                detail = "Most town halls require a signed long-term lease already in force. Align your arrival timing so your lease has started before you attempt Padrón registration."
            ),
            JourneyGotcha(
                id = "g_disclaimer",
                title = "Province-specific variation is real",
                detail = "Wait times and document requirements differ by province. The timing in this journey reflects Málaga/Andalucía — Barcelona, Madrid, and Valencia can be longer."
            )
        ),
        disclaimer = "Illustrative content based on a real 2023 family case (US → Málaga). Not legal advice. Visa rules, fees, and processing times change — verify with a licensed Spanish immigration lawyer before acting."
    ) }

    private val intakePhase = JourneyPhase(
        id = "p_intake",
        order = 1,
        title = "1. Intake & Engagement",
        timeframe = "Week 1",
        summary = "Initial consultation with a Spanish immigration law firm. The firm assesses eligibility, sends a fee quote, and (on payment) introduces the assigned case handler. Expect a 30-minute video call before any paperwork.",
        documents = listOf(
            "Passport bio-page scans for all family members",
            "Recent payslips (3 months) or proof of self-employment income",
            "Employer letter confirming remote-work allowance and tenure",
            "Marriage certificate (if applying as family)",
            "Birth certificates for dependants",
            "Background check from country of origin (apostilled, recent)"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_intake_1", "Initial pitch", "We recap your eligibility against the income, tenure, and remote-work tests."),
            LawyerPattern("lp_intake_2", "Fee proposal", "Quote arrives by email with itemized scope and a split-payment option."),
            LawyerPattern("lp_intake_3", "Case handler intro", "Originating advisor CCs the assigned lawyer and remains available as backup contact.")
        ),
        gotchas = listOf(
            "Tax-advisor referrals are inside the same firm but separately billed — quote arrives via a Tax Questionnaire link",
            "The intake advisor often isn't your case handler — expect a deliberate, named handover within a few days"
        )
    )

    private val submissionPhase = JourneyPhase(
        id = "p_submission",
        order = 2,
        title = "2. Document Gathering & Submission",
        timeframe = "Weeks 2–6",
        summary = "The law firm commissions sworn translations of every non-Spanish document, prepares the dossier, and submits to the Unidad de Grandes Empresas y Colectivos Estratégicos (UGE-CE). You ship apostilles, payslips, and bank statements to the firm as you collect them.",
        documents = listOf(
            "Apostilled background check (recent, country of origin)",
            "Sworn Spanish translations of: payslips, employer letter, contracts, civil status docs",
            "Bank statements showing financial means (last 3–6 months)",
            "Private Spanish health insurance policy proof",
            "Application forms (firm prepares — you sign)"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_sub_1", "Translation hand-off", "Firm forwards docs to their sworn translator and bills the translation through."),
            LawyerPattern("lp_sub_2", "Pre-submission checklist", "Final email enumerates every dossier component and asks for confirmation."),
            LawyerPattern("lp_sub_3", "Submission proof", "After filing, the firm sends a PDF receipt as evidence of the submission date.")
        ),
        gotchas = listOf(
            "Sworn translations are billed at per-page rates — expect €30–€60/page, paid via the firm",
            "Health insurance must be private + co-pay-free for residency purposes — generic travel policies are rejected"
        )
    )

    private val correctionsPhase = JourneyPhase(
        id = "p_corrections",
        order = 3,
        title = "3. Subsanación (Admin Corrections)",
        timeframe = "Variable, 0–4 weeks after submission",
        summary = "Spain's UGE-CE may raise a requerimiento de subsanación — a formal request for additional or corrected documents. The window to respond is short: typically 5 business days. The lawyer drafts the response and resubmits.",
        documents = listOf(
            "Whatever the admin specifically flagged (commonly: re-translated payslips, updated financial means proof)",
            "Translator's amended sworn declaration if a prior translation was deficient"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_corr_1", "Subsanación notification", "Firm forwards the admin's notice and proposes a fix plan that same day."),
            LawyerPattern("lp_corr_2", "Vacation handover", "Outgoing lawyer names the covering colleague before any deadline lands.")
        ),
        gotchas = listOf(
            "The 5-business-day clock can include a weekend — read the notice's exact date carefully",
            "Re-translation costs are not always included in the original retainer — confirm before paying"
        )
    )

    private val arrivalPhase = JourneyPhase(
        id = "p_arrival",
        order = 4,
        title = "4. Resolution & Arrival in Spain",
        timeframe = "1–6 weeks after submission",
        summary = "The admin issues a resolution granting the residence permit. The lawyer emails the resolution PDF. You now have 30 days to book the TIE fingerprint appointment (booking-not-attending — see gotcha above). Time your flights to land after your lease is signed.",
        documents = listOf(
            "Resolution letter (PDF from the firm)",
            "Signed long-term lease (originals, both parties)",
            "Flight itinerary to align with lease start date"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_arr_1", "Approval notification", "Often delivered with all-caps celebration line and an emoji."),
            LawyerPattern("lp_arr_2", "Next-steps brief", "Firm attaches a step-by-step PDF for Padrón, TIE, and arrival logistics.")
        ),
        gotchas = listOf(
            "Some firms include arrival logistics in the retainer; others charge it as 'final-step assistance'",
            "Resolution validity has a ticking 30-day clock for the TIE booking — don't wait past week 3"
        )
    )

    private val empadronamientoPhase = JourneyPhase(
        id = "p_padron",
        order = 5,
        title = "5. Empadronamiento (Padrón)",
        timeframe = "Within first 2 weeks of arrival",
        summary = "Register your address at the local town hall. The Padrón certificate is required for the TIE appointment and for nearly every subsequent administrative step (school enrolment, healthcare card, driver's licence exchange).",
        documents = listOf(
            "Municipal Padrón form (city-specific — download from town hall site)",
            "Original passports for every family member",
            "Signed rental contract, currently in force",
            "Recent utility bill or rent-payment proof (often required, not always)"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_pad_1", "City-specific guidance", "Firm sends the exact PDF form for your municipality and the booking link.")
        ),
        gotchas = listOf(
            "Some cities require an appointment booked online weeks in advance — check first",
            "Padrón certificate must be issued within the last 3 months for the TIE appointment"
        )
    )

    private val tiePhase = JourneyPhase(
        id = "p_tie",
        order = 6,
        title = "6. TIE Card Issuance",
        timeframe = "4–10 weeks after fingerprints",
        summary = "Two appointments at the National Police (CNP): a fingerprint appointment, then a card-pickup appointment ~4–6 weeks later (the card is printed in Madrid). Each family member needs their own slot for each step.",
        documents = listOf(
            "Form EX-17 (firm provides)",
            "Tasa 790-012 fee form, paid in cash at a Spanish bank the morning of the appointment",
            "Recent passport photo on white background",
            "Passport + photocopy",
            "Resolution letter granting the permit",
            "Empadronamiento certificate (within last 3 months) + photocopy",
            "Appointment receipt printout"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_tie_1", "Slot scarcity update", "Firm polls for cancellations weekly and re-books earlier if one opens."),
            LawyerPattern("lp_tie_2", "Pickup confirmation", "Admin assistant emails when the card is printed and ready for pickup.")
        ),
        gotchas = listOf(
            "High-demand cities (Barcelona, Madrid) may have a 6–8 week wait for the first available slot",
            "Bring physical photocopies — many police stations don't have working copiers",
            "Children get a slightly shorter validity period than parents on the first card — this is normal"
        )
    )

    private val healthcarePhase = JourneyPhase(
        id = "p_health",
        order = 7,
        title = "7. Private Health Insurance Certificate",
        timeframe = "Renewed annually",
        summary = "Non-EU residents not enrolled in Seguridad Social must show continuous private coverage with no co-pays and no coverage gaps. The 'Conditions Certificate' + payment receipt from your Spanish private insurer is what immigration accepts as proof.",
        documents = listOf(
            "Active policy with a Spain-based insurer (e.g. Sanitas, ASSSA, DKV, Adeslas)",
            "Conditions Certificate naming every insured family member and the policy year",
            "Payment receipt showing premiums paid for the current year",
            "Copies of every family member's NIE/TIE (insurer needs these before issuing the certificate)"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_hc_1", "Insurer request", "Email your assigned commercial agent — generic info@ inboxes take longer."),
            LawyerPattern("lp_hc_2", "Turnaround", "Local branch typically quotes 24 hours from head office, then postal delivery.")
        ),
        gotchas = listOf(
            "Insurer may default to postal mail — confirm your current Spanish address explicitly",
            "Multi-policy households (adults + kids on separate plans) need to list every policy number"
        )
    )

    private val taxPhase = JourneyPhase(
        id = "p_tax",
        order = 8,
        title = "8. Tax & Beckham Law (Optional Parallel)",
        timeframe = "Within first 6 months of residency",
        summary = "DNV holders can apply for the Beckham Law regime — a special tax status that caps non-Spanish income at a flat ~24% rate for the first 6 years. The window to apply is narrow. Most immigration firms have an internal tax department that handles this; you complete a separate tax questionnaire and get a separate quote.",
        documents = listOf(
            "Modelo 149 (Beckham application form — firm prepares)",
            "Proof of employment relationship and start date",
            "DNV resolution + TIE",
            "Padrón certificate"
        ),
        lawyerPatterns = listOf(
            LawyerPattern("lp_tax_1", "Cross-referral", "Immigration lawyer forwards a tax questionnaire link from the tax team."),
            LawyerPattern("lp_tax_2", "Capacity disclosure", "Firm may flag department-level delays in advance so you can plan timing.")
        ),
        gotchas = listOf(
            "Beckham must be applied for within 6 months of starting Spanish residency — miss it and you wait years",
            "Beckham is rarely the right choice if most income is from Spanish sources — model both scenarios first"
        )
    )
}
