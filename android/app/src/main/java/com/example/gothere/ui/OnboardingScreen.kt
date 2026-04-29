package com.example.gothere.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gothere.R

private data class OnboardingPage(
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        title = "Welcome to GoThere",
        description = "Your relocation guide for Spain, Portugal and Mexico. We'll walk you through every step of the move.",
    ),
    OnboardingPage(
        title = "Smart Decision Engine",
        description = "Answer a few questions about your lifestyle and budget, and we'll recommend the best city for you — with cost breakdowns and neighborhood tips.",
    ),
    OnboardingPage(
        title = "Track Everything",
        description = "Checklists, calendar events, documents, visa resources — all organized by phase so nothing falls through the cracks.",
    ),
)

object OnboardingPrefs {
    private const val PREFS = "onboarding_prefs"
    private const val KEY = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, true)
            .apply()
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "GoThere Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Spacer(Modifier.height(40.dp))

        AnimatedContent(targetState = currentPage, label = "onboarding") { page ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = pages[page].description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Page indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Text(
                    text = if (i == currentPage) "\u25CF" else "\u25CB",
                    color = if (i == currentPage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = { currentPage-- },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back")
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (currentPage < pages.lastIndex) "Next" else "Get Started")
            }
        }
    }
}
