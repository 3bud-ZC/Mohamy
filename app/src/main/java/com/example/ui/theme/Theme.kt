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
    primary = LegalGoldSecondary,
    secondary = LegalGoldLight,
    tertiary = LegalGoldSecondary,
    background = Color(0xFF0D1424),
    surface = Color(0xFF141F35),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFF3F4F6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LegalNavyPrimary,
    secondary = LegalGoldSecondary,
    tertiary = LegalGoldLight,
    background = Color(0xFFF9FAFB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937)
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
