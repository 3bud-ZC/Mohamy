package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.sp
import com.example.R

val TajawalFamily =
  FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_bold, FontWeight.ExtraBold),
  )

val Typography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = TajawalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = TajawalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = TajawalFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 28.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    bodyMedium =
      TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    labelLarge =
      TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    labelSmall =
      TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
  )
