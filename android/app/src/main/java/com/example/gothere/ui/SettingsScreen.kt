package com.example.gothere.ui

import com.example.gothere.R
import androidx.compose.ui.res.stringResource
import com.example.gothere.ui.theme.RaisedButton
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.util.AppearancePrefs
import com.example.gothere.util.CrossPromo
import com.example.gothere.util.PromoApp

/**
 * One place for plan, appearance, tools and account. Opened from the gear in the
 * top bar. Mirrors the iOS Settings sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearance: String,
    onAppearanceChange: (String) -> Unit,
    hasAllAccess: Boolean,
    accessReason: Int?,
    unlockedCount: Int,
    totalCountries: Int,
    onSeePlans: () -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenAI: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenVisaWizard: () -> Unit,
    onOpenAncestry: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    appVersion: String
) {
    val context = LocalContext.current
    var restoreNote by remember { mutableStateOf<String?>(null) }
    val restoreStartedMsg = stringResource(R.string.settings_restore_note)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Plan
        SectionTitle(stringResource(R.string.settings_your_plan))
        if (hasAllAccess) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_all_access), color = MaterialTheme.colorScheme.primary)
            }
            accessReason?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text(stringResource(R.string.settings_free_plan), style = MaterialTheme.typography.titleMedium)
            Text(
                pluralRes(R.plurals.settings_unlocked_count, totalCountries, unlockedCount, totalCountries),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RaisedButton(onClick = onSeePlans, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_see_plans))
            }
        }
        SettingsRow(stringResource(R.string.restore_purchases), Icons.Outlined.Refresh) {
            onRestorePurchases()
            restoreNote = restoreStartedMsg
        }
        restoreNote?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Appearance
        SectionTitle(stringResource(R.string.settings_appearance))
        val options = listOf(
            AppearancePrefs.SYSTEM to stringResource(R.string.appearance_system),
            AppearancePrefs.LIGHT to stringResource(R.string.appearance_light),
            AppearancePrefs.DARK to stringResource(R.string.appearance_dark)
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = appearance == value,
                    onClick = { onAppearanceChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) { Text(label) }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Language: AppCompat stores the choice and recreates the activity, and on
        // Android 13+ it is the same setting as system Settings > Apps > Language.
        SectionTitle(stringResource(R.string.settings_language))
        LanguagePicker()

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Tools
        SectionTitle(stringResource(R.string.settings_tools))
        SettingsRow(stringResource(R.string.tool_ai), Icons.Outlined.AutoAwesome, onOpenAI)
        SettingsRow(stringResource(R.string.tool_scan), Icons.Outlined.DocumentScanner, onOpenScan)
        SettingsRow(stringResource(R.string.tool_visa_wizard), Icons.Outlined.AutoAwesome, onOpenVisaWizard)
        SettingsRow(stringResource(R.string.tool_ancestry), Icons.Outlined.AccountTree, onOpenAncestry)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Account
        SectionTitle(stringResource(R.string.settings_account))
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
        TextButton(onClick = onDeleteAccount) {
            Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Our other apps
        SectionTitle(stringResource(R.string.settings_more_from_us))
        CrossPromoRow(PromoApp.LOCALISTA) {
            CrossPromo.open(context, PromoApp.LOCALISTA, CrossPromo.PLACEMENT_SETTINGS)
        }
        CrossPromoRow(PromoApp.PASSDGT) {
            CrossPromo.open(context, PromoApp.PASSDGT, CrossPromo.PLACEMENT_SETTINGS)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Official sources and the not-a-government notice (Play Misleading Claims policy)
        SectionTitle(stringResource(R.string.settings_official_sources))
        OfficialSourcesSection()

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // About
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://getgothere.app/privacy.html")))
        }) { Text(stringResource(R.string.privacy_policy)) }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:gabriel@getgothere.app")))
        }) { Text(stringResource(R.string.contact_support)) }
        Text(
            "GoThere $appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** System default / English / Español. Empty tag = follow the device language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker() {
    var current by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "")
    }
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "en" to "English",
        "es" to "Español"
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (tag, label) ->
            SegmentedButton(
                selected = current == tag,
                onClick = {
                    if (current != tag) {
                        current = tag
                        AppCompatDelegate.setApplicationLocales(
                            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                            else LocaleListCompat.forLanguageTags(tag)
                        )
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Two-line SettingsRow for a sister app: name plus one-line pitch. */
@Composable
private fun CrossPromoRow(app: PromoApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(app.iconRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(7.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(app.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(app.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
