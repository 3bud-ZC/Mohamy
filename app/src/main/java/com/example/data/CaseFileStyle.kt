package com.example.data

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

data class CaseFileStyle(
    val accentColorHex: String,
    val cardStyle: String
)

object CaseFileStylePalette {
    val colors = listOf(
        "#1D3B6B",
        "#8B6F2B",
        "#1B6B5A",
        "#6B1F2B",
        "#4C5A70",
        "#6B4E1F"
    )

    val styles = listOf("rounded", "paper", "sharp")
}

fun parseHexColorOrDefault(hex: String, fallback: Color = Color(0xFFE8EEF8)): Color {
    return try {
        val raw = hex.trim().ifBlank { "#E8EEF8" }
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: Exception) {
        fallback
    }
}

fun caseFileShape(style: String): RoundedCornerShape {
    return when (style.lowercase()) {
        "paper" -> RoundedCornerShape(10)
        "sharp" -> RoundedCornerShape(4)
        else -> RoundedCornerShape(18)
    }
}

fun defaultCaseFileStyle(docType: String, fileName: String): CaseFileStyle {
    val normalized = "${docType.lowercase()} ${fileName.lowercase()}"
    val color = when {
        normalized.contains("عقد") -> CaseFileStylePalette.colors[0]
        normalized.contains("توكيل") -> CaseFileStylePalette.colors[1]
        normalized.contains("مذكرة") || normalized.contains("صحيفة") -> CaseFileStylePalette.colors[2]
        normalized.contains("حكم") -> CaseFileStylePalette.colors[3]
        normalized.contains("إيصال") || normalized.contains("سداد") -> CaseFileStylePalette.colors[4]
        else -> CaseFileStylePalette.colors[5]
    }
    val style = when {
        normalized.contains("pdf") || normalized.contains("حكم") -> "paper"
        normalized.contains("صورة") || normalized.contains("png") || normalized.contains("jpg") -> "rounded"
        else -> "rounded"
    }
    return CaseFileStyle(color, style)
}

