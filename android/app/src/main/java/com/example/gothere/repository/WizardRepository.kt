package com.example.gothere.repository

import android.content.Context
import android.util.Log
import com.example.gothere.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the visa wizard config from assets and generates personalized tasks
 * based on user answers.
 */
object WizardRepository {
    private const val TAG = "WizardRepo"
    private const val CONFIG_PATH = "visa_wizard/wizard_config.json"

    private var cachedConfig: WizardConfig? = null

    fun loadConfig(context: Context): WizardConfig {
        cachedConfig?.let { return it }

        val json = context.assets.open(CONFIG_PATH)
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)

        val tracksObj = root.getJSONObject("tracks")
        val tracks = mutableMapOf<String, WizardTrack>()

        for (trackId in tracksObj.keys()) {
            tracks[trackId] = parseTrack(tracksObj.getJSONObject(trackId))
        }

        val config = WizardConfig(
            wizardVersion = root.getInt("wizardVersion"),
            tracks = tracks
        )
        cachedConfig = config
        return config
    }

    fun getTracksForCountry(config: WizardConfig, countryId: String): List<Pair<String, WizardTrack>> {
        return config.tracks.entries
            .filter { it.value.countryId == countryId }
            .map { it.key to it.value }
    }

    /**
     * Evaluate task rules against wizard answers and produce a list of personalized Tasks.
     *
     * @param track The selected visa track
     * @param answers Map of questionId -> user answer (String for choices, Boolean for toggles, Int for numbers)
     * @param countryId The country
     * @param targetWeeksFromNow Weeks until target move date (used to calculate due dates)
     */
    fun generateTasks(
        track: WizardTrack,
        answers: Map<String, Any>,
        countryId: String,
        targetWeeksFromNow: Int
    ): List<Task> {
        val now = System.currentTimeMillis()
        val msPerWeek = 7L * 24 * 60 * 60 * 1000

        return track.taskRules
            .filter { rule -> evaluateConditions(rule.conditions, answers) }
            .sortedBy { it.order }
            .map { rule ->
                val title = replaceTemplateVars(rule.taskTitle, answers)
                val description = rule.description?.let { replaceTemplateVars(it, answers) }

                val dueAt = if (rule.estimatedWeeks > 0 && targetWeeksFromNow > 0) {
                    // Due date = now + (targetWeeks - estimatedWeeks) * msPerWeek
                    // i.e., this task should be done estimatedWeeks before the move
                    val weeksBeforeMove = (targetWeeksFromNow - rule.estimatedWeeks).coerceAtLeast(0)
                    now + weeksBeforeMove * msPerWeek
                } else null

                val links = rule.links?.map { Link(label = it.label, url = it.url) }

                Task(
                    title = title,
                    description = description,
                    category = rule.phase,
                    completed = false,
                    dueAt = dueAt,
                    countryId = countryId,
                    links = links
                )
            }
    }

    /**
     * Evaluate whether all conditions in a rule match the user's answers.
     * Empty conditions = always matches.
     * Condition value can be a single string, boolean, or list of strings (OR match).
     */
    private fun evaluateConditions(conditions: Map<String, Any>, answers: Map<String, Any>): Boolean {
        if (conditions.isEmpty()) return true

        for ((questionId, expected) in conditions) {
            val actual = answers[questionId] ?: return false

            val matches = when (expected) {
                is Boolean -> actual == expected
                is String -> actual.toString() == expected
                is List<*> -> expected.any { it.toString() == actual.toString() }
                else -> actual.toString() == expected.toString()
            }

            if (!matches) return false
        }
        return true
    }

    private fun replaceTemplateVars(text: String, answers: Map<String, Any>): String {
        var result = text
        for ((key, value) in answers) {
            result = result.replace("{$key}", value.toString())
        }
        return result
    }

    fun targetWeeksFromChoice(choice: String): Int = when (choice) {
        "asap" -> 12
        "3_months" -> 13
        "6_months" -> 26
        "1_year" -> 52
        else -> 26
    }

    // ---- JSON Parsing ----

    private fun parseTrack(obj: JSONObject): WizardTrack {
        val stepsArr = obj.getJSONArray("steps")
        val rulesArr = obj.getJSONArray("taskRules")

        return WizardTrack(
            displayName = obj.getString("displayName"),
            countryId = obj.getString("countryId"),
            shortName = obj.getString("shortName"),
            steps = (0 until stepsArr.length()).map { parseStep(stepsArr.getJSONObject(it)) },
            taskRules = (0 until rulesArr.length()).map { parseRule(rulesArr.getJSONObject(it)) }
        )
    }

    private fun parseStep(obj: JSONObject): WizardStep {
        val questionsArr = obj.getJSONArray("questions")
        return WizardStep(
            id = obj.getString("id"),
            title = obj.getString("title"),
            subtitle = obj.optString("subtitle", null),
            questions = (0 until questionsArr.length()).map { parseQuestion(questionsArr.getJSONObject(it)) }
        )
    }

    private fun parseQuestion(obj: JSONObject): WizardQuestion {
        val optionsArr = obj.optJSONArray("options")
        val showIfObj = obj.optJSONObject("showIf")

        return WizardQuestion(
            id = obj.getString("id"),
            type = obj.getString("type"),
            label = obj.getString("label"),
            options = optionsArr?.let { arr ->
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    QuestionOption(o.getString("id"), o.getString("label"))
                }
            },
            required = obj.optBoolean("required", false),
            hint = obj.optString("hint", null),
            showIf = showIfObj?.let { parseConditionMap(it) },
            min = if (obj.has("min")) obj.getInt("min") else null,
            max = if (obj.has("max")) obj.getInt("max") else null
        )
    }

    private fun parseRule(obj: JSONObject): TaskRule {
        val linksArr = obj.optJSONArray("links")
        val condObj = obj.getJSONObject("conditions")

        return TaskRule(
            taskTitle = obj.getString("taskTitle"),
            phase = obj.getString("phase"),
            description = obj.optString("description", null),
            links = linksArr?.let { arr ->
                (0 until arr.length()).map {
                    val l = arr.getJSONObject(it)
                    TaskRuleLink(l.getString("label"), l.getString("url"))
                }
            },
            conditions = parseConditionMap(condObj),
            estimatedWeeks = obj.optInt("estimatedWeeks", 0),
            order = obj.optInt("order", 0)
        )
    }

    private fun parseConditionMap(obj: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in obj.keys()) {
            val value = obj.get(key)
            map[key] = when (value) {
                is JSONArray -> (0 until value.length()).map { value.getString(it) }
                is Boolean -> value
                else -> value.toString()
            }
        }
        return map
    }
}
