package com.example.gothere.util

import com.example.gothere.ui.OfficialSources
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Google Play rejected the app on 2026-11-25 for a broken government source link
 * (Misleading Claims policy). These URLs failed for bot and US clients on that date
 * (TLS failure, incomplete cert chain, 503 maintenance, 404) and must not come back.
 */
class OfficialSourcesTest {

    private val deadUrls = listOf(
        "vistos.mne.gov.pt",
        "aima.gov.pt",
        "oif.gov.hu",
        "enterhungary.gov.hu",
        "Restoration-of-citizenship/restoration-of-citizenship_node.html"
    )

    private val countries = listOf(
        "spain", "canada", "portugal", "mexico", "italy", "ireland",
        "germany", "poland", "hungary", "argentina", "uk_ancestry"
    )

    @Test
    fun everyCountryHasAnHttpsSource() {
        countries.forEach { id ->
            val links = OfficialSources.byCountry[id].orEmpty()
            assertTrue("no official source for $id", links.isNotEmpty())
            links.forEach { assertTrue("not https: $it", it.startsWith("https://")) }
        }
        OfficialSources.us.forEach { assertTrue("not https: $it", it.startsWith("https://")) }
    }

    @Test
    fun officialSourcesHaveNoDeadLinks() {
        val all = OfficialSources.byCountry.values.flatten() + OfficialSources.us
        all.forEach { url ->
            deadUrls.forEach { dead -> assertTrue("dead source $dead in $url", !url.contains(dead)) }
        }
    }

    @Test
    fun appSourcesHaveNoDeadLinks() {
        val hits = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "json", "xml") }
            .flatMap { f -> deadUrls.filter { f.readText().contains(it) }.map { "${f.path}: $it" } }
            .toList()
        assertTrue("dead government links still in app: $hits", hits.isEmpty())
    }
}
