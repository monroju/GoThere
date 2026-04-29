package com.example.gothere.repository

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

data class DailyTip(
    val id: Int,
    val country: String,
    val tip: String,
    val category: String
)

object TipsRepository {
    private var allTips: List<DailyTip>? = null

    private fun loadTips(context: Context): List<DailyTip> {
        allTips?.let { return it }
        val json = context.assets.open("daily_tips.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val tips = (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            DailyTip(
                id = obj.getInt("id"),
                country = obj.getString("country"),
                tip = obj.getString("tip"),
                category = obj.getString("category")
            )
        }
        allTips = tips
        return tips
    }

    /** Get today's tip for a given country, rotating daily. */
    fun getTodaysTip(context: Context, countryId: String): DailyTip? {
        val tips = loadTips(context)
        val filtered = tips.filter { it.country == countryId || it.country == "all" }
        if (filtered.isEmpty()) return null
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return filtered[dayOfYear % filtered.size]
    }
}
