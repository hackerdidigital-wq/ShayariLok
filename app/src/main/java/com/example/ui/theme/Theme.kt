package com.example.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.model.DynamicThemeConfig

@Immutable
data class ShayariPalette(
  val isDark: Boolean = true,
  val bgTop: Color = DeepRoyalPurple,
  val bgMid: Color = Color(0xFF1A0936),
  val bgBottom: Color = MidnightIndigo,
  val headerStart: Color = HeaderPurpleStart,
  val headerEnd: Color = HeaderPurpleEnd,
  val surface: Color = DarkSurface,
  val surfaceVariant: Color = DarkSurfaceVariant,
  val surfaceSubtle: Color = Color(0xFF1A0D34),
  val cardGradientStart: Color = Color(0xFF28124E),
  val cardGradientEnd: Color = Color(0xFF1B0A38),
  val bottomBarBg: Color = Color(0xFF160830),
  val bottomBarDivider: Color = Color(0xFF3E1F74),
  val textPrimary: Color = Color(0xFFFDFBFF),
  val textSecondary: Color = DarkOnSurfaceVariant,
  val textMuted: Color = Color(0xFFB5A3D6),
  val accentGold: Color = CrownGold,
  val accentPurple: Color = NeonPurple,
  val accentViolet: Color = NeonVioletGlow,
  val accentPink: Color = NeonPink,
  val chipSelectedBg: Color = NeonPurple,
  val chipSelectedText: Color = Color.White,
  val chipUnselectedBg: Color = Color(0xFF2B1654),
  val chipUnselectedText: Color = Color(0xFFE7DAFF),
  val outline: Color = DarkOutline,
  val pillSurface: Color = Color(0xFF2B1654),
  val drawerHeaderStart: Color = Color(0xFF330C6F),
  val drawerHeaderEnd: Color = Color(0xFF1E0A42),
  val snackBg: Color = Color(0xFF341666),
  val snackText: Color = Color.White,
)

val LocalShayariColors = staticCompositionLocalOf { ShayariPalette() }

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

private fun buildShayariPalette(
  isDark: Boolean,
  customPrimary: Color?,
  customAccent: Color?,
): ShayariPalette {
  val primaryBrand = customPrimary ?: NeonPurple
  val accentBrand = customAccent ?: NeonPink
  return if (isDark) {
    ShayariPalette(
      isDark = true,
      bgTop = lerp(DeepRoyalPurple, primaryBrand, 0.08f),
      bgMid = lerp(Color(0xFF1A0936), primaryBrand, 0.08f),
      bgBottom = lerp(MidnightIndigo, primaryBrand, 0.10f),
      headerStart = lerp(HeaderPurpleStart, primaryBrand, 0.30f),
      headerEnd = lerp(HeaderPurpleEnd, primaryBrand, 0.40f),
      surface = DarkSurface,
      surfaceVariant = DarkSurfaceVariant,
      surfaceSubtle = Color(0xFF1A0D34),
      cardGradientStart = lerp(Color(0xFF28124E), primaryBrand, 0.14f),
      cardGradientEnd = Color(0xFF1B0A38),
      bottomBarBg = Color(0xFF160830),
      bottomBarDivider = lerp(Color(0xFF3E1F74), primaryBrand, 0.25f),
      textPrimary = Color(0xFFFDFBFF),
      textSecondary = DarkOnSurfaceVariant,
      textMuted = Color(0xFFB5A3D6),
      accentGold = CrownGold,
      accentPurple = primaryBrand,
      accentViolet = lerp(NeonVioletGlow, primaryBrand, 0.30f),
      accentPink = accentBrand,
      chipSelectedBg = primaryBrand,
      chipSelectedText = Color.White,
      chipUnselectedBg = Color(0xFF2B1654),
      chipUnselectedText = Color(0xFFE7DAFF),
      outline = lerp(DarkOutline, primaryBrand, 0.20f),
      pillSurface = Color(0xFF2B1654),
      drawerHeaderStart = lerp(Color(0xFF330C6F), primaryBrand, 0.30f),
      drawerHeaderEnd = Color(0xFF1E0A42),
      snackBg = Color(0xFF341666),
      snackText = Color.White,
    )
  } else {
    ShayariPalette(
      isDark = false,
      bgTop = lerp(LightBackground, primaryBrand, 0.03f),
      bgMid = Color(0xFFF3ECFC),
      bgBottom = LightBackgroundBottom,
      headerStart = lerp(Color(0xFF5D13C7), primaryBrand, 0.45f),
      headerEnd = lerp(Color(0xFF7B1CFA), primaryBrand, 0.45f),
      surface = Color.White,
      surfaceVariant = Color(0xFFEDE4FA),
      surfaceSubtle = Color(0xFFF6F0FE),
      cardGradientStart = Color.White,
      cardGradientEnd = Color(0xFFF6EFFD),
      bottomBarBg = Color.White,
      bottomBarDivider = Color(0xFFDFD0F7),
      textPrimary = Color(0xFF1A0B33),
      textSecondary = Color(0xFF4B3770),
      textMuted = Color(0xFF6B598C),
      accentGold = Color(0xFF9E6500),
      accentPurple = primaryBrand,
      accentViolet = lerp(Color(0xFF6A1B9A), primaryBrand, 0.45f),
      accentPink = accentBrand,
      chipSelectedBg = primaryBrand,
      chipSelectedText = Color.White,
      chipUnselectedBg = Color(0xFFEBE1FC),
      chipUnselectedText = Color(0xFF2D1259),
      outline = Color(0xFFD0BEF2),
      pillSurface = Color(0xFFEFE6FD),
      drawerHeaderStart = lerp(Color(0xFF5A12BF), primaryBrand, 0.45f),
      drawerHeaderEnd = lerp(Color(0xFF7E22CE), primaryBrand, 0.45f),
      snackBg = Color(0xFF321061),
      snackText = Color.White,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShayariLokTheme(
  darkTheme: Boolean = true,
  dynamicTheme: DynamicThemeConfig = DynamicThemeConfig(),
  content: @Composable () -> Unit,
) {
  val resolvedDark = darkTheme
  val customPrimary = parseHexColorOrNull(dynamicTheme.primaryHex)
  val customAccent = parseHexColorOrNull(dynamicTheme.accentHex)
  val palette = buildShayariPalette(resolvedDark, customPrimary, customAccent)

  val colorScheme =
    if (resolvedDark) {
      DefaultDarkColorScheme.copy(
        primary = customPrimary ?: DefaultDarkColorScheme.primary,
        secondary = customAccent ?: DefaultDarkColorScheme.secondary,
        background = palette.bgTop,
        surface = palette.surface,
        surfaceVariant = palette.surfaceVariant,
        outline = palette.outline,
      )
    } else {
      DefaultLightColorScheme.copy(
        primary = customPrimary ?: DefaultLightColorScheme.primary,
        secondary = customAccent ?: DefaultLightColorScheme.secondary,
        background = palette.bgTop,
        surface = palette.surface,
        surfaceVariant = palette.surfaceVariant,
        outline = palette.outline,
      )
    }

  val subtleRipple =
    RippleConfiguration(
      color = if (resolvedDark) Color.White.copy(alpha = 0.08f) else Color(0xFF4A148C).copy(alpha = 0.08f)
    )

  CompositionLocalProvider(
    LocalRippleConfiguration provides subtleRipple,
    LocalShayariColors provides palette,
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
