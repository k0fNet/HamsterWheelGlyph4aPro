package com.nothing4apro.hamsterwheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Renders authentic 5×7 Nothing OS Dot-Matrix pixel typography (like "CHARGING HAMSTER"
 * and "WIDGETS  TOYS" in the reference screenshots).
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
        // 5x7 dot-matrix font bitmap rows (5 bits wide, 7 rows tall)
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
        val step = dotDiameterPx + dotGapPx
        val charWidth = 5 * step + step
        val totalW = (text.length * charWidth).toInt() + paddingLeft + paddingRight
        val totalH = (7 * step).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(totalW, widthMeasureSpec),
            resolveSize(totalH, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val step = dotDiameterPx + dotGapPx
        val radius = dotDiameterPx / 2f
        var cursorX = paddingLeft.toFloat()
        val startY = paddingTop.toFloat()

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
