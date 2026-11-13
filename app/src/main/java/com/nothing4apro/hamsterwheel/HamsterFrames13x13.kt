package com.nothing4apro.hamsterwheel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 13×13 (137 mini-LEDs) frame generator for Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`).
 */
object HamsterFrames13x13 {

    const val MATRIX_SIZE = 13
    const val TOTAL_CELLS = MATRIX_SIZE * MATRIX_SIZE // 169

    val ROWS_13 = intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5)

    private const val WHEEL_BRIGHTNESS = 75
    private const val HAMSTER_BRIGHTNESS = 255

    const val SLEEP_CYCLE_LENGTH = 160 // 160 * 190 ms = 30.4s loop
    const val WHEEL_LOOP_STEPS = 12
    const val DANCE_CYCLE_LENGTH = 16

    private val RIM_COORDS: List<Pair<Int, Int>> = listOf(
        Pair(0, 4), Pair(0, 5), Pair(0, 6), Pair(0, 7), Pair(0, 8),
        Pair(1, 2), Pair(1, 3), Pair(1, 9), Pair(1, 10),
        Pair(2, 1), Pair(2, 11),
        Pair(3, 1), Pair(3, 11),
        Pair(4, 0), Pair(4, 12),
        Pair(5, 0), Pair(5, 12),
        Pair(6, 0), Pair(6, 12),
        Pair(7, 0), Pair(7, 12),
        Pair(8, 0), Pair(8, 12),
        Pair(9, 1), Pair(9, 11),
        Pair(10, 1), Pair(10, 11),
        Pair(11, 2), Pair(11, 3), Pair(11, 9), Pair(11, 10),
        Pair(12, 4), Pair(12, 5), Pair(12, 6), Pair(12, 7), Pair(12, 8)
    )

    private val RIM_SET: Set<Pair<Int, Int>> = RIM_COORDS.toSet()

    /**
     * Exact 4 wheel spoke states ordered so that as the hamster runs forward (facing right),
     * its paws push the bottom spokes from RIGHT to LEFT (`col 10 -> col 8 -> col 6 -> col 4 -> col 2`),
     * while the top spokes move from LEFT to RIGHT (`col 2 -> col 4 -> col 6 -> col 8 -> col 10`).
     */
    private val SPOKE_PHASES_FORWARD: Array<List<Pair<Int, Int>>> = arrayOf(
        // Phase 0: 45° diagonal X-spokes (bottom spokes at col 2 and col 10)
        listOf(
            Pair(2, 2), Pair(3, 3), Pair(4, 4), Pair(5, 5),
            Pair(2, 10), Pair(3, 9), Pair(4, 8), Pair(5, 7),
            Pair(6, 6),
            Pair(7, 5), Pair(8, 4), Pair(9, 3), Pair(10, 2),
            Pair(7, 7), Pair(8, 8), Pair(9, 9), Pair(10, 10)
        ),
        // Phase 1: Bottom spoke pushed left to col 8, top spoke moved right to col 4 (Screenshot 3)
        listOf(
            Pair(1, 4), Pair(2, 4), Pair(3, 5), Pair(4, 5), Pair(5, 5),
            Pair(4, 10), Pair(4, 11), Pair(5, 7), Pair(5, 8), Pair(5, 9),
            Pair(6, 6),
            Pair(7, 3), Pair(7, 4), Pair(7, 5), Pair(8, 1), Pair(8, 2),
            Pair(7, 7), Pair(8, 7), Pair(9, 8), Pair(10, 8), Pair(11, 8)
        ),
        // Phase 2: Bottom spoke pushed left to col 6, top spoke moved right to col 6 ('+' cross)
        listOf(
            Pair(1, 6), Pair(2, 6), Pair(3, 6), Pair(4, 6), Pair(5, 6),
            Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5),
            Pair(6, 6),
            Pair(6, 7), Pair(6, 8), Pair(6, 9), Pair(6, 10), Pair(6, 11),
            Pair(7, 6), Pair(8, 6), Pair(9, 6), Pair(10, 6), Pair(11, 6)
        ),
        // Phase 3: Bottom spoke pushed left to col 4, top spoke moved right to col 8 (Screenshot 4)
        listOf(
            Pair(1, 8), Pair(2, 8), Pair(3, 7), Pair(4, 7), Pair(5, 7),
            Pair(4, 1), Pair(4, 2), Pair(5, 3), Pair(5, 4), Pair(5, 5),
            Pair(6, 5), Pair(6, 6), Pair(6, 7),
            Pair(7, 6), Pair(8, 5), Pair(9, 5), Pair(10, 4), Pair(11, 4),
            Pair(7, 8), Pair(7, 9), Pair(8, 10), Pair(8, 11)
        )
    )

    private data class DanceStep(
        val body: List<Pair<Int, Int>>,
        val eye: Pair<Int, Int>,
        val notes: List<Pair<Int, Int>>
    )

    // 16-frame smooth chubby hamster dance on a stationary wheel
    private val DANCE_STEPS_16: Array<DanceStep> = arrayOf(
        DanceStep(
            body = listOf(Pair(8,6), Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,8)),
            eye = Pair(9,7),
            notes = listOf(Pair(6,9), Pair(6,10), Pair(7,9))
        ),
        DanceStep(
            body = listOf(Pair(8,7), Pair(9,4),Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8),Pair(10,9), Pair(11,4),Pair(11,7)),
            eye = Pair(9,7),
            notes = listOf(Pair(5,9), Pair(5,10), Pair(6,9))
        ),
        DanceStep(
            body = listOf(Pair(8,7), Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,5),Pair(11,8)),
            eye = Pair(9,7),
            notes = listOf(Pair(4,9), Pair(4,10), Pair(5,9))
        ),
        DanceStep(
            body = listOf(Pair(9,5),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,5),Pair(11,7),Pair(11,8)),
            eye = Pair(9,6),
            notes = listOf(Pair(3,9), Pair(3,10), Pair(4,9))
        ),
        DanceStep(
            body = listOf(Pair(8,6), Pair(9,4),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,8)),
            eye = Pair(9,5),
            notes = listOf(Pair(2,9), Pair(6,3), Pair(6,4), Pair(7,4))
        ),
        DanceStep(
            body = listOf(Pair(8,5), Pair(9,4),Pair(9,6),Pair(9,7),Pair(9,8), Pair(10,3),Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,5),Pair(11,8)),
            eye = Pair(9,5),
            notes = listOf(Pair(5,3), Pair(5,4), Pair(6,4))
        ),
        DanceStep(
            body = listOf(Pair(8,5), Pair(9,4),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,7)),
            eye = Pair(9,5),
            notes = listOf(Pair(4,3), Pair(4,4), Pair(5,4))
        ),
        DanceStep(
            body = listOf(Pair(9,5),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,5),Pair(11,7),Pair(11,8)),
            eye = Pair(9,6),
            notes = listOf(Pair(3,3), Pair(3,4), Pair(4,4))
        ),
        DanceStep(
            body = listOf(Pair(8,6), Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,8)),
            eye = Pair(9,7),
            notes = listOf(Pair(2,3), Pair(6,8), Pair(5,9))
        ),
        DanceStep(
            body = listOf(Pair(8,7), Pair(9,4),Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8),Pair(10,9), Pair(11,5),Pair(11,7)),
            eye = Pair(9,7),
            notes = listOf(Pair(5,8), Pair(4,9))
        ),
        DanceStep(
            body = listOf(Pair(8,7), Pair(9,5),Pair(9,6),Pair(9,8), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,3),Pair(11,8)),
            eye = Pair(9,7),
            notes = listOf(Pair(4,8), Pair(3,9))
        ),
        DanceStep(
            body = listOf(Pair(9,5),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,6),Pair(11,8)),
            eye = Pair(9,6),
            notes = listOf(Pair(3,8), Pair(2,9))
        ),
        DanceStep(
            body = listOf(Pair(8,6), Pair(9,4),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,8)),
            eye = Pair(9,5),
            notes = listOf(Pair(2,8), Pair(6,4), Pair(5,3))
        ),
        DanceStep(
            body = listOf(Pair(8,5), Pair(9,4),Pair(9,6),Pair(9,7),Pair(9,8), Pair(10,3),Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,5),Pair(11,7)),
            eye = Pair(9,5),
            notes = listOf(Pair(5,4), Pair(4,3))
        ),
        DanceStep(
            body = listOf(Pair(8,5), Pair(9,4),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,9)),
            eye = Pair(9,5),
            notes = listOf(Pair(4,4), Pair(3,3))
        ),
        DanceStep(
            body = listOf(Pair(9,5),Pair(9,6),Pair(9,7), Pair(10,4),Pair(10,5),Pair(10,6),Pair(10,7),Pair(10,8), Pair(11,4),Pair(11,6),Pair(11,8)),
            eye = Pair(9,6),
            notes = listOf(Pair(3,4), Pair(2,3))
        )
    )

    fun isValidCell(r: Int, c: Int): Boolean {
        if (r !in 0 until MATRIX_SIZE || c !in 0 until MATRIX_SIZE) return false
        val ledsInRow = ROWS_13[r]
        val colStart = (MATRIX_SIZE - ledsInRow) / 2
        return c in colStart until (colStart + ledsInRow)
    }

    private fun buildWheelBase(spokePhase: Int): IntArray {
        val grid = IntArray(TOTAL_CELLS)
        for ((r, c) in RIM_COORDS) {
            grid[r * MATRIX_SIZE + c] = WHEEL_BRIGHTNESS
        }
        val phase = ((spokePhase % 4) + 4) % 4
        for ((r, c) in SPOKE_PHASES_FORWARD[phase]) {
            if (isValidCell(r, c)) {
                grid[r * MATRIX_SIZE + c] = WHEEL_BRIGHTNESS
            }
        }
        return grid
    }

    fun getSleepingFrame(stepIndex: Int): IntArray {
        val s = ((stepIndex % SLEEP_CYCLE_LENGTH) + SLEEP_CYCLE_LENGTH) % SLEEP_CYCLE_LENGTH
        val grid = buildWheelBase(spokePhase = 0)

        grid[9 * MATRIX_SIZE + 6] = HAMSTER_BRIGHTNESS
        grid[9 * MATRIX_SIZE + 7] = HAMSTER_BRIGHTNESS
        for (c in 5..8) {
            grid[10 * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        for (c in 4..8) {
            grid[11 * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }

        for (k in 0 until 32) {
            val spawnStep = k * 5
            val age = (s - spawnStep + SLEEP_CYCLE_LENGTH) % SLEEP_CYCLE_LENGTH
            if (stepIndex < 35 && spawnStep > stepIndex) continue
            if (age in 0..7) {
                val r = 8 - age
                val wave = sin(2.0 * PI * (spawnStep / 160.0) * 3.0 + age * 0.45)
                val c = (7.5 + 1.5 * wave).roundToInt().coerceIn(5, 9)
                if (isValidCell(r, c)) {
                    grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
                }
            }
        }

        return grid
    }

    fun getRunningFrame(stepIndex: Int): IntArray {
        val phase = ((stepIndex % 4) + 4) % 4
        val grid = buildWheelBase(spokePhase = phase)

        val bodyPixels = listOf(
            Pair(8, 7),
            Pair(9, 5), Pair(9, 6), Pair(9, 8),
            Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(10, 7), Pair(10, 8)
        )
        for ((r, c) in bodyPixels) {
            grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        grid[9 * MATRIX_SIZE + 7] = 0

        val legs = when (phase) {
            0 -> listOf(Pair(11, 3), Pair(11, 7))
            1 -> listOf(Pair(11, 4), Pair(11, 8)) // Screenshot 3 stride matches Phase 1 spoke!
            2 -> listOf(Pair(11, 5), Pair(11, 7))
            else -> listOf(Pair(11, 4), Pair(11, 6)) // Screenshot 4 stride matches Phase 3 spoke!
        }
        for ((r, c) in legs) {
            grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }

        return grid
    }

    /**
     * Full 360-degree CLOCKWISE revolution (`loopStep` 0..11) on the wheel when unplugged:
     * Since the bottom of the wheel moves from right to left (clockwise), the hamster is carried
     * from Bottom (6 o'clock) -> Left wall (9 o'clock) -> Top (12 o'clock) -> Right wall (3 o'clock) -> Bottom,
     * while the wheel spokes also rotate in the exact same forward clockwise direction (`step % 4`).
     */
    fun getWheelLoopFrame(loopStep: Int): IntArray {
        val step = ((loopStep % WHEEL_LOOP_STEPS) + WHEEL_LOOP_STEPS) % WHEEL_LOOP_STEPS
        val grid = buildWheelBase(spokePhase = step % 4)

        val angleRad = Math.toRadians(90.0 + step * (360.0 / WHEEL_LOOP_STEPS))
        val cx = 6.0 + 3.5 * cos(angleRad)
        val cy = 6.0 + 3.5 * sin(angleRad)

        for (r in 0 until MATRIX_SIZE) {
            for (c in 0 until MATRIX_SIZE) {
                if (!isValidCell(r, c)) continue
                if (RIM_SET.contains(Pair(r, c))) continue
                if (hypot(r - cy, c - cx) <= 1.65) {
                    grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
                }
            }
        }
        return grid
    }

    fun getDancingFrame(stepIndex: Int): IntArray {
        val idx = ((stepIndex % DANCE_CYCLE_LENGTH) + DANCE_CYCLE_LENGTH) % DANCE_CYCLE_LENGTH
        val grid = buildWheelBase(spokePhase = 0)
        val step = DANCE_STEPS_16[idx]

        for ((r, c) in step.body) {
            if (isValidCell(r, c)) grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        grid[step.eye.first * MATRIX_SIZE + step.eye.second] = 0
        for ((r, c) in step.notes) {
            if (isValidCell(r, c)) grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }

        return grid
    }

    fun toBitmap(frame169: IntArray): Bitmap {
        val pixels = IntArray(TOTAL_CELLS)
        for (i in 0 until TOTAL_CELLS) {
            val r = i / MATRIX_SIZE
            val c = i % MATRIX_SIZE
            val b = if (isValidCell(r, c)) frame169[i].coerceIn(0, 255) else 0
            pixels[i] = if (b > 0) Color.argb(255, b, b, b) else Color.TRANSPARENT
        }
        return Bitmap.createBitmap(pixels, MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
    }

    fun toWidgetBitmap(frame169: IntArray, sizePx: Int = 280): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0A0D")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#242429")
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.015f
        }
        canvas.drawCircle(cx, cy, sizePx * 0.48f, bgPaint)
        canvas.drawCircle(cx, cy, sizePx * 0.48f, borderPaint)

        val gridSpan = sizePx * 0.82f
        val cellStep = gridSpan / MATRIX_SIZE
        val gap = cellStep * 0.12f
        val startX = cx - gridSpan / 2f
        val startY = cy - gridSpan / 2f
        val rect = RectF()
        val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (r in 0 until MATRIX_SIZE) {
            for (c in 0 until MATRIX_SIZE) {
                if (!isValidCell(r, c)) continue
                val left = startX + c * cellStep + gap / 2f
                val top = startY + r * cellStep + gap / 2f
                rect.set(left, top, left + cellStep - gap, top + cellStep - gap)

                val b = frame169[r * MATRIX_SIZE + c].coerceIn(0, 255)
                ledPaint.color = when {
                    b >= 180 -> Color.WHITE
                    b > 0 -> Color.parseColor("#42424C")
                    else -> Color.parseColor("#141419")
                }
                canvas.drawRoundRect(rect, 3f, 3f, ledPaint)
            }
        }
        return bmp
    }
}
