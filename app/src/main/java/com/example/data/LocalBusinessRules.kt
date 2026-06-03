package com.example.data

object LocalBusinessRules {
    private val searchableExtensions = setOf("txt", "md", "csv", "json", "xml", "html", "htm", "log")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic")

    fun normalizeDateInput(dateText: String): String {
        val raw = dateText.trim()
        if (raw.isBlank()) return ""
        val slash = raw.split("/")
        if (slash.size == 3) {
            val day = slash[0].padStart(2, '0')
            val month = slash[1].padStart(2, '0')
            val year = slash[2]
            return "$year-$month-$day"
        }
        val dash = raw.split("-")
        if (dash.size == 3 && dash[0].length == 4) return raw
        return raw
    }

    fun readinessLabel(score: Int): String {
        return when (score.coerceIn(0, 100)) {
            in 0..39 -> "تحتاج استكمال"
            in 40..69 -> "جاهزية متوسطة"
            in 70..89 -> "جاهزة للمراجعة"
            else -> "جاهزة إجرائياً"
        }
    }

    fun isSearchableTextExtension(extension: String): Boolean {
        return searchableExtensions.contains(extension.trim().lowercase())
    }

    fun isImageExtension(extension: String): Boolean {
        return imageExtensions.contains(extension.trim().lowercase())
    }
}
