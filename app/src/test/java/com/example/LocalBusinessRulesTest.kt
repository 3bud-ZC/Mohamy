package com.example

import com.example.data.LocalBusinessRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBusinessRulesTest {

    @Test
    fun normalizeDateInput_convertsSlashesToIso() {
        assertEquals("2026-06-02", LocalBusinessRules.normalizeDateInput("2/6/2026"))
        assertEquals("2026-06-02", LocalBusinessRules.normalizeDateInput("2026-06-02"))
    }

    @Test
    fun readinessLabel_mapsRangesCorrectly() {
        assertEquals("تحتاج استكمال", LocalBusinessRules.readinessLabel(10))
        assertEquals("جاهزية متوسطة", LocalBusinessRules.readinessLabel(45))
        assertEquals("جاهزة للمراجعة", LocalBusinessRules.readinessLabel(75))
        assertEquals("جاهزة إجرائياً", LocalBusinessRules.readinessLabel(95))
    }

    @Test
    fun searchableTextExtension_supportsExpectedTypes() {
        assertTrue(LocalBusinessRules.isSearchableTextExtension("txt"))
        assertTrue(LocalBusinessRules.isSearchableTextExtension("HTML"))
        assertFalse(LocalBusinessRules.isSearchableTextExtension("pdf"))
    }
}
