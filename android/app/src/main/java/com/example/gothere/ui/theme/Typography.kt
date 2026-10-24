package com.example.gothere.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Material 3 type scale, tightened: semibold headings with slightly negative
// tracking read as more editorial than the default regular-weight headlines.
private val Base = Typography()

val Typography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.005).em),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.005.em),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.005.em),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.em),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.01.em),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.03.em),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.03.em)
)
