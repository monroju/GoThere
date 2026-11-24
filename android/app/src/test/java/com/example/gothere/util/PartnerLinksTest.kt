package com.example.gothere.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PartnerLinksTest {
    private val feed = """
        {"version":1,"partners":[
          {"id":"feather_es","countries":["spain"],"url":"https://feather-insurance.com/en-es/x?utm_source=a"},
          {"id":"feather_de","countries":["germany"],"url":"https://feather-insurance.com/de/x?utm_source=a"},
          {"id":"wise","countries":["*"],"url":"https://wise.prf.hn/click/x"},
          {"id":"bad","countries":["*"],"url":"http://insecure.example"}
        ]}
    """.trimIndent()

    @Test fun countrySpecificEntriesResolvePerCountry() {
        val map = PartnerLinks.parseFeed(feed)
        val feather = "https://feather-insurance.com/en-es/expat-health-insurance"
        assertEquals("https://feather-insurance.com/en-es/x?utm_source=a", PartnerLinks.resolve(map, feather, "spain"))
        assertEquals("https://feather-insurance.com/de/x?utm_source=a", PartnerLinks.resolve(map, feather, "germany"))
        assertEquals(feather, PartnerLinks.resolve(map, feather, "portugal"))
    }

    @Test fun globalEntryMatchesByHost() {
        val map = PartnerLinks.parseFeed(feed)
        assertEquals("https://wise.prf.hn/click/x", PartnerLinks.resolve(map, "https://wise.com", "canada"))
        assertEquals("https://www.sanitas.es", PartnerLinks.resolve(map, "https://www.sanitas.es", "spain"))
    }

    @Test fun insecureUrlsAreDropped() {
        assertEquals(null, PartnerLinks.parseFeed(feed)["bad"])
    }
}
