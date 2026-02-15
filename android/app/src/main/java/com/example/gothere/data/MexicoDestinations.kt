package com.example.gothere.data

object MexicoDestinations {
    val all = listOf(
        Destination(
            "mexico_city", "Mexico City", "CDMX", "Big City",
            setOf("tech", "startup", "airport", "culture", "singles", "families", "intl_schools", "remote_work", "business"),
            2, 3, "temperate", 5
        ),
        Destination(
            "guadalajara", "Guadalajara", "Jalisco", "Big City",
            setOf("tech", "business", "airport", "culture", "families", "remote_work", "affordable"),
            2, 3, "temperate", 4
        ),
        Destination(
            "monterrey", "Monterrey", "Nuevo León", "Big City",
            setOf("business", "industry", "airport", "families", "intl_schools", "tech"),
            2, 3, "continental", 3
        ),
        Destination(
            "merida", "Mérida", "Yucatán", "Mid City",
            setOf("safe", "families", "culture", "affordable", "retiree_friendly", "airport", "remote_work"),
            1, 5, "warm_coastal", 4
        ),
        Destination(
            "playa_del_carmen", "Playa del Carmen", "Quintana Roo", "Town",
            setOf("coastal", "tourism", "expat_hub", "singles", "beach", "nightlife", "remote_work"),
            2, 3, "warm_coastal", 5
        ),
        Destination(
            "cancun", "Cancún", "Quintana Roo", "Mid City",
            setOf("coastal", "airport", "tourism", "beach", "families", "nightlife"),
            2, 3, "warm_coastal", 4
        ),
        Destination(
            "puerto_vallarta", "Puerto Vallarta", "Jalisco", "Mid City",
            setOf("coastal", "airport", "tourism", "expat_hub", "retiree_friendly", "beach", "families"),
            2, 4, "warm_coastal", 5
        ),
        Destination(
            "oaxaca", "Oaxaca City", "Oaxaca", "Mid City",
            setOf("culture", "affordable", "food", "families", "art", "remote_work"),
            1, 4, "temperate", 3
        ),
        Destination(
            "san_miguel", "San Miguel de Allende", "Guanajuato", "Town",
            setOf("culture", "expat_hub", "retiree_friendly", "art", "families", "premium", "safe"),
            2, 5, "temperate", 5
        ),
        Destination(
            "queretaro", "Querétaro", "Querétaro", "Mid City",
            setOf("business", "industry", "families", "safe", "affordable", "airport"),
            1, 5, "temperate", 2
        ),
        Destination(
            "puebla", "Puebla", "Puebla", "Mid City",
            setOf("culture", "affordable", "families", "food", "university"),
            1, 4, "temperate", 2
        ),
        Destination(
            "guanajuato", "Guanajuato City", "Guanajuato", "Town",
            setOf("culture", "students", "affordable", "art", "heritage"),
            1, 4, "temperate", 2
        ),
        Destination(
            "tulum", "Tulum", "Quintana Roo", "Town",
            setOf("coastal", "beach", "remote_work", "singles", "tourism", "yoga", "expat_hub"),
            3, 3, "warm_coastal", 5
        ),
        Destination(
            "los_cabos", "Los Cabos", "Baja California Sur", "Town",
            setOf("coastal", "airport", "tourism", "premium", "beach", "families", "retiree_friendly"),
            3, 4, "warm_coastal", 4
        ),
        Destination(
            "tijuana", "Tijuana", "Baja California", "Big City",
            setOf("border", "business", "food", "affordable", "tech_seed"),
            1, 2, "temperate", 2
        ),
        Destination(
            "leon", "León", "Guanajuato", "Mid City",
            setOf("industry", "business", "affordable", "families"),
            1, 4, "temperate", 1
        ),
    )
}