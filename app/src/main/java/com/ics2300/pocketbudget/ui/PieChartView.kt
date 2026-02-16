package com.ics2300.pocketbudget.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class PieSlice(val label: String, val value: Double, val color: Int)

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 60f // Thicker ring
        strokeCap = Paint.Cap.ROUND
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private var slices: List<PieSlice> = emptyList()
    private val rect = RectF()

    fun setData(newSlices: List<PieSlice>) {
        this.slices = newSlices
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (slices.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) - paint.strokeWidth * 2) / 2f
        
        // Ensure valid radius
        if (radius <= 0) return

        rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        
        var startAngle = -90f // Start from top
        val total = slices.sumOf { it.value }
        
        if (total <= 0.0) return

        slices.forEach { slice ->
            val sweepAngle = ((slice.value / total) * 360).toFloat()
            paint.color = slice.color
            
            // Draw arc
            // Use sweepAngle directly if it's large enough, otherwise ensure visibility
            val drawSweep = if (sweepAngle > 2) sweepAngle - 2 else sweepAngle
            
            canvas.drawArc(rect, startAngle, drawSweep, false, paint)
            
            startAngle += sweepAngle
        }
        
        // Draw total text in center
        // val totalText = "Total"
        // canvas.drawText(totalText, centerX, centerY - 20, textPaint) 
    }
}
