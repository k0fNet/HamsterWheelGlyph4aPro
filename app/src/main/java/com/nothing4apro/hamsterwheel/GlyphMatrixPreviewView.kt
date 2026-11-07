package com.nothing4apro.hamsterwheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Custom View that renders the 13×13 circular Nothing Phone (4a) Pro Glyph Matrix
 * in the exact square-pixel style of the "Charging Hamster" reference app.
 */
class GlyphMatrixPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var frame169 = IntArray(HamsterFrames13x13.TOTAL_CELLS)
    private val cellRect = RectF()
    private val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun updateFrame(newFrame: IntArray) {
        if (newFrame.size == HamsterFrames13x13.TOTAL_CELLS) {
            System.arraycopy(newFrame, 0, frame169, 0, newFrame.size)
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f

        val gridSpan = size * 0.90f
        val cellStep = gridSpan / HamsterFrames13x13.MATRIX_SIZE
        val gap = cellStep * 0.10f
        val startX = cx - gridSpan / 2f
        val startY = cy - gridSpan / 2f

        for (r in 0 until HamsterFrames13x13.MATRIX_SIZE) {
            for (c in 0 until HamsterFrames13x13.MATRIX_SIZE) {
                if (!HamsterFrames13x13.isValidCell(r, c)) continue
                val left = startX + c * cellStep + gap / 2f
                val top = startY + r * cellStep + gap / 2f
                val right = left + cellStep - gap
                val bottom = top + cellStep - gap
                cellRect.set(left, top, right, bottom)

                val brightness = frame169[r * HamsterFrames13x13.MATRIX_SIZE + c].coerceIn(0, 255)
                ledPaint.color = when {
                    brightness >= 180 -> Color.WHITE
                    brightness > 0 -> Color.parseColor("#42424C")
                    else -> Color.parseColor("#0D0D11")
                }
                canvas.drawRoundRect(cellRect, 2f, 2f, ledPaint)
            }
        }
    }
}
