package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseRulesEngineTest {

    private val normalize: (String) -> String = {
        it.trim()
            .lowercase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
    }

    @Test
    fun resolveCaseType_matchesKnownArabicBuckets() {
        assertEquals("إيجارات", CaseRulesEngine.resolveCaseType("نزاع ايجار", normalize))
        assertEquals("أسرة", CaseRulesEngine.resolveCaseType("دعوى اسره", normalize))
        assertEquals("عقارات", CaseRulesEngine.resolveCaseType("نزاع عقاري", normalize))
    }

    @Test
    fun getRules_fallsBackToGeneralRulesForUnknownTypes() {
        val rules = CaseRulesEngine.getRules("نوع غير مصنف", normalize)

        assertEquals("عام", rules.key)
        assertTrue(rules.requiredDocuments.contains("التوكيل"))
    }
}
