package com.example.gothere.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Brand palette ----------

// Bright teal core (matches logo)
val Teal      = Color(0xFF15B8A6)
val TealDark  = Color(0xFF0F8F82)
val TealLight = Color(0xFF7BE3D6)

// Dark background like your mock (soft dark blue/gray)
// OLD: val Night = Color(0xFF0D1117)
val Night = Color(0xFF151B23)

// Light mode neutrals
val White        = Color(0xFFFFFFFF)
val Black        = Color(0xFF111111)
val LightSurface = Color(0xFFF7F8FA)
val LightCard    = Color(0xFFFFFFFF)

// ---------- Material 3 tokens (LIGHT) ----------

val md_theme_light_primary            = Teal
val md_theme_light_onPrimary          = White
val md_theme_light_primaryContainer   = Color(0xFFC5F4EE)
val md_theme_light_onPrimaryContainer = TealDark

val md_theme_light_secondary          = TealDark
val md_theme_light_onSecondary        = White
val md_theme_light_secondaryContainer = Color(0xFFB8E8E1)
val md_theme_light_onSecondaryContainer = Color(0xFF003732)

val md_theme_light_tertiary           = Color(0xFF2E8BC0)   // soft blue accent
val md_theme_light_onTertiary         = White
val md_theme_light_tertiaryContainer  = Color(0xFFCAE6FF)
val md_theme_light_onTertiaryContainer = Color(0xFF003353)

val md_theme_light_error              = Color(0xFFBA1A1A)
val md_theme_light_errorContainer     = Color(0xFFFFDAD6)
val md_theme_light_onError            = White
val md_theme_light_onErrorContainer   = Color(0xFF410002)

val md_theme_light_background         = White
val md_theme_light_onBackground       = Black
val md_theme_light_surface            = LightSurface
val md_theme_light_onSurface          = Black

val md_theme_light_surfaceVariant     = Color(0xFFE0E3EB)
val md_theme_light_onSurfaceVariant   = Color(0xFF44474F)
val md_theme_light_outline            = Color(0xFF75777F)
val md_theme_light_outlineVariant     = Color(0xFFC4C6CF)

val md_theme_light_inverseOnSurface   = White
val md_theme_light_inverseSurface     = Color(0xFF2F3137)
val md_theme_light_inversePrimary     = TealDark

val md_theme_light_scrim              = Color(0x66000000)
val md_theme_light_surfaceTint        = md_theme_light_primary

// ---------- Material 3 tokens (DARK) ----------

val md_theme_dark_primary             = TealLight
val md_theme_dark_onPrimary           = Color(0xFF003732)
val md_theme_dark_primaryContainer    = TealDark
val md_theme_dark_onPrimaryContainer  = White

val md_theme_dark_secondary           = Teal
val md_theme_dark_onSecondary         = Black
val md_theme_dark_secondaryContainer  = TealDark
val md_theme_dark_onSecondaryContainer = White

val md_theme_dark_tertiary            = Color(0xFF7CCBFF)
val md_theme_dark_onTertiary          = Color(0xFF00344F)
val md_theme_dark_tertiaryContainer   = Color(0xFF004B71)
val md_theme_dark_onTertiaryContainer = Color(0xFFF0EBE3) // warm off-white

val md_theme_dark_error               = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer      = Color(0xFF93000A)
val md_theme_dark_onError             = Color(0xFF690005)
val md_theme_dark_onErrorContainer    = Color(0xFFFFDAD6)

val md_theme_dark_background          = Night
val md_theme_dark_onBackground        = Color(0xFFF5F0E8) // bone white
val md_theme_dark_surface             = Night
val md_theme_dark_onSurface           = Color(0xFFF5F0E8) // bone white

val md_theme_dark_surfaceVariant      = Color(0xFF2A2F37)
val md_theme_dark_onSurfaceVariant    = Color(0xFFD9D3C7) // cream white (no purple)
val md_theme_dark_outline             = Color(0xFF9E9890) // warm gray
val md_theme_dark_outlineVariant      = Color(0xFF4A4740)

val md_theme_dark_inverseOnSurface    = Black
val md_theme_dark_inverseSurface      = LightSurface
val md_theme_dark_inversePrimary      = Teal

val md_theme_dark_scrim               = Color(0x99000000)
val md_theme_dark_surfaceTint         = md_theme_dark_primary
