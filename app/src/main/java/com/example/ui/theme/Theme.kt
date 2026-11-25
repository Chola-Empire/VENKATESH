package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CholaBronzeDark,
    onPrimary = Color(0xFF1A1205),
    primaryContainer = Color(0xFF43200B),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = IncomeGreenDark,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = WarningAmberDark,
    background = SlateCanvas,
    onBackground = DarkInkPrimary,
    surface = SlateSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = ExpenseCrimsonDark,
    onError = Color(0xFF450A0A),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CholaBronze,
    onPrimary = Color.White,
    primaryContainer = CholaGoldSoft,
    onPrimaryContainer = CholaBronze,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    secondaryContainer = IncomeGreenBg,
    onSecondaryContainer = IncomeGreen,
    tertiary = WarningAmber,
    background = StoneIvory,
    onBackground = InkPrimary,
    surface = StoneSurface,
    onSurface = InkPrimary,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = StoneBorder,
    outlineVariant = StoneBorder,
    error = ExpenseCrimson,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
