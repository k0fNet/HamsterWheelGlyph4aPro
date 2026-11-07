package com.nothing4apro.hamsterwheel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Core state machine and physics loop for the 13×13 Charging Hamster Glyph Toy.
 *
 * Priorities:
 * 1. CHARGING -> RUNNING (70 ms/step)
 * 2. UNPLUGGED FROM CHARGING -> WHEEL_LOOP_STOP (360° clockwise ride slowing down 80ms -> 250ms)
 * 3. MUSIC PLAYING -> DANCING (140 ms/step with musical notes ♪/♫ and head bobs)
 * 4. IDLE -> SLEEPING (190 ms/step with 30s seamless single-pixel sleep particles)
 */
object HamsterAnimatorEngine {

    private const val TAG = "HamsterAnimatorEngine"
    private const val PREFS_NAME = "hamster_glyph_prefs"
    private const val KEY_ENABLED = "glyph_enabled"
    private const val KEY_SCREEN_ON_PERSIST = "screen_on_persist"
    private const val KEY_ONLY_WHILE_CHARGING = "only_while_charging"
    private const val KEY_MUSIC_DANCE = "music_dance_enabled"

    enum class State {
        SLEEPING,
        RUNNING,
        WHEEL_LOOP_STOP,
        DANCING,
        DISABLED
    }

    interface FrameListener {
        fun onFrameUpdated(
            frame169: IntArray,
            state: State,
            frameDelayMs: Long,
            isCharging: Boolean,
            isMusicPlaying: Boolean,
            isEnabled: Boolean
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<FrameListener>()

    private var appContext: Context? = null
    private var glyphManager: GlyphMatrixManager? = null
    private var isSdkConnected = false
    private var isLoopRunning = false
    var isToyServiceBound: Boolean = false
        private set

    var isGlyphEnabled: Boolean = true
        private set

    var keepScreenOnActive: Boolean = true
        private set

    var onlyShowWhileCharging: Boolean = false
        private set

    var musicDanceEnabled: Boolean = true
        private set

    var currentState: State = State.SLEEPING
        private set

    var isChargingNow: Boolean = false
        private set

    var isMusicPlayingNow: Boolean = false
        private set

    var manualChargingOverride: Boolean? = null
        private set

    var manualMusicOverride: Boolean? = null
        private set

    private var runStepIndex = 0
    private var sleepStepIndex = 0
    private var danceStepIndex = 0
    private var wheelLoopStep = 0

    private const val SLEEP_STEP_DELAY_MS = 190L
    private const val RUN_STEP_DELAY_MS = 70L
    private const val DANCE_STEP_DELAY_MS = 140L

    private var currentDelayMs: Long = SLEEP_STEP_DELAY_MS
    private var lastFrame169: IntArray = HamsterFrames13x13.getSleepingFrame(0)

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isLoopRunning) return
            try {
                stepSimulation()
            } catch (t: Throwable) {
                Log.e(TAG, "Error in animation tick", t)
            } finally {
                if (isLoopRunning && isGlyphEnabled) {
                    handler.postDelayed(this, currentDelayMs)
                }
            }
        }
    }

    fun loadPrefs(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isGlyphEnabled = prefs.getBoolean(KEY_ENABLED, true)
        keepScreenOnActive = prefs.getBoolean(KEY_SCREEN_ON_PERSIST, true)
        onlyShowWhileCharging = prefs.getBoolean(KEY_ONLY_WHILE_CHARGING, false)
        musicDanceEnabled = prefs.getBoolean(KEY_MUSIC_DANCE, true)
    }

    fun setGlyphEnabled(context: Context, enabled: Boolean) {
        val ctx = context.applicationContext
        appContext = ctx
        isGlyphEnabled = enabled
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (!enabled) {
            currentState = State.DISABLED
            handler.removeCallbacks(tickRunnable)
            clearHardwareMatrix()
            HamsterForegroundService.stopService(ctx)
            notifyListeners()
        } else {
            recalculateState()
            HamsterForegroundService.startService(ctx)
            start(ctx)
        }
    }

    fun setKeepScreenOnActive(context: Context, active: Boolean) {
        val ctx = context.applicationContext
        keepScreenOnActive = active
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SCREEN_ON_PERSIST, active)
            .apply()
        if (!active && !isToyServiceBound) {
            clearAppMatrixOnly()
        } else if (active && isGlyphEnabled) {
            pushFrameToHardware(lastFrame169)
        }
    }

    fun setOnlyShowWhileCharging(context: Context, onlyCharging: Boolean) {
        val ctx = context.applicationContext
        onlyShowWhileCharging = onlyCharging
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONLY_WHILE_CHARGING, onlyCharging)
            .apply()
        pushFrameToHardware(lastFrame169)
    }

    fun setMusicDanceEnabled(context: Context, enabled: Boolean) {
        val ctx = context.applicationContext
        musicDanceEnabled = enabled
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MUSIC_DANCE, enabled)
            .apply()
        recalculateState()
    }

    fun start(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        loadPrefs(ctx)
        syncBatteryChargingState(ctx)
        checkMusicPlaybackState(ctx)

        if (!isGlyphEnabled) {
            currentState = State.DISABLED
            clearHardwareMatrix()
            notifyListeners()
            return
        }

        if (glyphManager == null) {
            initGlyphSdk(ctx)
        }

        if (!isLoopRunning) {
            isLoopRunning = true
            handler.removeCallbacks(tickRunnable)
            handler.post(tickRunnable)
        }
    }

    fun stop() {
        isLoopRunning = false
        handler.removeCallbacks(tickRunnable)
        clearHardwareMatrix()
        try {
            glyphManager?.unInit()
        } catch (t: Throwable) {
            Log.w(TAG, "Error uninitializing GlyphMatrixManager", t)
        }
        glyphManager = null
        isSdkConnected = false
    }

    fun onToyBindStateChanged(context: Context, bound: Boolean) {
        isToyServiceBound = bound
        if (!bound) {
            clearAppMatrixOnly()
        } else if (isGlyphEnabled) {
            start(context)
        }
    }

    fun addListener(listener: FrameListener) {
        listeners.add(listener)
        listener.onFrameUpdated(
            lastFrame169,
            currentState,
            currentDelayMs,
            isEffectiveCharging(),
            isEffectiveMusicPlaying(),
            isGlyphEnabled
        )
    }

    fun removeListener(listener: FrameListener) {
        listeners.remove(listener)
    }

    fun setManualOverride(overrideCharging: Boolean?) {
        manualChargingOverride = overrideCharging
        applyChargingTransition(isEffectiveCharging())
    }

    fun setManualMusicOverride(overrideMusic: Boolean?) {
        manualMusicOverride = overrideMusic
        recalculateState()
    }

    fun onPowerStateChanged(context: Context, charging: Boolean) {
        appContext = context.applicationContext
        isChargingNow = charging
        if (manualChargingOverride == null) {
            applyChargingTransition(charging)
        }
    }

    fun onScreenWake() {
        if (!isGlyphEnabled || !keepScreenOnActive) return
        handler.post { pushFrameToHardware(lastFrame169) }
        handler.postDelayed({ pushFrameToHardware(lastFrame169) }, 220L)
    }

    fun checkMusicPlaybackState(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isMusic = audioManager?.isMusicActive ?: false
        if (isMusic != isMusicPlayingNow) {
            isMusicPlayingNow = isMusic
            if (manualMusicOverride == null && currentState != State.RUNNING && currentState != State.WHEEL_LOOP_STOP) {
                recalculateState()
            }
        }
    }

    private fun isEffectiveCharging(): Boolean {
        return manualChargingOverride ?: isChargingNow
    }

    private fun isEffectiveMusicPlaying(): Boolean {
        return manualMusicOverride ?: isMusicPlayingNow
    }

    fun syncBatteryChargingState(context: Context) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
        isChargingNow = charging
        applyChargingTransition(isEffectiveCharging())
    }

    private fun applyChargingTransition(activeCharging: Boolean) {
        if (!isGlyphEnabled) {
            currentState = State.DISABLED
            return
        }
        if (activeCharging) {
            if (currentState != State.RUNNING) {
                currentState = State.RUNNING
                currentDelayMs = RUN_STEP_DELAY_MS
                if (isLoopRunning) {
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                }
            }
        } else {
            if (currentState == State.RUNNING) {
                // Unplugged! Hamster clings to wheel and spins 360° clockwise
                currentState = State.WHEEL_LOOP_STOP
                wheelLoopStep = 0
                currentDelayMs = 80L
                if (isLoopRunning) {
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                }
            } else {
                recalculateState()
            }
        }
    }

    private fun recalculateState() {
        if (!isGlyphEnabled) {
            currentState = State.DISABLED
            return
        }
        val charging = isEffectiveCharging()
        val music = isEffectiveMusicPlaying() && musicDanceEnabled

        when {
            charging -> {
                currentState = State.RUNNING
                currentDelayMs = RUN_STEP_DELAY_MS
            }
            music -> {
                currentState = State.DANCING
                currentDelayMs = DANCE_STEP_DELAY_MS
            }
            else -> {
                currentState = State.SLEEPING
                currentDelayMs = SLEEP_STEP_DELAY_MS
            }
        }
        if (isLoopRunning) {
            handler.removeCallbacks(tickRunnable)
            handler.post(tickRunnable)
        }
    }

    private fun stepSimulation() {
        if (!isGlyphEnabled) {
            currentState = State.DISABLED
            clearHardwareMatrix()
            notifyListeners()
            return
        }

        appContext?.let { checkMusicPlaybackState(it) }

        val activeCharging = isEffectiveCharging()
        val activeMusic = isEffectiveMusicPlaying() && musicDanceEnabled

        // Enforce Priority 1: Charging always beats music
        if (activeCharging && currentState != State.RUNNING) {
            currentState = State.RUNNING
            currentDelayMs = RUN_STEP_DELAY_MS
        } else if (!activeCharging && currentState == State.RUNNING) {
            currentState = State.WHEEL_LOOP_STOP
            wheelLoopStep = 0
            currentDelayMs = 80L
        }

        when (currentState) {
            State.RUNNING -> {
                currentDelayMs = RUN_STEP_DELAY_MS
                runStepIndex = (runStepIndex + 1) and 3
                lastFrame169 = HamsterFrames13x13.getRunningFrame(runStepIndex)
            }

            State.WHEEL_LOOP_STOP -> {
                lastFrame169 = HamsterFrames13x13.getWheelLoopFrame(wheelLoopStep)
                wheelLoopStep++
                currentDelayMs = 80L + (wheelLoopStep * 14L)
                if (wheelLoopStep >= HamsterFrames13x13.WHEEL_LOOP_STEPS) {
                    // Wheel spin completed! Transition to music or sleep
                    if (activeMusic) {
                        currentState = State.DANCING
                        danceStepIndex = 0
                        currentDelayMs = DANCE_STEP_DELAY_MS
                    } else {
                        currentState = State.SLEEPING
                        // Crucial fix: Reset sleep step index so particles spawn fresh above hamster without premature particles in sky!
                        sleepStepIndex = 0
                        currentDelayMs = SLEEP_STEP_DELAY_MS
                    }
                }
            }

            State.DANCING -> {
                if (!activeMusic) {
                    currentState = State.SLEEPING
                    sleepStepIndex = 0
                    currentDelayMs = SLEEP_STEP_DELAY_MS
                    lastFrame169 = HamsterFrames13x13.getSleepingFrame(sleepStepIndex)
                } else {
                    currentDelayMs = DANCE_STEP_DELAY_MS
                    danceStepIndex = (danceStepIndex + 1) and 3
                    lastFrame169 = HamsterFrames13x13.getDancingFrame(danceStepIndex)
                }
            }

            State.SLEEPING -> {
                if (activeMusic) {
                    currentState = State.DANCING
                    danceStepIndex = 0
                    currentDelayMs = DANCE_STEP_DELAY_MS
                    lastFrame169 = HamsterFrames13x13.getDancingFrame(danceStepIndex)
                } else {
                    currentDelayMs = SLEEP_STEP_DELAY_MS
                    sleepStepIndex = (sleepStepIndex + 1) % HamsterFrames13x13.SLEEP_CYCLE_LENGTH
                    lastFrame169 = HamsterFrames13x13.getSleepingFrame(sleepStepIndex)
                }
            }

            State.DISABLED -> {
                lastFrame169 = IntArray(HamsterFrames13x13.TOTAL_CELLS)
            }
        }

        pushFrameToHardware(lastFrame169)
        notifyListeners()
    }

    private fun notifyListeners() {
        val charging = isEffectiveCharging()
        val music = isEffectiveMusicPlaying()
        for (listener in listeners) {
            listener.onFrameUpdated(lastFrame169, currentState, currentDelayMs, charging, music, isGlyphEnabled)
        }
    }

    private fun initGlyphSdk(context: Context) {
        try {
            val gm = GlyphMatrixManager.getInstance(context)
            glyphManager = gm
            gm.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    try {
                        gm.register(Glyph.DEVICE_25111p)
                        isSdkConnected = true
                        Log.i(TAG, "Registered DEVICE_25111p (Phone 4a Pro 13x13)")
                        if (isGlyphEnabled) {
                            pushFrameToHardware(lastFrame169)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to register DEVICE_25111p", t)
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    isSdkConnected = false
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "GlyphMatrix SDK init failed", t)
        }
    }

    fun clearAppMatrixOnly() {
        try {
            glyphManager?.closeAppMatrix()
        } catch (_: Throwable) {}
    }

    fun clearHardwareMatrix() {
        val ctx = appContext
        val gm = glyphManager
        if (ctx != null && gm != null && isSdkConnected) {
            try {
                val blankBitmap = HamsterFrames13x13.toBitmap(IntArray(HamsterFrames13x13.TOTAL_CELLS))
                val obj = GlyphMatrixObject.Builder()
                    .setImageSource(blankBitmap)
                    .setBrightness(0)
                    .build()
                val emptyFrame = GlyphMatrixFrame.Builder().addTop(obj).build(ctx).render()
                gm.setMatrixFrame(emptyFrame)
            } catch (_: Throwable) {}
            try {
                gm.closeAppMatrix()
            } catch (_: Throwable) {}
        }
    }

    private fun pushFrameToHardware(frame169: IntArray) {
        val ctx = appContext ?: return
        val gm = glyphManager ?: return
        if (!isSdkConnected || !isGlyphEnabled) return

        val displayArray = if (onlyShowWhileCharging && currentState == State.SLEEPING) {
            IntArray(HamsterFrames13x13.TOTAL_CELLS)
        } else {
            frame169
        }

        try {
            val bitmap = HamsterFrames13x13.toBitmap(displayArray)
            val matrixObject = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setScale(100)
                .setOrientation(0)
                .setBrightness(255)
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(matrixObject)
                .build(ctx)

            val rendered = frame.render()

            if (isToyServiceBound) {
                gm.setMatrixFrame(rendered)
            }
            if (keepScreenOnActive) {
                gm.setAppMatrixFrame(rendered)
            } else {
                gm.closeAppMatrix()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not push frame to GlyphMatrixManager", t)
        }
    }
}
