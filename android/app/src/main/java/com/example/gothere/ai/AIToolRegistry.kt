package com.example.gothere.ai

import com.example.gothere.decision.Budget
import com.example.gothere.decision.Household
import com.example.gothere.model.WizardConfig
import com.example.gothere.repository.WizardRepository
import com.example.gothere.ui.cityData
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * On-device tool implementations for the AI conversational entry point — Kotlin
 * port of iOS `AIToolRegistry.swift`. When the model emits a `tool_use` block,
 * AIViewModel routes it here. Output is a JSON string (Claude-friendly) so the
 * model can pick fields by name in the follow-up turn.
 *
 * Keep the implementations pure (no Firestore, no network) — they run hot inside
 * the conversation loop. The only state is the [WizardConfig] the caller passes
 * in (loaded once from assets).
 */
object AIToolRegistry {

    data class ToolResult(val payload: String, val isError: Boolean)

    // MARK: - Tool catalog advertised to Claude (matches iOS schema verbatim).

    fun toolsJson(): JSONArray = JSONArray()
        .put(recommendVisasDef())
        .put(listCitiesDef())
        .put(listWizardTracksDef())

    private fun recommendVisasDef(): JSONObject = toolDef(
        name = "recommend_visas",
        description = "Rank up to 5 visa tracks for the given destination country against a household profile, budget, and optional ancestry. Mirrors the Decision Tree's visa-recommendation logic on the device.",
        properties = JSONObject()
            .put("country_id", prop("string", "Destination country slug, e.g. 'spain', 'portugal', 'mexico', 'canada', 'italy', 'ireland', 'germany', 'poland', 'hungary', 'argentina', 'uk_ancestry'."))
            .put("household", propEnum("string", "Household shape.", listOf("single", "couple", "family_kids", "single_parent", "retiree")))
            .put("budget", propEnum("string", "Approximate monthly budget tier.", listOf("low", "medium", "high")))
            .put("ancestry", propEnum("string", "Optional ancestry, if the user has disclosed one.", listOf("none", "italian", "irish", "polish", "german", "hungarian", "argentine", "british"))),
        required = listOf("country_id", "household", "budget")
    )

    private fun listCitiesDef(): JSONObject = toolDef(
        name = "list_cities_for_country",
        description = "Return GoThere's bundled cost-of-living data for cities in a country (USD per month: rent, groceries, transport, etc.). Use this when the user asks about cost of living or asks to compare cities within a country.",
        properties = JSONObject().put("country_id", prop("string", "Destination country slug.")),
        required = listOf("country_id")
    )

    private fun listWizardTracksDef(): JSONObject = toolDef(
        name = "list_wizard_tracks_for_country",
        description = "Return the visa-wizard tracks GoThere ships for a given destination country, including each track's step/task counts and whether the underlying law is currently in flux.",
        properties = JSONObject().put("country_id", prop("string", "Destination country slug.")),
        required = listOf("country_id")
    )

    // MARK: - Execution

    fun execute(name: String, input: JSONObject, wizardConfig: WizardConfig?): ToolResult = when (name) {
        "recommend_visas" -> recommendVisas(input)
        "list_cities_for_country" -> listCities(input)
        "list_wizard_tracks_for_country" -> listWizardTracks(input, wizardConfig)
        else -> ToolResult("Unknown tool: $name", isError = true)
    }

    private fun recommendVisas(input: JSONObject): ToolResult {
        val countryId = input.optString("country_id").ifEmpty {
            return ToolResult("country_id is required", isError = true)
        }
        val household = mapHousehold(input.optString("household"))
        val budget = mapBudget(input.optString("budget"))
        val ancestry = input.optString("ancestry", "none")

        val ranked = VisaRecommenderAI.recommend(countryId, household, budget, ancestry)
        val results = JSONArray()
        ranked.forEach { r ->
            results.put(
                JSONObject()
                    .put("visa_id", r.visa.id)
                    .put("name", r.visa.name)
                    .put("short_name", r.visa.shortName)
                    .put("category", r.visa.category.name)
                    .put("income_summary", r.visa.income)
                    .put("monthly_income_eur", r.visa.monthlyIncomeEUR ?: JSONObject.NULL)
                    .put("wizard_track_id", r.visa.wizardTrackId ?: JSONObject.NULL)
                    .put("official_url", r.visa.officialUrl)
                    .put("score", r.score)
                    .put("reasons", JSONArray(r.reasons))
            )
        }
        return ok(JSONObject().put("results", results))
    }

    private fun listCities(input: JSONObject): ToolResult {
        val countryId = input.optString("country_id").ifEmpty {
            return ToolResult("country_id is required", isError = true)
        }
        val cities = JSONArray()
        cityData.filter { it.value.countryId == countryId }.forEach { (id, c) ->
            val rate = c.exchangeRateToUSD
            fun usd(local: Int) = (local * rate).roundToInt()
            // Conservative single-person monthly total: studio rent + utilities +
            // groceries + transport + private health insurance.
            val totalSingle = usd(c.avgRentStudio + c.utilities + c.groceries + c.publicTransport + c.healthInsurance)
            // Family-of-three proxy: 2BR rent + utilities + ~2.6× groceries +
            // transport + ~3× health insurance.
            val totalFamily = usd(c.avgRentTwoBR + c.utilities) +
                (usd(c.groceries) * 2.6).roundToInt() + usd(c.publicTransport) + usd(c.healthInsurance) * 3
            cities.put(
                JSONObject()
                    .put("city_id", id)
                    .put("city_name", c.cityName)
                    .put("country_id", c.countryId)
                    .put("local_currency", c.currency)
                    .put("rent_studio_usd", usd(c.avgRentStudio))
                    .put("rent_1bed_usd", usd(c.avgRentOneBR))
                    .put("rent_2bed_usd", usd(c.avgRentTwoBR))
                    .put("utilities_usd", usd(c.utilities))
                    .put("groceries_usd", usd(c.groceries))
                    .put("transport_usd", usd(c.publicTransport))
                    .put("health_insurance_usd", usd(c.healthInsurance))
                    .put("dining_out_usd", usd(c.diningOut))
                    .put("total_single_usd", totalSingle)
                    .put("total_family_usd", totalFamily)
            )
        }
        return ok(JSONObject().put("cities", cities))
    }

    private fun listWizardTracks(input: JSONObject, config: WizardConfig?): ToolResult {
        val countryId = input.optString("country_id").ifEmpty {
            return ToolResult("country_id is required", isError = true)
        }
        if (config == null) {
            return ToolResult("{\"tracks\":[],\"note\":\"wizard config unavailable\"}", isError = false)
        }
        val tracks = JSONArray()
        WizardRepository.getTracksForCountry(config, countryId).forEach { (id, track) ->
            val entry = JSONObject()
                .put("track_id", id)
                .put("display_name", track.displayName)
                .put("short_name", track.shortName)
                .put("step_count", track.steps.size)
                .put("task_count", track.taskRules.size)
            IN_FLUX_NOTES[id]?.let {
                entry.put("in_flux", true)
                entry.put("in_flux_note", it)
            }
            tracks.put(entry)
        }
        return ok(JSONObject().put("tracks", tracks))
    }

    /** Laws currently in flux — keyed by wizard track id. Mirrors the warnings the
     *  iOS eligibility rules + system prompt surface. Refresh as statutes settle. */
    private val IN_FLUX_NOTES = mapOf(
        "it_jure_sanguinis" to "Italian citizenship by descent is in flux under DL 36/2025 + Law 74/2025 — the generational limit and minor-issue rules changed in 2025. Verify current eligibility with an Italian consulate or lawyer.",
        "ca_descent" to "Canadian citizenship by descent is being reshaped by Bill C-3 (first-generation-limit fix). Rules and effective dates are still settling — verify with IRCC."
    )

    // MARK: - Slug mapping

    private fun mapHousehold(slug: String): Household = when (slug.lowercase()) {
        "single", "singles" -> Household.Singles
        "couple" -> Household.Couple
        "family_kids", "family" -> Household.FamilyKids
        "single_parent" -> Household.SingleParent
        "retiree", "retired" -> Household.Retiree
        else -> Household.Couple
    }

    private fun mapBudget(slug: String): Budget = when (slug.lowercase()) {
        "low", "budget", "budget_friendly" -> Budget.Low
        "high", "premium" -> Budget.High
        else -> Budget.Medium
    }

    // MARK: - JSON helpers

    private fun ok(obj: JSONObject) = ToolResult(obj.toString(), isError = false)

    private fun toolDef(name: String, description: String, properties: JSONObject, required: List<String>): JSONObject =
        JSONObject()
            .put("name", name)
            .put("description", description)
            .put(
                "input_schema",
                JSONObject()
                    .put("type", "object")
                    .put("properties", properties)
                    .put("required", JSONArray(required))
            )

    private fun prop(type: String, description: String): JSONObject =
        JSONObject().put("type", type).put("description", description)

    private fun propEnum(type: String, description: String, values: List<String>): JSONObject =
        prop(type, description).put("enum", JSONArray(values))
}
