package com.example.gothere.decision

object MicroAdvice {
    // Overloaded function with countryId parameter
    fun tipsFor(destinationId: String, countryId: String = "spain"): List<String> {
        return when (countryId) {
            "portugal" -> portugalTips(destinationId)
            "mexico" -> mexicoTips(destinationId)
            else -> spainTips(destinationId)
        }
    }

    private fun spainTips(destinationId: String): List<String> = when (destinationId) {
        "malaga" -> listOf(
            "Families: Teatinos / El Cónsul (schools, parks).",
            "Singles: Centro Histórico / Soho for walkability & nightlife.",
            "AGP airport ~10–20 min; great US-EU travel hub."
        )
        "marbella" -> listOf(
            "Families: Nueva Andalucía / San Pedro de Alcántara.",
            "Singles: Puerto Banús nightlife.",
            "Higher costs; budget accordingly."
        )
        "valencia" -> listOf(
            "Singles: Ruzafa / El Carmen (cafés & culture).",
            "Families: Algirós / Camins al Grau.",
            "Tram/metro to Malvarrosa beach."
        )
        "madrid" -> listOf(
            "Families: Hortaleza / Montecarmelo / Mirasierra.",
            "Airport access: Barajas / Alameda de Osuna.",
            "Excellent intl schools & business links."
        )
        "barcelona" -> listOf(
            "Singles: Eixample / Gràcia.",
            "Families: Sarrià-Sant Gervasi / Les Corts.",
            "Higher rents; check TMB lines."
        )
        "alicante" -> listOf(
            "Affordable coastal; airport nearby.",
            "Families: Playa de San Juan.",
            "Check Valencian language at schools."
        )
        "granada" -> listOf(
            "Student vibe; historic center.",
            "Affordable; Sierra Nevada access.",
            "Hot summers, cool winters."
        )
        "bilbao" -> listOf(
            "Families: Deusto / Indautxu.",
            "Very safe; rich food culture.",
            "Temperate climate; more rain."
        )
        "sansebastian" -> listOf(
            "Premium, safe, family-friendly.",
            "La Concha beach lifestyle.",
            "Higher cost of living."
        )
        "palma" -> listOf(
            "Island logistics + great airport.",
            "Seasonality affects rents.",
            "Mix of expat/local areas."
        )
        "tenerife" -> listOf(
            "Year-round warm; affordable inland.",
            "South: expat/tourism; North: greener.",
            "Check nearby schools/health centers."
        )
        "vitoria" -> listOf(
            "Very safe, green, family-focused.",
            "Affordable vs Donostia.",
            "Colder winters."
        )
        "coruna" -> listOf(
            "Coastal, slower pace, great food.",
            "Affordable; tech seeding nearby.",
            "More rain; mild summers."
        )
        "santander" -> listOf(
            "Safe, scenic beaches; calm nightlife.",
            "Affordable vs big cities.",
            "Great family vibe."
        )
        "zaragoza" -> listOf(
            "Logistics hub between Madrid & Barcelona.",
            "Affordable; dry climate.",
            "Families: Actur / Romareda."
        )
        else -> listOf("Check padrón, schools, and healthcare zones before signing a lease.")
    }

    private fun portugalTips(destinationId: String): List<String> = when (destinationId) {
        "lisbon" -> listOf(
            "Families: Parque das Nações / Belém.",
            "Singles: Príncipe Real / Bairro Alto.",
            "High rents; consider outskirts for value."
        )
        "porto" -> listOf(
            "Families: Foz do Douro / Boavista.",
            "Singles: Cedofeita / Ribeira.",
            "More affordable than Lisbon."
        )
        "cascais" -> listOf(
            "Premium coastal; great intl schools.",
            "30 min train to Lisbon.",
            "Higher costs; expat-friendly."
        )
        "sintra" -> listOf(
            "Green, historic, family-friendly.",
            "Tourist crowds; quieter inland.",
            "Cooler than coastal areas."
        )
        "faro" -> listOf(
            "Algarve gateway; affordable.",
            "Airport nearby for travel.",
            "Hot summers; mild winters."
        )
        "lagos" -> listOf(
            "Beach town; expat community.",
            "Singles: town center nightlife.",
            "Seasonal tourism fluctuations."
        )
        "funchal" -> listOf(
            "Madeira: year-round mild weather.",
            "Growing digital nomad scene.",
            "Island living logistics."
        )
        "coimbra" -> listOf(
            "University town; student vibe.",
            "Very affordable.",
            "Rich culture and history."
        )
        "braga" -> listOf(
            "Young, tech-growing city.",
            "Affordable; great food.",
            "Very safe; family-friendly."
        )
        else -> listOf("Register for NIF early; join expat groups for housing tips.")
    }

    private fun mexicoTips(destinationId: String): List<String> = when (destinationId) {
        "mexico_city" -> listOf(
            "Families: Coyoacán / Del Valle / Polanco.",
            "Singles: Roma Norte / Condesa.",
            "Traffic is intense; live near work."
        )
        "guadalajara" -> listOf(
            "Families: Providencia / Zapopan.",
            "Tech hub growing rapidly.",
            "More affordable than CDMX."
        )
        "monterrey" -> listOf(
            "Business hub; industrial focus.",
            "Families: San Pedro Garza García.",
            "Hot summers; AC essential."
        )
        "merida" -> listOf(
            "Very safe; great for families.",
            "Hot & humid; plan accordingly.",
            "Strong expat community."
        )
        "playa_del_carmen" -> listOf(
            "Beach town; tourist-heavy.",
            "Digital nomad scene.",
            "Party vibe; consider quieter areas."
        )
        "puerto_vallarta" -> listOf(
            "Established expat community.",
            "Beach + mountain access.",
            "Families: Marina Vallarta."
        )
        "oaxaca" -> listOf(
            "Art & food culture capital.",
            "Affordable; great weather.",
            "Limited intl schools."
        )
        "san_miguel" -> listOf(
            "Art & expat haven.",
            "Premium prices; beautiful.",
            "Retiree-friendly; quieter."
        )
        "queretaro" -> listOf(
            "Industrial hub; very safe.",
            "Families: Juriquilla / El Refugio.",
            "Growing expat community."
        )
        "tulum" -> listOf(
            "Beach + cenotes lifestyle.",
            "Higher costs; trendy.",
            "Infrastructure still developing."
        )
        else -> listOf("Get a trusted notary for housing; join local expat groups.")
    }
}
