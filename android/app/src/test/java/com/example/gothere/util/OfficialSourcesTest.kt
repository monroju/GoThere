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
        "Restoration-of-citizenship/restoration-of-citizenship_node.html",
        // GoThere#10 (2026-11-25): 404/410 pages, gone domains, TLS failures.
        "employment-social-affairs.ec.europa.eu/policies-and-activities/social-protection-social-inclusion/persons-disabilities/european-disability-card_en",
        "vistoperitalia.esteri.it/home/dettagliovisto",
        "agenziaentrate.gov.it/portale/codice-fiscale-tessera-sanitaria",
        "argentina.gob.ar/interior/renaper/dni",
        "cancilleria.gob.ar/en/foreign-policy/embassies-and-consulates",
        "cancilleria.gob.ar/en/services/argentinians-abroad",
        "esteri.it/en/ministero/laministero/lerappresentanze",
        "esteri.it/en/servizi-consolari-e-visti/italiani-all-estero/aire",
        "esteri.it/en/servizi-consolari-e-visti/visti-per-litalia",
        "\"https://www.exteriores.gob.es/Consulados/\"",
        "/InformacionParaExtranjeros/Paginas/Estudios.aspx",
        "/InformacionParaExtranjeros/Paginas/Visados.aspx",
        "germany.info/us-en/service/passport",
        "gov.ie/en/service/civil-registration-service",
        "irishimmigration.ie/coming-to-join-family-in-ireland/stamp-0",
        "nidirect.gov.uk/articles/general-register-office",
        "www2.hse.ie/services/maternity-services",
        "buenosaires.gob.ar/registrocivil",
        "berlin.de/en/visa/visa-categories/freelance-self-employment",
        "cbi.gov.gd",
        "nemzetikartya.gov.hu",
        "nyilvantarto.hu",
        "sanidad.gob.es",
        "chlc.min-saude.pt",
        "hjmatos.min-saude.pt",
        "hff.min-saude.pt",
        "chualgarve.min-saude.pt",
        "chbv.min-saude.pt",
        "bicsp.min-saude.pt",
        "inclusion.gob.es/web/migraciones/w/",
        "permisos-de-conducir/canje-de-permisos/",
        "benefits.va.gov/persona/veteran-livingabroad.asp"
    )

    /** Claims that were true once and are wrong now. They must not come back. */
    private val staleClaims = listOf(
        // EU Disability Card: Directive 2024/2841, cards issued from 5 June 2028.
        "EU Disability Card recognised",
        "Mutual recognition across EU member states",
        // Portugal nationality law (Lei Organica 1/2026): 10 years, 7 for EU/CPLP, from 19 May 2026.
        "Citizenship after 5 years applies to the whole family"
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

    @Test
    fun appHasNoStaleClaims() {
        val hits = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "json", "xml", "html") }
            .flatMap { f -> staleClaims.filter { f.readText().contains(it) }.map { "${f.path}: $it" } }
            .toList()
        assertTrue("outdated claims still in app: $hits", hits.isEmpty())
    }

    @Test
    fun govLinksUseHttps() {
        val plainHttpGov = Regex("""http://[^"\s]*\.(gov|gob|min-saude)\.[a-z.]*[^"\s]*""")
        val hits = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "json", "xml") }
            .flatMap { f -> plainHttpGov.findAll(f.readText()).map { "${f.path}: ${it.value}" } }
            .toList()
        assertTrue("government links must use https: $hits", hits.isEmpty())
    }
}
