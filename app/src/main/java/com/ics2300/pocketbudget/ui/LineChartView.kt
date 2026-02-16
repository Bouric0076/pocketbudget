package com.ics2300.pocketbudget.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3") // Blue
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private var data: List<ChartData> = emptyList()
    
    fun setData(newData: List<ChartData>) {
        this.data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return
        
        val maxValue = data.maxOfOrNull { it.value } ?: 0.0
        if (maxValue == 0.0) return

        val paddingStart = 50f
        val paddingEnd = 50f
        val paddingBottom = 60f
        val availableWidth = width - paddingStart - paddingEnd
        val availableHeight = height - paddingBottom - 50f
        
        val stepX = availableWidth / (data.size - 1).coerceAtLeast(1)
        
        val path = Path()
        
        data.forEachIndexed { index, item ->
            val x = paddingStart + index * stepX
            val y = height - paddingBottom - (item.value / maxValue * availableHeight).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            // Draw Point
            canvas.drawCircle(x, y, 10f, pointPaint)
            
            // Draw Label
            canvas.drawText(item.label, x, height - 20f, textPaint)
        }
        
        canvas.drawPath(path, linePaint)
    }
}
