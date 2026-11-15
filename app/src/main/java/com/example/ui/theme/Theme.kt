package com.example.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.model.DynamicThemeConfig

private val DefaultDarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
  )

private val DefaultLightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
  )

fun parseHexColorOrNull(hex: String): Color? {
  val clean = hex.trim().removePrefix("#")
  if (clean.length != 6 && clean.length != 8) return null
  return try {
    val parsed = clean.toLong(16)
    if (clean.length == 6) {
      Color(0xFF000000L or parsed)
    } else {
      Color(parsed)
    }
  } catch (_: Exception) {
    null
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShayariLokTheme(
  darkTheme: Boolean = true,
  dynamicTheme: DynamicThemeConfig = DynamicThemeConfig(),
  content: @Composable () -> Unit,
) {
  val resolvedDark = dynamicTheme.darkMode && darkTheme
  val customPrimary = parseHexColorOrNull(dynamicTheme.primaryHex)
  val customAccent = parseHexColorOrNull(dynamicTheme.accentHex)

  val colorScheme =
    if (resolvedDark) {
      DefaultDarkColorScheme.copy(
        primary = customPrimary ?: DefaultDarkColorScheme.primary,
        secondary = customAccent ?: DefaultDarkColorScheme.secondary,
      )
    } else {
      DefaultLightColorScheme.copy(
        primary = customPrimary ?: DefaultLightColorScheme.primary,
        secondary = customAccent ?: DefaultLightColorScheme.secondary,
      )
    }

  val subtleRipple =
    RippleConfiguration(
      color = if (resolvedDark) Color.White.copy(alpha = 0.08f) else Color(0xFF4A148C).copy(alpha = 0.08f)
    )

  CompositionLocalProvider(LocalRippleConfiguration provides subtleRipple) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
