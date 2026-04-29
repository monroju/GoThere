package com.example.gothere.repository

import android.content.Context
import com.example.gothere.model.CityPoiData
import com.example.gothere.model.Poi
import org.json.JSONObject

/**
 * Loads POI data from bundled JSON files in assets/poi/.
 */
object PoiRepository {
    private val cache = mutableMapOf<String, CityPoiData>()

    fun loadCity(context: Context, cityId: String): CityPoiData? {
        cache[cityId]?.let { return it }

        val fileName = "poi/poi_$cityId.json"
        val json = try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return null
        }

        val obj = JSONObject(json)
        val poisArr = obj.getJSONArray("pois")
        val pois = (0 until poisArr.length()).map { i ->
            val p = poisArr.getJSONObject(i)
            Poi(
                id = p.getString("id"),
                name = p.getString("name"),
                category = p.getString("category"),
                lat = p.getDouble("lat"),
                lng = p.getDouble("lng"),
                address = p.optString("address", ""),
                phone = p.optString("phone", ""),
                website = p.optString("website", ""),
                notes = p.optString("notes", "")
            )
        }

        val data = CityPoiData(
            cityId = obj.getString("cityId"),
            countryId = obj.optString("countryId", ""),
            centerLat = obj.getDouble("centerLat"),
            centerLng = obj.getDouble("centerLng"),
            defaultZoom = obj.getInt("defaultZoom"),
            lastUpdated = obj.optString("lastUpdated", ""),
            pois = pois
        )

        cache[cityId] = data
        return data
    }
}
