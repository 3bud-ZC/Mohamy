package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ImportedTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

object DataImportParser {

    fun parse(context: Context, uri: Uri): Result<ImportedTable> {
        return try {
            val name = resolveName(context, uri).lowercase()
            val table = when {
                name.endsWith(".xlsx") -> parseXlsx(context, uri)
                else -> parseCsv(context, uri)
            }
            Result.success(table)
        } catch (e: Exception) {
            Result.failure(Exception("تعذر قراءة الملف: ${e.message}"))
        }
    }

    private fun resolveName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                return cursor.getString(index) ?: "import.csv"
            }
        }
        return "import.csv"
    }

    private fun parseCsv(context: Context, uri: Uri): ImportedTable {
        val rows = mutableListOf<List<String>>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val raw = line ?: continue
                    if (raw.isBlank()) continue
                    rows.add(parseCsvLine(raw))
                }
            }
        }

        if (rows.isEmpty()) return ImportedTable(emptyList(), emptyList())
        val headers = rows.first().map { it.trim() }
        val dataRows = rows.drop(1).map { padRow(it, headers.size) }
        return ImportedTable(headers, dataRows)
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        out.add(current.toString().trim())
        return out
    }

    private fun parseXlsx(context: Context, uri: Uri): ImportedTable {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val shared = parseSharedStrings(entries["xl/sharedStrings.xml"]?.toString(Charsets.UTF_8) ?: "")
        val sheetEntryName = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
            .firstOrNull() ?: throw IllegalStateException("لا توجد sheets في ملف xlsx")
        val sheetXml = entries[sheetEntryName]?.toString(Charsets.UTF_8)
            ?: throw IllegalStateException("تعذر قراءة أول sheet")

        val rows = parseSheetRows(sheetXml, shared)
        if (rows.isEmpty()) return ImportedTable(emptyList(), emptyList())
        val headers = rows.first().map { it.trim() }
        val dataRows = rows.drop(1).map { padRow(it, headers.size) }
        return ImportedTable(headers, dataRows)
    }

    private fun parseSharedStrings(xml: String): List<String> {
        if (xml.isBlank()) return emptyList()
        val values = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())

        var event = parser.eventType
        var inSi = false
        var inT = false
        val current = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        inSi = true
                        current.clear()
                    }
                    if (inSi && parser.name == "t") {
                        inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        current.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") inT = false
                    if (parser.name == "si") {
                        values.add(current.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return values
    }

    private fun parseSheetRows(xml: String, shared: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())

        var event = parser.eventType
        var rowMap = mutableMapOf<Int, String>()
        var cellRef = ""
        var cellType = ""
        var readingValue = false
        var valueText = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> rowMap = mutableMapOf()
                        "c" -> {
                            cellRef = parser.getAttributeValue(null, "r") ?: "A1"
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            valueText = StringBuilder()
                        }
                        "v" -> readingValue = true
                        "t" -> if (cellType == "inlineStr") readingValue = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (readingValue) valueText.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> readingValue = false
                        "t" -> if (cellType == "inlineStr") readingValue = false
                        "c" -> {
                            val col = excelColToIndex(cellRef)
                            val raw = valueText.toString()
                            val finalVal = if (cellType == "s") {
                                raw.toIntOrNull()?.let { idx -> shared.getOrNull(idx) } ?: ""
                            } else raw
                            rowMap[col] = finalVal
                        }
                        "row" -> {
                            val maxCol = rowMap.keys.maxOrNull() ?: -1
                            if (maxCol >= 0) {
                                val row = (0..maxCol).map { rowMap[it] ?: "" }
                                if (row.any { it.isNotBlank() }) result.add(row)
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun excelColToIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        var value = 0
        for (ch in letters) {
            value = value * 26 + (ch.code - 'A'.code + 1)
        }
        return (value - 1).coerceAtLeast(0)
    }

    private fun padRow(row: List<String>, size: Int): List<String> {
        if (row.size >= size) return row.take(size)
        return row + List(size - row.size) { "" }
    }
}
