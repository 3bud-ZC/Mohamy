package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DateParsingTest {

    private fun String.toEnglishDigits(): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = this
        for (i in arabicDigits.indices) {
            result = result.replace(arabicDigits[i], englishDigits[i])
        }
        return result
    }

    private fun parseDate(dateText: String): Long? {
        val normalized = dateText.toEnglishDigits().trim()
        if (normalized.isBlank()) return null
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy")
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                val parsed = format.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }

    @Test
    fun testToEnglishDigits() {
        assertEquals("2024-05-12", "٢٠٢٤-٠٥-١٢".toEnglishDigits())
        assertEquals("12/03/2023", "١٢/٠٣/٢٠٢٣".toEnglishDigits())
    }

    @Test
    fun testParseArabicDate() {
        val parsedTime = parseDate("٢٠٢٤-٠٥-١٢")
        // "2024-05-12" UTC time
        val expectedFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        assertEquals("2024-05-12", expectedFormat.format(parsedTime))
    }

    @Test
    fun testParseEnglishDate() {
        val parsedTime = parseDate("12-05-2024")
        val expectedFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        assertEquals("2024-05-12", expectedFormat.format(parsedTime))
    }
}
