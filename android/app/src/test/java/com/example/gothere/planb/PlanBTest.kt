package com.example.gothere.planb

import com.example.gothere.notify.FcmTopicManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Score math must match shared/data/planb_score.py via the copied vectors. */
class PlanBScoreTest {
    private fun vectors(): JSONObject =
        JSONObject(javaClass.classLoader!!.getResource("planb_score_vectors.json")!!.readText())

    private fun path(o: JSONObject): PlanBPath {
        val items = o.optJSONArray("items")
        val visa = o.optJSONObject("visa")
        return PlanBPath(
            id = "t", type = o.optString("type", "descent"), countryCode = "xx", pathwayId = "xx_test",
            createdAt = "", planMode = "planB", lineage = emptyList(),
            items = (0 until (items?.length() ?: 0)).map { i ->
                val it = items!!.getJSONObject(i)
                PlanBItem(id = "$i", personRole = "self", kind = "birth_cert",
                    state = PlanBDocState.from(it.optString("state")), skipped = it.optBoolean("skipped", false))
            },
            filedOn = if (o.has("filedOn")) o.getString("filedOn") else null,
            appointmentOn = if (o.has("appointmentOn")) o.getString("appointmentOn") else null,
            incomeFit = visa?.let { if (it.isNull("incomeFit")) null else it.optDouble("incomeFit") },
            savingsGoalSet = visa?.optBoolean("savingsGoalSet", false) ?: false,
        )
    }

    @Test
    fun pathScoresMatchSharedVectors() {
        val cases = vectors().getJSONArray("paths")
        assertTrue(cases.length() > 10)
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            assertEquals(c.getString("name"), c.getInt("score"), PlanBScore.path(path(c.getJSONObject("path"))))
        }
    }

    @Test
    fun overallScoresMatchSharedVectors() {
        val cases = vectors().getJSONArray("overall")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val ps = c.getJSONArray("paths").let { a -> (0 until a.length()).map { path(a.getJSONObject(it)) } }
            assertEquals(c.getString("name"), c.getInt("score"), PlanBScore.overall(ps))
        }
    }

    @Test
    fun buckets() {
        assertEquals(listOf("0", "1-24", "25-49", "50-74", "75-99", "100"),
            listOf(0, 1, 30, 60, 90, 100).map(PlanBScore::bucket))
    }
}

class DescentChainTest {
    // Unit tests run with the module directory as working directory.
    private val bundle = DescentChainRepository.parse(File("src/main/assets/descent_chains.json").readText())

    @Test
    fun bundledChainsLoad() {
        assertEquals(setOf("ie_foreign_births_register", "it_jure_sanguinis", "de_stag",
            "pl_citizenship_confirmation", "hu_simplified_naturalization"), bundle.chains.keys)
        assertTrue(bundle.disclaimer.isNotEmpty())
    }

    @Test
    fun everyCheckerPathHasAChain() {
        val ancestry = JSONObject(File("src/main/assets/ancestry_paths.json").readText()).getJSONArray("paths")
        for (i in 0 until ancestry.length()) {
            val id = ancestry.getJSONObject(i).getString("id")
            assertNotNull("no Plan B chain for $id", bundle.chainForAncestryPath(id))
        }
    }

    @Test
    fun everyOrderHintResolves() {
        for (chain in bundle.chains.values) for (doc in chain.documents) {
            assertEquals("${chain.pathwayId}/${doc.kind}", doc.orderHintIds.size, bundle.hints(chain, doc.orderHintIds).size)
        }
    }

    @Test
    fun builderDropsRolesAboveAncestor() {
        val chain = bundle.chains.getValue("ie_foreign_births_register")
        assertFalse(PlanBChainBuilder.items(chain, PlanBRole.PARENT).any { it.personRole == "grandparent" })
        val items = PlanBChainBuilder.items(chain, PlanBRole.GRANDPARENT)
        assertTrue(items.any { it.personRole == "grandparent" })
        assertEquals(items.size, items.map { it.id }.toSet().size)
        items.forEach { assertNotNull(it.id, PlanBChainBuilder.document(it, chain)) }
    }

    @Test
    fun ancestorChoicesRespectMaxRole() {
        assertEquals(listOf(PlanBRole.PARENT, PlanBRole.GRANDPARENT),
            bundle.chains.getValue("it_jure_sanguinis").ancestorChoices)
    }

    @Test
    fun pathJsonRoundTrips() {
        val chain = bundle.chains.getValue("ie_foreign_births_register")
        val p = PlanBPath(id = "a", type = "descent", countryCode = "ie", pathwayId = chain.pathwayId,
            createdAt = "2026-11-07T10:00:00Z", planMode = "planB",
            lineage = PlanBChainBuilder.lineage(PlanBRole.GRANDPARENT),
            items = PlanBChainBuilder.items(chain, PlanBRole.GRANDPARENT).mapIndexed { i, it ->
                if (i == 0) it.copy(state = PlanBDocState.ORDERED, orderedOn = "2026-11-07", skipped = true) else it
            },
            filedOn = "2026-12-01")
        assertEquals(p, PlanBPath.fromJson(JSONObject(p.toJson().toString())))
    }
}

class PlanBRuleTopicTest {
    @Test
    fun freeDestinationOnlyWithoutPlanB() {
        assertEquals(listOf("rules_es"), FcmTopicManager.desiredRuleTopics("spain", emptyList(), false))
    }

    @Test
    fun freeFirstAlertablePlanBCountryReplacesDestination() {
        assertEquals(listOf("rules_ie"), FcmTopicManager.desiredRuleTopics("spain", listOf("de", "ie", "it"), false))
    }

    @Test
    fun allAccessDestinationPlusEveryPathNoDuplicates() {
        assertEquals(listOf("rules_ie", "rules_it"),
            FcmTopicManager.desiredRuleTopics("ireland", listOf("ie", "it"), true))
    }
}
