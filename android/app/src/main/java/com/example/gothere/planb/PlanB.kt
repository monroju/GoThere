package com.example.gothere.planb

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale
import kotlin.math.floor

/*
 * Plan B (growth/PLAN_B_SPEC.md). Same data and JSON shape as iOS:
 *  - descent_chains.json is built in the parent repo by shared/data/descent_chains.py.
 *  - PlanBPath matches shared/data/schema/planb_path.schema.json and is stored on device only.
 *  - PlanBScore is a port of shared/data/planb_score.py and must pass its shared vectors.
 */

enum class PlanBRole(val raw: String, val depth: Int) {
    ME("self", 0), PARENT("parent", 1), GRANDPARENT("grandparent", 2), GGP("ggp", 3);

    companion object {
        fun from(raw: String): PlanBRole? = entries.firstOrNull { it.raw == raw }
    }
}

enum class PlanBDocState(val raw: String) {
    NOT_STARTED("not_started"), ORDERED("ordered"), RECEIVED("received"),
    APOSTILLED("apostilled"), TRANSLATED("translated"), DONE("done");

    companion object {
        fun from(raw: String?): PlanBDocState = entries.firstOrNull { it.raw == raw } ?: NOT_STARTED
    }
}

val PLANB_STAGES = listOf("gather", "apply", "wait", "decision", "passport")

// MARK: Descent chain data (read only)

data class ChainSource(val title: String, val url: String, val type: String)

data class ChainLineage(val maxRole: PlanBRole, val note: String?, val noteEs: String?)

data class ChainDocument(
    val kind: String,
    val label: String,
    val labelEs: String,
    val appliesTo: List<String>,
    val condition: String?,
    val conditionEs: String?,
    val needsApostille: Boolean?,
    val needsTranslation: Boolean?,
    val orderHintIds: List<String>,
)

data class ChainStage(
    val id: String,
    val label: String,
    val labelEs: String,
    val lowMonths: Double?,
    val highMonths: Double?,
)

data class OrderHint(
    val id: String,
    val label: String,
    val labelEs: String,
    val howTo: String?,
    val howToEs: String?,
    val url: String?,
    val fee: String?,
    val lastVerified: String?,
)

data class DescentChain(
    val pathwayId: String,
    val ancestryPathId: String,
    val countryCode: String,
    val lastVerified: String?,
    val sources: List<ChainSource>,
    val lineage: ChainLineage,
    val documents: List<ChainDocument>,
    val orderHints: List<OrderHint>,
    val stages: List<ChainStage>,
) {
    /** Generations the citizen ancestor can sit at, from parent up to maxRole. */
    val ancestorChoices: List<PlanBRole>
        get() = PlanBRole.entries.filter { it != PlanBRole.ME && it.depth <= lineage.maxRole.depth }
}

data class DescentChainBundle(
    val disclaimer: String,
    val disclaimerEs: String,
    val usOrderHints: List<OrderHint>,
    val chains: Map<String, DescentChain>,
) {
    fun chainForAncestryPath(ancestryPathId: String): DescentChain? =
        chains.values.firstOrNull { it.ancestryPathId == ancestryPathId }

    /** Country hints first, then the shared US hints. */
    fun hints(chain: DescentChain, ids: List<String>): List<OrderHint> {
        val all = chain.orderHints + usOrderHints
        return ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
}

/** Spanish copy of a data string when the device runs in Spanish. */
object DataLanguage {
    fun isSpanish(): Boolean = Locale.getDefault().language == "es"
    fun pick(en: String, es: String?): String = if (isSpanish() && !es.isNullOrEmpty()) es else en
}

object DescentChainRepository {
    @Volatile private var cached: DescentChainBundle? = null

    fun load(context: Context): DescentChainBundle? {
        cached?.let { return it }
        val text = runCatching {
            context.assets.open("descent_chains.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return null
        return runCatching { parse(text) }.getOrNull()?.also { cached = it }
    }

    fun parse(text: String): DescentChainBundle {
        val root = JSONObject(text)
        val chainsObj = root.getJSONObject("chains")
        val chains = chainsObj.keys().asSequence().associateWith { parseChain(chainsObj.getJSONObject(it)) }
        return DescentChainBundle(
            disclaimer = root.optString("disclaimer"),
            disclaimerEs = root.optString("disclaimerEs"),
            usOrderHints = root.optJSONArray("usOrderHints").objects().map(::parseHint),
            chains = chains,
        )
    }

    private fun parseChain(o: JSONObject): DescentChain {
        val lin = o.getJSONObject("lineage")
        return DescentChain(
            pathwayId = o.getString("pathwayId"),
            ancestryPathId = o.getString("ancestryPathId"),
            countryCode = o.getString("countryCode"),
            lastVerified = o.str("lastVerified"),
            sources = o.optJSONArray("sources").objects().map {
                ChainSource(it.getString("title"), it.getString("url"), it.getString("type"))
            },
            lineage = ChainLineage(
                maxRole = PlanBRole.from(lin.getString("maxRole")) ?: PlanBRole.GRANDPARENT,
                note = lin.str("note"),
                noteEs = lin.str("noteEs"),
            ),
            documents = o.getJSONArray("documents").objects().map { d ->
                ChainDocument(
                    kind = d.getString("kind"),
                    label = d.getString("label"),
                    labelEs = d.getString("labelEs"),
                    appliesTo = d.getJSONArray("appliesTo").strings(),
                    condition = d.str("condition"),
                    conditionEs = d.str("conditionEs"),
                    needsApostille = d.bool("needsApostille"),
                    needsTranslation = d.bool("needsTranslation"),
                    orderHintIds = d.optJSONArray("orderHintIds").strings(),
                )
            },
            orderHints = o.optJSONArray("orderHints").objects().map(::parseHint),
            stages = o.getJSONArray("stages").objects().map { s ->
                val m = s.optJSONObject("typicalMonths")
                ChainStage(
                    id = s.getString("id"),
                    label = s.getString("label"),
                    labelEs = s.getString("labelEs"),
                    lowMonths = m?.optDouble("low"),
                    highMonths = m?.optDouble("high"),
                )
            },
        )
    }

    private fun parseHint(h: JSONObject) = OrderHint(
        id = h.getString("id"),
        label = h.getString("label"),
        labelEs = h.getString("labelEs"),
        howTo = h.str("howTo"),
        howToEs = h.str("howToEs"),
        url = h.str("url"),
        fee = h.str("fee"),
        lastVerified = h.str("lastVerified"),
    )
}

// MARK: Tracked path (stored on device)

data class PlanBPerson(val role: PlanBRole, val name: String? = null, val born: String? = null)

data class PlanBItem(
    val id: String,
    val personRole: String,
    val kind: String,
    val state: PlanBDocState = PlanBDocState.NOT_STARTED,
    val orderedOn: String? = null,
    val expectedDays: Int? = null,
    val documentId: String? = null,
    val skipped: Boolean = false,
)

data class PlanBPath(
    val id: String,
    val type: String,
    val countryCode: String,
    val pathwayId: String,
    val createdAt: String,
    val planMode: String,
    val lineage: List<PlanBPerson>,
    val items: List<PlanBItem>,
    val stage: String = "gather",
    val filedOn: String? = null,
    val appointmentOn: String? = null,
    val alertsOn: Boolean = true,
    val incomeFit: Double? = null,
    val savingsGoalSet: Boolean = false,
) {
    val activeItems: List<PlanBItem> get() = items.filter { !it.skipped }
    val doneCount: Int get() = activeItems.count { it.state == PlanBDocState.DONE }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("type", type); put("countryCode", countryCode); put("pathwayId", pathwayId)
        put("createdAt", createdAt); put("planMode", planMode); put("stage", stage)
        put("lineage", JSONArray(lineage.map { p ->
            JSONObject().apply { put("role", p.role.raw); p.name?.let { put("name", it) }; p.born?.let { put("born", it) } }
        }))
        put("items", JSONArray(items.map { i ->
            JSONObject().apply {
                put("id", i.id); put("personRole", i.personRole); put("kind", i.kind); put("state", i.state.raw)
                i.orderedOn?.let { put("orderedOn", it) }
                i.expectedDays?.let { put("expectedDays", it) }
                i.documentId?.let { put("documentId", it) }
                if (i.skipped) put("skipped", true)
            }
        }))
        filedOn?.let { put("filedOn", it) }
        appointmentOn?.let { put("appointmentOn", it) }
        put("alertsOn", alertsOn)
        if (type == "visa") put("visa", JSONObject().apply {
            incomeFit?.let { put("incomeFit", it) }; put("savingsGoalSet", savingsGoalSet)
        })
    }

    companion object {
        fun fromJson(o: JSONObject): PlanBPath {
            val visa = o.optJSONObject("visa")
            return PlanBPath(
                id = o.optString("id"),
                type = o.optString("type", "descent"),
                countryCode = o.optString("countryCode"),
                pathwayId = o.optString("pathwayId"),
                createdAt = o.optString("createdAt"),
                planMode = o.optString("planMode", "planB"),
                lineage = o.optJSONArray("lineage").objects().mapNotNull { p ->
                    PlanBRole.from(p.optString("role"))?.let { PlanBPerson(it, p.str("name"), p.str("born")) }
                },
                items = o.optJSONArray("items").objects().map { i ->
                    PlanBItem(
                        id = i.optString("id"),
                        personRole = i.optString("personRole"),
                        kind = i.optString("kind"),
                        state = PlanBDocState.from(i.str("state")),
                        orderedOn = i.str("orderedOn"),
                        expectedDays = if (i.has("expectedDays") && !i.isNull("expectedDays")) i.getInt("expectedDays") else null,
                        documentId = i.str("documentId"),
                        skipped = i.optBoolean("skipped", false),
                    )
                },
                stage = o.optString("stage", "gather"),
                filedOn = o.str("filedOn"),
                appointmentOn = o.str("appointmentOn"),
                alertsOn = o.optBoolean("alertsOn", true),
                incomeFit = visa?.let { if (it.has("incomeFit") && !it.isNull("incomeFit")) it.getDouble("incomeFit") else null },
                savingsGoalSet = visa?.optBoolean("savingsGoalSet", false) ?: false,
            )
        }
    }
}

/** Readiness score (growth item F15). Port of shared/data/planb_score.py. */
object PlanBScore {
    fun path(p: PlanBPath): Int {
        val active = p.activeItems
        val ratio = if (active.isEmpty()) 0.0 else p.doneCount.toDouble() / active.size
        val raw = if (p.type == "visa") {
            val fit = (p.incomeFit ?: 0.0).coerceIn(0.0, 1.0)
            30 * fit + 50 * ratio + (if (p.savingsGoalSet) 20 else 0)
        } else {
            70 * ratio + (if (p.filedOn != null) 20 else 0) + (if (p.appointmentOn != null) 10 else 0)
        }
        return floor(raw + 0.5).toInt().coerceIn(0, 100)
    }

    fun overall(paths: List<PlanBPath>): Int = paths.maxOfOrNull(::path) ?: 0

    /** Analytics bucket for planb_score_viewed; the raw score is never sent. */
    fun bucket(score: Int): String = when {
        score <= 0 -> "0"
        score < 25 -> "1-24"
        score < 50 -> "25-49"
        score < 75 -> "50-74"
        score < 100 -> "75-99"
        else -> "100"
    }
}

object PlanBChainBuilder {
    fun lineage(upTo: PlanBRole): List<PlanBPerson> =
        PlanBRole.entries.filter { it.depth <= upTo.depth }.map { PlanBPerson(it) }

    /** One item per document per person it applies to; roles above the ancestor are dropped. */
    fun items(chain: DescentChain, ancestor: PlanBRole): List<PlanBItem> {
        val out = mutableListOf<PlanBItem>()
        val seen = mutableSetOf<String>()
        for (doc in chain.documents) {
            for (role in doc.appliesTo) {
                if (role != "ancestor") {
                    val r = PlanBRole.from(role) ?: continue
                    if (r.depth > ancestor.depth) continue
                }
                val id = "${doc.kind}|$role|${doc.condition ?: ""}"
                if (!seen.add(id)) continue
                out += PlanBItem(id = id, personRole = role, kind = doc.kind)
            }
        }
        return out
    }

    fun document(item: PlanBItem, chain: DescentChain): ChainDocument? {
        val condition = item.id.split("|").getOrNull(2) ?: ""
        return chain.documents.firstOrNull {
            it.kind == item.kind && item.personRole in it.appliesTo && (it.condition ?: "") == condition
        }
    }
}

fun todayIso(): String = LocalDate.now().toString()

// MARK: org.json helpers

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

private fun JSONObject.str(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONObject.bool(key: String): Boolean? =
    if (has(key) && !isNull(key)) getBoolean(key) else null
