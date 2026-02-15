package com.example.gothere.data

data class Destination(
    val id: String,
    val name: String,
    val region: String,
    val type: String, // Big City, Mid City, Town, Village
    val tags: Set<String>,
    val costLevel: Int,   // 1 low .. 3 high
    val safety: Int,      // 1 .. 5
    val climate: String,  // warm_coastal, temperate, mountain, continental
    val expatDensity: Int // 1 .. 5
)

object SpainDestinations {
    val all = listOf(
        Destination("madrid", "Madrid", "Comunidad de Madrid", "Big City",
            setOf("tech","business","airport","culture","singles","families","intl_schools","remote_work"),
            3, 4, "continental", 4),
        Destination("barcelona", "Barcelona", "Catalunya", "Big City",
            setOf("tech","startup","tourism","airport","coastal","culture","singles","intl_schools","remote_work"),
            3, 4, "warm_coastal", 5),
        Destination("valencia", "València", "Comunitat Valenciana", "Mid City",
            setOf("coastal","families","airport","tourism","remote_work","culture","singles","affordable"),
            2, 4, "warm_coastal", 4),
        Destination("malaga", "Málaga", "Andalucía", "Mid City",
            setOf("coastal","airport","tourism","families","business","remote_work","expat_hub"),
            2, 4, "warm_coastal", 5),
        Destination("marbella", "Marbella", "Andalucía (Costa del Sol)", "Town",
            setOf("coastal","expat_hub","singles","families","tourism","intl_schools","high_end"),
            3, 4, "warm_coastal", 5),
        Destination("seville", "Sevilla", "Andalucía", "Mid City",
            setOf("culture","families","business","remote_work"),
            2, 4, "warm_coastal", 3),
        Destination("granada", "Granada", "Andalucía", "Mid City",
            setOf("students","culture","mountain_access","affordable"),
            1, 4, "temperate", 3),
        Destination("alicante", "Alicante", "Comunitat Valenciana", "Mid City",
            setOf("coastal","airport","tourism","retiree_friendly","affordable","families"),
            1, 4, "warm_coastal", 4),
        Destination("bilbao", "Bilbao", "Basque Country", "Mid City",
            setOf("industry","culture","business","families","food","green"),
            2, 5, "temperate", 3),
        Destination("sansebastian", "San Sebastián (Donostia)", "Basque Country", "Town",
            setOf("families","food","beach","safe","premium"),
            3, 5, "temperate", 3),
        Destination("palma", "Palma de Mallorca", "Illes Balears", "Mid City",
            setOf("island","coastal","airport","tourism","remote_work","families","singles"),
            3, 4, "warm_coastal", 5),
        Destination("tenerife", "Tenerife (Santa Cruz / Costa Adeje)", "Canarias", "Mid City",
            setOf("island","coastal","tourism","remote_work","affordable","families","good_weather"),
            1, 4, "warm_coastal", 4),
        Destination("vitoria", "Vitoria-Gasteiz", "Basque Country", "Mid City",
            setOf("families","green","safe","affordable"),
            2, 5, "temperate", 2),
        Destination("coruna", "A Coruña", "Galicia", "Mid City",
            setOf("coastal","families","affordable","slower_pace","tech_seed"),
            1, 4, "temperate", 2),
        Destination("santander", "Santander", "Cantabria", "Mid City",
            setOf("coastal","families","safe","affordable","slower_pace"),
            1, 5, "temperate", 2),
        Destination("zaragoza", "Zaragoza", "Aragón", "Mid City",
            setOf("logistics","industry","affordable","families","well_connected"),
            1, 4, "continental", 2),
    )
}
