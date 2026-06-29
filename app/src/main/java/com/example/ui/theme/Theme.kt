package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = MohamyGoldBright,
    onPrimary = MohamyBlack,
    primaryContainer = Color(0xFF4D3B14),
    onPrimaryContainer = MohamyIvory,
    secondary = MohamyIvory,
    onSecondary = MohamyBlack,
    secondaryContainer = MohamySurfaceSoft,
    onSecondaryContainer = MohamyWarmText,
    tertiary = MohamySuccess,
    onTertiary = Color.White,
    tertiaryContainer = MohamySuccessSoft.copy(alpha = 0.18f),
    onTertiaryContainer = MohamySuccessSoft,
    background = MohamyInk,
    onBackground = MohamyWarmText,
    surface = MohamySurface,
    onSurface = MohamyWarmText,
    surfaceVariant = MohamySurfaceRaised,
    onSurfaceVariant = MohamyWarmTextSoft,
    error = MohamyDanger,
    onError = MohamyIvory,
    errorContainer = Color(0xFF53202B),
    onErrorContainer = Color(0xFFFFE7EA),
    outline = Color(0xFF746655),
    outlineVariant = Color(0xFF453D32)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MohamyGold,
    onPrimary = MohamyBlack,
    primaryContainer = Color(0xFFF7E5AD),
    onPrimaryContainer = MohamyBlack,
    secondary = MohamySurface,
    onSecondary = MohamyIvory,
    secondaryContainer = Color(0xFFF2E7D4),
    onSecondaryContainer = MohamyBlack,
    tertiary = MohamySuccess,
    onTertiary = Color.White,
    tertiaryContainer = MohamySuccessSoft,
    onTertiaryContainer = MohamySuccess,
    background = MohamyBeigeSoft,
    onBackground = MohamyBlack,
    surface = MohamyIvory,
    onSurface = Color(0xFF1F1B17),
    surfaceVariant = Color(0xFFF4ECE0),
    onSurfaceVariant = Color(0xFF62584A),
    error = MohamyDanger,
    onError = Color.White,
    errorContainer = MohamyDangerSoft,
    onErrorContainer = MohamyDanger,
    outline = Color(0xFFB7A17A),
    outlineVariant = Color(0xFFE5D8BF)
  )


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled for consistent premium branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
