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
 * Keep in sync with the OFFICIAL SOURCES block in the Play store description.
 */
object OfficialSources {
    val byCountry: Map<String, List<String>> = mapOf(
        "spain" to listOf("https://www.exteriores.gob.es", "https://www.inclusion.gob.es/web/migraciones"),
        "canada" to listOf("https://www.canada.ca/en/immigration-refugees-citizenship.html"),
        "portugal" to listOf("https://vistos.mne.gov.pt", "https://aima.gov.pt"),
        "mexico" to listOf("https://www.gob.mx/inm"),
        "italy" to listOf("https://vistoperitalia.esteri.it", "https://www.esteri.it"),
        "ireland" to listOf("https://www.irishimmigration.ie"),
        "germany" to listOf("https://www.auswaertiges-amt.de/en"),
        "poland" to listOf("https://www.gov.pl/web/udsc-en"),
        "hungary" to listOf("https://enterhungary.gov.hu"),
        "argentina" to listOf("https://www.argentina.gob.ar/interior/migraciones"),
        "uk_ancestry" to listOf("https://www.gov.uk/browse/visas-immigration")
    )

    /** US-side steps the task tracker covers (FBI check, SSA abroad, VA care). */
    val us: List<String> = listOf(
        "https://www.fbi.gov/how-we-can-help-you/more-fbi-services-and-information/identity-history-summary-checks",
        "https://www.ssa.gov/international/",
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
