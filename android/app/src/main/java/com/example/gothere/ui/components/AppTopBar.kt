package com.example.gothere.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gothere.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        // 🔹 Centered logo as the title
        title = {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "GoThere logo",
                modifier = Modifier.size(32.dp)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.primary,
            titleContentColor = colors.onPrimary,
            navigationIconContentColor = colors.onPrimary,
            actionIconContentColor = colors.onPrimary
        ),
        // no left icon, we want the logo visually centered
        navigationIcon = {},
        actions = {
            // Theme toggle
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }

            // Settings / overflow with logout
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open menu"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Log out") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Log out"
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onLogout()
                    }
                )
            }
        }
    )
}
