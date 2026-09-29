package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.util.AppearancePrefs

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
    accessReason: String?,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Plan
        SectionTitle("Your plan")
        if (hasAllAccess) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("All Access: every country unlocked", color = MaterialTheme.colorScheme.primary)
            }
            accessReason?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("Free plan", style = MaterialTheme.typography.titleMedium)
            Text(
                "$unlockedCount of $totalCountries countries unlocked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSeePlans, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("See plans and bundles")
            }
        }
        SettingsRow("Restore Purchases", Icons.Outlined.Refresh) {
            onRestorePurchases()
            restoreNote = "Restore started. Anything bought with this Google account will unlock."
        }
        restoreNote?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Appearance
        SectionTitle("Appearance")
        val options = listOf(
            AppearancePrefs.SYSTEM to "System",
            AppearancePrefs.LIGHT to "Light",
            AppearancePrefs.DARK to "Dark"
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

        // Tools
        SectionTitle("Tools")
        SettingsRow("Where to start (AI)", Icons.Outlined.AutoAwesome, onOpenAI)
        SettingsRow("Scan a Document", Icons.Outlined.DocumentScanner, onOpenScan)
        SettingsRow("Visa Wizard", Icons.Outlined.AutoAwesome, onOpenVisaWizard)
        SettingsRow("Ancestry Citizenship", Icons.Outlined.AccountTree, onOpenAncestry)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Account
        SectionTitle("Account")
        TextButton(onClick = onSignOut) { Text("Sign Out") }
        TextButton(onClick = onDeleteAccount) {
            Text("Delete Account", color = MaterialTheme.colorScheme.error)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // About
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://getgothere.app/privacy.html")))
        }) { Text("Privacy Policy") }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:gabriel@getgothere.app")))
        }) { Text("Contact Support") }
        Text(
            "GoThere $appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
