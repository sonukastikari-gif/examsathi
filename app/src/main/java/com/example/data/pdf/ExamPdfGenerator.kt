package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.branding.BrandingConfig
import com.example.data.branding.BrandingManager
import com.example.domain.model.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-performance, offline PDF scorecard and performance report generator.
 * Uses Android's native android.graphics.pdf.PdfDocument for zero-dependency,
 * completely offline, non-blocking PDF generation.
 */
class ExamPdfGenerator(
    private val context: Context,
    private val brandingManager: BrandingManager = BrandingManager.getInstance(context)
) {

    // A4 dimensions in points (72 points per inch)
    private val pageWidth = 595
    private val pageHeight = 842
    private val marginLeft = 36f
    private val marginRight = 559f
    private val contentWidth = marginRight - marginLeft // 523 points
    private val marginTop = 36f
    private val marginBottom = 806f

    /**
     * Generates a complete, multi-page ExamSathi scorecard PDF report offline.
     */
    suspend fun generateScorecardPdf(result: TestResult, currentContext: Context? = null): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val activeContext = currentContext?.applicationContext ?: context.applicationContext
            val brandingConfig = brandingManager.brandingConfig.value
            val logoBitmap = brandingManager.getLogoBitmap(targetSize = 100)

            val reportsDir = File(activeContext.cacheDir, "reports").apply { mkdirs() }
            val sanitizedId = result.id.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val pdfFile = File(reportsDir, "ExamSathi_Scorecard_$sanitizedId.pdf")

            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 10f
            }

            // Draw Header on Page 1
            var currentY = marginTop
            currentY = drawReportHeader(canvas, brandingConfig, logoBitmap, currentY)
            currentY = drawPerformanceSummaryCard(canvas, result, currentY)

            // Section Header: Question Wise Review
            currentY += 15f
            currentY = drawSectionTitle(canvas, "QUESTION-WISE PERFORMANCE & SOLUTIONS", currentY)

            // Iterate and draw each response snapshot
            for (response in result.responses) {
                val estimatedHeight = estimateQuestionBlockHeight(response)
                // Check page overflow
                if (currentY + estimatedHeight > marginBottom - 30f) {
                    drawPageFooter(canvas, brandingConfig, pageNumber)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = marginTop
                    currentY = drawRunningPageHeader(canvas, brandingConfig, result.testTitle, currentY)
                }

                currentY = drawQuestionBlock(canvas, response, currentY)
                currentY += 10f
            }

            // Finish the last page with footer
            drawPageFooter(canvas, brandingConfig, pageNumber)
            pdfDocument.finishPage(page)

            // Write to file
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            pdfFile
        }
    }

    private fun drawReportHeader(
        canvas: Canvas,
        branding: BrandingConfig,
        logo: Bitmap,
        startY: Float
    ): Float {
        var y = startY

        // Background banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1B4B") // Deep Navy Indigo
            style = Paint.Style.FILL
        }
        val bannerRect = RectF(marginLeft, y, marginRight, y + 68f)
        canvas.drawRoundRect(bannerRect, 10f, 10f, bannerPaint)

        // Logo
        val logoDst = Rect((marginLeft + 12).toInt(), (y + 10).toInt(), (marginLeft + 60).toInt(), (y + 58).toInt())
        canvas.drawBitmap(logo, null, logoDst, null)

        // Brand Name
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText(branding.brandName, marginLeft + 72f, y + 28f, titlePaint)

        // Tagline
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 9.5f
        }
        canvas.drawText(branding.tagline, marginLeft + 72f, y + 42f, tagPaint)

        // Organization
        val orgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE047") // Amber accent
            textSize = 8.5f
            isFakeBoldText = true
        }
        canvas.drawText(branding.organizationName.uppercase(Locale.ROOT), marginLeft + 72f, y + 55f, orgPaint)

        // Subtitle badge on right side
        val badgeText = "OFFICIAL SCORECARD"
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(badgeText, marginRight - 14f, y + 36f, badgePaint)

        return y + 78f
    }

    private fun drawRunningPageHeader(
        canvas: Canvas,
        branding: BrandingConfig,
        testTitle: String,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4338CA")
            textSize = 9f
            isFakeBoldText = true
        }
        canvas.drawText("${branding.brandName} Scorecard — $testTitle", marginLeft, y + 10f, paint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(marginLeft, y + 16f, marginRight, y + 16f, linePaint)
        return y + 26f
    }

    private fun drawPerformanceSummaryCard(canvas: Canvas, result: TestResult, startY: Float): Float {
        var y = startY

        // Card Container
        val cardHeight = 135f
        val cardRect = RectF(marginLeft, y, marginRight, y + cardHeight)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Test Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            isFakeBoldText = true
        }
        canvas.drawText(result.testTitle, marginLeft + 14f, y + 20f, titlePaint)

        // Date & Duration
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(result.timestamp))
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
        }
        val minutesTaken = result.timeTakenSeconds / 60
        val secondsTaken = result.timeTakenSeconds % 60
        val durationInfo = "Exam Category: ${result.testCategory}  |  Attempted: $dateStr  |  Time: ${minutesTaken}m ${secondsTaken}s (of ${result.durationMinutes}m)"
        canvas.drawText(durationInfo, marginLeft + 14f, y + 34f, subPaint)

        // Separator line
        val sepPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(marginLeft + 14f, y + 42f, marginRight - 14f, y + 42f, sepPaint)

        // 4 Stat Metric Cards (Score, Accuracy, Correct/Wrong/Skipped, Net Marks)
        val statY = y + 50f
        val statWidth = (contentWidth - 28f - 18f) / 3f

        // Box 1: Overall Score
        drawStatBadge(
            canvas,
            x = marginLeft + 14f,
            y = statY,
            width = statWidth,
            height = 42f,
            label = "TOTAL SCORE",
            value = String.format(Locale.US, "%.1f / %.1f", result.score, result.maxScore),
            subValue = "${result.totalQuestions} Total Questions",
            bgColor = "#EFF6FF",
            accentColor = "#1D4ED8"
        )

        // Box 2: Accuracy
        drawStatBadge(
            canvas,
            x = marginLeft + 14f + statWidth + 9f,
            y = statY,
            width = statWidth,
            height = 42f,
            label = "ACCURACY",
            value = String.format(Locale.US, "%.1f%%", result.accuracyPercentage),
            subValue = "${result.correctAnswers + result.wrongAnswers} Attempted",
            bgColor = "#F0FDF4",
            accentColor = "#15803D"
        )

        // Box 3: Time Taken
        drawStatBadge(
            canvas,
            x = marginLeft + 14f + (statWidth + 9f) * 2,
            y = statY,
            width = statWidth,
            height = 42f,
            label = "TIME SPENT",
            value = String.format(Locale.US, "%dm %02ds", minutesTaken, secondsTaken),
            subValue = "Allotted: ${result.durationMinutes}m",
            bgColor = "#FAF5FF",
            accentColor = "#7E22CE"
        )

        // Breakdown row (Correct / Wrong / Skipped)
        val breakdownY = y + 104f
        val badgeW = (contentWidth - 28f - 16f) / 3f
        drawMiniBadge(canvas, marginLeft + 14f, breakdownY, badgeW, "Correct: ${result.correctAnswers}", "#DCFCE7", "#166534")
        drawMiniBadge(canvas, marginLeft + 14f + badgeW + 8f, breakdownY, badgeW, "Wrong: ${result.wrongAnswers}", "#FEE2E2", "#991B1B")
        drawMiniBadge(canvas, marginLeft + 14f + (badgeW + 8f) * 2, breakdownY, badgeW, "Skipped: ${result.skippedAnswers}", "#F1F5F9", "#475569")

        return y + cardHeight
    }

    private fun drawStatBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        subValue: String,
        bgColor: String,
        accentColor: String
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(bgColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, bgPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(accentColor)
            textSize = 7.5f
            isFakeBoldText = true
        }
        canvas.drawText(label, x + 8f, y + 12f, labelPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(accentColor)
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText(value, x + 8f, y + 26f, valPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 7.5f
        }
        canvas.drawText(subValue, x + 8f, y + 37f, subPaint)
    }

    private fun drawMiniBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        text: String,
        bgColor: String,
        textColor: String
    ) {
        val rect = RectF(x, y, x + width, y + 20f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(bgColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(textColor)
            textSize = 9f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, x + width / 2f, y + 14f, textPaint)
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, startY: Float): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            isFakeBoldText = true
        }
        canvas.drawText(title, marginLeft, y + 12f, paint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#6366F1") // Indigo
            strokeWidth = 2f
        }
        canvas.drawLine(marginLeft, y + 16f, marginLeft + 80f, y + 16f, linePaint)
        return y + 24f
    }

    private fun drawQuestionBlock(
        canvas: Canvas,
        q: com.example.domain.model.StudentResponseSnapshot,
        startY: Float
    ): Float {
        var y = startY
        val height = estimateQuestionBlockHeight(q)

        // Card boundary
        val cardRect = RectF(marginLeft, y, marginRight, y + height)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        var blockY = y + 14f

        // Question header: Q. 1 | Subject: ... | Status
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
            isFakeBoldText = true
        }
        val qLabel = "QUESTION ${q.questionNumber}  •  ${q.subject} > ${q.topic}"
        canvas.drawText(qLabel, marginLeft + 10f, blockY, headerPaint)

        // Status badge
        val statusText: String
        val statusBg: String
        val statusColor: String
        if (q.selectedOptionIndex == null) {
            statusText = "SKIPPED (0.0)"
            statusBg = "#F1F5F9"
            statusColor = "#475569"
        } else if (q.isCorrect) {
            statusText = String.format(Locale.US, "CORRECT (+%.1f)", q.marksEarned)
            statusBg = "#DCFCE7"
            statusColor = "#166534"
        } else {
            statusText = String.format(Locale.US, "INCORRECT (%.2f)", q.marksEarned)
            statusBg = "#FEE2E2"
            statusColor = "#991B1B"
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(statusBg)
            style = Paint.Style.FILL
        }
        val badgeW = 90f
        canvas.drawRoundRect(RectF(marginRight - 10f - badgeW, blockY - 10f, marginRight - 10f, blockY + 4f), 3f, 3f, badgePaint)

        val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(statusColor)
            textSize = 7.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(statusText, marginRight - 10f - badgeW / 2f, blockY - 1f, statusTextPaint)

        blockY += 14f

        // Question Text (Word wrapped)
        val qTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            isFakeBoldText = true
        }
        val qLines = wrapText(q.questionText, qTextPaint, contentWidth - 20f)
        for (line in qLines) {
            canvas.drawText(line, marginLeft + 10f, blockY, qTextPaint)
            blockY += 12f
        }
        blockY += 4f

        // Options List (A, B, C, D)
        val optPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
        }
        for (i in q.options.indices) {
            val letter = ('A' + i).toString()
            val isSelected = q.selectedOptionIndex == i
            val isCorrectOpt = q.correctOptionIndex == i

            val prefix: String
            val optColor: String
            if (isSelected && isCorrectOpt) {
                prefix = "[$letter] (Your Answer - Correct) "
                optColor = "#166534"
                optPaint.isFakeBoldText = true
            } else if (isSelected && !isCorrectOpt) {
                prefix = "[$letter] (Your Answer - Incorrect) "
                optColor = "#991B1B"
                optPaint.isFakeBoldText = true
            } else if (isCorrectOpt) {
                prefix = "[$letter] (Correct Answer) "
                optColor = "#15803D"
                optPaint.isFakeBoldText = true
            } else {
                prefix = "[$letter] "
                optColor = "#475569"
                optPaint.isFakeBoldText = false
            }

            optPaint.color = Color.parseColor(optColor)
            val fullOptionText = "$prefix${q.options[i]}"
            val optLines = wrapText(fullOptionText, optPaint, contentWidth - 20f)
            for (line in optLines) {
                canvas.drawText(line, marginLeft + 10f, blockY, optPaint)
                blockY += 11f
            }
        }

        // Explanation Box
        if (q.explanation.isNotBlank()) {
            blockY += 4f
            val expLines = wrapText("Solution & Explanation: ${q.explanation}", Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f }, contentWidth - 32f)
            val expHeight = expLines.size * 11f + 12f

            val expBoxRect = RectF(marginLeft + 8f, blockY, marginRight - 8f, blockY + expHeight)
            val expBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val expBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawRoundRect(expBoxRect, 4f, 4f, expBgPaint)
            canvas.drawRoundRect(expBoxRect, 4f, 4f, expBorderPaint)

            var expY = blockY + 10f
            val expTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 8f
            }
            for (line in expLines) {
                canvas.drawText(line, marginLeft + 14f, expY, expTextPaint)
                expY += 11f
            }
        }

        return y + height
    }

    private fun estimateQuestionBlockHeight(q: com.example.domain.model.StudentResponseSnapshot): Float {
        var h = 32f // header & padding

        // Question text lines
        val qPaint = Paint().apply { textSize = 9.5f }
        val qLines = wrapText(q.questionText, qPaint, contentWidth - 20f)
        h += qLines.size * 12f + 4f

        // Options lines
        val optPaint = Paint().apply { textSize = 8.5f }
        for (i in q.options.indices) {
            val optLines = wrapText("[D] ${q.options[i]}", optPaint, contentWidth - 20f)
            h += optLines.size * 11f
        }

        // Explanation
        if (q.explanation.isNotBlank()) {
            val expPaint = Paint().apply { textSize = 8f }
            val expLines = wrapText("Solution & Explanation: ${q.explanation}", expPaint, contentWidth - 32f)
            h += expLines.size * 11f + 16f
        }

        return h + 10f
    }

    private fun drawPageFooter(canvas: Canvas, branding: BrandingConfig, pageNum: Int) {
        val y = pageHeight - 24f
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(marginLeft, y - 8f, marginRight, y - 8f, linePaint)

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8f
        }
        canvas.drawText("${branding.brandName} • ${branding.subTagline}", marginLeft, y, leftPaint)

        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNum", marginRight, y, rightPaint)
    }

    /**
     * Splits text into wrapped lines fitting within the specified maximum width.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (para in paragraphs) {
            if (para.isBlank()) {
                lines.add("")
                continue
            }
            val words = para.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        return lines
    }

    companion object {
        @Volatile
        private var INSTANCE: ExamPdfGenerator? = null

        fun getInstance(context: Context): ExamPdfGenerator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExamPdfGenerator(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun resetForTesting() {
            INSTANCE = null
        }
    }
}
