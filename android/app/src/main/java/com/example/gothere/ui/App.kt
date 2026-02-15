package com.example.gothere.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.example.gothere.ui.theme.GoThereTheme

/**
 * App root used by MainActivity and Previews.
 */
@Composable
fun GoThereApp() {
    GoThereTheme {
        Surface {
            AppNavHost()
        }
    }
}

/**
 * Minimal NavHost stub so previews and app run even if navigation isn’t wired yet.
 * Replace contents with your real NavHost when ready.
 */
@Composable
fun AppNavHost() {
    // TODO: Replace with your Navigation-Compose NavHost
    // For now, show a simple placeholder to keep previews compiling.
    PlaceholderScreen()
}

@Composable
private fun PlaceholderScreen() {
    // Simple composable using MaterialTheme colors/typography to validate theming
    androidx.compose.material3.Text(
        text = "GoThere placeholder",
        style = MaterialTheme.typography.titleMedium
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GoTherePreview() {
    GoThereTheme {
        Surface {
            // Put a root screen here if you want to preview a specific one,
            // e.g., LoginScreen(), HomeScreen(), etc. For now, show the NavHost stub.
            AppNavHost()
        }
    }
}
