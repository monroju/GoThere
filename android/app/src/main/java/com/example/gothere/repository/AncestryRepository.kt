package com.example.gothere.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AncestryEligibilityRule(
    val id: String,
    val label: String,
    val explanation: String?,
    val required: Boolean,
    val openWorkaround: String?
)

data class AncestryPath(
    val id: String,
    val countryId: String,
    val countryName: String,
    val shortName: String,
    val fullName: String,
    val summary: String,
    val incomeRequired: Boolean,
    val estCostLowUSD: Int,
    val estCostHighUSD: Int,
    val estCostNote: String,
    val estTimelineLowMonths: Int,
    val estTimelineHighMonths: Int,
    val outcome: String,
    val eligibilityRules: List<AncestryEligibilityRule>,
    val documents: List<String>,
    val lowIncomeNotes: String,
    val officialUrl: String
)

data class AncestryCatalog(
    val version: Int,
    val lastUpdated: String,
    val disclaimer: String,
    val paths: List<AncestryPath>
)

object AncestryRepository {
    private var cached: AncestryCatalog? = null

    fun load(context: Context): AncestryCatalog {
        cached?.let { return it }
        val json = context.assets.open("ancestry_paths.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val pathsArr = obj.getJSONArray("paths")
        val paths = (0 until pathsArr.length()).map { i -> parsePath(pathsArr.getJSONObject(i)) }
        val catalog = AncestryCatalog(
            version = obj.optInt("version", 0),
            lastUpdated = obj.optString("lastUpdated"),
            disclaimer = obj.optString("disclaimer"),
            paths = paths
        )
        cached = catalog
        return catalog
    }

    private fun parsePath(o: JSONObject): AncestryPath {
        val cost = o.getJSONObject("estCostUSD")
        val timeline = o.getJSONObject("estTimelineMonths")
        return AncestryPath(
            id = o.getString("id"),
            countryId = o.getString("countryId"),
            countryName = o.getString("countryName"),
            shortName = o.getString("shortName"),
            fullName = o.getString("fullName"),
            summary = o.getString("summary"),
            incomeRequired = o.optBoolean("incomeRequired", false),
            estCostLowUSD = cost.getInt("low"),
            estCostHighUSD = cost.getInt("high"),
            estCostNote = cost.optString("note"),
            estTimelineLowMonths = timeline.getInt("low"),
            estTimelineHighMonths = timeline.getInt("high"),
            outcome = o.getString("outcome"),
            eligibilityRules = parseRules(o.getJSONArray("eligibilityRules")),
            documents = jsonStringArray(o.getJSONArray("documents")),
            lowIncomeNotes = o.optString("lowIncomeNotes"),
            officialUrl = o.optString("officialUrl")
        )
    }

    private fun parseRules(arr: JSONArray): List<AncestryEligibilityRule> =
        (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            AncestryEligibilityRule(
                id = r.getString("id"),
                label = r.getString("label"),
                explanation = if (r.has("explanation")) r.getString("explanation") else null,
                required = r.optBoolean("required", false),
                openWorkaround = if (r.has("openWorkaround")) r.getString("openWorkaround") else null
            )
        }

    private fun jsonStringArray(arr: JSONArray): List<String> =
        (0 until arr.length()).map { i -> arr.getString(i) }
}
