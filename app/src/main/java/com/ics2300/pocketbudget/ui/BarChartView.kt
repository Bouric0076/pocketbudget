package com.ics2300.pocketbudget.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ics2300.pocketbudget.R
import kotlin.math.max

data class ChartData(val label: String, val value: Double)

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
        color = Color.GRAY // Fallback
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.LTGRAY // Fallback
    }

    private var data: List<ChartData> = emptyList()
    
    // Config
    private val cornerRadius = 12f
    private val barWidthPercent = 0.4f // Thin bars
    
    // Colors
    private var colorPositive = 0
    private var colorGrid = 0
    private var colorText = 0
    
    init {
        colorPositive = ContextCompat.getColor(context, R.color.analytics_teal)
        colorGrid = ContextCompat.getColor(context, R.color.chart_grid_line)
        colorText = ContextCompat.getColor(context, R.color.chart_axis_text)
        
        gridPaint.color = colorGrid
        textPaint.color = colorText
    }

    fun setData(newData: List<ChartData>) {
        this.data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return

        val paddingStart = 80f // Increased for Y labels
        val paddingBottom = 60f 
        val availableWidth = width - paddingStart
        val availableHeight = height - paddingBottom - 40f // Top padding
        
        val maxValue = (data.maxOfOrNull { it.value } ?: 0.0) * 1.2
        
        // Draw Grid Lines (3 lines)
        val steps = 3
        for (i in 0..steps) {
            val y = 40f + (availableHeight / steps) * i
            canvas.drawLine(paddingStart, y, width.toFloat(), y, gridPaint)
            
            // Y Axis Labels
            if (maxValue > 0) {
                val value = maxValue * (1 - i.toFloat() / steps)
                val label = if (value >= 1000) "${(value/1000).toInt()}k" else "${value.toInt()}"
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(label, paddingStart - 20f, y + 10f, textPaint)
            }
        }
        
        if (maxValue == 0.0) return

        val stepX = availableWidth / data.size
        val barWidth = stepX * barWidthPercent
        
        data.forEachIndexed { index, item ->
            val x = paddingStart + index * stepX + (stepX - barWidth) / 2
            val barHeight = (item.value / maxValue * availableHeight).toFloat()
            val top = height - paddingBottom - barHeight
            val bottom = height - paddingBottom
            
            barPaint.color = colorPositive
            
            // Draw Rounded Rect (Top only simulation by drawing full rounded rect clipped? Or just rounded rect)
            // Just standard rounded rect is fine for modern look
            val rect = RectF(x, top, x + barWidth, bottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)
            
            // X Axis Label
            textPaint.textAlign = Paint.Align.CENTER
            val label = if (item.label.length > 3) item.label.substring(0, 3) else item.label
            canvas.drawText(label, x + barWidth / 2, height - 20f, textPaint)
        }
    }
}
