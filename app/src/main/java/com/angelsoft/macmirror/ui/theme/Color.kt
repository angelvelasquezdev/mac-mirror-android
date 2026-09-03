package com.angelsoft.macmirror.ui.theme

import androidx.compose.ui.graphics.Color

// Official Apple Accent & Semantic Colors
val AppleBlue = Color(0xFF007AFF)
val AppleGreen = Color(0xFF34C759)
val AppleAmber = Color(0xFFFF9500)
val AppleRed = Color(0xFFFF3B30)
val AppleIndigo = Color(0xFF5856D6)

// Apple iOS System Grays - Light
val IosLightGray = Color(0xFF8E8E93)
val IosLightGray2 = Color(0xFFAEAEC2)
val IosLightGray3 = Color(0xFFC7C7CC)
val IosLightGray4 = Color(0xFFD1D1D6)
val IosLightGray5 = Color(0xFFE5E5EA)
val IosLightGray6 = Color(0xFFF2F2F7) // Standard Grouped Background

// Apple iOS System Grays - Dark
val IosDarkGray = Color(0xFF8E8E93)
val IosDarkGray2 = Color(0xFF636366)
val IosDarkGray3 = Color(0xFF48484A)
val IosDarkGray4 = Color(0xFF3A3A3C)
val IosDarkGray5 = Color(0xFF2C2C2E)
val IosDarkGray6 = Color(0xFF1C1C1E) // Standard Grouped Card Surface

// Light Theme Palettes (Apple HIG)
val LightPrimary = AppleBlue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE5F1FF)
val LightOnPrimaryContainer = Color(0xFF004080)

val LightSecondary = AppleIndigo
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFECEBFA)
val LightOnSecondaryContainer = Color(0xFF2B2580)

val LightTertiary = AppleGreen
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFE3F9E9)
val LightOnTertiaryContainer = Color(0xFF0C5620)

val LightBackground = IosLightGray6
val LightOnBackground = Color(0xFF000000)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF000000)
val LightSurfaceVariant = IosLightGray5
val LightOnSurfaceVariant = Color(0xFF6C6C70)

val LightOutline = Color(0xFFE5E5EA)
val LightOutlineVariant = Color(0xFFECECEF)

// Dark Theme Palettes (Apple HIG)
val DarkPrimary = AppleBlue
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF0A2E5C)
val DarkOnPrimaryContainer = Color(0xFFCCE4FF)

val DarkSecondary = Color(0xFF7D7AFF)
val DarkOnSecondary = Color(0xFF000000)
val DarkSecondaryContainer = Color(0xFF2C2A6B)
val DarkOnSecondaryContainer = Color(0xFFE3E2FF)

val DarkTertiary = AppleGreen
val DarkOnTertiary = Color(0xFF000000)
val DarkTertiaryContainer = Color(0xFF0D4715)
val DarkOnTertiaryContainer = Color(0xFFB4F2C2)

val DarkBackground = Color(0xFF000000) // True OLED Black
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkSurface = IosDarkGray6       // #1C1C1E
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkSurfaceVariant = IosDarkGray5// #2C2C2E
val DarkOnSurfaceVariant = Color(0xFF8E8E93)

val DarkOutline = Color(0xFF38383A)
val DarkOutlineVariant = Color(0xFF2C2C2E)