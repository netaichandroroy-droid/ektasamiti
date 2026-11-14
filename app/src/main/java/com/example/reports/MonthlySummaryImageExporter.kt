package com.example.reports

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MonthlySummaryImageExporter {

    fun defaultImageFileName(summary: MonthlyFinancialSummary): String {
        return "EktaSamiti_Summary_${summary.monthFileSlug}.png"
    }

    fun renderMonthlySummaryBitmap(summary: MonthlyFinancialSummary): Bitmap {
        val width = 1080
        val sideMargin = 48f
        val contentWidth = width - sideMargin * 2f

        val breakdownRows = summary.memberBreakdowns.take(8)
        val breakdownHeight = if (breakdownRows.isNotEmpty()) {
            110f + breakdownRows.size * 58f + 32f
        } else {
            120f
        }
        val height = (1180f + breakdownHeight + 110f).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Page background
        canvas.drawColor(Color.parseColor("#F4F7F5"))

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCFCE7")
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // 1. Header Banner
        val headerRect = RectF(sideMargin, 44f, width - sideMargin, 248f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left, headerRect.top, headerRect.right, headerRect.bottom,
                Color.parseColor("#0D683B"),
                Color.parseColor("#1B7A49"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, 28f, 28f, headerPaint)
        canvas.drawText("একতা সঞ্চয় ও ঋণ সমিতি", headerRect.left + 36f, headerRect.top + 78f, titlePaint)
        canvas.drawText("মাসিক আর্থিক সারসংক্ষেপ বিবরণী", headerRect.left + 36f, headerRect.top + 124f, subtitlePaint)

        // Month badge inside header
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#92400E")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val monthTextWidth = pillTextPaint.measureText(summary.monthTitleBn)
        val pillRect = RectF(
            headerRect.left + 36f,
            headerRect.top + 146f,
            headerRect.left + 36f + monthTextWidth + 44f,
            headerRect.top + 194f
        )
        canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
        canvas.drawText(summary.monthTitleBn, pillRect.left + 22f, pillRect.top + 33f, pillTextPaint)

        // 2. Hero Total Collection Card
        var cursorY = 278f
        val heroRect = RectF(sideMargin, cursorY, width - sideMargin, cursorY + 148f)
        cardBgPaint.color = Color.parseColor("#E8F7EE")
        borderPaint.color = Color.parseColor("#1B7A49")
        canvas.drawRoundRect(heroRect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(heroRect, 24f, 24f, borderPaint)

        val heroLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#166534")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val heroAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D683B")
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "মাসিক সর্বমোট আদায় (সঞ্চয় জমা + কিস্তি আদায়)",
            heroRect.left + 32f,
            heroRect.top + 52f,
            heroLabelPaint
        )
        canvas.drawText(
            MonthlySummaryCalculator.formatAmount(summary.totalMonthlyCollection),
            heroRect.left + 32f,
            heroRect.top + 116f,
            heroAmountPaint
        )

        // 3. Savings & Loan Cards Side by Side
        cursorY += 176f
        val gap = 22f
        val halfWidth = (contentWidth - gap) / 2f
        val cardHeight = 286f

        // Left: Savings Card
        val savingsRect = RectF(sideMargin, cursorY, sideMargin + halfWidth, cursorY + cardHeight)
        cardBgPaint.color = Color.WHITE
        borderPaint.color = Color.parseColor("#15803D")
        canvas.drawRoundRect(savingsRect, 22f, 22f, cardBgPaint)
        canvas.drawRoundRect(savingsRect, 22f, 22f, borderPaint)

        sectionTitlePaint.color = Color.parseColor("#15803D")
        canvas.drawText("সঞ্চয় হিসাব", savingsRect.left + 26f, savingsRect.top + 52f, sectionTitlePaint)
        drawDivider(canvas, savingsRect.left + 26f, savingsRect.right - 26f, savingsRect.top + 72f)

        drawKeyValueRow(
            canvas = canvas,
            left = savingsRect.left + 26f,
            right = savingsRect.right - 26f,
            y = savingsRect.top + 118f,
            label = "সঞ্চয় জমা (${summary.savingsDepositCount})",
            value = MonthlySummaryCalculator.formatAmount(summary.savingsDepositTotal),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#15803D") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = savingsRect.left + 26f,
            right = savingsRect.right - 26f,
            y = savingsRect.top + 178f,
            label = "সঞ্চয় উত্তোলন (${summary.savingsWithdrawalCount})",
            value = MonthlySummaryCalculator.formatAmount(summary.savingsWithdrawalTotal),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#DC2626") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = savingsRect.left + 26f,
            right = savingsRect.right - 26f,
            y = savingsRect.top + 238f,
            label = "নিট সঞ্চয়",
            value = MonthlySummaryCalculator.formatAmount(summary.netSavings),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#111827") }
        )

        // Right: Loan & Repayment Card
        val loanRect = RectF(savingsRect.right + gap, cursorY, width - sideMargin, cursorY + cardHeight)
        cardBgPaint.color = Color.parseColor("#FFFDF5")
        borderPaint.color = Color.parseColor("#D97706")
        canvas.drawRoundRect(loanRect, 22f, 22f, cardBgPaint)
        canvas.drawRoundRect(loanRect, 22f, 22f, borderPaint)

        sectionTitlePaint.color = Color.parseColor("#B45309")
        canvas.drawText("ঋণ ও কিস্তি হিসাব", loanRect.left + 26f, loanRect.top + 52f, sectionTitlePaint)
        drawDivider(canvas, loanRect.left + 26f, loanRect.right - 26f, loanRect.top + 72f)

        drawKeyValueRow(
            canvas = canvas,
            left = loanRect.left + 26f,
            right = loanRect.right - 26f,
            y = loanRect.top + 118f,
            label = "কিস্তি আদায় (${summary.loanRepaymentCount})",
            value = MonthlySummaryCalculator.formatAmount(summary.loanRepaymentTotal),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#B45309") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = loanRect.left + 26f,
            right = loanRect.right - 26f,
            y = loanRect.top + 178f,
            label = "জরিমানা আদায়",
            value = MonthlySummaryCalculator.formatAmount(summary.loanPenaltyTotal),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#374151") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = loanRect.left + 26f,
            right = loanRect.right - 26f,
            y = loanRect.top + 238f,
            label = "ঋণ বিতরণ (${summary.loanDisbursedCount})",
            value = MonthlySummaryCalculator.formatAmount(summary.loanDisbursedTotal),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#7C3AED") }
        )

        // 4. Overall Standing Card
        cursorY += cardHeight + 28f
        val standingHeight = 250f
        val standingRect = RectF(sideMargin, cursorY, width - sideMargin, cursorY + standingHeight)
        cardBgPaint.color = Color.parseColor("#EFF6FF")
        borderPaint.color = Color.parseColor("#2563EB")
        canvas.drawRoundRect(standingRect, 22f, 22f, cardBgPaint)
        canvas.drawRoundRect(standingRect, 22f, 22f, borderPaint)

        sectionTitlePaint.color = Color.parseColor("#1D4ED8")
        canvas.drawText("সমিতির সার্বিক স্থিতি", standingRect.left + 28f, standingRect.top + 52f, sectionTitlePaint)
        drawDivider(canvas, standingRect.left + 28f, standingRect.right - 28f, standingRect.top + 72f)

        drawKeyValueRow(
            canvas = canvas,
            left = standingRect.left + 28f,
            right = standingRect.right - 28f,
            y = standingRect.top + 116f,
            label = "সক্রিয় সদস্য সংখ্যা",
            value = "${summary.totalActiveMembers} জন",
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#111827") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = standingRect.left + 28f,
            right = standingRect.right - 28f,
            y = standingRect.top + 168f,
            label = "মোট সঞ্চয় আমানত তহবিল",
            value = MonthlySummaryCalculator.formatAmount(summary.totalSavingsFundBalance),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#15803D") }
        )
        drawKeyValueRow(
            canvas = canvas,
            left = standingRect.left + 28f,
            right = standingRect.right - 28f,
            y = standingRect.top + 220f,
            label = "চলমান ঋণ (${summary.activeLoansCount}টি) বকেয়া",
            value = MonthlySummaryCalculator.formatAmount(summary.totalOutstandingLoanDue),
            labelPaint = labelPaint,
            valuePaint = valuePaint.apply { color = Color.parseColor("#DC2626") }
        )

        // 5. Member Breakdown Table
        cursorY += standingHeight + 28f
        val tableRect = RectF(sideMargin, cursorY, width - sideMargin, cursorY + breakdownHeight)
        cardBgPaint.color = Color.WHITE
        borderPaint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(tableRect, 22f, 22f, cardBgPaint)
        canvas.drawRoundRect(tableRect, 22f, 22f, borderPaint)

        sectionTitlePaint.color = Color.parseColor("#111827")
        canvas.drawText("সদস্যভিত্তিক মাসিক আদায়", tableRect.left + 28f, tableRect.top + 52f, sectionTitlePaint)
        drawDivider(canvas, tableRect.left + 28f, tableRect.right - 28f, tableRect.top + 72f)

        if (breakdownRows.isEmpty()) {
            labelPaint.color = Color.parseColor("#6B7280")
            canvas.drawText(
                "এই মাসে কোন সঞ্চয় বা কিস্তি লেনদেন পাওয়া যায়নি।",
                tableRect.left + 28f,
                tableRect.top + 114f,
                labelPaint
            )
        } else {
            val rowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F2937")
                textSize = 24f
            }
            val rowAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D683B")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            breakdownRows.forEachIndexed { idx, item ->
                val rowY = tableRect.top + 118f + idx * 58f
                val detailText = buildList {
                    if (item.savingsDeposited > 0) add("সঞ্চয় ${MonthlySummaryCalculator.formatAmount(item.savingsDeposited)}")
                    if (item.loanRepaid > 0) add("কিস্তি ${MonthlySummaryCalculator.formatAmount(item.loanRepaid)}")
                    if (item.savingsWithdrawn > 0) add("উত্তোলন ${MonthlySummaryCalculator.formatAmount(item.savingsWithdrawn)}")
                }.joinToString(" • ")

                val detailWidth = rowAmountPaint.measureText(detailText)
                val maxNameWidth = (contentWidth - 56f - detailWidth - 28f).coerceAtLeast(220f)
                val rawName = "${idx + 1}. ${item.memberName} (${item.memberCode})"
                val safeName = TextUtils.ellipsize(rawName, rowPaint, maxNameWidth, TextUtils.TruncateAt.END).toString()

                canvas.drawText(safeName, tableRect.left + 28f, rowY, rowPaint)
                canvas.drawText(detailText, tableRect.right - 28f, rowY, rowAmountPaint)
                if (idx < breakdownRows.lastIndex) {
                    drawDivider(canvas, tableRect.left + 28f, tableRect.right - 28f, rowY + 18f)
                }
            }
        }

        // 6. Footer
        val footerY = tableRect.bottom + 54f
        val dateSdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 23f
        }
        canvas.drawText(
            "প্রস্তুতকারক: ${summary.generatedBy}  |  তারিখ: ${dateSdf.format(Date(summary.generatedAtMillis))}",
            sideMargin + 8f,
            footerY,
            footerPaint
        )

        return bitmap
    }

    fun saveBitmapToPictures(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Result<String> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/EktaSamiti")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("ছবি সংরক্ষণের জন্য গ্যালারি তৈরি করা যায়নি")

                try {
                    resolver.openOutputStream(uri)?.use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            "PNG ছবি তৈরি করতে ব্যর্থ হয়েছে"
                        }
                    } ?: error("আউটপুট স্ট্রিম খোলা যায়নি")

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    "Pictures/EktaSamiti/$fileName"
                } catch (t: Throwable) {
                    resolver.delete(uri, null, null)
                    throw t
                }
            } else {
                error("API_BELOW_Q_USE_SAF")
            }
        }
    }

    fun writeBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        uri: Uri
    ): Result<Unit> {
        return runCatching {
            val stream: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: error("ফাইল লেখা যায়নি")
            stream.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                    "PNG ছবি সংরক্ষণ করা যায়নি"
                }
            }
        }
    }

    private fun drawDivider(canvas: Canvas, startX: Float, endX: Float, y: Float) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 2f
        }
        canvas.drawLine(startX, y, endX, y, dividerPaint)
    }

    private fun drawKeyValueRow(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawText(label, left, y, labelPaint)
        canvas.drawText(value, right, y, valuePaint)
    }
}
