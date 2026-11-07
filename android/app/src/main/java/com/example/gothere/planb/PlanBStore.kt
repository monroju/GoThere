package com.example.gothere.planb

import android.content.Context
import android.content.SharedPreferences
import com.example.gothere.notify.FcmTopicManager
import com.example.gothere.util.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.time.Instant
import java.util.UUID

/**
 * Plan B tracked paths, stored on device only (SharedPreferences JSON). Names and dates
 * never leave the phone; analytics carry country, kind and state only. Mirror of iOS PlanBStore.
 */
class PlanBStore private constructor(
    private val prefs: SharedPreferences,
    private val onChange: (List<PlanBPath>) -> Unit,
) {
    private val _paths = MutableStateFlow(load())
    val paths: StateFlow<List<PlanBPath>> = _paths.asStateFlow()

    val isPlanBMode: Boolean get() = prefs.getString(KEY_PLAN_MODE, null) == "planB"

    fun selectPlanBIntent(source: String) {
        if (isPlanBMode) return
        prefs.edit().putString(KEY_PLAN_MODE, "planB").apply()
        Analytics.log(Analytics.PLANB_INTENT_SELECTED, mapOf("source" to source))
    }

    val overallScore: Int get() = PlanBScore.overall(_paths.value)

    fun isTracking(pathwayId: String): Boolean = _paths.value.any { it.pathwayId == pathwayId }

    fun trackDescent(chain: DescentChain, ancestor: PlanBRole): PlanBPath {
        _paths.value.firstOrNull { it.pathwayId == chain.pathwayId }?.let { return it }
        val path = PlanBPath(
            id = UUID.randomUUID().toString(),
            type = "descent",
            countryCode = chain.countryCode,
            pathwayId = chain.pathwayId,
            createdAt = Instant.now().toString(),
            planMode = if (isPlanBMode) "planB" else "move",
            lineage = PlanBChainBuilder.lineage(ancestor),
            items = PlanBChainBuilder.items(chain, ancestor),
        )
        save(_paths.value + path)
        Analytics.log(Analytics.PLANB_PATH_TRACKED, mapOf("cc" to chain.countryCode, "type" to "descent",
            "ancestor" to ancestor.raw))
        return path
    }

    fun remove(pathId: String) = save(_paths.value.filterNot { it.id == pathId })

    fun setState(pathId: String, itemId: String, state: PlanBDocState) = update(pathId) { p ->
        Analytics.log(Analytics.PLANB_DOC_STATE, mapOf("cc" to p.countryCode,
            "kind" to p.items.firstOrNull { it.id == itemId }?.kind, "state" to state.raw))
        p.copy(items = p.items.map {
            if (it.id != itemId) it
            else it.copy(state = state,
                orderedOn = if (state == PlanBDocState.ORDERED && it.orderedOn == null) todayIso() else it.orderedOn)
        })
    }

    fun setSkipped(pathId: String, itemId: String, skipped: Boolean) = update(pathId) { p ->
        p.copy(items = p.items.map { if (it.id == itemId) it.copy(skipped = skipped) else it })
    }

    fun setStage(pathId: String, stage: String) = update(pathId) { it.copy(stage = stage) }
    fun setFiled(pathId: String, date: String?) = update(pathId) { it.copy(filedOn = date) }
    fun setAppointment(pathId: String, date: String?) = update(pathId) { it.copy(appointmentOn = date) }
    fun setAlerts(pathId: String, on: Boolean) = update(pathId) { it.copy(alertsOn = on) }

    /** Two-letter codes of tracked paths that want rule alerts, oldest first. */
    val alertCountryCodes: List<String> get() = _paths.value.filter { it.alertsOn }.map { it.countryCode }

    private fun update(pathId: String, change: (PlanBPath) -> PlanBPath) {
        save(_paths.value.map { if (it.id == pathId) change(it) else it })
    }

    private fun save(paths: List<PlanBPath>) {
        _paths.value = paths
        prefs.edit().putString(KEY_PATHS, JSONArray(paths.map { it.toJson() }).toString()).apply()
        onChange(paths)
    }

    private fun load(): List<PlanBPath> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_PATHS, "[]"))
        (0 until arr.length()).map { PlanBPath.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    companion object {
        const val PREFS = "planb_prefs"
        const val KEY_PATHS = "paths_v1"
        const val KEY_PLAN_MODE = "plan_mode"

        @Volatile private var instance: PlanBStore? = null

        fun get(context: Context): PlanBStore = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                PlanBStore(app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)) {
                    FcmTopicManager.syncRuleAlertTopics(app)
                }.also { instance = it }
            }
        }

        /** For tests: a store over any SharedPreferences, with no FCM side effects. */
        fun forTesting(prefs: SharedPreferences): PlanBStore = PlanBStore(prefs) {}
    }
}
