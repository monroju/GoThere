package com.example.gothere.repository

import android.content.Context
import org.json.JSONObject

/**
 * Per-country medication & care continuity notes. Loaded from
 * `assets/care_continuity_profiles.json`, mirror of iOS [CareContinuityProfiles].
 *
 * Covers ADHD stimulant availability, gender-affirming hormone access, insulin,
 * and the rules for carrying prescription meds at entry (Schengen Art. 75,
 * COFEPRIS, etc.). Canonical sources: INCB country regulations, CDC Yellow Book,
 * national medicine agencies. Refresh annually alongside the safety profiles.
 */
data class CareContinuityProfile(
    val adhd: String?,
    val hrt: String?,
    val insulin: String?,
    val bringIn: String?
)

object CareContinuityProfiles {
    private var cache: Map<String, CareContinuityProfile>? = null

    private fun load(context: Context): Map<String, CareContinuityProfile> {
        cache?.let { return it }
        val raw = try {
            context.assets.open("care_continuity_profiles.json")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) { return emptyMap() }
        val envelope = JSONObject(raw)
        val countries = envelope.optJSONObject("countries") ?: return emptyMap()
        val parsed = mutableMapOf<String, CareContinuityProfile>()
        val keys = countries.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val obj = countries.getJSONObject(id)
            parsed[id] = CareContinuityProfile(
                adhd    = obj.optString("adhd").ifBlank { null },
                hrt     = obj.optString("hrt").ifBlank { null },
                insulin = obj.optString("insulin").ifBlank { null },
                bringIn = obj.optString("bring_in").ifBlank { null }
            )
        }
        cache = parsed
        return parsed
    }

    fun profile(context: Context, countryId: String): CareContinuityProfile? =
        load(context)[countryId]
}
