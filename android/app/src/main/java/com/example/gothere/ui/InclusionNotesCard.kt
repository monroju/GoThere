package com.example.gothere.ui

import androidx.compose.ui.res.stringResource
import com.example.gothere.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.decision.PersonalConsideration
import com.example.gothere.repository.CountrySafetyProfiles

/**
 * Inclusion notes card surfaced in both DecisionTreeScreen results and
 * (optionally) ResourcesScreen. Renders 1-2 sentence safety/inclusion notes
 * per active persona for the selected country.
 *
 * Renders nothing when no personas + non-single-parent household → no UI noise
 * for users who didn't engage with the inclusivity prompts.
 */
@Composable
fun InclusionNotesCard(
    countryId: String,
    considerations: Set<PersonalConsideration>,
    isSingleParent: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes = CountrySafetyProfiles.notes(context, considerations, countryId)
    val singleParentNote = if (isSingleParent) {
        CountrySafetyProfiles.singleParentNote(context, countryId)
    } else null

    if (notes.isEmpty() && singleParentNote == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(R.string.incl_what_means),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            notes.forEach { (persona, note) ->
                Column {
                    Text(
                        text = personaLabel(persona),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(text = note, style = MaterialTheme.typography.bodySmall)
                }
            }
            singleParentNote?.let { note ->
                Column {
                    Text(
                        text = stringResource(R.string.incl_single_parent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(text = note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun personaLabel(c: PersonalConsideration): String = stringResource(
    when (c) {
        PersonalConsideration.LGBTQ          -> R.string.consideration_lgbtq
        PersonalConsideration.Trans          -> R.string.consideration_trans
        PersonalConsideration.Disabled       -> R.string.consideration_disabled
        PersonalConsideration.Veteran        -> R.string.consideration_veteran
        PersonalConsideration.Pregnant       -> R.string.consideration_pregnant
        PersonalConsideration.Neurodivergent -> R.string.consideration_neurodivergent
        PersonalConsideration.Senior         -> R.string.consideration_senior
        PersonalConsideration.Poc            -> R.string.consideration_poc
    }
)
