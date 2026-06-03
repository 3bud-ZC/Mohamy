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
    primary = Color(0xFFE2C27A),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF3A2D12),
    onPrimaryContainer = Color(0xFFFFEFD0),
    secondary = Color(0xFFAFC4FF),
    onSecondary = Color(0xFF0D1728),
    secondaryContainer = Color(0xFF1E2B47),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF8EE3CF),
    onTertiary = Color(0xFF07211A),
    tertiaryContainer = Color(0xFF123B31),
    onTertiaryContainer = Color(0xFFC9F8EE),
    background = Color(0xFF08111F),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF101A2C),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1A263D),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF53627C),
    outlineVariant = Color(0xFF2A3955)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LegalNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE9FF),
    onPrimaryContainer = Color(0xFF0D1B36),
    secondary = LegalGoldSecondary,
    onSecondary = Color(0xFF1B1406),
    secondaryContainer = Color(0xFFF4E2BF),
    onSecondaryContainer = Color(0xFF402E05),
    tertiary = LegalGoldLight,
    onTertiary = Color(0xFF281E04),
    background = Color(0xFFF7F9FD),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE8EEF8),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
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
