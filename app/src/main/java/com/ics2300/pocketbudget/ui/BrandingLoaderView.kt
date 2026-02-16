package com.ics2300.pocketbudget.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.ics2300.pocketbudget.R
import kotlin.math.min
import android.graphics.drawable.Drawable

class BrandingLoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = ContextCompat.getColor(context, R.color.brand_light_green)
        strokeCap = Paint.Cap.ROUND
    }

    private val bgCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = Color.parseColor("#33FFFFFF")
    }

    private var iconDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_pocketbudget_logo)?.mutate()

    private var progress = 0f
    private val rectF = RectF()
    private var animator: ValueAnimator? = null

    init {
        iconDrawable?.setTint(Color.WHITE)
        startAnimation()
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = (min(w, h) / 2f) - 20f

        if (radius <= 0) return

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Draw background circle
        canvas.drawCircle(cx, cy, radius, bgCirclePaint)

        // Draw animated arc
        val sweepAngle = 360 * progress
        canvas.drawArc(rectF, -90f, sweepAngle, false, circlePaint)

        // Draw Icon in center
        iconDrawable?.let {
            val iconSize = (radius * 1.0f).toInt() 
            // Pulse effect
            val pulse = 1.0f + (0.1f * kotlin.math.sin(progress * Math.PI).toFloat())
            
            canvas.save()
            canvas.scale(pulse, pulse, cx, cy)
            
            val left = (cx - iconSize / 2).toInt()
            val top = (cy - iconSize / 2).toInt()
            val right = left + iconSize
            val bottom = top + iconSize
            
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)
            canvas.restore()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
