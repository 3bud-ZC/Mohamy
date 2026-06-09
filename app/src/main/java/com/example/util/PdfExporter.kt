package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private fun getBaseTextPaint(textSize: Float, isBold: Boolean = false): TextPaint {
        val paint = TextPaint()
        paint.color = Color.BLACK
        paint.textSize = textSize
        paint.isAntiAlias = true
        paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        return paint
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_OPPOSITE
    ): Float {
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(align)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, align, 1.2f, 0f, false)
        }
        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
        return staticLayout.height.toFloat()
    }

    private fun savePdfDocument(context: Context, document: PdfDocument, filename: String): Uri? {
        val dir = File(context.cacheDir, "exported_pdfs")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "$filename.pdf")
        try {
            document.writeTo(FileOutputStream(file))
            document.close()
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
        }
        return null
    }

    private fun generateStandardPdf(
        context: Context,
        filename: String,
        title: String,
        contentBuilder: (Canvas, Float, Int) -> Unit
    ): Uri? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size approx
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 50f
        val contentWidth = pageInfo.pageWidth - (margin * 2).toInt()
        var currentY = margin

        // Draw Header
        val titlePaint = getBaseTextPaint(24f, true)
        val datePaint = getBaseTextPaint(12f, false)
        
        currentY += drawText(canvas, title, titlePaint, contentWidth, margin, currentY, Layout.Alignment.ALIGN_CENTER)
        currentY += 10f
        
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
        currentY += drawText(canvas, "تاريخ الإصدار: $dateStr", datePaint, contentWidth, margin, currentY, Layout.Alignment.ALIGN_NORMAL)
        currentY += 20f

        // Draw line
        val linePaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 2f }
        canvas.drawLine(margin, currentY, pageInfo.pageWidth - margin, currentY, linePaint)
        currentY += 20f

        // Call content builder
        contentBuilder(canvas, currentY, contentWidth)

        document.finishPage(page)
        return savePdfDocument(context, document, filename)
    }

    fun exportCaseSummary(
        context: Context,
        legalCase: LegalCase,
        clientName: String,
        sessions: List<CaseSession>,
        tasks: List<LegalTask>
    ): Uri? {
        return generateStandardPdf(context, "Case_${legalCase.caseNumber}", "ملخص القضية") { canvas, startY, width ->
            var y = startY
            val headerPaint = getBaseTextPaint(16f, true)
            val bodyPaint = getBaseTextPaint(14f, false)
            val margin = 50f

            y += drawText(canvas, "تفاصيل القضية", headerPaint, width, margin, y) + 10f
            val details = """
                رقم القضية: ${legalCase.caseNumber}
                اسم الموكل: $clientName
                المحكمة: ${legalCase.court}
                النوع: ${legalCase.caseType}
                الحالة: ${legalCase.status}
                ملاحظات: ${legalCase.notes}
            """.trimIndent()
            y += drawText(canvas, details, bodyPaint, width, margin, y) + 20f

            y += drawText(canvas, "الجلسات (${sessions.size})", headerPaint, width, margin, y) + 10f
            if (sessions.isEmpty()) {
                y += drawText(canvas, "لا توجد جلسات.", bodyPaint, width, margin, y) + 10f
            } else {
                sessions.forEach { s ->
                    val sTxt = "• ${s.date} ${s.time} - المحكمة: ${s.court} - القرار: ${s.decision}"
                    y += drawText(canvas, sTxt, bodyPaint, width - 20, margin + 20f, y) + 5f
                }
            }
            y += 10f

            y += drawText(canvas, "المهام (${tasks.size})", headerPaint, width, margin, y) + 10f
            if (tasks.isEmpty()) {
                y += drawText(canvas, "لا توجد مهام.", bodyPaint, width, margin, y) + 10f
            } else {
                tasks.forEach { t ->
                    val tTxt = "• ${t.title} - ${t.dueDate} - الحالة: ${t.status}"
                    y += drawText(canvas, tTxt, bodyPaint, width - 20, margin + 20f, y) + 5f
                }
            }
        }
    }

    fun exportClientFinancials(
        context: Context,
        client: Client,
        cases: List<LegalCase>,
        fees: List<CaseFee>
    ): Uri? {
        return generateStandardPdf(context, "Financials_${client.name}", "كشف حساب الموكل") { canvas, startY, width ->
            var y = startY
            val headerPaint = getBaseTextPaint(16f, true)
            val bodyPaint = getBaseTextPaint(14f, false)
            val margin = 50f

            y += drawText(canvas, "اسم الموكل: ${client.name}", headerPaint, width, margin, y) + 20f

            var totalAmount = 0.0
            var totalPaid = 0.0

            cases.forEach { c ->
                val caseFees = fees.filter { it.caseId == c.id }
                if (caseFees.isNotEmpty()) {
                    y += drawText(canvas, "القضية: ${c.title} (${c.caseNumber})", headerPaint, width, margin, y) + 10f
                    caseFees.forEach { fee ->
                        totalAmount += fee.totalAmount
                        totalPaid += fee.paidAmount
                        val fTxt = "• إجمالي الأتعاب: ${fee.totalAmount} ج.م | المدفوع: ${fee.paidAmount} ج.م | المتبقي: ${fee.totalAmount - fee.paidAmount} ج.م"
                        y += drawText(canvas, fTxt, bodyPaint, width - 20, margin + 20f, y) + 5f
                        if (fee.paymentNotes.isNotBlank()) {
                            y += drawText(canvas, "  ملاحظات الدفع: ${fee.paymentNotes}", bodyPaint, width - 20, margin + 20f, y) + 5f
                        }
                    }
                    y += 10f
                }
            }

            y += 20f
            val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
            canvas.drawLine(margin, y, margin + width, y, linePaint)
            y += 15f

            val summaryTxt = """
                إجمالي الأتعاب: $totalAmount ج.م
                إجمالي المدفوع: $totalPaid ج.م
                إجمالي المتبقي: ${totalAmount - totalPaid} ج.م
            """.trimIndent()
            
            drawText(canvas, summaryTxt, getBaseTextPaint(16f, true), width, margin, y)
        }
    }

    fun exportUpcomingSessions(
        context: Context,
        sessionsWithCaseNames: List<Pair<CaseSession, String>>
    ): Uri? {
        return generateStandardPdf(context, "Sessions_Report", "تقرير الجلسات القادمة") { canvas, startY, width ->
            var y = startY
            val bodyPaint = getBaseTextPaint(14f, false)
            val boldPaint = getBaseTextPaint(14f, true)
            val margin = 50f

            if (sessionsWithCaseNames.isEmpty()) {
                drawText(canvas, "لا توجد جلسات قادمة خلال الفترة المحددة.", bodyPaint, width, margin, y)
                return@generateStandardPdf
            }

            sessionsWithCaseNames.forEach { (session, caseName) ->
                y += drawText(canvas, "• تاريخ: ${session.date} | الوقت: ${session.time}", boldPaint, width, margin, y) + 5f
                val details = """
                    القضية: $caseName
                    المحكمة: ${session.court}
                    المطلوب: ${session.requirements}
                """.trimIndent()
                y += drawText(canvas, details, bodyPaint, width - 20, margin + 20f, y) + 15f
            }
        }
    }
}
