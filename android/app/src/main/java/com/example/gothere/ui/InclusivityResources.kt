package com.example.gothere.ui

import com.example.gothere.decision.PersonalConsideration

/**
 * Curated resource lists for users with active PersonalConsiderations.
 * Mirror of iOS [InclusivityResources] — surfaced as a "For You" section in
 * ResourcesScreen when the wizard collected at least one persona.
 *
 * Audit cadence: refresh URLs annually; if a link 404s, prefer umbrella page.
 */
object InclusivityResources {

    fun categories(
        considerations: Set<PersonalConsideration>,
        isSingleParent: Boolean,
        countryId: String
    ): List<ResourceCategory> {
        val out = mutableListOf<ResourceCategory>()
        val order = listOf(
            PersonalConsideration.LGBTQ, PersonalConsideration.Trans,
            PersonalConsideration.Disabled, PersonalConsideration.Veteran,
            PersonalConsideration.Pregnant, PersonalConsideration.Neurodivergent,
            PersonalConsideration.Senior, PersonalConsideration.Poc
        )
        for (c in order) {
            if (c in considerations) {
                categoryFor(c, countryId)?.let { out += it }
            }
        }
        if (isSingleParent) {
            singleParentCategory(countryId)?.let { out += it }
        }
        return out
    }

    // ---- LGBTQ+ ----

    private fun lgbtq(countryId: String): ResourceCategory {
        val common = listOf(
            ResourceItem("ilga-rainbow", "ILGA-Europe Rainbow Map",
                "Country ranking by legal & social rights",
                "https://www.rainbow-europe.org/", "official")
        )
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("felgtb", "FELGTBI+", "Federación Estatal LGTBI+",
                    "https://felgtbi.org/", "community"),
                ResourceItem("cogam", "COGAM (Madrid)", "Madrid LGBT+ collective",
                    "https://cogam.es/", "community"),
                ResourceItem("casal-lambda", "Casal Lambda (Barcelona)", "Barcelona LGBTI+ centre",
                    "https://lambda.cat/", "community")
            )
            "portugal" -> listOf(
                ResourceItem("ilga-pt", "ILGA Portugal", "Legal advice + helpline",
                    "https://ilga-portugal.pt/", "service"),
                ResourceItem("rea-lisbon", "rede ex aequo", "Youth LGBTI+ network",
                    "https://www.rea.pt/", "community")
            )
            "germany" -> listOf(
                ResourceItem("lsvd", "LSVD", "National LGBT advocacy",
                    "https://www.lsvd.de/en/", "community"),
                ResourceItem("mann-o-meter", "Mann-O-Meter (Berlin)", "Berlin gay men's info centre",
                    "https://mann-o-meter.de/", "community")
            )
            "ireland" -> listOf(
                ResourceItem("belongto", "BeLonG To", "LGBTQ+ youth support",
                    "https://www.belongto.org/", "community"),
                ResourceItem("lgbt-ie", "LGBT Ireland", "Helpline + peer support",
                    "https://lgbt.ie/", "service")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("stonewall", "Stonewall UK", "Workplace + legal info",
                    "https://www.stonewall.org.uk/", "community"),
                ResourceItem("switchboard", "Switchboard LGBT+", "Confidential helpline",
                    "https://switchboard.lgbt/", "service")
            )
            "canada" -> listOf(
                ResourceItem("egale", "Egale Canada", "National LGBTQI2S advocacy",
                    "https://egale.ca/", "community")
            )
            "italy" -> listOf(
                ResourceItem("arcigay", "Arcigay", "Italy's largest LGBT+ association",
                    "https://www.arcigay.it/", "community")
            )
            "argentina" -> listOf(
                ResourceItem("100x100", "100% Diversidad y Derechos", "LGBT+ rights organisation",
                    "https://100porciento.com.ar/", "community")
            )
            "mexico" -> listOf(
                ResourceItem("yaaj", "Yaaj México", "Mental health + community",
                    "https://yaajmexico.org/", "community")
            )
            "poland" -> listOf(
                ResourceItem("kph", "Kampania Przeciw Homofobii", "Anti-homophobia + legal aid",
                    "https://kph.org.pl/en/", "community"),
                ResourceItem("lambda-warsaw", "Lambda Warszawa", "Warsaw LGBT+ centre",
                    "https://lambdawarszawa.org/en/", "service")
            )
            "hungary" -> listOf(
                ResourceItem("hatter", "Háttér Society", "Hungary's largest LGBTQ+ org",
                    "https://en.hatter.hu/", "community")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_lgbtq", "LGBTQ+ Resources", "favorite", specific + common)
    }

    // ---- Disabled / Accessibility ----

    private fun disabled(countryId: String): ResourceCategory {
        val common = listOf(
            ResourceItem("eu-disab-card", "EU Disability Card",
                "Mutual recognition across EU member states",
                "https://employment-social-affairs.ec.europa.eu/policies-and-activities/social-protection-social-inclusion/persons-disabilities/european-disability-card_en",
                "official")
        )
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("imserso", "IMSERSO", "Elderly & disability services",
                    "https://www.imserso.es/", "official"),
                ResourceItem("once", "ONCE", "Blind & low-vision services",
                    "https://www.once.es/", "service")
            )
            "portugal" -> listOf(
                ResourceItem("inr-pt", "INR Portugal", "National disability authority",
                    "https://www.inr.pt/", "official")
            )
            "germany" -> listOf(
                ResourceItem("schwerb", "Schwerbehindertenausweis", "Severe disability ID",
                    "https://www.einfach-teilhaben.de/", "official"),
                ResourceItem("integrationsamt", "Integrationsamt", "Workplace accommodation",
                    "https://www.integrationsaemter.de/", "service")
            )
            "ireland" -> listOf(
                ResourceItem("dfi", "Disability Federation of Ireland", "Umbrella + advocacy",
                    "https://www.disability-federation.ie/", "community"),
                ResourceItem("ihrec", "IHREC", "Discrimination complaints",
                    "https://www.ihrec.ie/", "official")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("scope", "Scope", "UK disability equality charity",
                    "https://www.scope.org.uk/", "community"),
                ResourceItem("pip", "Personal Independence Payment", "Disability benefit info",
                    "https://www.gov.uk/pip", "official")
            )
            "canada" -> listOf(
                ResourceItem("rdsp", "Registered Disability Savings Plan", "Federal disability savings",
                    "https://www.canada.ca/en/revenue-agency/services/tax/individuals/topics/registered-disability-savings-plan-rdsp.html",
                    "official"),
                ResourceItem("aoda", "AODA (Ontario)", "Accessibility Act",
                    "https://www.aoda.ca/", "official")
            )
            "italy" -> listOf(
                ResourceItem("law-104", "Legge 104/1992", "Disability framework + caregiver leave",
                    "https://www.inps.it/", "official")
            )
            "mexico" -> listOf(
                ResourceItem("conadis", "CONADIS", "Consejo Nacional Inclusión",
                    "https://www.gob.mx/conadis", "official")
            )
            "argentina" -> listOf(
                ResourceItem("andis", "ANDIS", "Agencia Nacional Discapacidad — CUD",
                    "https://www.argentina.gob.ar/andis", "official")
            )
            "poland" -> listOf(
                ResourceItem("pfron", "PFRON", "Disabled rehabilitation & employment",
                    "https://www.pfron.org.pl/", "official")
            )
            "hungary" -> listOf(
                ResourceItem("meosz", "MEOSZ", "National Federation Disabled Persons",
                    "https://meosz.hu/", "community")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_disabled", "Disability & Accessibility", "accessible", specific + common)
    }

    // ---- Veteran ----

    private fun veteran(countryId: String): ResourceCategory {
        val common = listOf(
            ResourceItem("va-fmp", "VA Foreign Medical Program",
                "Service-connected care reimbursement abroad",
                "https://www.va.gov/communitycare/programs/veterans/fmp/", "official"),
            ResourceItem("ssa-tot", "SSA Totalization Agreements",
                "How US credits transfer to host pensions",
                "https://www.ssa.gov/international/agreements_overview.html", "official")
        )
        val specific = when (countryId) {
            "germany" -> listOf(
                ResourceItem("ramstein-va", "Ramstein VA Outreach", "USAF Ramstein veteran services",
                    "https://www.benefits.va.gov/persona/veteran-livingabroad.asp", "official")
            )
            "mexico" -> listOf(
                ResourceItem("ajijic-vets", "American Legion Post 7 Ajijic",
                    "Largest US-veteran post outside the US",
                    "https://www.legion.org/", "community")
            )
            "italy" -> listOf(
                ResourceItem("aviano-vets", "Aviano AB Veteran Services", "US military community Italy",
                    "https://www.aviano.af.mil/", "official")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_veteran", "Veteran Resources", "shield", specific + common)
    }

    // ---- Pregnant ----

    private fun pregnant(countryId: String): ResourceCategory {
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("sespa-mat", "Maternity in the SNS", "Public maternity care",
                    "https://www.sanidad.gob.es/", "official"),
                ResourceItem("elparto-es", "El Parto es Nuestro", "Respectful-birth advocacy",
                    "https://www.elpartoesnuestro.es/", "community")
            )
            "portugal" -> listOf(
                ResourceItem("sns-mat", "SNS Saúde Materna", "Maternal health entry-point",
                    "https://www.sns24.gov.pt/", "official")
            )
            "germany" -> listOf(
                ResourceItem("mutterschutz", "Mutterschutzgesetz", "14-week paid maternity protection",
                    "https://www.bmfsfj.de/", "official"),
                ResourceItem("elterngeld", "Elterngeld portal", "Parental allowance",
                    "https://www.elterngeld-digital.de/", "official")
            )
            "ireland" -> listOf(
                ResourceItem("hse-mat", "HSE Maternity Service", "Public maternity",
                    "https://www2.hse.ie/services/maternity-services/", "official"),
                ResourceItem("cuidiu", "Cuidiú", "Parent support + antenatal",
                    "https://www.cuidiu.ie/", "community")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("nhs-pregnancy", "NHS Pregnancy", "Free maternity care for residents",
                    "https://www.nhs.uk/pregnancy/", "official"),
                ResourceItem("nct", "NCT", "Antenatal classes + parent network",
                    "https://www.nct.org.uk/", "community")
            )
            "canada" -> listOf(
                ResourceItem("ei-maternity", "EI Maternity & Parental Benefits",
                    "Federal maternity/parental leave",
                    "https://www.canada.ca/en/services/benefits/ei/ei-maternity-parental.html",
                    "official")
            )
            "italy" -> listOf(
                ResourceItem("ssn-maternita", "SSN Maternità", "Public maternity + INPS",
                    "https://www.inps.it/", "official")
            )
            "mexico" -> listOf(
                ResourceItem("imss-mat", "IMSS Maternidad", "Public maternity care",
                    "https://www.imss.gob.mx/", "official")
            )
            "argentina" -> listOf(
                ResourceItem("msal-mat", "Ministerio de Salud Maternidad", "Maternal & child health",
                    "https://www.argentina.gob.ar/salud", "official")
            )
            "poland" -> listOf(
                ResourceItem("nfz-mat", "NFZ — Opieka okołoporodowa", "Public perinatal care",
                    "https://www.nfz.gov.pl/", "official"),
                ResourceItem("rodzic-po-ludzku", "Rodzić po Ludzku", "Birth advocacy + ratings",
                    "https://www.rodzicpoludzku.pl/", "community")
            )
            "hungary" -> listOf(
                ResourceItem("neak-csed", "NEAK CSED", "Maternity allowance",
                    "https://www.neak.gov.hu/", "official")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_pregnant", "Pregnancy & Maternity Care", "child_care", specific)
    }

    // ---- Neurodivergent ----

    private fun neurodivergent(countryId: String): ResourceCategory {
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("autismo-es", "Confederación Autismo España", "Diagnostic + adult support",
                    "https://autismo.org.es/", "community"),
                ResourceItem("tdah-es", "Federación Española TDAH", "ADHD adult resources",
                    "https://feaadah.org/", "community")
            )
            "portugal" -> listOf(
                ResourceItem("fpda", "FPDA Portugal", "National autism federation",
                    "https://www.fpda.pt/", "community")
            )
            "germany" -> listOf(
                ResourceItem("autismus-de", "autismus Deutschland", "National federation; SPZ referrals",
                    "https://www.autismus.de/", "community"),
                ResourceItem("adhs-de", "ADHS Deutschland", "Adult ADHD support",
                    "https://www.adhs-deutschland.de/", "community")
            )
            "ireland" -> listOf(
                ResourceItem("asiam", "AsIAm", "Ireland autism advocacy + adult community",
                    "https://asiam.ie/", "community"),
                ResourceItem("adhd-ie", "ADHD Ireland", "Adult ADHD + Right to Choose",
                    "https://adhdireland.ie/", "community")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("nas-uk", "National Autistic Society", "UK adult diagnostic guide",
                    "https://www.autism.org.uk/", "community"),
                ResourceItem("adhd-uk", "ADHD UK", "Right-to-Choose NHS clinics list",
                    "https://adhduk.co.uk/", "community")
            )
            "canada" -> listOf(
                ResourceItem("autism-ca", "Autism Canada", "Province-by-province navigation",
                    "https://autismcanada.org/", "community"),
                ResourceItem("caddra", "CADDRA", "Canadian ADHD Resource Alliance",
                    "https://www.caddra.ca/", "community")
            )
            "italy" -> listOf(
                ResourceItem("angsa", "ANGSA", "Italy parent + adult autism association",
                    "https://www.angsa.it/", "community")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_neurodivergent", "Neurodivergent Support", "psychology", specific)
    }

    // ---- Senior ----

    private fun senior(countryId: String): ResourceCategory {
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("imserso-senior", "IMSERSO Programs", "Senior services + travel",
                    "https://www.imserso.es/", "official")
            )
            "portugal" -> listOf(
                ResourceItem("seg-social-idosos", "Segurança Social — Idosos",
                    "Pension reciprocity + senior services",
                    "https://www.seg-social.pt/", "official")
            )
            "italy" -> listOf(
                ResourceItem("inps-pensioni", "INPS Pensioni", "Pension + totalization",
                    "https://www.inps.it/", "official")
            )
            "mexico" -> listOf(
                ResourceItem("inapam", "INAPAM", "Senior discount card (60+)",
                    "https://www.gob.mx/inapam", "official")
            )
            "argentina" -> listOf(
                ResourceItem("pami", "PAMI", "Public senior healthcare",
                    "https://www.pami.org.ar/", "official")
            )
            "germany" -> listOf(
                ResourceItem("dt-rv", "Deutsche Rentenversicherung", "Pension + international",
                    "https://www.deutsche-rentenversicherung.de/", "official")
            )
            "ireland" -> listOf(
                ResourceItem("age-action", "Age Action Ireland", "Senior advocacy + benefits",
                    "https://www.ageaction.ie/", "community")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("age-uk", "Age UK", "Pension + care navigation",
                    "https://www.ageuk.org.uk/", "community")
            )
            "canada" -> listOf(
                ResourceItem("oas-cpp", "OAS / CPP", "Old Age Security + CPP",
                    "https://www.canada.ca/en/services/benefits/publicpensions.html", "official")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_senior", "Senior Living Resources", "elderly", specific)
    }

    // ---- Single Parent (household-driven, not PersonalConsideration) ----

    private fun singleParentCategory(countryId: String): ResourceCategory? {
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("es-monoparental", "Familia Monoparental status",
                    "Tax credits + reduced daycare (Ley 18/2022)",
                    "https://www.boe.es/", "official"),
                ResourceItem("fnumf", "FNUMF",
                    "Federación Nacional de Familias Monoparentales",
                    "https://familiasmonoparentales.es/", "community")
            )
            "portugal" -> listOf(
                ResourceItem("iss-monoparental", "ISS Família Monoparental",
                    "Single-parent allowances",
                    "https://www.seg-social.pt/", "official")
            )
            "germany" -> listOf(
                ResourceItem("unterhaltsv", "Unterhaltsvorschuss", "State child-support advance",
                    "https://familienportal.de/", "official"),
                ResourceItem("vamv", "VAMV", "Single-parents association",
                    "https://www.vamv.de/", "community")
            )
            "ireland" -> listOf(
                ResourceItem("ofp-ie", "One-Parent Family Payment", "Until youngest is 7",
                    "https://www.gov.ie/", "official"),
                ResourceItem("treoir", "Treoir", "Federation unmarried parents",
                    "https://www.treoir.ie/", "community")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("gingerbread", "Gingerbread", "UK single parents",
                    "https://www.gingerbread.org.uk/", "community"),
                ResourceItem("uc-uk", "Universal Credit", "Replaces older single-parent benefits",
                    "https://www.gov.uk/universal-credit", "official")
            )
            "canada" -> listOf(
                ResourceItem("ccb", "Canada Child Benefit", "Tax-free monthly per-child",
                    "https://www.canada.ca/en/revenue-agency/services/child-family-benefits/canada-child-benefit-overview.html",
                    "official")
            )
            "italy" -> listOf(
                ResourceItem("assegno-unico", "Assegno Unico Universale",
                    "INPS universal child allowance",
                    "https://www.inps.it/", "official")
            )
            "poland" -> listOf(
                ResourceItem("rodzina-800", "Rodzina 800+", "PLN 800/mo per child",
                    "https://www.zus.pl/", "official")
            )
            "hungary" -> listOf(
                ResourceItem("csaladi", "Családi adókedvezmény",
                    "Family tax credit (doubled for single parents)",
                    "https://nav.gov.hu/", "official")
            )
            else -> emptyList()
        }
        if (specific.isEmpty()) return null
        return ResourceCategory("incl_single_parent", "Single Parent Support", "child_care", specific)
    }

    // ---- Transgender ----

    private fun trans(countryId: String): ResourceCategory {
        val common = listOf(
            ResourceItem("tgeu-map", "TGEU Trans Rights Map",
                "Legal gender recognition + care access by country",
                "https://transrightsmap.tgeu.org/", "official")
        )
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("plataforma-trans", "Federación Plataforma Trans",
                    "National trans federation; 2023 trans-law guidance",
                    "https://plataformatrans.org/", "community")
            )
            "portugal" -> listOf(
                ResourceItem("ilga-pt-trans", "ILGA Portugal",
                    "Self-ID law guidance + trans peer support",
                    "https://ilga-portugal.pt/", "service")
            )
            "germany" -> listOf(
                ResourceItem("dgti", "dgti e.V.",
                    "Trans association; supplementary ID (Ergänzungsausweis)",
                    "https://dgti.org/", "community")
            )
            "ireland" -> listOf(
                ResourceItem("teni", "TENI",
                    "Transgender Equality Network Ireland — legal + peer support",
                    "https://teni.ie/", "community")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("gendered-intelligence", "Gendered Intelligence",
                    "UK trans charity; care-pathway navigation",
                    "https://genderedintelligence.co.uk/", "community")
            )
            "canada" -> listOf(
                ResourceItem("egale-trans", "Egale Canada — Trans resources",
                    "Documents, provincial care coverage guides",
                    "https://egale.ca/", "community")
            )
            "argentina" -> listOf(
                ResourceItem("ley-identidad", "Ley de Identidad de Género",
                    "Official guide to document change + guaranteed care",
                    "https://www.argentina.gob.ar/justicia/derechofacil/leysimple/identidad-de-genero",
                    "official")
            )
            "mexico" -> listOf(
                ResourceItem("condesa", "Clínica Especializada Condesa",
                    "Mexico City public HRT + trans health clinic",
                    "https://condesadf.mx/", "service")
            )
            "italy" -> listOf(
                ResourceItem("mit-italia", "MIT — Movimento Identità Trans",
                    "Bologna-based trans org; legal + health desk",
                    "https://mit-italia.it/", "community")
            )
            "poland" -> listOf(
                ResourceItem("trans-fuzja", "Fundacja Trans-Fuzja",
                    "Polish trans foundation; court-process guidance",
                    "https://transfuzja.org/", "community")
            )
            "hungary" -> listOf(
                ResourceItem("transvanilla", "Transvanilla",
                    "Hungarian trans association; recognition-ban updates",
                    "https://transvanilla.hu/", "community")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_trans", "Trans Resources", "diversity", specific + common)
    }

    // VERIFY URLs before release — official/established equality bodies.
    private fun poc(countryId: String): ResourceCategory {
        val specific = when (countryId) {
            "spain" -> listOf(
                ResourceItem("sos-racismo-es", "SOS Racismo", "Anti-racism federation; reporting + support",
                    "https://sosracismo.eu/", "community")
            )
            "italy" -> listOf(
                ResourceItem("unar-it", "UNAR", "National Office Against Racial Discrimination",
                    "https://www.unar.it/", "official")
            )
            "germany" -> listOf(
                ResourceItem("ads-de", "Antidiskriminierungsstelle", "Federal Anti-Discrimination Agency",
                    "https://www.antidiskriminierungsstelle.de/", "official")
            )
            "ireland" -> listOf(
                ResourceItem("ihrec-ie", "IHREC", "Irish Human Rights & Equality Commission",
                    "https://www.ihrec.ie/", "official")
            )
            "uk_ancestry" -> listOf(
                ResourceItem("ehrc-uk", "EHRC", "Equality & Human Rights Commission",
                    "https://www.equalityhumanrights.com/", "official")
            )
            "canada" -> listOf(
                ResourceItem("crrf-ca", "Canadian Race Relations Foundation", "Resources + reporting",
                    "https://www.crrf-fcrr.ca/", "official")
            )
            "mexico" -> listOf(
                ResourceItem("conapred-mx", "CONAPRED", "National Council to Prevent Discrimination",
                    "https://www.conapred.org.mx/", "official")
            )
            else -> emptyList()
        }
        return ResourceCategory("incl_poc", "Anti-Racism & Equality", "diversity", specific)
    }

    private fun categoryFor(c: PersonalConsideration, countryId: String): ResourceCategory? {
        val cat = when (c) {
            PersonalConsideration.LGBTQ          -> lgbtq(countryId)
            PersonalConsideration.Trans          -> trans(countryId)
            PersonalConsideration.Disabled       -> disabled(countryId)
            PersonalConsideration.Veteran        -> veteran(countryId)
            PersonalConsideration.Pregnant       -> pregnant(countryId)
            PersonalConsideration.Neurodivergent -> neurodivergent(countryId)
            PersonalConsideration.Senior         -> senior(countryId)
            PersonalConsideration.Poc            -> poc(countryId)
        }
        return cat.takeIf { it.resources.isNotEmpty() }
    }
}
