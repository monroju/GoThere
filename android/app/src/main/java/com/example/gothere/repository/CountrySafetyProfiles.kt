package com.example.gothere.repository

import android.content.Context
import com.example.gothere.decision.PersonalConsideration
import org.json.JSONObject

/**
 * Per-country, per-persona inclusion notes. Loaded from
 * `assets/country_safety_profiles.json`, mirror of iOS [CountrySafetyProfiles].
 *
 * Source: ILGA-Europe Rainbow Index, EU Disability Card directives, OECD maternity
 * data, SSA totalization agreements. Refresh annually.
 */
data class CountrySafetyProfile(
    val lgbtq: String?,
    val trans: String?,
    val disabled: String?,
    val singleParent: String?,
    val veteran: String?,
    val pregnant: String?,
    val neurodivergent: String?,
    val senior: String?,
    val poc: String?
) {
    fun note(consideration: PersonalConsideration): String? = when (consideration) {
        PersonalConsideration.LGBTQ          -> lgbtq
        PersonalConsideration.Trans          -> trans
        PersonalConsideration.Disabled       -> disabled
        PersonalConsideration.Veteran        -> veteran
        PersonalConsideration.Pregnant       -> pregnant
        PersonalConsideration.Neurodivergent -> neurodivergent
        PersonalConsideration.Senior         -> senior
        PersonalConsideration.Poc            -> poc
    }
}

object CountrySafetyProfiles {
    private var cache: Map<String, CountrySafetyProfile>? = null

    private fun load(context: Context): Map<String, CountrySafetyProfile> {
        cache?.let { return it }
        val raw = try {
            context.assets.open("country_safety_profiles.json")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) { return emptyMap() }
        val envelope = JSONObject(raw)
        val countries = envelope.optJSONObject("countries") ?: return emptyMap()
        val parsed = mutableMapOf<String, CountrySafetyProfile>()
        val keys = countries.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val obj = countries.getJSONObject(id)
            parsed[id] = CountrySafetyProfile(
                lgbtq          = obj.optString("lgbtq").ifBlank { null },
                trans          = obj.optString("trans").ifBlank { null },
                disabled       = obj.optString("disabled").ifBlank { null },
                singleParent   = obj.optString("single_parent").ifBlank { null },
                veteran        = obj.optString("veteran").ifBlank { null },
                pregnant       = obj.optString("pregnant").ifBlank { null },
                neurodivergent = obj.optString("neurodivergent").ifBlank { null },
                senior         = obj.optString("senior").ifBlank { null },
                poc            = obj.optString("poc").ifBlank { null }
            )
        }
        cache = parsed
        return parsed
    }

    fun profile(context: Context, countryId: String): CountrySafetyProfile? =
        load(context)[countryId]

    fun singleParentNote(context: Context, countryId: String): String? =
        load(context)[countryId]?.singleParent

    /** Returns (persona, note) pairs in stable display order for active personas. */
    fun notes(
        context: Context,
        considerations: Set<PersonalConsideration>,
        countryId: String
    ): List<Pair<PersonalConsideration, String>> {
        val profile = profile(context, countryId) ?: return emptyList()
        val order = listOf(
            PersonalConsideration.LGBTQ, PersonalConsideration.Trans,
            PersonalConsideration.Disabled, PersonalConsideration.Veteran,
            PersonalConsideration.Pregnant, PersonalConsideration.Neurodivergent,
            PersonalConsideration.Senior, PersonalConsideration.Poc
        )
        return order.mapNotNull { c ->
            if (c in considerations) profile.note(c)?.let { c to it } else null
        }
    }
}
