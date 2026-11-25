package com.example.gothere.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.R

/**
 * Official government sources behind the visa and citizenship content, plus the
 * "not a government app" notice. Google Play's Misleading Claims policy requires
 * both for apps that present government information (rejection of 2026-10-31).
 * Keep in sync with the OFFICIAL SOURCES block in the Play store description
 * (every URL must load with HTTP 200 for a plain bot client from a US IP; checked 2026-11-25).
 */
object OfficialSources {
    val byCountry: Map<String, List<String>> = mapOf(
        "spain" to listOf("https://www.exteriores.gob.es/en/ServiciosAlCiudadano/Paginas/Conoce-Espana.aspx"),
        "canada" to listOf("https://ircc.canada.ca"),
        "portugal" to listOf(
            "https://www.gov.pt/guias/migrantes-vistos-e-autorizacoes-para-entrar-e-viver-em-portugal",
            "https://www.gov.pt/entidades/agencia-para-a-integracao-migracoes-e-asilo"
        ),
        "mexico" to listOf("https://www.gob.mx/inm"),
        "italy" to listOf(
            "https://vistoperitalia.esteri.it",
            "https://www.interno.gov.it/it/temi/cittadinanza-e-altri-diritti-civili/cittadinanza"
        ),
        "ireland" to listOf(
            "https://www.irishimmigration.ie",
            "https://www.irishstatutebook.ie/eli/1956/act/26/enacted/en/html"
        ),
        "germany" to listOf(
            "https://www.auswaertiges-amt.de/en",
            "https://www.bva.bund.de/EN/Services/Citizens/ID-Documents-Law/Citizenship/citizenship_node.html"
        ),
        "poland" to listOf(
            "https://www.gov.pl/web/udsc-en",
            "https://www.gov.pl/web/usa-en/confirming-polish-citizenship-or-its-loss"
        ),
        "hungary" to listOf("https://konzinfo.mfa.gov.hu/en"),
        "argentina" to listOf(
            "https://www.argentina.gob.ar/migraciones",
            "https://www.argentina.gob.ar/interior/renaper"
        ),
        "uk_ancestry" to listOf("https://www.gov.uk/ancestry-visa")
    )

    /** Apostilles (HCCH) and US veterans' care abroad (VA). */
    val us: List<String> = listOf(
        "https://www.hcch.net/en/instruments/conventions/specialised-sections/apostille",
        "https://www.va.gov/health-care/foreign-medical-program/"
    )
}

/** "www.gob.mx/inm" from "https://www.gob.mx/inm", short enough for one line. */
private fun displayUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removeSuffix("/")

/**
 * Notice card: GoThere is not a government entity, plus the official links for
 * [countryId]. Pass null to show the notice alone.
 */
@Composable
fun OfficialSourceNotice(countryId: String?, modifier: Modifier = Modifier) {
    val links = countryId?.let { OfficialSources.byCountry[it] }.orEmpty()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.gov_notice_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                stringResource(R.string.gov_notice_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (links.isNotEmpty()) {
                Text(
                    stringResource(R.string.gov_sources_for, stringResource(countryNameRes(countryId!!))),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                links.forEach { SourceLink(it) }
            }
        }
    }
}

/** Full list for Settings: the notice, every country, then the US-side sources. */
@Composable
fun OfficialSourcesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OfficialSourceNotice(countryId = null)
        OfficialSources.byCountry.forEach { (id, links) ->
            Text(
                stringResource(countryNameRes(id)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            links.forEach { SourceLink(it) }
        }
        Text(
            stringResource(R.string.gov_sources_us),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        OfficialSources.us.forEach { SourceLink(it) }
    }
}

@Composable
private fun SourceLink(url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            displayUrl(url),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
