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
    primaryContainer = Color(0xFF3B3113),
    onPrimaryContainer = MohamyWarmText,
    secondary = MohamyWarmText,
    onSecondary = MohamyBlack,
    secondaryContainer = MohamyCharcoalSoft,
    onSecondaryContainer = MohamyWarmTextSoft,
    tertiary = MohamySuccess,
    onTertiary = Color.White,
    tertiaryContainer = MohamySuccess.copy(alpha = 0.24f),
    onTertiaryContainer = Color(0xFFD8F8EA),
    background = MohamyBlack,
    onBackground = MohamyWarmText,
    surface = MohamyCharcoal,
    onSurface = MohamyWarmTextSoft,
    surfaceVariant = MohamyCharcoalSoft,
    onSurfaceVariant = MohamyMutedText,
    error = MohamyDanger,
    onError = Color.White,
    errorContainer = MohamyDanger.copy(alpha = 0.22f),
    onErrorContainer = Color(0xFFFFE3E6),
    outline = Color(0xFF635D54),
    outlineVariant = Color(0xFF38332B)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MohamyGold,
    onPrimary = MohamyBlack,
    primaryContainer = Color(0xFFF4E3A6),
    onPrimaryContainer = MohamyBlack,
    secondary = MohamyCharcoal,
    onSecondary = MohamyWarmText,
    secondaryContainer = MohamyBeige,
    onSecondaryContainer = MohamyBlack,
    tertiary = MohamySuccess,
    onTertiary = Color.White,
    background = MohamyBeigeSoft,
    onBackground = MohamyBlack,
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFF0E8D8),
    onSurfaceVariant = Color(0xFF5E5B56),
    error = MohamyDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFAD8DC),
    onErrorContainer = MohamyDanger,
    outline = Color(0xFFC4B08A),
    outlineVariant = Color(0xFFE5D9BF)
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
