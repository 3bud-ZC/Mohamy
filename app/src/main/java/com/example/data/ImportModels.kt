package com.example.data

enum class ImportTarget {
    CLIENTS,
    CASES,
    SESSIONS
}

enum class DuplicateStrategy {
    SKIP,
    UPDATE,
    CREATE_NEW
}

data class ImportRowPreview(
    val rowNumber: Int,
    val status: String,
    val reason: String,
    val values: Map<String, String>
)

data class ImportPreview(
    val target: ImportTarget,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val duplicates: Int,
    val warnings: Int,
    val rows: List<ImportRowPreview>
)
