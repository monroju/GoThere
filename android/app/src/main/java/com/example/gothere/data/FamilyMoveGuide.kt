package com.example.gothere.data

/**
 * Family-with-kids relocation guide. Mirror of iOS FamilyMoveGuide.swift.
 * Schooling / healthcare / child-visa answers for relocating parents. Informational,
 * refreshed 2025-2026 — verify school fees locally.
 */
data class FamilyMoveProfile(
    val countryId: String,
    val flag: String,
    val name: String,
    val publicSchooling: String,
    val internationalSchooling: String,
    val homeschooling: String,
    val childHealthcare: String,
    val childVisaNote: String,
    val tips: List<String>
)

object FamilyMoveData {
    val profiles: List<FamilyMoveProfile> = listOf(
        FamilyMoveProfile("spain", "🇪🇸", "Spain",
            "Free public schools, strong quality. Under-7s adapt to Spanish fast; immersion is the norm. Concertado (semi-private) schools are low-cost and popular.",
            "Many British/American/IB schools in Madrid, Barcelona, Málaga, Valencia. ~\$6,000–\$15,000/yr.",
            "Legally grey — tolerated but not formally recognized. Most families enroll locally.",
            "Children covered under the family's private policy (visa requirement) or public system once registered (empadronamiento + Seguridad Social).",
            "Children included as dependents on NLV/DNV. NLV adds +25% IPREM income per child; DNV +25% per child.",
            listOf(
                "Empadronar (register) the whole family at the town hall ASAP — it's the gateway to school placement and healthcare.",
                "Public school spots are assigned by catchment; secure housing before the spring enrollment window.",
                "Kids under 6 often skip the language barrier entirely within a term."
            )),
        FamilyMoveProfile("portugal", "🇵🇹", "Portugal",
            "Free public schools; quality varies by region. Lisbon/Porto have waitlists. Portuguese immersion for young kids works well.",
            "Strong international-school scene (Lisbon, Cascais, Algarve). ~\$8,000–\$20,000/yr.",
            "Legal and recognized (ensino doméstico) with registration and annual exams.",
            "SNS public health covers registered residents including children; private insurance common as a top-up.",
            "Children included as dependents on D7/D8/D2. Citizenship after 5 years applies to the whole family.",
            listOf(
                "Get each child a NIF (tax number) early — needed for school and health registration.",
                "Algarve and Cascais have the densest English-speaking family communities.",
                "AIMA appointment backlogs are real — book the family's biometrics the moment you can."
            )),
        FamilyMoveProfile("mexico", "🇲🇽", "Mexico",
            "Free public schools but variable; most expat families choose private/bilingual, which is affordable by US standards.",
            "Abundant bilingual + American/IB schools (CDMX, Guadalajara, Mérida, San Miguel). ~\$3,000–\$12,000/yr.",
            "Legal; many expat families homeschool, especially in coastal/SMA communities.",
            "Private care is inexpensive and high-quality in cities; IMSS voluntary enrollment covers children cheaply.",
            "Children included on Temporary/Permanent Resident family unity. Spouse of Mexican-born has a 2-yr citizenship fast-track for the family.",
            listOf(
                "Private bilingual school often costs less than US daycare — a major draw for families.",
                "Apostille US birth certificates before you go; you'll need them for school and CURP.",
                "San Miguel de Allende and Mérida have established American family scenes."
            )),
        FamilyMoveProfile("canada", "🇨🇦", "Canada",
            "Excellent free public schools in English (or French). No language barrier for US kids.",
            "Available in major cities but rarely needed given strong public system. ~\$15,000–\$30,000/yr.",
            "Legal and well-supported, regulated by province.",
            "Provincial health plans cover children (waiting period varies by province; bridge with private insurance).",
            "Children included on PR (Express Entry/PNP/family sponsorship) or via citizenship-by-descent (Bill C-3) — instant for eligible kids.",
            listOf(
                "If you qualify for citizenship by descent, your children likely do too — claim together.",
                "Check the provincial health waiting period (e.g. BC/Ontario ~3 months) and insure the gap.",
                "Public schools are genuinely strong — international school is usually unnecessary."
            )),
        FamilyMoveProfile("ireland", "🇮🇪", "Ireland",
            "Free English-language public schools. Many are state-funded but church-affiliated; multidenominational (Educate Together) growing.",
            "Limited — public system is the norm. A few international schools in Dublin.",
            "Constitutionally protected; register with Tusla.",
            "Public system covers residents; under-8s get free GP care. Private insurance common.",
            "Children included on Critical Skills/General permits and Stamp 0. Citizenship by descent (FBR) covers eligible children — but register before they're born to pass it on.",
            listOf(
                "Under-8s qualify for a free GP visit card — register once you have a PPS number.",
                "School places are tight in Dublin; apply early and widely.",
                "Housing shortage is the real constraint — secure it before enrolling kids."
            )),
        FamilyMoveProfile("italy", "🇮🇹", "Italy",
            "Free public schools; young children immerse in Italian quickly. Quality strong in the north.",
            "International/IB schools in Rome, Milan, Florence. ~\$8,000–\$18,000/yr.",
            "Legal (istruzione parentale) with annual exams.",
            "SSN public health covers registered residents including children.",
            "Children included on Elective Residency/DNV. Jure sanguinis (citizenship by descent) covers eligible children automatically.",
            listOf(
                "If claiming jure sanguinis, minor children are recognized alongside the parent.",
                "Enroll kids by presenting your codice fiscale + residency to the local school.",
                "Northern regions (Lombardy, Emilia-Romagna) have stronger public schools."
            )),
        FamilyMoveProfile("germany", "🇩🇪", "Germany",
            "Free, high-quality public schools. Kids stream into German quickly; Willkommensklassen ease the transition.",
            "Strong international scene (Berlin, Munich, Frankfurt). ~\$12,000–\$25,000/yr.",
            "Effectively illegal — compulsory school attendance is strictly enforced.",
            "Statutory health insurance (GKV) covers children of insured parents at no extra premium.",
            "Children included on Blue Card/Freelancer family reunification. StAG §15 restoration covers eligible descendants.",
            listOf(
                "Homeschooling is not an option — plan on local or international enrollment.",
                "GKV family coverage adds children free of charge — a big saving vs the US.",
                "Anmeldung (address registration) for the whole family unlocks school + health."
            )),
        FamilyMoveProfile("poland", "🇵🇱", "Poland",
            "Free public schools; quality solid. Younger kids pick up Polish fast.",
            "International/American schools in Warsaw, Kraków, Wrocław. ~\$8,000–\$18,000/yr.",
            "Legal (edukacja domowa) with registration and exams.",
            "NFZ public health covers children of insured residents.",
            "Children included on Visa D/Temp Residence. Confirmation of citizenship covers eligible descendants — no language test.",
            listOf(
                "If confirming Polish citizenship, eligible children gain EU citizenship too.",
                "Warsaw and Kraków have the most established international family communities.",
                "Low cost of living makes private/international school more attainable than in the West."
            )),
        FamilyMoveProfile("argentina", "🇦🇷", "Argentina",
            "Free public schools; private bilingual schools are affordable and popular with expats.",
            "British/American/IB schools in Buenos Aires. Affordable vs US/Europe.",
            "Legally grey; most families enroll in private bilingual schools.",
            "Universal public healthcare is free even for residents-in-process; private obras sociales/prepagas are inexpensive.",
            "Children included on Rentista/Pensionado. Kids of native-born Argentines qualify for citizenship by option — fast 2-yr family path.",
            listOf(
                "Healthcare is free and open even while your residency is processing — a rare safety net.",
                "Buenos Aires has excellent, affordable bilingual schools.",
                "Peso volatility means USD income stretches very far on school + living costs."
            )),
        FamilyMoveProfile("hungary", "🇭🇺", "Hungary",
            "Free public schools; Hungarian is hard, so young kids adapt better than teens.",
            "International/American/British schools in Budapest. ~\$8,000–\$20,000/yr.",
            "Legal (magántanuló / private-student status) with registration.",
            "Public health (TAJ) covers insured residents' children; private clinics inexpensive.",
            "Children included on D-Visa/residence. Simplified naturalization covers eligible descendants (basic Hungarian needed for adults).",
            listOf(
                "Budapest's international schools are the practical choice given the language barrier.",
                "Get each child a TAJ card for public healthcare access.",
                "Central location makes weekend travel across Europe easy for families."
            )),
        FamilyMoveProfile("uk_ancestry", "🇬🇧", "UK (Ancestry)",
            "Free state schools in English — no language barrier. Quality varies by catchment.",
            "Private/independent schools widely available but pricey. ~\$20,000–\$45,000/yr.",
            "Legal (elective home education); minimal regulation.",
            "NHS covers residents including children (visa holders pay the IHS up front).",
            "Children included on the Ancestry visa as dependents. The IHS health surcharge is per-person, including children — budget for it.",
            listOf(
                "Budget the Immigration Health Surcharge for every family member — it adds up fast over 5 years.",
                "State school admission is catchment-based; housing location drives school options.",
                "No language barrier makes the UK one of the smoothest moves for US kids."
            ))
    )

    fun profile(countryId: String): FamilyMoveProfile? = profiles.firstOrNull { it.countryId == countryId }

    val universalTips: List<String> = listOf(
        "Apostille every child's birth certificate (and adoption/custody papers) before you leave the US — you'll need them for school, visas, and healthcare.",
        "Single parents: carry notarized consent from the other parent for international relocation — many consulates require it.",
        "Keep a digital + paper folder of each child's vaccination records; most school systems require proof.",
        "Children under ~7 typically absorb the local language within a few months — earlier moves are easier on kids.",
        "Budget a 1–3 month healthcare gap on arrival and bridge it with travel/expat insurance until residency registration completes."
    )
}
