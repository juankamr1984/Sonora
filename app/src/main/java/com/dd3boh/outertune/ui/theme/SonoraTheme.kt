/*
 * Sonora — basado en OuterTune (GPL-3.0)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta base de Sonora
object SonoraColors {
    val Accent = Color(0xFFFF8686)          // coral
    val AccentPressed = Color(0xFFE56F6F)
    val Background = Color(0xFF121212)
    val Elevated = Color(0xFF181818)
    val Card = Color(0xFF242424)
    val CardHigh = Color(0xFF2A2A2A)
    val Highlight = Color(0xFF333333)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF7A7A7A)
}

val SonoraDarkColorScheme = darkColorScheme(
    primary = SonoraColors.Accent,
    onPrimary = Color(0xFF121212),
    primaryContainer = SonoraColors.Accent,
    onPrimaryContainer = Color(0xFF121212),
    inversePrimary = SonoraColors.AccentPressed,
    secondary = SonoraColors.TextPrimary,
    onSecondary = Color(0xFF121212),
    secondaryContainer = SonoraColors.Highlight,
    onSecondaryContainer = SonoraColors.TextPrimary,
    tertiary = SonoraColors.Accent,
    onTertiary = Color(0xFF121212),
    tertiaryContainer = SonoraColors.CardHigh,
    onTertiaryContainer = SonoraColors.TextPrimary,
    background = SonoraColors.Background,
    onBackground = SonoraColors.TextPrimary,
    surface = SonoraColors.Background,
    onSurface = SonoraColors.TextPrimary,
    surfaceVariant = SonoraColors.Card,
    onSurfaceVariant = SonoraColors.TextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = SonoraColors.TextPrimary,
    inverseOnSurface = Color(0xFF121212),
    outline = SonoraColors.TextMuted,
    outlineVariant = SonoraColors.Highlight,
    scrim = Color.Black,
    surfaceBright = SonoraColors.CardHigh,
    surfaceDim = SonoraColors.Background,
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = SonoraColors.Elevated,
    surfaceContainer = SonoraColors.Elevated,
    surfaceContainerHigh = SonoraColors.Card,
    surfaceContainerHighest = SonoraColors.CardHigh,
)

val SonoraLightColorScheme = lightColorScheme(
    primary = Color(0xFFD9545A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF121212),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF121212),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5A5A5A),
    surfaceContainerLow = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFDDDDDD),
)

private val Base = Typography()

// Títulos más gruesos y compactos, como en las apps de streaming
val SonoraTypography = Base.copy(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

val SonoraSectionTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp)
