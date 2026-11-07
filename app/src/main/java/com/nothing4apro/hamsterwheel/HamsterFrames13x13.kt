package com.nothing4apro.hamsterwheel

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pixel-accurate 13×13 (137 mini-LEDs) frame generator for Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`).
 *
 * Supported Animations:
 * 1. SLEEPING: Hamster sleeps at the bottom of the wheel. Clean body shape (no stray pixels),
 *    single-pixel sleep particles spawn at row 8 and drift upward over 30 seconds.
 * 2. RUNNING: Fast 70ms 4-step gallop with clockwise rotating wheel spokes.
 * 3. WHEEL_LOOP_STOP: Unplug transition — hamster clings to wheel for 1 full clockwise 360° turn as wheel slows down.
 * 4. DANCING: Music reactive mode — hamster bobs, taps paws, throws arms up with floating musical notes ♪/♫.
 */
object HamsterFrames13x13 {

    const val MATRIX_SIZE = 13
    const val TOTAL_CELLS = MATRIX_SIZE * MATRIX_SIZE // 169

    val ROWS_13 = intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5)

    private const val WHEEL_BRIGHTNESS = 75
    private const val HAMSTER_BRIGHTNESS = 255

    const val SLEEP_CYCLE_LENGTH = 160 // 160 * 190 ms = 30.4 seconds loop
    const val WHEEL_LOOP_STEPS = 12

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

    // Clockwise rotating wheel spoke phases: 0 -> 1 -> 2 -> 3
    private val SPOKE_PHASES_CW: Array<List<Pair<Int, Int>>> = arrayOf(
        // Phase 0: 45° diagonal X-spokes
        listOf(
            Pair(2, 2), Pair(3, 3), Pair(4, 4), Pair(5, 5),
            Pair(2, 10), Pair(3, 9), Pair(4, 8), Pair(5, 7),
            Pair(6, 6),
            Pair(7, 5), Pair(8, 4), Pair(9, 3), Pair(10, 2),
            Pair(7, 7), Pair(8, 8), Pair(9, 9), Pair(10, 10)
        ),
        // Phase 1: Clockwise +22.5°
        listOf(
            Pair(1, 8), Pair(2, 8), Pair(3, 7), Pair(4, 7), Pair(5, 7),
            Pair(4, 1), Pair(4, 2), Pair(5, 3), Pair(5, 4), Pair(5, 5),
            Pair(6, 5), Pair(6, 6), Pair(6, 7),
            Pair(7, 6), Pair(8, 5), Pair(9, 5), Pair(10, 4), Pair(11, 4),
            Pair(7, 8), Pair(7, 9), Pair(8, 10), Pair(8, 11)
        ),
        // Phase 2: Clockwise +45° (90° '+' cross)
        listOf(
            Pair(1, 6), Pair(2, 6), Pair(3, 6), Pair(4, 6), Pair(5, 6),
            Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5),
            Pair(6, 6),
            Pair(6, 7), Pair(6, 8), Pair(6, 9), Pair(6, 10), Pair(6, 11),
            Pair(7, 6), Pair(8, 6), Pair(9, 6), Pair(10, 6), Pair(11, 6)
        ),
        // Phase 3: Clockwise +67.5°
        listOf(
            Pair(1, 4), Pair(2, 4), Pair(3, 5), Pair(4, 5), Pair(5, 5),
            Pair(4, 10), Pair(4, 11), Pair(5, 7), Pair(5, 8), Pair(5, 9),
            Pair(6, 6),
            Pair(7, 3), Pair(7, 4), Pair(7, 5), Pair(8, 1), Pair(8, 2),
            Pair(7, 7), Pair(8, 7), Pair(9, 8), Pair(10, 8), Pair(11, 8)
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
        for ((r, c) in SPOKE_PHASES_CW[phase]) {
            if (isValidCell(r, c)) {
                grid[r * MATRIX_SIZE + c] = WHEEL_BRIGHTNESS
            }
        }
        return grid
    }

    /**
     * Seamless 160-step sleeping animation.
     * Clean sleeping hamster body at rows 9..11 (stray pixel on (9,8) removed!).
     * Single-pixel sleep particles spawn at row 8 and drift upward over 8 steps.
     * Starts with empty sky if `sleepStepIndex == 0` (e.g. freshly stopped from wheel loop).
     */
    fun getSleepingFrame(stepIndex: Int): IntArray {
        val s = ((stepIndex % SLEEP_CYCLE_LENGTH) + SLEEP_CYCLE_LENGTH) % SLEEP_CYCLE_LENGTH
        val grid = buildWheelBase(spokePhase = 0)

        // Clean sleeping hamster body:
        // Row 9: cols 6..7 strictly (never lights up col 8!)
        // Row 10: cols 5..8
        // Row 11: cols 4..8
        grid[9 * MATRIX_SIZE + 6] = HAMSTER_BRIGHTNESS
        grid[9 * MATRIX_SIZE + 7] = HAMSTER_BRIGHTNESS
        for (c in 5..8) {
            grid[10 * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        for (c in 4..8) {
            grid[11 * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }

        // Particle emitter spawning every 5 steps
        for (k in 0 until 32) {
            val spawnStep = k * 5
            val age = (s - spawnStep + SLEEP_CYCLE_LENGTH) % SLEEP_CYCLE_LENGTH
            // Only show particles that spawned after step 0 when freshly sleeping
            if (stepIndex < 35 && spawnStep > stepIndex) continue
            if (age in 0..7) {
                val r = 8 - age // Rises 1 row per step from row 8 up to row 1
                val wave = sin(2.0 * PI * (spawnStep / 160.0) * 3.0 + age * 0.45)
                val c = (7.5 + 1.5 * wave).roundToInt().coerceIn(5, 9)
                if (isValidCell(r, c)) {
                    grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
                }
            }
        }

        return grid
    }

    /**
     * Running hamster frame matching Screenshots 3 & 4 with clockwise rotating spokes.
     */
    fun getRunningFrame(stepIndex: Int): IntArray {
        val phase = ((stepIndex % 4) + 4) % 4
        val grid = buildWheelBase(spokePhase = phase)

        val bodyPixels = listOf(
            Pair(8, 7),                                         // Ear
            Pair(9, 5), Pair(9, 6), Pair(9, 8),                 // Head ((9,7) is eye)
            Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(10, 7), Pair(10, 8) // Torso
        )
        for ((r, c) in bodyPixels) {
            grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        grid[9 * MATRIX_SIZE + 7] = 0 // Keep eye dark

        val legs = when (phase) {
            0 -> listOf(Pair(11, 3), Pair(11, 7))
            1 -> listOf(Pair(11, 4), Pair(11, 6)) // Stride 1
            2 -> listOf(Pair(11, 5), Pair(11, 7))
            else -> listOf(Pair(11, 4), Pair(11, 8)) // Stride 3
        }
        for ((r, c) in legs) {
            grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }

        return grid
    }

    /**
     * Full 360-degree CLOCKWISE revolution (`loopStep` 0..11) on the wheel when unplugged:
     * Bottom (6 o'clock) -> Left wall (9 o'clock) -> Top (12 o'clock) -> Right wall (3 o'clock) -> Bottom.
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

    /**
     * 4-phase Dancing Hamster animation with floating musical notes ♪ / ♫ when audio is playing.
     */
    fun getDancingFrame(stepIndex: Int): IntArray {
        val p = ((stepIndex % 4) + 4) % 4
        val grid = buildWheelBase(spokePhase = p)

        val body: List<Pair<Int, Int>>
        val eyes: List<Pair<Int, Int>>
        val notes: List<Pair<Int, Int>>

        when (p) {
            0 -> {
                // Lean Right & groove
                body = listOf(
                    Pair(7, 7), Pair(7, 8),
                    Pair(8, 6), Pair(8, 7), Pair(8, 8), Pair(8, 9),
                    Pair(9, 5), Pair(9, 6), Pair(9, 7), Pair(9, 8), Pair(9, 9),
                    Pair(10, 5), Pair(10, 6), Pair(10, 7), Pair(10, 8),
                    Pair(11, 4), Pair(11, 5), Pair(11, 8), Pair(11, 9)
                )
                eyes = listOf(Pair(8, 7))
                notes = listOf(Pair(3, 4), Pair(4, 4), Pair(5, 3), Pair(5, 4)) // Note ♪ left
            }
            1 -> {
                // Hands high in the air!
                body = listOf(
                    Pair(6, 4), Pair(6, 8),
                    Pair(7, 5), Pair(7, 7),
                    Pair(8, 5), Pair(8, 6), Pair(8, 7),
                    Pair(9, 5), Pair(9, 6), Pair(9, 7),
                    Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(10, 7), Pair(10, 8),
                    Pair(11, 5), Pair(11, 7)
                )
                eyes = listOf(Pair(8, 6))
                notes = listOf(Pair(2, 8), Pair(2, 9), Pair(3, 8), Pair(3, 9), Pair(4, 8), Pair(4, 10)) // Note ♫ right
            }
            2 -> {
                // Lean Left & groove
                body = listOf(
                    Pair(7, 4), Pair(7, 5),
                    Pair(8, 3), Pair(8, 4), Pair(8, 5), Pair(8, 6),
                    Pair(9, 3), Pair(9, 4), Pair(9, 5), Pair(9, 6), Pair(9, 7),
                    Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(10, 7),
                    Pair(11, 3), Pair(11, 4), Pair(11, 7), Pair(11, 8)
                )
                eyes = listOf(Pair(8, 5))
                notes = listOf(Pair(3, 8), Pair(4, 8), Pair(5, 7), Pair(5, 8)) // Note ♪ right
            }
            else -> {
                // Bob Down & stomp feet
                body = listOf(
                    Pair(7, 5), Pair(7, 7),
                    Pair(8, 5), Pair(8, 6), Pair(8, 7),
                    Pair(9, 4), Pair(9, 5), Pair(9, 6), Pair(9, 7), Pair(9, 8),
                    Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(10, 7), Pair(10, 8),
                    Pair(11, 4), Pair(11, 6), Pair(11, 8)
                )
                eyes = listOf(Pair(8, 6))
                notes = listOf(Pair(3, 4), Pair(3, 5), Pair(4, 4), Pair(4, 5)) // Note ♪ left
            }
        }

        for ((r, c) in body) {
            if (isValidCell(r, c)) grid[r * MATRIX_SIZE + c] = HAMSTER_BRIGHTNESS
        }
        for ((r, c) in eyes) {
            grid[r * MATRIX_SIZE + c] = 0
        }
        for ((r, c) in notes) {
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
}
