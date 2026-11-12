package com.example.gothere.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpFeedParserTest {

    private val rainbow = """
        {
          "id": "rainbow-railroad",
          "name": "Rainbow Railroad",
          "blurb": "Helps LGBTQI+ people at risk find a way to safety.",
          "url": "https://www.rainbowrailroad.org",
          "audiences": ["lgbtq"],
          "apps": ["gothere", "localista"],
          "countries": ["*"]
        }
    """.trimIndent()

    private val localistaOnly = """
        {"id": "x", "name": "Local Only", "blurb": "b", "url": "https://x.org", "apps": ["localista"]}
    """.trimIndent()

    private fun feed(vararg orgs: String) = """{"version":1,"updated":"2026-11-12","orgs":[${orgs.joinToString(",")}]}"""

    @Test
    fun parsesSpecExample() {
        val orgs = HelpFeedParser.parse(feed(rainbow))!!
        assertEquals(1, orgs.size)
        val o = orgs[0]
        assertEquals("rainbow-railroad", o.id)
        assertEquals("Rainbow Railroad", o.name)
        assertEquals("https://www.rainbowrailroad.org", o.url)
        assertEquals(listOf("lgbtq"), o.audiences)
        assertEquals(listOf("gothere", "localista"), o.apps)
        assertEquals(listOf("*"), o.countries)
    }

    @Test
    fun emptyListHidesEntry() {
        val r = HelpFeedParser.resolve(feed(), cached = null, appId = "gothere")
        assertTrue(r.orgs.isEmpty())
        assertTrue("valid empty feed still replaces the cache", r.cacheable)
    }

    @Test
    fun filtersByApp() {
        val orgs = HelpFeedParser.parse(feed(rainbow, localistaOnly))!!
        assertEquals(listOf("rainbow-railroad"), HelpFeedParser.forApp(orgs, "gothere").map { it.id })
        assertEquals(2, HelpFeedParser.forApp(orgs, "localista").size)
    }

    @Test
    fun entryWithoutAppsIsNotShown() {
        val noApps = """{"id":"n","name":"N","url":"https://n.org"}"""
        val r = HelpFeedParser.resolve(feed(noApps), null, "gothere")
        assertTrue(r.orgs.isEmpty())
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val extra = """
            {"version":2,"newTopLevel":{"a":1},"orgs":[
              {"id":"a","name":"A","url":"https://a.org","apps":["gothere"],"logo":"https://a.org/l.png","priority":5,"tags":[1,2]}
            ]}
        """.trimIndent()
        val r = HelpFeedParser.resolve(extra, null, "gothere")
        assertEquals(1, r.orgs.size)
        assertEquals("", r.orgs[0].blurb)
    }

    @Test
    fun dropsEntriesMissingRequiredFieldsOrNonHttpsUrl() {
        val bad = listOf(
            """{"name":"No id","url":"https://a.org","apps":["gothere"]}""",
            """{"id":"b","url":"https://b.org","apps":["gothere"]}""",
            """{"id":"c","name":"C","url":"http://c.org","apps":["gothere"]}""",
            """{"id":"d","name":"D","url":"javascript:alert(1)","apps":["gothere"]}""",
            "42"
        )
        val orgs = HelpFeedParser.parse(feed(*(bad + rainbow).toTypedArray()))!!
        assertEquals(listOf("rainbow-railroad"), orgs.map { it.id })
    }

    @Test
    fun malformedJsonFallsBackToCache() {
        val cached = feed(rainbow)
        for (network in listOf("<html>502</html>", "{\"orgs\":", "", "[]", "{\"version\":1}")) {
            val r = HelpFeedParser.resolve(network, cached, "gothere")
            assertEquals("network=$network", listOf("rainbow-railroad"), r.orgs.map { it.id })
            assertFalse("malformed body must not overwrite the cache", r.cacheable)
        }
    }

    @Test
    fun offlineUsesCache() {
        val r = HelpFeedParser.resolve(null, feed(rainbow), "gothere")
        assertEquals(1, r.orgs.size)
        assertFalse(r.cacheable)
    }

    @Test
    fun networkWinsOverCache() {
        val r = HelpFeedParser.resolve(feed(), feed(rainbow), "gothere")
        assertTrue("an empty live feed hides orgs that were withdrawn", r.orgs.isEmpty())
        assertTrue(r.cacheable)
    }

    @Test
    fun malformedEverywhereGivesEmpty() {
        assertNull(HelpFeedParser.parse("nope"))
        val r = HelpFeedParser.resolve("nope", "also nope", "gothere")
        assertTrue(r.orgs.isEmpty())
        assertFalse(r.cacheable)
    }
}
