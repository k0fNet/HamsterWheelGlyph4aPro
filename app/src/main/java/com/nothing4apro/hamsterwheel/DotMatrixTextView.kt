package com.nothing4apro.hamsterwheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Renders authentic 5×7 Nothing OS Dot-Matrix pixel typography with automatic width-fitting
 * so long headings never clip against switches or card edges.
 */
class DotMatrixTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var text: String = "CHARGING HAMSTER"
        set(value) {
            field = value.uppercase()
            requestLayout()
            invalidate()
        }

    var dotColor: Int = Color.WHITE
        set(value) {
            field = value
            dotPaint.color = value
            invalidate()
        }

    var dotDiameterPx: Float = 6f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var dotGapPx: Float = 2f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    companion object {
        private val GLYPHS: Map<Char, IntArray> = mapOf(
            'A' to intArrayOf(0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
            'B' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110),
            'C' to intArrayOf(0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110),
            'D' to intArrayOf(0b11100, 0b10010, 0b10001, 0b10001, 0b10001, 0b10010, 0b11100),
            'E' to intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111),
            'F' to intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000),
            'G' to intArrayOf(0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110),
            'H' to intArrayOf(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
            'I' to intArrayOf(0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
            'J' to intArrayOf(0b00111, 0b00010, 0b00010, 0b00010, 0b10010, 0b10010, 0b01100),
            'K' to intArrayOf(0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001),
            'L' to intArrayOf(0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111),
            'M' to intArrayOf(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001),
            'N' to intArrayOf(0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001),
            'O' to intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
            'P' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
            'Q' to intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101),
            'R' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001),
            'S' to intArrayOf(0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110),
            'T' to intArrayOf(0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100),
            'U' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
            'V' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100),
            'W' to intArrayOf(0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001),
            'X' to intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001),
            'Y' to intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100),
            'Z' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111),
            '0' to intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
            '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
            '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111),
            '3' to intArrayOf(0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110),
            '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
            ' ' to intArrayOf(0, 0, 0, 0, 0, 0, 0),
            '-' to intArrayOf(0, 0, 0, 0b11111, 0, 0, 0),
            ':' to intArrayOf(0, 0b00100, 0b00100, 0, 0b00100, 0b00100, 0)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val baseStep = dotDiameterPx + dotGapPx
        val totalCols = if (text.isEmpty()) 1 else (text.length * 6 - 1)
        val desiredW = (totalCols * baseStep + paddingLeft + paddingRight + 4f).toInt()
        val resolvedW = resolveSize(desiredW, widthMeasureSpec)

        val availW = (resolvedW - paddingLeft - paddingRight).coerceAtLeast(1)
        val effectiveStep = min(baseStep, availW.toFloat() / totalCols.toFloat())
        val desiredH = (7 * effectiveStep + paddingTop + paddingBottom + 4f).toInt()
        val resolvedH = resolveSize(desiredH, heightMeasureSpec)

        setMeasuredDimension(resolvedW, resolvedH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (text.isEmpty()) return

        val baseStep = dotDiameterPx + dotGapPx
        val totalCols = (text.length * 6 - 1).coerceAtLeast(1)
        val availW = (width - paddingLeft - paddingRight).coerceAtLeast(1)

        // Automatically scale down step & dot radius if the available width is narrower than baseStep
        val step = min(baseStep, availW.toFloat() / totalCols.toFloat())
        val scale = step / baseStep
        val radius = (dotDiameterPx * scale) / 2f

        val textHeight = 7 * step
        val startY = paddingTop + ((height - paddingTop - paddingBottom - textHeight) / 2f).coerceAtLeast(0f)
        var cursorX = paddingLeft.toFloat()

        for (ch in text) {
            val rows = GLYPHS[ch] ?: GLYPHS[' ']!!
            for (r in 0 until 7) {
                val bits = rows[r]
                for (c in 0 until 5) {
                    if (((bits shr (4 - c)) and 1) == 1) {
                        val cx = cursorX + c * step + radius
                        val cy = startY + r * step + radius
                        canvas.drawCircle(cx, cy, radius, dotPaint)
                    }
                }
            }
            cursorX += 6 * step
        }
    }
}
