package com.example.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExportManager {

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
        return if (cleaned.isBlank()) "document" else cleaned
    }

    private fun exportsDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "exports")
        val dir = File(base, "MohamyPhoneExports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
    }

    fun exportToPdf(context: Context, title: String, content: String): Result<File> {
        return runCatching {
            val fileName = "${sanitizeFileName(title)}_${timestamp()}.pdf"
            val outFile = File(exportsDir(context), fileName)

            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 28
            val lineHeight = 18

            val paint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
            }

            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas = page.canvas
            var y = margin + 16

            val renderedTitle = title.ifBlank { "MohamyPhone Document" }
            val titlePaint = Paint(paint).apply { textSize = 15f }
            canvas.drawText(renderedTitle, margin.toFloat(), y.toFloat(), titlePaint)
            y += 28

            val wrappedLines = content
                .replace("\r\n", "\n")
                .split("\n")
                .flatMap { paragraph ->
                    if (paragraph.isBlank()) {
                        listOf("")
                    } else {
                        paragraph.chunked(90)
                    }
                }

            wrappedLines.forEach { line ->
                if (y > pageHeight - margin) {
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = page.canvas
                    y = margin + 16
                }
                canvas.drawText(line, margin.toFloat(), y.toFloat(), paint)
                y += lineHeight
            }

            document.finishPage(page)
            FileOutputStream(outFile).use { output ->
                document.writeTo(output)
            }
            document.close()
            outFile
        }
    }

    fun exportToWord(context: Context, title: String, content: String): Result<File> {
        return runCatching {
            val fileName = "${sanitizeFileName(title)}_${timestamp()}.doc"
            val outFile = File(exportsDir(context), fileName)
            val body = buildString {
                append("<html><head><meta charset=\"UTF-8\"></head><body dir=\"rtl\" style=\"font-family:'Segoe UI',Tahoma;line-height:1.9;text-align:right;padding:24px;\">")
                append("<h2>")
                append(title.ifBlank { "مستند قانوني" })
                append("</h2><pre style=\"white-space:pre-wrap;font-family:Tahoma;\">")
                append(content)
                append("</pre></body></html>")
            }
            outFile.writeText(body, Charsets.UTF_8)
            outFile
        }
    }

    fun exportToHtml(context: Context, title: String, content: String): Result<File> {
        return runCatching {
            val fileName = "${sanitizeFileName(title)}_${timestamp()}.html"
            val outFile = File(exportsDir(context), fileName)
            val body = buildString {
                append("<!doctype html><html lang=\"ar\" dir=\"rtl\"><head><meta charset=\"UTF-8\">")
                append("<title>${title.ifBlank { "مستند قانوني" }}</title>")
                append("</head><body style=\"font-family:'Segoe UI',Tahoma;line-height:1.9;text-align:right;padding:32px;background:#fff;color:#111827;\">")
                append("<h1 style=\"margin-bottom:24px;\">${title.ifBlank { "مستند قانوني" }}</h1>")
                append("<div style=\"white-space:pre-wrap;\">")
                append(content)
                append("</div></body></html>")
            }
            outFile.writeText(body, Charsets.UTF_8)
            outFile
        }
    }

    fun exportToText(context: Context, title: String, content: String): Result<File> {
        return runCatching {
            val fileName = "${sanitizeFileName(title)}_${timestamp()}.txt"
            val outFile = File(exportsDir(context), fileName)
            outFile.writeText(content, Charsets.UTF_8)
            outFile
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
    }
}
