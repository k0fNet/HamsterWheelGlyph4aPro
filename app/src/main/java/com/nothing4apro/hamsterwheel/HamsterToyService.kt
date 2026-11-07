package com.nothing4apro.hamsterwheel

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphToy

/**
 * Official Nothing OS Glyph Toy Service for Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`).
 */
class HamsterToyService : Service() {

    companion object {
        private const val TAG = "HamsterToyService"
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val bundle = msg.data
                val event = bundle?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                when (event) {
                    GlyphToy.EVENT_AOD -> {
                        HamsterAnimatorEngine.loadPrefs(applicationContext)
                        if (HamsterAnimatorEngine.isGlyphEnabled) {
                            HamsterAnimatorEngine.start(applicationContext)
                        }
                    }
                    GlyphToy.EVENT_CHANGE -> {
                        val currentOverride = HamsterAnimatorEngine.manualChargingOverride
                        val next = if (currentOverride == null) {
                            !HamsterAnimatorEngine.isChargingNow
                        } else {
                            !currentOverride
                        }
                        HamsterAnimatorEngine.setManualOverride(next)
                    }
                }
            } else {
                super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "HamsterToyService bound by Nothing OS")
        HamsterAnimatorEngine.loadPrefs(applicationContext)
        HamsterAnimatorEngine.onToyBindStateChanged(applicationContext, true)
        if (HamsterAnimatorEngine.isGlyphEnabled && HamsterAnimatorEngine.keepScreenOnActive) {
            HamsterForegroundService.startService(applicationContext)
        }
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "HamsterToyService unbound by Nothing OS")
        HamsterAnimatorEngine.onToyBindStateChanged(applicationContext, false)
        return false
    }
}
