package com.ics2300.pocketbudget.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40

    // Brand Colors (Hardcoded for simplicity or retrieved from resources if Context available)
    private val COLOR_BRAND_DARK = Color.parseColor("#0A3D2E")
    private val COLOR_BRAND_LIGHT = Color.parseColor("#D4E157")
    private val COLOR_BRAND_SECONDARY = Color.parseColor("#1B5E20")
    private val COLOR_TEXT_PRIMARY = Color.parseColor("#212121")
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#757575")
    private val COLOR_ACCENT = Color.parseColor("#00BFA5")
    private val COLOR_TABLE_HEADER = Color.parseColor("#E0E0E0")

    suspend fun exportTransactionsToPdf(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        reportPeriod: String = "All Time"
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            try {
                val paint = Paint()
                val titlePaint = Paint()

                // Create Category Map
                val categoryMap = categories.associate { it.id to it.name }

                // Calculate Summary Data
                val totalIncome = transactions.filter { it.type == "Received" || it.type == "Deposit" }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type != "Received" && it.type != "Deposit" }.sumOf { it.amount }
                val balance = totalIncome - totalExpense
                
                // Group Expenses by Category for Chart
                val expensesByCategory = transactions
                    .filter { it.type != "Received" && it.type != "Deposit" }
                    .groupBy { it.categoryId ?: -1 }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5) // Top 5

                var pageNumber = 1
                var yPosition = MARGIN

                // --- Page 1: Dashboard & Summary ---
                var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                // Draw Header
                drawHeader(canvas, titlePaint, paint, context, reportPeriod)
                yPosition += 80

                // Draw Summary Cards
                drawSummary(canvas, paint, totalIncome, totalExpense, balance, yPosition)
                yPosition += 100

                // Draw Top Categories
                drawTopCategories(canvas, paint, expensesByCategory, categoryMap, yPosition)
                yPosition += 160

                // Draw Transaction Table Header
                drawTableHeader(canvas, paint, yPosition)
                yPosition += 30

                // Draw Transactions
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                
                // Sort transactions by date desc
                val sortedTransactions = transactions.sortedByDescending { it.timestamp }

                for (transaction in sortedTransactions) {
                    if (yPosition > PAGE_HEIGHT - MARGIN) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = MARGIN + 40 // Leave space for header on subsequent pages if needed
                        drawTableHeader(canvas, paint, yPosition) // Redraw table header
                        yPosition += 30
                    }

                    drawTransactionRow(canvas, paint, transaction, categoryMap, dateFormat, yPosition)
                    yPosition += 25
                }

                pdfDocument.finishPage(page)

                // Write to file
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    throw Exception("Could not open output stream for URI: $uri")
                }

                outputStream.use { stream ->
                    pdfDocument.writeTo(stream)
                }
                
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            } finally {
                pdfDocument.close()
            }
        }
    }

    private fun drawHeader(canvas: Canvas, titlePaint: Paint, paint: Paint, context: Context, reportPeriod: String) {
        // App Title
        titlePaint.color = COLOR_BRAND_DARK
        titlePaint.textSize = 24f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("PocketBudget", MARGIN.toFloat(), MARGIN + 20f, titlePaint)

        // Subtitle / Tagline
        paint.color = COLOR_TEXT_SECONDARY
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Financial Overview: $reportPeriod", MARGIN.toFloat(), MARGIN + 40f, paint)

        // Date
        val dateParams = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: $dateParams", (PAGE_WIDTH - MARGIN).toFloat(), MARGIN + 20f, paint)
        
        // Separator Line
        paint.color = COLOR_BRAND_SECONDARY
        paint.strokeWidth = 2f
        canvas.drawLine(MARGIN.toFloat(), MARGIN + 55f, (PAGE_WIDTH - MARGIN).toFloat(), MARGIN + 55f, paint)
    }

    private fun drawSummary(canvas: Canvas, paint: Paint, income: Double, expense: Double, balance: Double, startY: Int) {
        val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 20) / 3
        val boxHeight = 60f
        
        // Income Box
        drawStatBox(canvas, paint, "Income", income, MARGIN.toFloat(), startY.toFloat(), boxWidth.toFloat(), boxHeight, Color.parseColor("#E8F5E9"), COLOR_BRAND_SECONDARY)

        // Expense Box
        drawStatBox(canvas, paint, "Expense", expense, MARGIN.toFloat() + boxWidth + 10, startY.toFloat(), boxWidth.toFloat(), boxHeight, Color.parseColor("#FFEBEE"), Color.RED)

        // Balance Box
        drawStatBox(canvas, paint, "Balance", balance, MARGIN.toFloat() + (boxWidth * 2) + 20, startY.toFloat(), boxWidth.toFloat(), boxHeight, Color.parseColor("#E3F2FD"), COLOR_BRAND_DARK)
    }

    private fun drawStatBox(canvas: Canvas, paint: Paint, label: String, amount: Double, x: Float, y: Float, width: Float, height: Float, bgColor: Int, textColor: Int) {
        // Background
        paint.style = Paint.Style.FILL
        paint.color = bgColor
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Label
        paint.color = COLOR_TEXT_SECONDARY
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, x + 10, y + 20, paint)

        // Amount
        paint.color = textColor
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(CurrencyFormatter.formatKsh(amount), x + 10, y + 45, paint)
    }

    private fun drawTopCategories(canvas: Canvas, paint: Paint, expenses: List<Pair<Int, Double>>, categoryMap: Map<Int, String>, startY: Int) {
        paint.color = COLOR_TEXT_PRIMARY
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Top Spending Categories", MARGIN.toFloat(), startY.toFloat(), paint)

        var y = startY + 20f
        val maxAmount = expenses.maxOfOrNull { it.second } ?: 1.0
        val barMaxWidth = PAGE_WIDTH - (MARGIN * 2) - 150f // Leave space for text

        for ((catId, amount) in expenses) {
            val catName = categoryMap[catId] ?: "Uncategorized"
            
            // Category Name
            paint.color = COLOR_TEXT_SECONDARY
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(catName, MARGIN.toFloat(), y + 10, paint)

            // Bar
            var barWidth = 0f
            if (maxAmount > 0) {
                barWidth = (amount / maxAmount * barMaxWidth).toFloat()
                paint.color = COLOR_ACCENT
                canvas.drawRect(MARGIN + 100f, y, MARGIN + 100f + barWidth, y + 15, paint)
            }

            // Amount
            paint.color = COLOR_TEXT_PRIMARY
            canvas.drawText(CurrencyFormatter.formatKsh(amount), MARGIN + 100f + barWidth + 10, y + 12, paint)

            y += 25
        }
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Int) {
        // Background
        paint.color = COLOR_BRAND_DARK
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN.toFloat(), y.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), y + 25f, paint)

        // Text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT

        val col1 = MARGIN + 10f
        val col2 = MARGIN + 100f
        val col3 = MARGIN + 280f
        val col4 = MARGIN + 420f

        canvas.drawText("Date", col1, y + 17f, paint)
        canvas.drawText("Party / Description", col2, y + 17f, paint)
        canvas.drawText("Category", col3, y + 17f, paint)
        canvas.drawText("Amount", col4, y + 17f, paint)
    }

    private fun drawTransactionRow(canvas: Canvas, paint: Paint, transaction: TransactionEntity, categoryMap: Map<Int, String>, dateFormat: SimpleDateFormat, y: Int) {
        paint.color = COLOR_TEXT_PRIMARY
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT

        val col1 = MARGIN + 10f
        val col2 = MARGIN + 100f
        val col3 = MARGIN + 280f
        val col4 = MARGIN + 420f

        // Date
        canvas.drawText(dateFormat.format(Date(transaction.timestamp)), col1, y + 15f, paint)

        // Party (Truncate if too long)
        var party = transaction.partyName
        if (party.length > 25) party = party.substring(0, 22) + "..."
        canvas.drawText(party, col2, y + 15f, paint)

        // Category
        val catName = categoryMap[transaction.categoryId] ?: "Uncategorized"
        canvas.drawText(catName, col3, y + 15f, paint)

        // Amount & Type Color
        if (transaction.type == "Received" || transaction.type == "Deposit") {
            paint.color = COLOR_BRAND_SECONDARY
        } else {
            paint.color = Color.RED // Expense
        }
        canvas.drawText(CurrencyFormatter.formatKsh(transaction.amount), col4, y + 15f, paint)

        // Divider
        paint.color = Color.LTGRAY
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN.toFloat(), y + 22f, (PAGE_WIDTH - MARGIN).toFloat(), y + 22f, paint)
    }
}
