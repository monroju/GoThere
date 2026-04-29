package com.example.gothere.model

data class CityPoiData(
    val cityId: String,
    val countryId: String,
    val centerLat: Double,
    val centerLng: Double,
    val defaultZoom: Int,
    val lastUpdated: String,
    val pois: List<Poi>
)

data class Poi(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val notes: String = ""
)

enum class PoiCategory(val id: String, val label: String, val color: Long) {
    BANK("bank", "Banks", 0xFF2196F3),
    HOSPITAL("hospital", "Hospitals", 0xFFF44336),
    CLINIC("clinic", "Clinics", 0xFFE91E63),
    GOVERNMENT_IMMIGRATION("government_immigration", "Immigration", 0xFF9C27B0),
    GOVERNMENT_TAX("government_tax", "Tax Office", 0xFF673AB7),
    COWORKING("coworking", "Coworking", 0xFFFF9800),
    SUPERMARKET("supermarket", "Supermarkets", 0xFF4CAF50),
    INTERNATIONAL_SCHOOL("international_school", "Intl Schools", 0xFF00BCD4);

    companion object {
        fun fromId(id: String): PoiCategory? = entries.find { it.id == id }
        val all = entries.toList()
    }
}
