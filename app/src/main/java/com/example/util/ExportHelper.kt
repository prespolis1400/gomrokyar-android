package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.logic.CalculationResult
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private fun formatNum(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(number)
    }

    private fun getPersianTypeface(context: Context, isBold: Boolean = false): Typeface {
        return try {
            val base = ResourcesCompat.getFont(context, R.font.vazirmatn) ?: Typeface.SANS_SERIF
            if (isBold) Typeface.create(base, Typeface.BOLD) else base
        } catch (_: Exception) {
            if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    /**
     * Generates an official, beautifully structured PDF document of the calculation results.
     */
    fun generateResultPdf(context: Context, result: CalculationResult): Uri {
        val pdfDocument = PdfDocument()
        
        // Standard A4 size: 595 x 842 points
        // If box042 has many chips, extend height so all data fits comfortably on 1 pristine page
        val extraRows = result.box042Chips.size
        val pageHeight = (842 + (extraRows * 18).coerceAtLeast(0)).coerceAtLeast(842)
        val pageWidth = 595
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val regularFont = getPersianTypeface(context, false)
        val boldFont = getPersianTypeface(context, true)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Header Background Card
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Deep navy
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, headerPaint)

        // Top Accent Stripe
        val stripePaint = Paint().apply {
            color = Color.parseColor("#00F3FF") // Neon Cyan
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 5f, stripePaint)

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 17f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("گزارش رسمی محاسبات حقوق و عوارض گمرکی", pageWidth - 30f, 40f, titlePaint)

        // Header Subtitle & Date
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = regularFont
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val modeText = if (result.mode == "taavoni") "رویه: تعاونی مرزنشینان" else "رویه: تجاری آزاد"
        val dateString = DateTimeHelper.getDualDateStringForPdf()
        canvas.drawText("$modeText   |   $dateString", pageWidth - 30f, 62f, subTitlePaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F3FF")
            typeface = boldFont
            textSize = 10f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("IRAN CUSTOMS CALC", 30f, 50f, badgePaint)

        // --- HERO CARDS ---
        var currentY = 115f

        // 1. Cost Per Kg Card
        val heroBoxPaint = Paint().apply {
            color = Color.parseColor("#0284C7") // Vibrant primary blue
            style = Paint.Style.FILL
        }
        val heroRect = RectF(30f, currentY, pageWidth - 30f, currentY + 70f)
        canvas.drawRoundRect(heroRect, 10f, 10f, heroBoxPaint)

        val heroLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            typeface = regularFont
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("بهای تمام‌شده هر کیلوگرم خالص (با احتساب کلیه هزینه‌ها)", pageWidth / 2f, currentY + 25f, heroLabelPaint)

        val heroValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${formatNum(result.costPerKg)} ریال", pageWidth / 2f, currentY + 55f, heroValuePaint)

        currentY += 85f

        // --- TABLE SECTION ---
        val tableMargin = 30f
        val tableWidth = pageWidth - (tableMargin * 2)

        // Table Header
        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val tableHeaderRect = RectF(tableMargin, currentY, tableMargin + tableWidth, currentY + 26f)
        canvas.drawRoundRect(tableHeaderRect, 6f, 6f, tableHeaderPaint)

        val tableHeaderTxtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 10f
        }
        tableHeaderTxtPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("شرح آیتم و مبانی محاسبه", tableMargin + tableWidth - 14f, currentY + 17f, tableHeaderTxtPaint)

        tableHeaderTxtPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("مبلغ محاسباتی (ریال)", tableMargin + 14f, currentY + 17f, tableHeaderTxtPaint)

        currentY += 28f

        // Table Rows List
        data class TableRowData(val title: String, val amount: String, val isSubItem: Boolean = false, val isHighlight: Boolean = false)
        val rows = mutableListOf<TableRowData>()

        rows.add(TableRowData("بیمه حمل و نقل کالا", "${formatNum(result.insurance)} ریال"))
        rows.add(TableRowData("ارزش کل ریالی (سیف / CIF)", "${formatNum(result.cifValue)} ریال"))
        rows.add(TableRowData("حقوق ورودی گمرکی", "${formatNum(result.tariff)} ریال"))
        rows.add(TableRowData("مالیات بر ارزش افزوده (۱۰٪)", "${formatNum(result.vat)} ریال"))
        rows.add(TableRowData("مالیات ۲٪ علی‌الحساب واردات", "${formatNum(result.tax4)} ریال"))
        rows.add(TableRowData("عوارض استاندارد", "${formatNum(result.standard)} ریال"))
        rows.add(TableRowData("عوارض نظارت بر غذا و دارو", "${formatNum(result.foodDrug)} ریال"))

        // Box 042 Summary
        rows.add(TableRowData("تجمیع عوارض باکس ۰۴۲ (مجموع)", "${formatNum(result.box042Total)} ریال", isHighlight = true))
        for (chip in result.box042Chips) {
            rows.add(TableRowData("   • ${chip.name}", "${formatNum(chip.cost)} ریال", isSubItem = true))
        }

        // Taavoni discount
        if (result.mode == "taavoni") {
            val discountStatus = if (result.applyDiscount) " (اعمال تخفیف ۵۰٪)" else " (تخفیف کامل)"
            rows.add(TableRowData("باکس ۰۴۹ - تخفیف حقوق ورودی$discountStatus", "- ${formatNum(result.box049)} ریال", isHighlight = true))
        }

        // Paint definitions for rows
        val rowBgPaint1 = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val rowBgPaint2 = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val rowTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            typeface = regularFont
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val rowSubTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = regularFont
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        val rowAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = boldFont
            textSize = 9.5f
            textAlign = Paint.Align.LEFT
        }

        val rowHeight = 22f

        rows.forEachIndexed { index, rowData ->
            val rY = currentY
            val bg = if (rowData.isHighlight) {
                Paint().apply { color = Color.parseColor("#E0F2FE"); style = Paint.Style.FILL }
            } else if (index % 2 == 0) rowBgPaint1 else rowBgPaint2

            canvas.drawRect(tableMargin, rY, tableMargin + tableWidth, rY + rowHeight, bg)
            canvas.drawRect(tableMargin, rY, tableMargin + tableWidth, rY + rowHeight, borderPaint)

            val tPaint = if (rowData.isSubItem) rowSubTitlePaint else rowTitlePaint
            canvas.drawText(rowData.title, tableMargin + tableWidth - 12f, rY + 15f, tPaint)
            canvas.drawText(rowData.amount, tableMargin + 12f, rY + 15f, rowAmountPaint)

            currentY += rowHeight
        }

        // Final Total Row
        currentY += 6f
        val totalBgPaint = Paint().apply {
            color = Color.parseColor("#059669") // Forest emerald green
            style = Paint.Style.FILL
        }
        val totalRect = RectF(tableMargin, currentY, tableMargin + tableWidth, currentY + 36f)
        canvas.drawRoundRect(totalRect, 6f, 6f, totalBgPaint)

        val totalTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val totalText = if (result.mode == "taavoni") "جمع پرداختی نهایی (تعاونی):" else "جمع کل پرداختی حقوق و عوارض:"
        canvas.drawText(totalText, tableMargin + tableWidth - 14f, currentY + 23f, totalTitlePaint)

        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 14f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("${formatNum(result.totalPayment)} ریال", tableMargin + 14f, currentY + 23f, totalValuePaint)

        // Footer note
        currentY += 50f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = regularFont
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("این سند به صورت خودکار توسط سامانه محاسبه‌گر هوشمند ترخیص کالا و گمرک تهیه و تولید شده است.", pageWidth / 2f, currentY, footerPaint)

        pdfDocument.finishPage(page)

        // Write to cache file
        val pdfFile = File(context.cacheDir, "customs_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
    }

    /**
     * Generates a high-resolution, pixel-perfect styled screenshot card image (PNG)
     * of all the calculation results.
     */
    fun generateResultBitmap(context: Context, result: CalculationResult): Uri {
        val width = 1080
        // Dynamically compute height based on number of box 042 chips
        val baseHeight = 1750
        val chipHeight = result.box042Chips.size * 45
        val height = baseHeight + chipHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val regularFont = getPersianTypeface(context, false)
        val boldFont = getPersianTypeface(context, true)

        // Dark modern background with subtle gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#0B132B"),
                Color.parseColor("#1C2541"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top Accent Neon Glow Bar
        val accentPaint = Paint().apply {
            color = Color.parseColor("#00F3FF")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, accentPaint)

        var y = 80f

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("نتایج رسمی محاسبات گمرک", width / 2f, y, titlePaint)
        y += 50f

        // Mode badge & Dual Date
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F3FF")
            typeface = boldFont
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        val modeText = if (result.mode == "taavoni") "رویه: تعاونی مرزنشینان" else "رویه: تجاری آزاد"
        canvas.drawText(modeText, width / 2f, y, subPaint)
        y += 36f

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = regularFont
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val dualDate = DateTimeHelper.getDualDateStringForImage()
        canvas.drawText(dualDate, width / 2f, y, datePaint)
        y += 50f

        val margin = 50f
        val cardWidth = width - (margin * 2)

        // Hero Card 1: Cost Per Kg
        val heroRect = RectF(margin, y, margin + cardWidth, y + 170f)
        val heroBg = Paint().apply {
            shader = LinearGradient(
                margin, y, margin + cardWidth, y + 170f,
                Color.parseColor("#0284C7"),
                Color.parseColor("#0369A1"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(heroRect, 28f, 28f, heroBg)

        val heroLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            typeface = regularFont
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("بهای تمام‌شده هر کیلوگرم خالص", width / 2f, y + 55f, heroLabelPaint)

        val heroValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${formatNum(result.costPerKg)} ریال", width / 2f, y + 125f, heroValPaint)

        y += 205f

        // Main Detailed Breakdown Card (Dark Container)
        val containerTop = y
        val detailsCardPaint = Paint().apply {
            color = Color.parseColor("#151E33")
            style = Paint.Style.FILL
        }

        // Draw items
        data class ItemRow(val title: String, val amount: String, val isSub: Boolean = false, val isHighlight: Boolean = false)
        val items = mutableListOf<ItemRow>()
        items.add(ItemRow("بیمه حمل", "${formatNum(result.insurance)} ریال"))
        items.add(ItemRow("ارزش کل ریالی (سیف)", "${formatNum(result.cifValue)} ریال"))
        items.add(ItemRow("حقوق ورودی", "${formatNum(result.tariff)} ریال"))
        items.add(ItemRow("ارزش افزوده (۱۰٪)", "${formatNum(result.vat)} ریال"))
        items.add(ItemRow("مالیات ۲٪ علی‌الحساب", "${formatNum(result.tax4)} ریال"))
        items.add(ItemRow("عوارض استاندارد", "${formatNum(result.standard)} ریال"))
        items.add(ItemRow("غذا و دارو", "${formatNum(result.foodDrug)} ریال"))
        
        items.add(ItemRow("تجمیع عوارض باکس ۰۴۲ (مجموع)", "${formatNum(result.box042Total)} ریال", isHighlight = true))
        for (chip in result.box042Chips) {
            items.add(ItemRow("   • ${chip.name}", "${formatNum(chip.cost)} ریال", isSub = true))
        }

        if (result.mode == "taavoni") {
            items.add(ItemRow("باکس ۰۴۹ (تخفیف)", "- ${formatNum(result.box049)} ریال", isHighlight = true))
        }

        val rowHeight = 52f
        val containerHeight = (items.size * rowHeight) + 40f
        val containerRect = RectF(margin, containerTop, margin + cardWidth, containerTop + containerHeight)
        canvas.drawRoundRect(containerRect, 24f, 24f, detailsCardPaint)

        // Outline border
        val outlinePaint = Paint().apply {
            color = Color.parseColor("#243356")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(containerRect, 24f, 24f, outlinePaint)

        var itemY = containerTop + 35f

        val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            typeface = regularFont
            textSize = 25f
            textAlign = Paint.Align.RIGHT
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = regularFont
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }
        val itemValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 25f
            textAlign = Paint.Align.LEFT
        }
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            typeface = boldFont
            textSize = 25f
            textAlign = Paint.Align.RIGHT
        }
        val highlightValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            typeface = boldFont
            textSize = 25f
            textAlign = Paint.Align.LEFT
        }

        for (item in items) {
            val tPaint = if (item.isHighlight) highlightPaint else if (item.isSub) subTitlePaint else itemTitlePaint
            val vPaint = if (item.isHighlight) highlightValPaint else itemValPaint

            canvas.drawText(item.title, margin + cardWidth - 30f, itemY + 22f, tPaint)
            canvas.drawText(item.amount, margin + 30f, itemY + 22f, vPaint)

            itemY += rowHeight
        }

        y = containerTop + containerHeight + 35f

        // Hero Card 2: Total Payment
        val totalRect = RectF(margin, y, margin + cardWidth, y + 150f)
        val totalBg = Paint().apply {
            shader = LinearGradient(
                margin, y, margin + cardWidth, y + 150f,
                Color.parseColor("#059669"),
                Color.parseColor("#10B981"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(totalRect, 24f, 24f, totalBg)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1FAE5")
            typeface = boldFont
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }
        val totalPaymentTitle = if (result.mode == "taavoni") "جمع پرداختی (تعاونی):" else "جمع کل پرداختی به گمرک:"
        canvas.drawText(totalPaymentTitle, margin + cardWidth - 35f, y + 85f, totalLabelPaint)

        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = boldFont
            textSize = 38f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("${formatNum(result.totalPayment)} ریال", margin + 35f, y + 87f, totalValPaint)

        y += 190f

        // Watermark Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = regularFont
            textSize = 21f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("سامانه هوشمند محاسبات گمرک • تولید شده در نرم‌افزار", width / 2f, y, footerPaint)

        // Save Bitmap as PNG in cacheDir
        val imageFile = File(context.cacheDir, "customs_result_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }

    /**
     * Helper to launch chooser with appropriate intent flags.
     */
    fun shareContent(context: Context, uri: Uri, mimeType: String, subject: String = "نتایج محاسبه گمرک") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Share both PDF and Image together in one go!
     */
    fun shareMultiple(context: Context, uris: ArrayList<Uri>, subject: String = "نتایج کامل محاسبه گمرک (PDF و تصویر)") {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
